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
    }
    val authenticator = new Authenticator(TestStore)
    authenticator.authenticate("tester", "test") shouldBe true
  }
}
