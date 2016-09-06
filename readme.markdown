
# Stubby: Barebones Stubbing Library

Stubby is an extremely non-intrusive framework for generating stubs (method
implemetations that throw exceptions when called). The idea is to allow you to
use the standard syntax to implement the part of an interface that you need for
some specific test and have Stubby fill in the rest of the interface with stubs.

# Minimal Example

Stubby provides a single macro annotation, `@stubby`, which can be added to
classes, traits, and objects to have Stubby fill in stub implementations for
their abstract members. Here's a minimal example from the REPL:
```scala
scala> import net.clhodapp.stubby
import net.clhodapp.stubby

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
import net.clhodapp.stubby

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
    }
    val authenticator = new Authenticator(TestStore)
    authenticator.authenticate("tester", "test") shouldBe true
  }
}
```
