
# Stubby: Barebones Stubbing Library

Stubby is an extremely non-intrusive library for generating stub implementations
that throw exceptions at runtime. The idea is to allow you to use the standard
syntax to implement the part of an interface that you need for some specific
test and have Stubby fill in the rest of the interface with stubs.

# Minimal Example

Stubby provides a single macro annotation, `@stubby`, which can be added to
classes, traits, and objects to have Stubby fill in stub implementations for
their abstract members. Here's a minimal example from the REPL:
```scala
scala> import org.scala_stubby.stubby
import org.scala_stubby.stubby

scala> @stubby object Test { val test: Int }
defined object Test

scala> Test.test
scala.NotImplementedError: test is an unimplemented stub
  at Test$.test(<console>:12)
  ... 42 elided
```

# More Full-Featured Example

Here's an example that more fully shows the intended use-case. It assumes that
you have a modern version of ScalaTest on the classpath.
```scala
import org.scalatest.{FunSuite, Matchers}
import org.scala_stubby.stubby

trait Store[T] {
  def read(key: String): Option[T]
  def write(key: String, t: T): Unit
}
case class User(username: String, hashedPassword: Array[Byte])
object HashPassword {
  def apply(password: String): Array[Byte] = {
    // Please do better in your app
    import java.security.MessageDigest
    val digestMaker = MessageDigest.getInstance("md5")
    digestMaker.update(password.getBytes)
    digestMaker.digest
  }
}
class Authenticator(store: Store[User]) {
  def authenticate(username: String, password: String): Boolean = {
    store.read(username) match {
      case None => false
      case Some(user) =>
        HashPassword(password).deep == user.hashedPassword.deep
    }
  }
}
class AuthenticatorTest extends FunSuite with Matchers {
  test("finds password") {
    @stubby object TestStore extends Store[User] {
      def read(key: String): Option[User] = key match {
        case "tester" => Some(User("tester", HashPassword("test")))
        case other => None
      }
      // Note that write is not implemented
    }
    val authenticator = new Authenticator(TestStore)
    authenticator.authenticate("tester", "test") shouldBe true
  }
}
```

# Limitations
## Designed-In Limitations
Stubby is a relatively limited tool by design and will always be so. If you need
a tool that helps you do things like override final methods, record calls, or
validate expectations, you should use a mocking library like ScalaMock or
Mockito+PowerMock. For my part, I hate the user experience of these libraries
and avoid them whenever possible (but I acknowledge that occasionally you do
need the more-powerful features that they offer).
## Locally-Defined Parent Types
Stubby suffers from an unfortunate limitation due to the fact that it's built
around a macro annotation: due to the way that macro annotations are
implemented, it isn't possible for Stubby to see the interfaces of base types
that are defined in the same local scope as an `@stubby`-annotated object/type.
If you ever annotate an object/type that extends a type from its local scope
with `@stubby`, you'll get a compile-time error. For example:
```scala
scala> import org.scala_stubby.stubby
import org.scala_stubby.stubby

scala> def foo = {
     |   class C
     |   @stubby class D extends C
     |   new D
     | }
<console>:14: error: Stubby cannot see the signature for C. Perhaps it's defined as a local class/trait.
         @stubby class D extends C
                                 ^
```
