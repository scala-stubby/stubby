
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
