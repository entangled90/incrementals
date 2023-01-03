package incrementals

import java.util.concurrent.atomic.AtomicLong
import cats.{Eq, Order}
import Stabilization.*
import incrementals.Node.Id.inc

final class Node[@specialized A](
    id: Node.Id,
    var kind: Node.Kind[A],
    var value: Option[A],
    var recomputedAt: Tick,
    var changedAt: Tick
)(using s: State) {
  def state: State = s
}

object Node:

  def apply[A](kind: Kind[A])(using s: State) =
    val id = s.nodesCreated.inc
    s.nodesCreated = id

    new Node(id, kind, None, Tick.None, Tick.None)

  opaque type Id = Long

  object Id:

    given Eq[Id] with
      def eqv(a: Id, b: Id) = a == b

    given Order[Id] with
      def compare(a: Id, b: Id) = a.compare(b)

    def create(): Id = 0L

    extension (id: Id) def inc: Id = id + 1

  enum Kind[A]:
    // Needed for circular dependency... ugly!
    case Uninitialized[A]() extends Kind[A]
    case Const(value: A)
    case Var(v: incrementals.Var[A])
    case Map[A, B](f: A => B, node: Node[A]) extends Kind[B]
