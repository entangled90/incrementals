package incrementals

import incrementals.core.*

import Incremental.*

object Main {
  @main def run = {
    val (inc, (a, b, c, root)) = Incremental {
      val a = "a" @: input(2.0)
      val b = "b" @: input(9.2)
      val c = "c" @: input(4.3)
      val disc = "disc" @: (a, b, c).map3((a, b, c) => Math.sqrt(b * b - 4 * a * c))
      val root = "root" @: (a, b, disc).map3((a, b, disc) => (-b + disc) / 2 * a)

      (a, b, c, root)
    }

    root.addObserver(r => ())

    val max = 1000000
    for i <- 1 to max do {
      a.set(1.0 + i / 100.0)
      val before = System.nanoTime()
      inc.observe()
      if i == max then println(s"Took ${System.nanoTime() - before}nanos")
    }
  }

}
