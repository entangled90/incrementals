import Incremental.*

object Main {
  @main def run =
    val (inc, (a, b, c, root)) = Incremental {
      val a = "a" @: input(2.0)
      val b = "b" @: input(9.2)
      val c = "c" @: input(4.3)
      val disc = "disc" @: (a, b, c).map3((a, b, c) => Math.sqrt(b * b - 4 * a * c))
      val root = "root" @: (a, b, disc).map3((a, b, disc) => (-b + disc) / 2 * a)

      (a, b, c, root)
    }

    root.addObserver(r => println(s"Root is $r"))

    println(inc)
    a.set(1.0)
    println("Observe now")
    inc.observe()
    println(inc)

}
