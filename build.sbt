val scalaV = "3.3.0"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val core = project
  .in(file("core"))
  .settings(
    name := "incrementals",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      // "org.typelevel" %% "cats-core" % "2.9.0",
      // "org.typelevel" %% "cats-free" % "2.9.0",
      // "org.typelevel" %% "kittens" % "3.0.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test,
      // "org.typelevel" %% "cats-laws" % "2.9.0" % Test,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
    ),
    scalacOptions ++= Seq("-language:strictEquality", "-rewrite", "-indent")
  )

lazy val benchmarks = project
  .in(file("benchmarks"))
  .enablePlugins(JmhPlugin)
  .aggregate(core)
  .dependsOn(core)
  .settings(
    scalaVersion := scalaV,
    scalacOptions ++= Seq("-language:strictEquality", "-rewrite", "-indent")
  )
