package incrementals

import incrementals.State.Status
import incrementals.Stabilization.*
import scala.collection.immutable.ArraySeq
import Node.Kind

final class State private (
    var status: Status,
    var tick: Stabilization.Tick,
    var observers: ArraySeq[Observer],
    var nodesCreated: Node.Id
) {
  def createVar[A](a: A): Var[A] =
    // ugly but ok
    val watch = Node[A](null.asInstanceOf[Kind[A]])(using this)
    val createdVar: Var[A] = new Var(watch, a, a, None, Tick.None)
    watch.kind = Kind.Var(createdVar)
    createdVar

  def inc() =
    tick = tick.inc
    this

}

object State:
  def apply: State =
    // see state.ml#1919
    new State(
      Status.NotStabilizing,
      Tick.create,
      ArraySeq.empty,
      Node.Id.create()
    )

  enum Status:
    case Stabilizing
    case RunningOnUpdateHandlers
    case NotStabilizing
    case StabilizePreviouslyRaised(ex: Exception)
