import cats._
import cats.syntax.all._

/** * Each node of the computation is a function A => B when running the graph
  * we get a new "computation" where each node has a reference of the previous
  * value. e.g. x => x + 1
  *
  * let's run it with input = 1: 1 => 1 + 1 ~ state monad with S = itself
  */

trait MemoT[A, +B] { self =>
  def run(a: A): (MemoT[A, B], B)

  def name: Option[String] = None
  def previous: Option[(A, B)] = None

  def widen[C](focus: C => A): MemoT[C, B] =
    MemoT(c => self.run(focus(c))._2, self.name.map(_ + "(widened)"))

  override def toString = s"Memo(name=$name, previous=$previous)"
}

object MemoT:

  def apply[A, B](
      f: A => B,
      named: Option[String] = None,
      prevInput: Option[(A, B)] = None
  ): MemoT[A, B] =
    val wrapped = (a: A) => {
      val b = f(a)
      println(s"Computed f $named : $a => $b")
      b
    }

    new MemoT[A, B]:
      def run(a: A): (MemoT[A, B], B) =
        val previous @ (_, b) = prevInput match
          case Some(t @ (`a`, b)) =>
            t

          case memo =>
            println(s"Prev value is $memo, recomputing $named")
            val b: B = wrapped(a)
            (a, b)

        (apply(wrapped, named, Some(previous)), b)

      override def name = named

  type MemoF = [A] =>> [B] =>> MemoT[A, B]
  type MemoCon = [A] =>> [B] =>> MemoT[B, A]

  implicit def ContraV[A]: Contravariant[MemoCon[A]] =
    new Contravariant[MemoCon[A]] {
      def contramap[B, C](fa: MemoT[B, A])(f: C => B): MemoT[C, A] =
        apply[C, A](c => fa.run(f(c))._2)
    }

  given Instance[A]: Monad[MemoF[A]] with

    type M = [C] =>> MemoT[A, C]

    override final def map[B, C](fa: M[B])(f: B => C): M[C] =
      apply(x => f(fa.run(x)._2))

    override def ap[B, C](ff: M[B => C])(
        fa: M[B]
    ): M[C] =
      new MemoT[A, C]:
        override def run(a: A): (MemoT[A, C], C) =
          val (ma, b) = fa.run(a)
          val (mbc, bToC) = ff.run(a)
          val c = bToC(b)

          (mbc.ap(ma), c)

    def pure[B](b: B): MemoT[A, B] =
      new MemoT[A, B]:

        lazy val res = (this, b)

        def run(a: A): (MemoT[A, B], B) = res

    def flatMap[B, C](fa: MemoT[A, B])(
        f: B => MemoT[A, C]
    ): MemoT[A, C] =
      new MemoT[A, C]:
        def run(a: A) =
          val (mab, b) = fa.run(a)
          val mac = f(b)
          mac.run(a)

    final def tailRecM[B, C](b: B)(
        f: B => M[Either[B, C]]
    ): M[C] =
      val eitherMemo: M[Either[B, C]] = f(b)
      new MemoT[A, C]:
        def run(a: A): (M[C], C) =
          eitherMemo.run(a) match {
            case (_, Left(b)) =>
              // (tailRecM(b)(f)
              val finalM = tailRecM(b)(f)
              finalM.run(a)
            case (m, Right(c)) =>
              (this, c)
          }
