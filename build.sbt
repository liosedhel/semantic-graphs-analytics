val scala2Version = "3.2.1"

val circeVersion = "0.14.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "semantic-graphs-analytics",
    organization := "com.virtuslab.semanticgraphs",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala2Version,
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),
    scalacOptions ++= Seq("-indent", "-rewrite"),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    libraryDependencies += "org.jgrapht" % "jgrapht-core" % "1.5.1",
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.12",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
    libraryDependencies += "org.apache.spark" %% "spark-graphx" % "3.3.1" cross CrossVersion.for3Use2_13,
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.3.1" cross CrossVersion.for3Use2_13,
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.9.0",
    excludeDependencies += "org.scala-lang.modules" % "scala-collection-compat_2.13",
    excludeDependencies += "org.typelevel" % "cats-kernel_2.13"
  )
