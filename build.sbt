val scala3Version = "3.2.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "incrementals",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.typelevel" %% "cats-free" % "2.9.0",
      "org.typelevel" %% "kittens" % "3.0.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test,
      "org.typelevel" %% "cats-laws" % "2.9.0" % Test,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
    )

    // scalacOptions += "-explain"
  )
