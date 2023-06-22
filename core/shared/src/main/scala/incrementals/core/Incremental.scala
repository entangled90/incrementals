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

  var updatedInputs: ArrayBuffer[Node.Input[?]] = _

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
    inline def map[Z](f: A => Z)(using Incremental): Node[Z] = new Node.Computed[Z](IArray(t)):
      override def computeValue(): Z = f(t.value)

  extension[A, B] (t: (Node[A], Node[B]))
    inline def map2[Z](f: (A, B) => Z)(using Incremental): Node[Z] =
      new Node.Computed[Z](IArray(t._1, t._2)):
        override def computeValue(): Z = f(t._1.value, t._2.value)

  extension[A, B, C] (t: (Node[A], Node[B], Node[C]))
    inline def map3[Z](f: (A, B, C) => Z)(using Incremental): Node[Z] =
      new Node.Computed[Z](IArray(t._1, t._2, t._3)):
        override def computeValue(): Z = f(t._1.value, t._2.value, t._3.value)

  extension[A, B, C, D] (t: (Node[A], Node[B], Node[C], Node[D]))
    inline def map3[Z](f: (A, B, C, D) => Z)(using Incremental): Node[Z] =
      new Node.Computed[Z](IArray(t._1, t._2, t._3, t._4)):
        override def computeValue(): Z = f(t._1.value, t._2.value, t._3.value, t._4.value)

  inline def input[A](a: A)(using b: Incremental, eq: CanEqual[A, A]): Node.Input[A] = new Node.Input[A](a)


  def apply[A](init: Incremental ?=> A): (Incremental, A) =
    given inc: Incremental = new Incremental(Array.empty)

    val a = init
    inc.build()
    (inc, a)


trait Observer[A]:
  def observe(a: A): Unit

sealed class Node[A](protected val inputs: IArray[Node[?]])(using inc: Incremental):

  private var observers: Set[Observer[A]] = Set.empty

  // index is the height of the node in graph
  // input nodes have height = -1
  private var subscribedNodes: Array[MutSet[Node[?]]] = Array.empty

  val height: Int = if inputs.isEmpty then Node.inputHeight else inputs.view.map(_.height).max + 1

  inc.register(this)

  protected var v: A = _

  inline final def value: A = v

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


  inline def necessary: Boolean = observers.nonEmpty || subscribedNodes.nonEmpty

  override def toString: String =
    s"${getClass.getSimpleName}(value=$value, height=$height, necessary=$necessary)"


  private def subscribeToInputs(): Unit =
    if necessary then
      inputs.foreach { inputNode =>
        inputNode.subscribedNodes.lift(height) match
          case None => inputNode.subscribedNodes :+= MutSet(this)
          case Some(value) => value.add(this)

        inputNode.subscribeToInputs()
      }

  private def unsubscribeFromInputs(): Unit =
    if !necessary then
      inputs.foreach { inputNode =>
        inputNode.subscribedNodes.lift(height).foreach { value =>
          value.remove(this)
          if value.isEmpty then
            assert(height == inputNode.subscribedNodes.length)
            inputNode.subscribedNodes = inputNode.subscribedNodes.dropRight(1)
        }
        inputNode.unsubscribeFromInputs()
      }

object Node:
  inline def inputHeight: Int = -1

  class Input[A](_v: A)(using
                        inc: Incremental,
                        eq: CanEqual[A, A]
  ) extends Node[A](IArray.empty):

    v = _v

    def set(a: A): Any =
      if a != _v then
        v = a
        if necessary then
          inc.updatedInputs.addOne(this)

  sealed abstract class Computed[A](inputs: IArray[Node[?]])(using inc: Incremental
  ) extends Node[A](inputs):

    v = computeValue()

    def computeValue(): A

    override def stabilize(): Unit =
      if necessary then
        v = computeValue()
        super.stabilize()


  end Computed

//  trait IndexedNode[Idx, A]:
//    def get(idx: Idx): Option[Node[A]]
//
//
//  end IndexedNode
//
//  class MapNode[Idx, A](using Incremental) extends IndexedNode[Idx, A]:
//    self: Node[Map[Idx, A]] =>
//
//    override def get(idx: Idx): Option[Node[A]] = value.get(idx).map(a => new Node[A](IArray(this)))

end Node


