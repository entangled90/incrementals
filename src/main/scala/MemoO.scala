import cats._
import cats.syntax.all.*

trait MemoO[A, B]:
  def run(a: A): B

  def widen[C](focus: C => A): MemoO[C, B] =
    MemoO[C, B](c => run(focus(c)))

object MemoO:
  def apply[A, B](f: A => B): MemoO[A, B] =
    new MemoO[A, B]:
      var previous: Option[(A, B)] = None
      def run(a: A): B =
        previous match {
          case Some((oldA, b)) if a == oldA =>
            b
          case _ =>
            val b = f(a)
            println(s"computed $f: $a -> $b")
            previous = Some((a, b))
            b
        }

  given Instance[A, B]: Applicative[[B] =>> MemoO[A, B]] with

    type M = [C] =>> MemoO[A, C]

    override final def map[B, C](fa: M[B])(f: B => C): M[C] =
      apply[A, C](x => f(fa.run(x)))

    override def ap[B, C](ff: M[B => C])(
        fa: M[B]
    ): M[C] =
      new MemoO[A, C]:
        override def run(a: A): C =
          val b = fa.run(a)
          val bToC = ff.run(a)
          val c = bToC(b)

          c

    def pure[B](b: B): MemoO[A, B] =
      new MemoO[A, B]:
        def run(a: A): B = b
