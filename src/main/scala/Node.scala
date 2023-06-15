import scala.collection.mutable.ArrayBuffer

trait Observer[A]:
  def observe(a: A): Unit

sealed abstract class Node[A](
    protected val inputs: IArray[Node[?]],
    var name: String = ""
)(using inc: Incremental)
    extends Observer[Long]:

  var epoch: Long = 0L

  def height: Int

  var observers: Set[Observer[A]] = Set.empty

  var internalObservers: Set[Observer[Long]] = Set.empty

  inc.register(this)

  def value: A

  def observe(epoch: Long): Unit =
    println(s"Observing epoch $epoch in $this:}")
    for (obs <- internalObservers) do obs.observe(epoch)
    for (obs <- observers) do obs.observe(value)

  def addObserver(observer: Observer[A]): Unit = {
    observers += observer
  }

  def removeObserver(observer: Observer[A]): Unit =
    observers -= observer

  def necessary = observers.nonEmpty || internalObservers.nonEmpty

  override def toString(): String =
    s"[$name]@${getClass.getSimpleName()}(value=$value, height=$height, necessary=${necessary}, epoch=${epoch})"

  def @:(name: String): this.type =
    this.name = name
    this

final class InputNode[A](private var _v: A)(using
    inc: Incremental,
    eq: CanEqual[A, A]
) extends Node[A](IArray.empty):

  def height = 0
  def value = _v

  // FIXME THIS!
  def set(a: A) =
    if (a != _v) then
      epoch += 1
      _v = a
      inc.updatedInputs.addOne(this)

sealed abstract class ComputationNode[A](inputs: IArray[Node[?]])(using
    inc: Incremental
) extends Node[A](inputs: IArray[Node[?]]),
      Observer[Long]:

  val height = inputs.view.map(_.height).maxOption.getOrElse(0) + 1

  var value: A = computeValue()
  epoch = inputs.map(_.epoch).max

  def computeValue(): A

  override def observe(newEpoch: Long): Unit =
    println(s"Observing $newEpoch in $this")
    if newEpoch > epoch then
      value = computeValue()
      epoch = newEpoch
      super.observe(newEpoch)
    else println(s"Skipping $newEpoch $this")

  def subscribeToInputs(): Unit =
    if necessary then
      inputs.foreach { inputNode =>
        inputNode.internalObservers += (this)
        inputNode match
          case _: InputNode[?] =>
            ()
          case c: ComputationNode[?] =>
            c.subscribeToInputs()
      }

  def unsubscribeFromInputs(): Unit = {
    if !necessary then
      inputs.foreach { inputNode =>
        inputNode.internalObservers -= (this)
        inputNode match
          case _: InputNode[?] =>
            ()
          case c: ComputationNode[?] =>
            c.unsubscribeFromInputs()
      }
  }

  override def addObserver(observer: Observer[A]): Unit = {
    val wasNecessary = necessary
    super.addObserver(observer)
    if !wasNecessary then subscribeToInputs()
  }

  override def removeObserver(observer: Observer[A]): Unit =
    super.removeObserver(observer)
    if !necessary then unsubscribeFromInputs()

final class Map[A, B](f: A => B, a: Node[A])(using inc: Incremental)
    extends ComputationNode[B](IArray(a)) {
  def computeValue(): B = f(a.value)
}

final class Map2[A, B, Z](f: (A, B) => Z, a: Node[A], b: Node[B])(using
    inc: Incremental
) extends ComputationNode[Z](IArray(a, b)) {

  def computeValue(): Z = f(a.value, b.value)
}

final class Map3[A, B, C, Z](f: (A, B, C) => Z, a: Node[A], b: Node[B], c: Node[C])(using
    inc: Incremental
) extends ComputationNode[Z](IArray(a, b, c)) {
  def computeValue(): Z = f(a.value, b.value, c.value)
}
