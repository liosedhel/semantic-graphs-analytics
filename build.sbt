val scala2Version = "3.2.1"

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "semantic-graphs-analytics",
    organization := "com.virtuslab.semanticgraphs",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala2Version,
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),
    //Compile / mainClass := Some("org.virtuslab.semanticgraphs.analytics.cli.ScgCli"),
    //Universal / name := "scg-cli",
    //Universal / packageName := "scg-cli",
    //Compile / discoveredMainClasses := Seq(),
    scalacOptions ++= Seq("-new-syntax", "-rewrite"),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    libraryDependencies += "org.jgrapht" % "jgrapht-core" % "1.5.1",
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.12",
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.3",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.3",
    libraryDependencies += "com.lihaoyi" %% "mainargs" % "0.3.0",
    libraryDependencies += "org.apache.spark" %% "spark-graphx" % "3.3.1" cross CrossVersion.for3Use2_13,
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.3.1" cross CrossVersion.for3Use2_13,
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.9.0",
    excludeDependencies += "org.scala-lang.modules" % "scala-collection-compat_2.13",
    excludeDependencies += "org.typelevel" % "cats-kernel_2.13",
      // https://mvnrepository.com/artifact/info.picocli/picocli
    libraryDependencies += "info.picocli" % "picocli" % "4.7.0"


)
