package incrementals.core

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import incrementals.core.Incremental.input

trait Observer[@specialized A]:
  def observe(a: A): Unit

sealed abstract class Node[@specialized A](
    protected val inputs: Array[Node[?]],
    var name: String = ""
)(implicit inc: Incremental)
    extends Observer[Long]:

  var epoch: Long = 0L

  var observers: Set[Observer[A]] = Set.empty

  // index is the height of the node in graph
  var internalObservers: Array[mutable.Set[ComputationNode[?]]] = Array.empty

  inc.register(this)

  def height: Int

  def value: A

  def observe(epoch: Long): Unit =
    // println(s"Observing epoch $epoch in $this:}")
    for
      observers <- internalObservers
      obs <- observers
    do obs.observe(epoch)
    for obs <- observers do obs.observe(value)

  def addObserver(observer: Observer[A]): Unit =
    observers += observer

  def removeObserver(observer: Observer[A]): Unit =
    observers -= observer

  def necessary = observers.nonEmpty || internalObservers.nonEmpty

  override def toString(): String =
    s"[$name]@${getClass.getSimpleName()}(value=$value, height=$height, necessary=${necessary}, epoch=${epoch})"

  def @:(name: String): this.type =
    this.name = name
    this

final class InputNode[@specialized A](private var _v: A)(implicit
    inc: Incremental,
    eq: CanEqual[A, A]
) extends Node[A](Array.empty):

  def height = -1
  def value = _v

  def set(a: A) =
    if (a != _v) then
      epoch += 1
      _v = a
      inc.updatedInputs.addOne(this)

sealed abstract class ComputationNode[@specialized A](inputs: Array[Node[?]])(implicit
    inc: Incremental
) extends Node[A](inputs: Array[Node[?]])
    with Observer[Long]:

  val height = inputs.view.map(_.height).maxOption.getOrElse(-1) + 1

  var value: A = computeValue()

  epoch = inputs.map(_.epoch).max

  def computeValue(): A

  override def observe(newEpoch: Long): Unit =
    // println(s"Observing $newEpoch in $this")
    if newEpoch > epoch then
      value = computeValue()
      epoch = newEpoch
      super.observe(newEpoch)
    // else println(s"Skipping $newEpoch $this")

  def subscribeToInputs(): Unit =
    if necessary then
      inputs.foreach { inputNode =>
        inputNode.internalObservers.lift(height) match
          case None        => inputNode.internalObservers :+= mutable.Set(this)
          case Some(value) => value.add(this)

        inputNode match
          case _: InputNode[?] =>
            ()
          case c: ComputationNode[?] =>
            c.subscribeToInputs()
      }

  def unsubscribeFromInputs(): Unit =
    if !necessary then
      inputs.foreach { inputNode =>
        inputNode.internalObservers.lift(height) match
          case None =>
            ()
          case Some(value) =>
            value.remove(this)
            if value.isEmpty then
              assert(height == inputNode.internalObservers.length)
              inputNode.internalObservers = inputNode.internalObservers.dropRight(1)

        inputNode match
          case _: InputNode[?] =>
            ()
          case c: ComputationNode[?] =>
            c.unsubscribeFromInputs()
      }

  override def addObserver(observer: Observer[A]): Unit =
    val wasNecessary = necessary
    super.addObserver(observer)
    if !wasNecessary then subscribeToInputs()

  override def removeObserver(observer: Observer[A]): Unit =
    super.removeObserver(observer)
    if !necessary then unsubscribeFromInputs()

final class Map[A, B](f: A => B, a: Node[A])(implicit inc: Incremental)
    extends ComputationNode[B](Array(a)):
  def computeValue(): B = f(a.value)

final class Map2[A, B, Z](
    f: (A, B) => Z,
    a: Node[A],
    b: Node[B]
)(implicit
    inc: Incremental
) extends ComputationNode[Z](Array(a, b)):
  def computeValue(): Z = f(a.value, b.value)

final class Map3[A, B, C, Z](
    f: (A, B, C) => Z,
    a: Node[A],
    b: Node[B],
    c: Node[C]
)(implicit
    inc: Incremental
) extends ComputationNode[Z](Array(a, b, c)):
  def computeValue(): Z = f(a.value, b.value, c.value)
