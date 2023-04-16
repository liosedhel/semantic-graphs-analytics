val scala3Version = "3.2.1"

maintainer := "kborowski@virtuslab.com"

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scg-cli",
    organization := "com.virtuslab.semanticgraphs",
    version := "0.1.1",
    scalaVersion := scala3Version,
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),
    dockerBaseImage := "openjdk:11",
    packageName := "scg-cli",
    Compile / discoveredMainClasses := Seq("org.virtuslab.semanticgraphs.analytics.cli.ScgCli"),
    scalacOptions ++= Seq("-new-syntax", "-rewrite"),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    libraryDependencies += "org.jgrapht" % "jgrapht-core" % "1.5.1",
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.12",
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.3",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.3",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "3.0.0-M1",
    libraryDependencies += "com.lihaoyi" %% "mainargs" % "0.3.0",
    libraryDependencies += "org.apache.spark" %% "spark-graphx" % "3.3.1" cross CrossVersion.for3Use2_13,
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.3.1" cross CrossVersion.for3Use2_13,
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.9.0",
    excludeDependencies += "org.scala-lang.modules" % "scala-collection-compat_2.13",
    excludeDependencies += "org.typelevel" % "cats-kernel_2.13",
      // https://mvnrepository.com/artifact/info.picocli/picocli
    libraryDependencies += "info.picocli" % "picocli" % "4.7.0"
).aggregate(javaparser, parsercommons).dependsOn(javaparser)

lazy val javaparser = project.in(file("javaparser")).settings(
  scalaVersion := scala3Version,
  Compile / PB.targets := Seq(
    scalapb.gen() -> (Compile / sourceManaged).value
  ),
  libraryDependencies += "com.github.javaparser" % "javaparser-core" % "3.24.0",
  libraryDependencies += "com.github.javaparser" % "javaparser-symbol-solver-core" % "3.24.0"
).dependsOn(parsercommons).aggregate(parsercommons)

lazy val parsercommons = project.in(file("parsercommons")).settings(
  scalaVersion := scala3Version,
  Compile / PB.targets := Seq(
    scalapb.gen() -> (Compile / sourceManaged).value
  ),
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.10",
  libraryDependencies += "com.typesafe.scala-logging" % "scala-logging_3" % "3.9.4",
  libraryDependencies += "com.typesafe.play" % "play-json_3" % "2.10.0-RC5",
  libraryDependencies += "com.typesafe" % "config" % "1.4.2",
  libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "6.0.0.202111291000-r",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test
)
