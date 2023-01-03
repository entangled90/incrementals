package incrementals

import java.util.concurrent.atomic.AtomicLong
import cats.{Eq, Order}

object Stabilization:

  opaque type Tick = Long

  object Tick:
    extension (t: Tick) def inc: Tick = t + 1

    given Eq[Tick] with
      def eqv(a: Tick, b: Tick) = a == b

    given Order[Tick] with
      def compare(a: Tick, b: Tick) = a.compare(b)

    def None: Tick = -1
    def create: Tick = 0L

    def unapply(t: Tick): Boolean = t == None
