val Scala    = "2.12.2-bin-typelevel-4"
val Circe    = "0.8.0"
val Http4s   = "0.15.13"
val Pine     = "0.1.0"
val Trail    = "0.1.0"
val Paradise = "2.1.0"

val SharedSettings = Seq(
  name         := "pine-template",
  version      := "0.1.0",

  scalaVersion := Scala,
  scalaOrganization := "org.typelevel",
  scalacOptions += "-Yliteral-types",

  addCompilerPlugin(
    "org.scalamacros" % "paradise" % Paradise cross CrossVersion.patch
  )
)

val outPath = new File("assets")
val jsPath  = outPath / "js"

lazy val root = project.in(file("."))
  .aggregate(js, jvm)
  .settings(SharedSettings: _*)
  .settings(publishArtifact := false)

lazy val build = crossProject.in(file("."))
  .settings(SharedSettings: _*)
  .jvmSettings(Revolver.settings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "tech.sparse"  %%% "pine"          % Pine,
      "tech.sparse"  %%% "trail"         % Trail,
      "io.circe"     %%% "circe-core"    % Circe,
      "io.circe"     %%% "circe-generic" % Circe,
      "io.circe"     %%% "circe-parser"  % Circe
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.http4s"  %% "http4s-dsl"          % Http4s,
      "org.http4s"  %% "http4s-blaze-client" % Http4s,
      "org.http4s"  %% "http4s-blaze-server" % Http4s
    )
  )
  .jsSettings(
    // From https://github.com/scala-js/scala-js/pull/2954
    libraryDependencies := libraryDependencies.value.filterNot(_.name == "scalajs-compiler"),
    addCompilerPlugin("org.scala-js" % "scalajs-compiler" % scalaJSVersion cross CrossVersion.patch),

    scalaJSUseMainModuleInitializer := true,
    artifactPath in (Compile, fastOptJS) := jsPath / "application.js",
    artifactPath in (Compile, fullOptJS) := jsPath / "application.js"
  )

lazy val js = build.js

lazy val jvm = build.jvm.settings(
  baseDirectory in reStart := new File("."),
  reStart := reStart.dependsOn(fastOptJS in (js, Compile)).evaluated,
  mainClass in reStart := Some("example.Server")
)
