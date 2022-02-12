package org.virtuslab.semanticgraphs.analytics

import com.virtuslab.semanticgraphs.proto.model.graphnode.{GraphNode, SemanticGraphFile}
import org.jgrapht.{Graph, GraphMetrics}
import org.jgrapht.alg.scoring.{BetweennessCentrality, ClusteringCoefficient, KatzCentrality, PageRank}
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.builder.GraphTypeBuilder

import java.nio.file.{FileSystems, Files}
import java.util.UUID
import scala.collection.JavaConverters._
import org.jgrapht.alg.shortestpath.GraphMeasurer
import org.virtuslab.semanticgraphs.analytics.utils._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import java.io.File
import org.jgrapht.alg.scoring.ClosenessCentrality
import org.jgrapht.alg.scoring.EigenvectorCentrality

import java.lang

case class GraphStat(id: String, label: String, score: Double)
case class NodeScore(id: String, label: String, score: Double)
case class Statistic(id: String, description: String, nodes: List[NodeScore])
case class ProjectSummary(
  projectName: String,
  workspace: String,
  stats: List[Statistic],
  graphStats: List[GraphStat]
)

object AnalyzeAll extends App {

  private def dumpJsonFile(
    outputFileName: String,
    json: String
  ): Unit = {
    val f = new File(outputFileName)
    val printer = new java.io.PrintWriter(f)
    printer.write(json)
    printer.close()
  }

  def analyzeAll() = {
    List(
      // "akka" -> "/Users/kborowski/phd/akka",
      //  "spark" -> "/Users/kborowski/phd/spark",
      "reflek" -> "/Users/kborowski/virtuslab/reflek/monorepo",
      "phototrip" -> "/Users/kborowski/phd/phototrip-lagom",
      "metals" -> "/Users/kborowski/phd/metals",
      "a5-ordering" -> "/Users/kborowski/virtuslab/adstream/a5-backend/a5-ordering"
    ).foreach { case (projectName, workspace) => analyze(projectName, workspace); println(); Thread.sleep(100) }
  }

  def analyze(projectName: String, workspace: String): Unit = {
    println(s"Analyzing $projectName $workspace")
    val jGraphTExporter = JGraphTExporter()
    jGraphTExporter.fetch(workspace)
    val stats = jGraphTExporter.computeStatistics(projectName, workspace)
    dumpJsonFile(s"$projectName.json", stats.asJson.toString)
  }

}

class JGraphTExporter {

  val nodes: scala.collection.mutable.Map[String, GraphNode] =
    scala.collection.mutable.Map.empty

  case class LabeledEdge(parentId: String, childId: String, role: String) extends DefaultEdge

  private def emptyGraph(): Graph[String, LabeledEdge] = {
    GraphTypeBuilder
      .directed[String, LabeledEdge]()
      .allowingMultipleEdges(true)
      .allowingSelfLoops(false)
      .edgeClass(classOf[LabeledEdge])
      .buildGraph()
  }

  var graph: Graph[String, LabeledEdge] = emptyGraph()

  def addVertex(
    id: String
  ): Unit =
    graph.addVertex(id)

  def addVertex(node: GraphNode) = {
    graph.addVertex(node.id)
    nodes.update(node.id, node)
  }

  def addEdge(parentId: String, childId: String, role: String): Unit = {

    if (parentId != childId) {
      graph.addVertex(childId)
      graph.addVertex(parentId)
      graph.addEdge(
        parentId,
        childId,
        LabeledEdge(UUID.randomUUID().toString, childId, role)
      )
    }
  }

  def prettyPrint(node: GraphNode, stats: String) = {
    import node._
    println(
      s"${location.map(_.uri).getOrElse("")} $kind $displayName; Stats: $stats"
    )
  }

  def pickTopN[T](k: Int, iterable: Iterable[T])(implicit ord: Ordering[T]): List[T] = {
    val q = collection.mutable.PriorityQueue[T]()
    iterable.foreach { elem =>
      q.enqueue(elem)
      if (q.size > k) q.dequeue()
    }
    q.toList.sorted
  }

  def computeStats(
    id: String,
    desc: String,
    stats: Iterable[(String, java.lang.Double)],
    take: Int = 30
  ): Statistic = {
    println(s"Computing: $desc")

    val statistics = pickTopN(
      take,
      stats.filter { case (nodeId, _) => nodes.isDefinedAt(nodeId) }
    )((x: (String, lang.Double), y: (String, lang.Double)) => -x._2.compareTo(y._2))

    // statistics.foreach { case (nodeId, score) =>
    //   val formattedScore = "%9.6f".format(score)
    //   println(s"$formattedScore, $nodeId")
    // }

    // println()

    Statistic(
      id = id,
      description = desc,
      statistics.collect { case (nodeId, score) =>
        val node = nodes(nodeId)
        NodeScore(nodeId, node.displayName, score)
      }
    )
  }

  def computeStatistics(projectName: String, workspace: String) = {
    println(
      s"Nodes size: ${graph.vertexSet().size()}, Edges ${graph.edgeSet().size()}"
    )

    val statistics = List(
      computeStats(
        id = "locSize",
        desc = "Top by node LOC size",
        nodes.values
          .map(node =>
            (
              node.id,
              node.properties.get("LOC").map(_.toInt).getOrElse(0).toDouble
            )
          )
      ),
      computeStats(
        id = "nodedegree",
        desc = "Top by node degree",
        graph
          .vertexSet()
          .asScala
          .toList
          .map(v => (v, graph.degreeOf(v).toDouble))
      ),
      computeStats(
        id = "pagerank",
        desc = "PageRank - how influential the node is",
        new PageRank(graph, 0.99, 1000, 0.000001).getScores.asScala.toList
          .sortBy(-_._2)
      ),
      computeStats(
        id = "katzcentrality",
        desc = "KatzCentrality - how influential the node is",
        KatzCentrality(graph).getScores.asScala.toList
      ),
      computeStats(
        id = "codesmell",
        desc = "Code smell - methods with too many local declarations",
        nodes.values
          .filter(_.kind == "METHOD")
          .toList
          .map(node => (node.id, node.edges.count(_.`type` == "DECLARATION").toDouble))
      ),
      computeStats(
        id = "clustering_coefficient",
        desc = "Top clustering coefficient",
        new ClusteringCoefficient[String, LabeledEdge](graph).getScores.asScala.toList.sortBy(_._2).take(20)
      ),
      computeStats(
        id = "eigenvector",
        desc = "EigenvectorCentrality",
        new EigenvectorCentrality(graph, 100).getScores.asScala.toList
      ),
      computeStats(
        id = "closeness_centrality",
        desc = "Top closeness centrality",
        new ClosenessCentrality[String, LabeledEdge](graph).getScores.asScala.toList.sortBy(_._2).take(20)
      )
    )

    val averageClusteringCoefficient =
      new ClusteringCoefficient[String, LabeledEdge](graph).getAverageClusteringCoefficient()
    println(
      s"Average Clustering Coefficient $averageClusteringCoefficient"
    )
    val averageNodeDegree =
      graph.vertexSet().asScala.map(v => graph.degreeOf(v)).sum / graph.vertexSet().size().toDouble
    println(
      s"Average Degree $averageNodeDegree"
    )
    val clusteringCoefficient =
      GraphStat("clustering_coefficient", "Average Clustering Coefficient", averageClusteringCoefficient)
    val nodesDegree = GraphStat("average_node_degree", "Average Graph Degree", averageNodeDegree)

    ProjectSummary(
      projectName,
      workspace,
      statistics :+ computeCombinedMetrics(statistics),
      List(nodesDegree, clusteringCoefficient)
    )
  }

  def computeCombinedMetrics(stats: List[Statistic]): Statistic = {
    val list = stats.flatMap(_.nodes)
    val a = list.groupBy(node => (node.id, node.label)).mapValues(_.size)
    Statistic(
      id = "combined",
      description = "Crucial code elements, combined",
      a.toList.sortBy { case (_, size) => -size }.map { case ((id, label), score) =>
        NodeScore(id, label, score)
      }
    )
  }

  def fetch(workspace: String): Unit = {
    val dir = workspace.resolve(".semanticgraphs")
    Files
      .walk(dir)
      .iterator()
      .forEachRemaining { path =>
        if (
          Files
            .isRegularFile(path) && path.toString.endsWith(
            ".semanticgraphdb"
          )
        ) {
          val graphFile = SemanticGraphFile.parseFrom(Files.readAllBytes(path))
          graphFile.nodes
            .filter(node => node.kind.nonEmpty && node.location.isDefined)
            .foreach { node =>
              addVertex(node)
              node.edges
                .filter(_.location.isDefined)
                .foreach(edge => addEdge(node.id, edge.to, edge.`type`))
            }
        }
      }
  }

  def fetchCallGraph(workspace: String) = {
    val dir = workspace.resolve(".semanticgraphs")
    Files
      .walk(dir)
      .iterator()
      .forEachRemaining { path =>
        if (
          Files
            .isRegularFile(path) && path.toString.endsWith(
            ".semanticgraphdb"
          )
        ) {
          val graphFile = SemanticGraphFile.parseFrom(Files.readAllBytes(path))
          graphFile.nodes
            .filter(node => node.kind == "METHOD" && node.location.isDefined)
            .foreach { node =>
              addVertex(node)
              node.edges
                .filter(_.`type` == "CALL")
                .filter(_.location.isDefined)
                .foreach(edge => addEdge(node.id, edge.to, edge.`type`))
            }
        }
      }
  }
}
