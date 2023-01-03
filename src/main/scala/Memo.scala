// import cats.syntax.all.given
// import cats.Applicative

// enum Memo[I, O]:

//   def memo = Memoize(this)

//   /** Return a result * */
//   case Pure(o: O)

//   /** Lift a pure computation into the context * */
//   case Lift(f: I => O)

//   // case Merge[I1, I2, O1, O2](l: Memo[I1, O1], r: Memo[I2, O2])
//   //     extends Memo[(I1, I2), (O1, O2)]

//   case FlatMap[I, I1, O](fa: Memo[I, I1], f: I1 => Memo[I, O])
//       extends Memo[I, O]
//   case Memoize(m: Memo[I, O])

// object Memo:

//   def lift[A, B](f: A => B): Memo[A, B] = Lift(f)

//   def program: Memo[Int, (Int, Int)] = for {
//     o <- lift[Int, Int](_ * 2)
//     res <- (
//       lift[Int, Int](_ - 1),
//       lift[Int, Int](_ + 1)
//     ).tupled
//   } yield res

//   type MemoF[A] = [B] =>> Memo[A, B]

//   def run[I, O](m: Memo[I, O]): I => O =
//     var memoized = Map.empty[(Memo[I, O], I), O]

//     i => {
//       memoized.get((m, i)) match {
//         case None =>
//           println(s"Memo $m with $i not in map")
//           println(s"Map size == $memoized")

//           val result = m match
//             case Memo.Pure(o) => o
//             case Memo.Lift(f) =>
//               println(s"Executing Lift f $f with input $i")
//               f(i)
//             case Memo.FlatMap(fa, f) =>
//               val intermediate = run(fa)(i)
//               println(s"Executing FlatMap f $f with input $i")
//               val iToO = run(f(intermediate))
//               iToO(i)

//             case Memo.Memoize(m) =>
//               memoized.get((m, i)) match {
//                 case None =>
//                   val o = run(m)(i)
//                   memoized += (m, i) -> o
//                   o
//                 case Some(o) =>
//                   o
//               }
//           memoized += (m, i) -> result
//           result
//         case Some(o) =>
//           o
//       }
//     }

//   implicit def flatMapInstance[I, O]
//       : cats.FlatMap[MemoF[I]] with Applicative[MemoF[I]] =
//     new cats.FlatMap[MemoF[I]] with Applicative[MemoF[I]]:

//       override def map[A, B](fa: Memo[I, A])(f: A => B): Memo[I, B] =
//         FlatMap(fa, a => Pure(f(a)))

//       override def tailRecM[A, B](a: A)(
//           f: A => MemoF[I][Either[A, B]]
//       ): MemoF[I][B] = ???

//       def flatMap[A, B](fa: Memo[I, A])(
//           f: A => Memo[I, B]
//       ): Memo[I, B] =
//         Memo.FlatMap[I, A, B](fa, f)

//       def pure[A](a: A): Memo[I, A] = Pure(a)
