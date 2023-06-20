package incrementals.core

import Incremental.* 
import munit.*


class IncrementalsSpec extends munit.FunSuite:

  private def basicIncremental = Incremental:
    val a = "a" @: input(10)
    val b = "b" @: input(20)
    val c = "ab" @: (a, b).map2(_ + _)
    (a, b, c)

  test("Initial Value is correct"):
    val (inc, (a, b, c)) = basicIncremental
    assertEquals(c.value, 30)

  test("Value does not change if there is no observer"):
    val (inc, (a, b, c)) = basicIncremental
    a.set(20)
    assertEquals(c.value, 30)
    inc.stabilize()
    assertEquals(c.value, 30)

  test("Value change if there is an observer after stabilize"):
    val (inc, (a, b, c)) = basicIncremental
    c.addObserver(_ => ())
    a.set(20)
    assertEquals(c.value, 30)
    inc.stabilize()
    assertEquals(c.value, 40)


  test("Computation with many layers produces correct output"):
    val (inc, (a,b,c,d,e,f)) = Incremental:
      val a = "a" @: input(10)
      val b = "b" @: input(10)
      val c = "c" @: input(10)
      val d = "ab" @: (a,b).map2(_ + _)
      val e = "bc" @: (b,c).map2(_ + _)
      val f = "abbc" @: (d,e).map2(_ + _)
      (a,b,c,d,e,f)

    f.addObserver(_ => ())

    assertEquals(f.value, 40)

    b.set(20)
    inc.stabilize()
    assertEquals(f.value, 60)


