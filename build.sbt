name := "minthive-labelmaker"
maintainer := "as@arturaz.net"
version := "1.0.2"

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
)

libraryDependencies ++= Seq(
  "com.itextpdf" % "itext7-core" % "8.0.1",
  "org.typelevel" %% "cats-effect" % "3.5.1",
  "com.github.tototoshi" %% "scala-csv" % "1.3.10",
  "org.slf4j" % "slf4j-nop" % "2.0.5",
)

enablePlugins(JavaAppPackaging)