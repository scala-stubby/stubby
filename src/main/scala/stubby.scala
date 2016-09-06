
package net.clhodapp

import scala.reflect.macros.whitebox.Context
import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly

@compileTimeOnly("enable macro paradise to expand macro annotations")
class stubby extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro stubby.impl
}
object stubby {
  @compileTimeOnly("stubby.impl is internal and shouldn't be invoked")
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._
    def stub(enclosing: Type, member: TermSymbol) = {
      val name = member.name
      val tpe = member.infoIn(enclosing)
      val resultType = tpe.finalResultType
      val memberType = member.infoIn(enclosing)
      val params = memberType.paramLists.map { list =>
        list.map { param =>
          val name = param.name.toTermName
          val tpe = param.typeSignatureIn(memberType)
          val mods = Modifiers(Flag.PARAM)
          val tree = q"$mods val $name: $tpe = ${EmptyTree}"
          internal.setSymbol(tree, param)
          tree
        }
      }
      val typeParams = member.infoIn(enclosing).typeParams.map { param =>
        val name = param.name.toTypeName
        val mods = Modifiers(Flag.PARAM)
        val tree = q"$mods type $name = ${EmptyTree}"
        internal.setSymbol(tree, param)
        tree
      }
      val mods = {
        val visibility = {
          if (member.isPrivate) Flag.PRIVATE
          else if (member.isProtected) Flag.PROTECTED
          else NoFlags
        }
        val stability = {
          if (member.isStable) Flag.STABLE
          else NoFlags
        }
        if (member.privateWithin == NoSymbol) {
          Modifiers(visibility|stability)
        } else {
          Modifiers(
            visibility|stability,
            member.privateWithin.name
          )
        }
      }
      val nameString = name.decodedName.toString
      val errorMessage = Literal(Constant(s"$nameString is an unimplemented stub"))
      q"$mods def $name[..$typeParams](...${params}): $resultType = throw new _root_.scala.NotImplementedError($errorMessage)"
    }

    def stubAbstractMembers(t: Tree): List[Tree] = {
      val tpe = c.typecheck(t, c.TERMmode).tpe
      val abstractMembers =
        tpe.members
          .filter(_.isAbstract)
          .filter(_.isTerm)
          .map(_.asTerm)
      abstractMembers.map(stub(tpe, _)).toList
    }

    def pullOutAbstracts(
      ts: List[Tree]
    ): (List[Tree], List[Tree]) = ts.partition {
      case v: ValOrDefDef => v.rhs == EmptyTree
      case _ => false
    }

    val (head :: tail) = annottees.toList
    val transformed = head match {
      case q"""
        $mods class $name extends ..$parents {
          ..$members
        }
      """ =>
        val (abstractMembers, others) = pullOutAbstracts(members)
        val stubs = {
          stubAbstractMembers(
            q"{class Dummy extends ..$parents { ..$members }; null: Dummy}".duplicate
          )
        }
        List(
          q"""
            $mods class $name extends ..$parents {
              ..$stubs
              ..$others
            }
        """
        )
      case q"""
        $mods trait $name extends ..$parents {
          ..$members
        }
      """ =>
        val (abstractMembers, others) = pullOutAbstracts(members)
        val stubs = {
          stubAbstractMembers(
            q"{class Dummy extends ..$parents { ..$members }; null: Dummy}".duplicate
          )
        }
        List(
          q"""
            $mods trait $name extends ..$parents {
              ..$stubs
              ..$others
            }
        """
        )
      case q"""
        $mods object $name extends ..$parents {
          ..$members
        }
      """ =>
        val (abstractMembers, others) = pullOutAbstracts(members)
        val stubs = {
          stubAbstractMembers(
            q"{class Dummy extends ..$parents { ..$members }; null: Dummy}".duplicate
          )
        }
        List(
          q"""
            $mods object $name extends ..$parents {
              ..$stubs
              ..$others
            }
        """
        )
    }
    q"""
      ..$transformed
      ..$tail
    """
  }
}

