import Graph.*
import cats._
import cats.syntax.all.*
import cats.derived.*

trait Graph {
  def addInput[I](i: I): Input[I]

}

enum Diff[I] derives Eq, Show:
  def curr: I
  case Updated(prev: Option[I], curr: I)

  case Unchanged(curr: I)

object Diff:

  given DiffProduct: Applicative[Diff] with

    override def ap[A, B](ff: Diff[A => B])(fa: Diff[A]): Diff[B] =
      (fa, ff) match {
        case (Unchanged(a), Unchanged(fab)) =>
          Unchanged(fab(a))
        case (Updated(prev, curr), Unchanged(fab)) =>
          Updated(prev.map(fab), fab(curr))
        case (Unchanged(a), Updated(prev, curr)) =>
          Updated(prev.map(_(a)), curr(a))
        case (Updated(prevA, currA), Updated(prevB, currB)) =>
          val previous = (prevA, prevB).tupled.map((a, f) => f(a))
          Updated(previous, currB(currA))
      }

    def pure[A](x: A): Diff[A] = Unchanged(x)

object Graph:

  def apply: Graph =
    new Graph:
      var vars: Vector[Input[?]] = Vector.empty
      var idx = 0

      def addInput[I](initial: I) = ???
      // val input =
      //   new Input[I]:
      //     val id = idx
      //     var value: I = initial
      //     var listeners: Seq[Incr[I]] = Seq.empty

      //     def set(i: I) =
      //       value = i

      //     def get = value

      //     def watch: Incr[I] =
      //       val inc = new Incr[I]:
      //         def result = value
      //       listeners :+= inc
      //       inc

      // vars :+= input
      // idx += 1
      // input

  trait Input[I]:
    def set(i: I): Unit
    def get: I

    def watch: Incr[I]

  trait Incr[I]:
    def result: Diff[I]

  val example = {
    val graph = Graph.apply

    val x = graph.addInput[Int](10)
    val y = graph.addInput[Int](20)
    val z = graph.addInput[Int](30)

    val area = new Incr[Int]:
      def result = ??? // x.get + y.get

    val volume = new Incr[Int]:
      def result = ??? /// area.result * z.get

    (x, y, z, volume)
  }
