ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.3"

val deps = Seq(
  "io.circe"   %% "circe-core"       % "0.14.4",
  "io.circe"   %% "circe-parser"     % "0.14.4",
  "io.circe"   %% "circe-generic"    % "0.14.4",
  "org.http4s" %% "http4s-core"      % "0.23.18",
  "org.http4s" %% "http4s-client"    % "0.23.18",
  "org.http4s" %% "http4s-circe"     % "0.23.18",
  "org.http4s" %% "http4s-dsl"       % "0.23.18",
  "dev.zio"    %% "zio"              % "1.0.18",
  "dev.zio"    %% "zio-interop-cats" % "3.1.1.0"
)

val scalacOpts = Seq(
  "-Ywarn-unused:imports",
  "-language:postfixOps"
)

val scalafixConfig = Seq(
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val root = (project in file("."))
  .settings(
    name := "http4s-client-stub",
    libraryDependencies ++= deps,
    scalacOptions ++= scalacOpts
  )
  .settings(scalafixConfig)
