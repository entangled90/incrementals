import cats.implicits._

import MemoT.*

@main def main: Unit =
  val (x, y, z, result) = Graph.example

  println(s"${result.result}")

  println("x = 3")
  x.set(3)
  println(s"${result.result}")

// for i <- 1 to 10 do s"i = $i: ${run(Memo.program.memo)(1)}"
