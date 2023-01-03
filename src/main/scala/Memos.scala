// import cats.data.State
// import cats._

// final case class Memos[A, B](
//     run: State[(A, B), B]
// ) extends AnyVal

// object Memos:
//   given FlatMapMemos[A]: FlatMap[[B] =>> Memos[A, B]] with
//     def flatMap[B, C](fb: Memos[A, B])(f: B => Memos[A, C]): Memos[A, C] =
//       Memos {
//         val s = fb.run.flatMap(b => f(b).run)

//       }
