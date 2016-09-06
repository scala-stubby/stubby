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

import org.scalatest.FunSuite

abstract class BaseClass {
  def test(a: Int, b: Int): String = test
  val test: String
  def foo(s: String): String = s
}

trait AccessTestBase {
  private[scala_stubby] def foo: String
  protected val bar: Int
}

@stubby class AccessTest extends AccessTestBase

class stubbyTests extends FunSuite {

  test("works on classes") {
    @stubby class Test
  }

  test("works on traits") {
    @stubby trait Test
  }

  test("works on objects") {
    @stubby object Test
  }

  test("stubs vals") {
    @stubby class Test {
      val test: Int
    }
    intercept[NotImplementedError] {
      (new Test).test
    }
  }

  test("stubs defs") {
    @stubby class Test {
      def test(a: Int, b: Int): String
    }
    intercept[NotImplementedError] {
      (new Test).test(1, 2)
    }
  }

  test("stubs vars") {
    @stubby class Test {
      var test: Int
    }
    intercept[NotImplementedError] {
      (new Test).test
    }
    intercept[NotImplementedError] {
      (new Test).test = 4
    }
  }


  test("stubs vals and defs together") {
    @stubby class Test {
      def test(a: Int, b: Int): String
      val test: Int
    }
    intercept[NotImplementedError] {
      (new Test).test
    }
    intercept[NotImplementedError] {
      (new Test).test(1, 2)
    }
  }

  test("stubs and concrete implementations can coexist") {
    @stubby class Test {
      def test(a: Int, b: Int): String
      val test: Int = 4
    }
    (new Test).test
    intercept[NotImplementedError] {
      (new Test).test(1, 2)
    }
  }

  test("concrete methods can refer to stubs") {
    @stubby class Test {
      def test(a: Int, b: Int): String = test
      val test: String
    }
    intercept[NotImplementedError] {
      (new Test).test
    }
    intercept[NotImplementedError] {
      (new Test).test(1, 2)
    }
  }

  test("stubs inherited members") {

    @stubby object Test extends BaseClass {
      def bar(s: String): String = s
    }

    intercept[NotImplementedError] {
      Test.test
    }
    intercept[NotImplementedError] {
      Test.test(1, 2)
    }
    assert(Test.foo("3") === "3")
    assert(Test.bar("4") === "4")
  }

  test("retains access modifiers") {
    import reflect.runtime.universe._
    val tpe = typeOf[AccessTest]
    assert(tpe.member(TermName("foo")).privateWithin.name.toString === "scala_stubby")
    assert(tpe.member(TermName("bar")).isProtected)
  }

  test("allows implementing abstract members from the parent") {
    @stubby class Test extends BaseClass {
      val test = "3"
    }
    (new Test).test(1,2)
  }

}
