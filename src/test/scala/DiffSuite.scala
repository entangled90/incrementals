// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html

import Graph.*
import Diff.*
import cats.*
import cats.syntax.all.*
import cats.laws.discipline.ApplicativeTests
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._

class DiffSuite extends munit.ScalaCheckSuite {

  implicit def diffArb[A: Arbitrary]: Arbitrary[Diff[A]] =
    val unchanged = Arbitrary.arbitrary[A].map(a => Unchanged(a))
    val changed =
      Arbitrary.arbitrary[(Option[A], A)].map { case (a1, a2) =>
        Updated(a1, a2)
      }
    Arbitrary(Gen.oneOf(unchanged, changed))

  ApplicativeTests[Diff]
    .applicative[Int, Int, String]
    .all
    .properties
    .foreach((name, prop) => property(name)(prop))
}
