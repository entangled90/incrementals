package incrementals.core

import scala.collection.mutable.ArrayBuffer

class Incremental private (private val nodes: ArrayBuffer[Node[?]] = ArrayBuffer.empty):
  val updatedInputs: ArrayBuffer[Node[?]] = ArrayBuffer.empty

  def register(n: Node[?]) =
    nodes.addOne(n)

  def observe(): Unit =
    if updatedInputs.nonEmpty then
      val epoch = updatedInputs.maxBy(_.epoch).epoch
      // println(s"Observing epoch $epoch")
      for n <- updatedInputs do n.observe(epoch)
      updatedInputs.clear()

  def freeze() = nodes.sortInPlaceBy(_.height)

  override def toString(): String =
    (for
      i <- nodes.indices
    yield
      val n = nodes(i)
      val lineEnd =
        if i > 0 then
          val prev = nodes(i - 1)
          if prev.height < n.height then "\n" else ""
        else ""

      s"${lineEnd} $n "
    ).mkString

end Incremental

object Incremental:
  extension [A, B](t: (Node[A], Node[B]))
    inline def map2[Z](f: (A, B) => Z)(using Incremental): Node[Z] =
      new Map2(f, t._1, t._2)

  extension [A, B, C](t: (Node[A], Node[B], Node[C]))
    inline def map3[Z](f: (A, B, C) => Z)(using Incremental): Node[Z] =
      new Map3(f, t._1, t._2, t._3)

  def input[A](a: A)(using b: Incremental, eq: CanEqual[A, A]): InputNode[A] =
    val node = new InputNode[A](a)
    node

  def apply[A](init: Incremental ?=> A): (Incremental, A) =
    given b: Incremental = new Incremental
    val a = init
    b.freeze()
    (b, a)
