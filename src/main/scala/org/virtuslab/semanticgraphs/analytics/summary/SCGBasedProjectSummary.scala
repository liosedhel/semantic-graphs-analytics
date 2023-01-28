package org.virtuslab.semanticgraphs.analytics.summary

import scala.jdk.CollectionConverters._
import io.circe.generic.auto._
import io.circe.syntax._
import org.virtuslab.semanticgraphs.analytics.crucial.{CrucialNodes, ProjectScoringSummary}
import org.virtuslab.semanticgraphs.analytics.metrics.JGraphTMetrics
import org.virtuslab.semanticgraphs.analytics.scg.SemanticCodeGraph
import org.virtuslab.semanticgraphs.analytics.utils.JsonUtils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object SCGBasedProjectSummary extends App:

  val projects = SemanticCodeGraph.readAllProjects() ++ SemanticCodeGraph.readAllProjectsCallGraph()

  def exportSummary(): Unit =
    case class NodeKindAndNumber(kind: String, number: Int)
    case class EdgeTypeAndNumber(`type`: String, number: Int)
    case class SCGProjectSummary(
      name: String,
      version: String,
      nodes: Int,
      edges: Int,
      nodesDistribution: List[NodeKindAndNumber],
      edgesDistribution: List[EdgeTypeAndNumber]
    )
    val summary = projects.map { semanticCodeGraph =>

      val nodesTotal = semanticCodeGraph.graph.vertexSet().size()
      val edgesTotal = semanticCodeGraph.graph.edgeSet().size()

      val edgeDistribution = semanticCodeGraph.graph
        .edgeSet()
        .asScala
        .toList
        .map(_.role)
        .groupBy(identity)
        .map { case (role, set) => EdgeTypeAndNumber(role, set.size) }
        .toList
        .sortBy(-_.number)
      val nodesDistribution = semanticCodeGraph.nodes
        .map(_.kind)
        .groupBy(identity)
        .map { case (kind, set) => NodeKindAndNumber(kind, set.size) }
        .toList
        .sortBy(-_.number)

      SCGProjectSummary(
        semanticCodeGraph.project,
        semanticCodeGraph.version,
        nodesTotal,
        edgesTotal,
        nodesDistribution,
        edgeDistribution
      )
    }

    JsonUtils.dumpJsonFile("summary.json", summary.asJson.spaces2)

  def printSummaryLatexTable() =
    println()
    println("Name & Version & LOC & Nodes & Edges & Density & AOD & AID & & GCC & AC\\\\")
    println("\\hline")
    projects.map(_.withoutZeroDegreeNodes()).foreach { semanticCodeGraph =>
      val nodesTotal = semanticCodeGraph.graph.vertexSet().size()
      val edgesTotal = semanticCodeGraph.graph.edgeSet().size()
      val density = JGraphTMetrics.density(semanticCodeGraph.graph)

      val averageInDegree = JGraphTMetrics.averageInDegree(semanticCodeGraph.graph)
      val averageOutDegree = JGraphTMetrics.averageOutDegree(semanticCodeGraph.graph)
      val globalClusteringCoefficient = JGraphTMetrics.globalClusteringCoefficient(semanticCodeGraph.graph)
      val assortativityCoefficient = JGraphTMetrics.assortativityCoefficient2(semanticCodeGraph.graph)
      val totalLoc = SemanticCodeGraph.readLOCFromZip(semanticCodeGraph.projectAndVersion)

      println(
        f"${semanticCodeGraph.project} & ${semanticCodeGraph.version} & $totalLoc & $nodesTotal & $edgesTotal & $density%1.5f & $averageOutDegree%1.3f & $averageInDegree%1.3f & $globalClusteringCoefficient%1.2f & $assortativityCoefficient%1.4f\\\\"
      )
    }

    println()

  exportSummary()
  printSummaryLatexTable()

object ComputeProjectSummary extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  def analyze(projects: List[SemanticCodeGraph], filePrefix: String): Future[List[ProjectScoringSummary]] =
    Future.sequence(projects.map { project =>
      Future {
        CrucialNodes.analyze(project, filePrefix)
      }.recover { case e =>
        println(s"Exception for ${project.project} ${e.getMessage}")
        ProjectScoringSummary(project.project, project.projectAndVersion.workspace, Nil, Nil)
      }
    })

  def printStats(results: List[ProjectScoringSummary]) =
    println("Crucial nodes")
    println(results.head.stats.drop(3).map(_.id).mkString("Name & ", " & \\# & ", " & \\# \\\\"))
    results.foreach { stats =>
      val topStats = stats.stats.drop(3).map { s =>
        val topNode = s.nodes.head
        if topNode.score == topNode.score.toLong then f"${topNode.label} & ${topNode.score}"
        else f"${topNode.label} & ${topNode.score}%.4f"
      }
      println(s"${stats.projectName} & ${topStats.mkString("", " & ", " \\\\")}")
    }
    println("Finished")

  val all = analyze(SemanticCodeGraph.readAllProjects(), "scg")
  val fullCallGraph = analyze(SemanticCodeGraph.readAllProjectsFullCallGraph(), "call")
  val whole = for
    allR <- all
    // callR <- callGraph
    fullR <- fullCallGraph
  yield
    printStats(allR)
    // printStats(callR)
    printStats(fullR)

  Await.result(whole, Duration.Inf)
}
