/*  Copyright 2016 The Stubby Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
*/

package org.scala_stubby

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
    import c.internal.decorators._
    implicit class FlagOps(f: FlagSet) {
      def -(other: FlagSet): FlagSet = {
       (f.asInstanceOf[Long] & ~other.asInstanceOf[Long]).asInstanceOf[FlagSet]
      }
    }
    implicit class LongOps(l: Long) {
      def toFlagSet: FlagSet = l.asInstanceOf[FlagSet]
    }
    def stub(enclosing: Type, member: TermSymbol) = {
      val name = member.name
      val tpe = member.infoIn(enclosing)
      val resultType = tpe.finalResultType
      val memberType = member.infoIn(enclosing)
      val params = memberType.paramLists.map { list =>
        list.map { param =>
          val name = param.name.toTermName
          val tpe = param.typeSignatureIn(memberType)
          val mods = Modifiers(param.flags)
          val tree = q"$mods val $name: $tpe = ${EmptyTree}"
          internal.setSymbol(tree, param)
          tree
        }
      }
      val typeParams = member.infoIn(enclosing).typeParams.map { param =>
        val name = param.name.toTypeName
        val mods = Modifiers(param.flags)
        val tree = q"$mods type $name = ${EmptyTree}"
        internal.setSymbol(tree, param)
        tree
      }
      val mods = {
        val DEFERRED = reflect.internal.Flags.DEFERRED.toLong.asInstanceOf[FlagSet]
        if (member.privateWithin == NoSymbol) {
          Modifiers(
            member.flags - DEFERRED
          )
        } else {
          Modifiers(
            member.flags - DEFERRED,
            member.privateWithin.name
          )
        }
      }
      val nameString = name.decodedName.toString
      val errorMessage = Literal(Constant(s"$nameString is an unimplemented stub"))
      q"$mods def $name[..$typeParams](...${params}): $resultType = throw new _root_.scala.NotImplementedError($errorMessage)"
    }

    def stubAbstractMembers(t: Tree): List[Tree] = {
      val tpe = c.typecheck(t, c.TERMmode, silent = true).tpe
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

    def getValidTypes(trees: List[Tree]): (List[Tree], List[Tree]) =
      trees.partition {
        case q"$typeTree(...$args)" =>
          c.typecheck(
            tree = q"null: ${typeTree.duplicate}", c.TERMmode,
            silent = true
          ).tpe != NoType
        case typeTree =>
          c.typecheck(
            tree = q"null: ${typeTree.duplicate}", c.TERMmode,
            silent = true
          ).tpe != NoType
      }

    def warnInvalidParent(parentTree: Tree): Unit = parentTree match {
      case q"$typeTree(...$args)" =>
        c.warning(
          typeTree.pos,
          s"Stubby cannot see ${showCode(typeTree)}. Perhaps it's defined as a local class/trait."
        )
      case typeTree =>
        c.warning(
          typeTree.pos,
          s"Stubby cannot see ${showCode(typeTree)}. Perhaps it's defined as a local class/trait."
        )
    }

    val (head :: tail) = annottees.toList
    val transformed = head match {
      case q"""
        $mods class $name extends ..$parents {
          ..$members
        }
      """ =>
        val (validParents, invalidParents) = getValidTypes(parents)
        invalidParents.foreach(warnInvalidParent)
        val (abstractMembers, others) = pullOutAbstracts(members)
        val stubs = {
          stubAbstractMembers(
            q"{class Dummy extends ..$validParents { ..$members }; null: Dummy}".duplicate
          )
        }
        q"""
          $mods class $name extends ..$parents {
            ..$stubs
            ..$others
          }
      """
      case q"""
        $mods trait $name extends ..$parents {
          ..$members
        }
      """ =>
        val (validParents, invalidParents) = getValidTypes(parents)
        invalidParents.foreach(warnInvalidParent)
        val (abstractMembers, others) = pullOutAbstracts(members)
        val stubs = {
          stubAbstractMembers(
            q"{class Dummy extends ..$validParents { ..$members }; null: Dummy}".duplicate
          )
        }
        q"""
          $mods trait $name extends ..$parents {
            ..$stubs
            ..$others
          }
      """
      case q"""
        $mods object $name extends ..$parents {
          ..$members
        }
      """ =>
        val (validParents, invalidParents) = getValidTypes(parents)
        invalidParents.foreach(warnInvalidParent)
        val (abstractMembers, others) = pullOutAbstracts(members)
        val stubs = {
          stubAbstractMembers(
            q"{class Dummy extends ..$validParents { ..$members }; null: Dummy}".duplicate
          )
        }
        q"""
          $mods object $name extends ..$parents {
            ..$stubs
            ..$others
          }
        """
    }
    q"""
      $transformed
      ..$tail
    """
  }
}

