package incrementals

import incrementals.core.*

import Incremental.*

object Main:

  private def discriminant(a: Double, b: Double, c: Double): Double = Math.sqrt(b * b - 4 * a * c)

  @main def run(): Unit =
    val (inc, (a, b, c, root)) = Incremental:
      val a = "a" @: input(2.0)
      val b = "b" @: input(9.2)
      val c = "c" @: input(4.3)
      val root = "root" @: (a, b, c).map3((a, b, c) => (-b + discriminant(a,b,c)) / 2 * a)

      (a, b, c, root)

    root.addObserver(r => ())

    val max = 1000000
    for i <- 1 to max do
      a.set(1.0 + i / 100.0)
      val before = System.nanoTime()
      inc.stabilize()

