package org.virtuslab.semanticgraphs.analytics.print

import org.virtuslab.semanticgraphs.analytics.crucial.{MetricIdAndDescription, ProjectScoringSummary, Statistic}
import org.virtuslab.semanticgraphs.analytics.scg.SemanticCodeGraph

import java.nio.file.{Files, Path}
import scala.io.Source
import scala.jdk.CollectionConverters.IteratorHasAsScala

object LatexPrinter:

  def toTexTable(scores: List[ProjectScoringSummary], metrics: List[MetricIdAndDescription]): String =
    val stringBuilder = new StringBuilder()
    stringBuilder.addAll("\\hdashline\n")
    scores.foreach { stats =>
      val topStats = metrics.map(id => stats.stats.find(_.id == id.id).head).map { s =>
        val topNode = s.nodes.head
        if topNode.score == topNode.score.toLong then f"${topNode.label} & ${topNode.score}%.0f"
        else f"${topNode.label} & ${topNode.score}%.4f"
      }
      stringBuilder.addAll(s"${stats.projectName} & ${topStats.mkString("", " & ", " \\\\")} \n")
    }
    stringBuilder.toString()

  def topN(scores: List[ProjectScoringSummary], metric: MetricIdAndDescription, n: Int): String =
    val stringBuilder = new StringBuilder()
    stringBuilder.addAll("\\hdashline\n")
    scores.foreach { stats =>
      val topStats = stats.stats.find(_.id == metric.id).toList.flatMap { s =>
        s.nodes.take(n).map { topNode =>
          if topNode.score == topNode.score.toLong then f"${topNode.label} & ${topNode.score}%.0f"
          else f"${topNode.label} & ${topNode.score}"
        }
      }
      stringBuilder.addAll(s"${stats.projectName} & ${topStats.mkString("", " & ", " \\\\")} \n")
    }
    stringBuilder.toString()

  def tableHeader(headers: List[String]): String =
    headers.mkString("Name & ", " & \\# & ", " & \\# \\\\")

object LatexScoresPrinterApp extends App:

  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
  def readScores(filePrefix: String): List[ProjectScoringSummary] =
    Files
      .list(Path.of("./analysis"))
      .iterator()
      .asScala
      .filter(x => Files.isRegularFile(x) && x.getFileName.toString.startsWith(filePrefix))
      .map { file =>
        val fileContent = Source.fromFile(file.toUri).getLines().mkString
        decode[ProjectScoringSummary](fileContent).getOrElse(throw new RuntimeException("Parsing problem"))
      }
      .toList
      .sortBy(summary => SemanticCodeGraph.allProjects.map(_.projectName).indexOf(summary.projectName))

  def printWholeTable(
    scgScores: List[ProjectScoringSummary],
    callScores: List[ProjectScoringSummary],
    fullCallScores: List[ProjectScoringSummary],
    metrics: List[MetricIdAndDescription]
  ) =
    // println(s"\\begin{tabular}{${"r|".repeat(metrics.size * 2 + 1)}}")
    println("\\hline")
    println(LatexPrinter.tableHeader(metrics.map(_.desc)))
    println("\\hline")
    println(s"\\multicolumn{${metrics.size * 2 + 1}}{l|}{Semantic Code Graph} \\\\")
    println(LatexPrinter.toTexTable(scgScores, metrics))
//    println("\\hline")
//    println(s"\\multicolumn{${metrics.size * 2 + 1}}{l|}{Call Graph}\\\\")
//    println(LatexPrinter.toTexTable(callScores, metrics))
    println("\\hline")
    println(s"\\multicolumn{${metrics.size * 2 + 1}}{l|}{SCG based Call Graph}\\\\")
    println(LatexPrinter.toTexTable(fullCallScores, metrics))
    // println(s"\\end{tabular}")
    println()

  println(Path.of("analysis").toAbsolutePath.toString)
  val scgScores = readScores("scg-v2")
  val callScores = readScores("call")
  val fullCallScores = readScores("full-v4-call")

  val general = List(Statistic.loc, Statistic.outDegree, Statistic.inDegree)
  printWholeTable(scgScores, callScores, fullCallScores, general)

  val influenceBased = List(Statistic.eigenvector, Statistic.katz, Statistic.pageRank)
  printWholeTable(scgScores, callScores, fullCallScores, influenceBased)

  val distanceBased = List(Statistic.betweenness, Statistic.harmonic, Statistic.combined)
  printWholeTable(scgScores, callScores, fullCallScores, distanceBased)

object LatexCombinedPrinterApp extends App:

  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
  def readScores(filePrefix: String): List[ProjectScoringSummary] =
    Files
      .list(Path.of("./analysis"))
      .iterator()
      .asScala
      .filter(x => Files.isRegularFile(x) && x.getFileName.toString.startsWith(filePrefix))
      .map { file =>
        val fileContent = Source.fromFile(file.toUri).getLines().mkString
        decode[ProjectScoringSummary](fileContent).getOrElse(throw new RuntimeException("Parsing problem"))
      }
      .toList
      .sortBy(summary => SemanticCodeGraph.allProjects.map(_.projectName).indexOf(summary.projectName))

  def printWholeTable(
    scgScores: List[ProjectScoringSummary],
    callScores: List[ProjectScoringSummary],
    fullCallScore: List[ProjectScoringSummary],
    n: Int
  ) =
    // println(s"\\begin{tabular}{${"r|".repeat(metrics.size * 2 + 1)}}")
    println("\\hline")
    println(LatexPrinter.tableHeader((1 to n).map(i => s"\\#$i").toList))
    println("\\hline")
    println(s"\\multicolumn{${n * 2 + 1}}{l|}{Semantic Code Graph} \\\\")
    println(LatexPrinter.topN(scgScores, Statistic.combined, n))
//    println("\\hline")
//    println(s"\\multicolumn{${n * 2 + 1}}{l|}{Call Graph}\\\\")
//    println(LatexPrinter.topN(callScores, "combined", n))
    println("\\hline")
    println(s"\\multicolumn{${n * 2 + 1}}{l|}{SCG based Call Graph}\\\\")
    println(LatexPrinter.topN(fullCallScore, Statistic.combined, n))
    // println(s"\\end{tabular}")
    println()

  println(Path.of("analysis").toAbsolutePath.toString)
  val scgScores = readScores("scg")
  val callScores = readScores("call")
  val fullCallScores = readScores("full-call")

  printWholeTable(scgScores, callScores, fullCallScores, 3)
