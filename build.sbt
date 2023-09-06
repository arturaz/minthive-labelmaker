name := "labelmaker"
organization := "minthive"

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-language:strictEquality",
  "-Xfatal-warnings",
)

val Version = "1.0.6"

lazy val library = project
  .in(file("library"))
  .settings(
    name := "labelmaker",
    version := Version,
    libraryDependencies ++= Seq(
      "com.itextpdf" % "itext7-core" % "8.0.1",
      "org.typelevel" %% "cats-effect" % "3.5.1",
    )
  )

lazy val cli = project
  .in(file("cli"))
  .dependsOn(library)
  .settings(
    maintainer := "Artūras Šlajus <as@arturaz.net>",
    version := Version,
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-nop" % "2.0.5",
      "com.github.tototoshi" %% "scala-csv" % "1.3.10",
    )
  )
  .enablePlugins(JavaAppPackaging)