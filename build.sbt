// Shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

val Scala    = "2.12.4-bin-typelevel-4"
val Circe    = "0.8.0"
val Http4s   = "0.17.6"
val Pine     = "0.1.1"
val Trail    = "0.1.1"
val Paradise = "2.1.1"

val jsPath = file("assets") / "js"

lazy val example = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("."))
  .settings(
    name    := "pine-example",
    version := "0.1.0",

    scalaVersion      := Scala,
    scalaOrganization := "org.typelevel",
    scalacOptions     += "-Yliteral-types",

    addCompilerPlugin(
      "org.scalamacros" % "paradise" % Paradise cross CrossVersion.patch
    ),

    libraryDependencies ++= Vector(
      "tech.sparse"  %%% "pine"          % Pine,
      "tech.sparse"  %%% "trail"         % Trail,
      "io.circe"     %%% "circe-core"    % Circe,
      "io.circe"     %%% "circe-generic" % Circe,
      "io.circe"     %%% "circe-parser"  % Circe
    )
  )
  .jvmSettings(
    libraryDependencies ++= Vector(
      "org.http4s"  %% "http4s-dsl"          % Http4s,
      "org.http4s"  %% "http4s-blaze-client" % Http4s,
      "org.http4s"  %% "http4s-blaze-server" % Http4s
    ),

    reStart / mainClass     := Some("example.Server"),
    reStart / baseDirectory := file(".")
  )
  .jsSettings(
    // From https://github.com/scala-js/scala-js/pull/2954
    libraryDependencies := libraryDependencies.value.filterNot(_.name == "scalajs-compiler"),
    addCompilerPlugin("org.scala-js" % "scalajs-compiler" % scalaJSVersion cross CrossVersion.patch),

    scalaJSUseMainModuleInitializer := true,
    Compile / fastOptJS / artifactPath := jsPath / "application.js",
    Compile / fullOptJS / artifactPath := jsPath / "application.js"
  )

lazy val js  = example.js
lazy val jvm = example.jvm.settings(
  reStart := reStart.dependsOn(js / Compile / fastOptJS).evaluated,
)
