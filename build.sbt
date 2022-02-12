val scala3Version = "2.13.7"

val circeVersion = "0.14.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "semantic-graphs-analytics",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    libraryDependencies += "org.jgrapht" % "jgrapht-core" % "1.5.1",
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
    libraryDependencies += "org.apache.spark" %% "spark-graphx" % "3.2.0"
  )
