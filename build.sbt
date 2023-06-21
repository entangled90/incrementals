val scalaV = "3.3.0"

Global / onChangedBuildSource := ReloadOnSourceChanges

val sharedSettings = Seq(
  scalaVersion := scalaV,
  scalacOptions ++= Seq("-language:strictEquality", "-rewrite", "-indent")

)
lazy val core =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings(
      name := "incrementals",
      version := "0.1.0-SNAPSHOT",
      libraryDependencies ++= Seq(
        "org.scalameta" %%% "munit" % "1.0.0-M8" % Test,
//        "org.scalameta" %%% "munit-scalacheck" % "1.0.0-M8" % Test,
//        "org.scalacheck" %%% "scalacheck" % "1.17.0" % Test
      ),
    )
    .settings(sharedSettings)
    .jsSettings(
      scalaJSUseMainModuleInitializer := true,
      mainClass := Some("incrementals.web.run"),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "2.6.0"
      )
    )

lazy val coreJVM = core.jvm

lazy val coreNative = core.native

lazy val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(coreJVM)
  .enablePlugins(JmhPlugin)
  .settings(sharedSettings)

