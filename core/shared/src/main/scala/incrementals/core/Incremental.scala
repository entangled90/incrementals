package incrementals.core

import scala.collection.mutable.Set as MutSet
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import Incremental.*

import scala.collection.mutable

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Set as MutSet
import incrementals.core.Incremental.input


/**
 *
 * @param nodes  array sorted by height (topological sort) of the nods of the graph
 * @param locked once locked, the array is sorted and is not mutated anymore
 */
class Incremental private(
                           var nodes: Array[Node[?]],
                           var locked: Boolean = false):

  var updatedInputs: ArrayBuffer[InputNode[?]] = _

  def register(n: Node[?]): Unit = nodes :+= n

  def build(): Unit =
    nodes.sortInPlaceBy(_.height)
    updatedInputs = new ArrayBuffer(nodes.count(_.height == Node.inputHeight))
    locked = true

  def stabilize(): Unit =
    if updatedInputs.nonEmpty then
      for
        node <- nodes
      do
        node.stabilize()
      updatedInputs.clear()

end Incremental

object Incremental:


  extension[A] (t: Node[A])
    inline def map[Z](f: A => Z)(using Incremental): Node[Z] = new Map(f, t)

  extension[A, B] (t: (Node[A], Node[B]))
    inline def map2[Z](f: (A, B) => Z)(using Incremental): Node[Z] =
      val (a, b) = (t._1, t._2)
      new ComputationNode[Z](IArray(a, b)):
        override def computeValue(): Z = f(a.value, b.value)

  extension[A, B, C] (t: (Node[A], Node[B], Node[C]))
    inline def map3[Z](f: (A, B, C) => Z)(using Incremental): Node[Z] =
      val (a, b, c) = (t._1, t._2, t._3)
      new ComputationNode[Z](IArray(a, b, c)):
        override def computeValue(): Z = f(a.value, b.value, c.value)

  extension[A, B, C, D] (t: (Node[A], Node[B], Node[C], Node[D]))
    inline def map3[Z](f: (A, B, C, D) => Z)(using Incremental): Node[Z] =
      val (a, b, c, d) = (t._1, t._2, t._3, t._4)
      new ComputationNode[Z](IArray(a, b, c)):
        override def computeValue(): Z = f(a.value, b.value, c.value, d.value)

  def input[A](a: A)(using b: Incremental, eq: CanEqual[A, A]): InputNode[A] =
    val node = new InputNode[A](a)
    node

  def apply[A](init: Incremental ?=> A): (Incremental, A) =
    given inc: Incremental = new Incremental(Array.empty)

    val a = init
    inc.build()
    (inc, a)


trait Observer[A]:
  def observe(a: A): Unit

sealed abstract class Node[A](protected val inputs: IArray[Node[?]])(using inc: Incremental):

  var observers: Set[Observer[A]] = Set.empty

  // index is the height of the node in graph
  // input nodes have height = -1
  var internalObservers: Array[MutSet[Node[?]]] = Array.empty

  val height: Int = if inputs.isEmpty then Node.inputHeight else inputs.view.map(_.height).max + 1

  inc.register(this)

  def value: A

  /** Stabilize nodes up to height == upToHeight */
  def stabilize(): Unit =
    for obs <- observers do obs.observe(value)

  def addObserver(observer: Observer[A]): Unit =
    val wasNecessary = necessary
    observers += observer
    if !wasNecessary then subscribeToInputs()
    observer.observe(value)

  def removeObserver(observer: Observer[A]): Unit =
    observers -= observer
    if !necessary then unsubscribeFromInputs()


  def necessary: Boolean = observers.nonEmpty || internalObservers.nonEmpty

  override def toString: String =
    s"${getClass.getSimpleName}(value=$value, height=$height, necessary=$necessary)"


  private def subscribeToInputs(): Unit =
    if necessary then
      inputs.foreach { inputNode =>
        inputNode.internalObservers.lift(height) match
          case None => inputNode.internalObservers :+= MutSet(this)
          case Some(value) => value.add(this)

        inputNode.subscribeToInputs()
      }


  private def unsubscribeFromInputs(): Unit =
    if !necessary then
      inputs.foreach { inputNode =>
        inputNode.internalObservers.lift(height).foreach { value =>
          value.remove(this)
          if value.isEmpty then
            assert(height == inputNode.internalObservers.length)
            inputNode.internalObservers = inputNode.internalObservers.dropRight(1)
        }
        inputNode.unsubscribeFromInputs()
      }

//trait IndexedNode[Idx, A](using Incremental):
//  self : Node[Idx => Option[Node[A]]] =>
//
//end IndexedNode


object Node:
  inline def inputHeight: Int = -1
end Node

final class InputNode[A](private var _v: A)(using
                                            inc: Incremental,
                                            eq: CanEqual[A, A]
) extends Node[A](IArray.empty):
  inline def value: A = _v

  def set(a: A): Any =
    if a != _v then
      _v = a
      if necessary then
        inc.updatedInputs.addOne(this)

sealed abstract class ComputationNode[A](inputs: IArray[Node[?]])(using inc: Incremental
) extends Node[A](inputs):


  var value: A = computeValue()

  def computeValue(): A

  override def stabilize(): Unit =
    if  necessary then
      value = computeValue()
      super.stabilize()


end ComputationNode

final class Map[A, B](f: A => B, a: Node[A])(using Incremental)
  extends ComputationNode[B](IArray(a)):
  def computeValue(): B = f(a.value)

final class Map2[A, B, Z](
                           f: (A, B) => Z,
                           a: Node[A],
                           b: Node[B]
                         )(using Incremental)
  extends ComputationNode[Z](IArray(a, b)):
  def computeValue(): Z = f(a.value, b.value)

