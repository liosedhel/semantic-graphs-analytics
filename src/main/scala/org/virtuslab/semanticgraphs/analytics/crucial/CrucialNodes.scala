package org.virtuslab.semanticgraphs.analytics.crucial

import io.circe.generic.auto._
import io.circe.syntax._
import org.jgrapht.alg.scoring._
import org.virtuslab.semanticgraphs.analytics.metrics.JGraphTMetrics.LabeledEdge
import org.virtuslab.semanticgraphs.analytics.metrics.JGraphTMetrics
import org.virtuslab.semanticgraphs.analytics.scg.{ProjectAndVersion, SemanticCodeGraph}
import org.virtuslab.semanticgraphs.analytics.utils.JsonUtils

import java.lang
import java.util.concurrent.Executors
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala}

case class GraphStat(id: String, label: String, score: Double)
case class NodeScore(id: String, label: String, score: Double)
case class Statistic(id: String, description: String, nodes: List[NodeScore])

case class MetricIdAndDescription(id: String, desc: String):
  override def toString(): String = desc
object Statistic:
  val loc = MetricIdAndDescription("loc", "Lines of Code")
  val outDegree = MetricIdAndDescription("out-degree", "Outgoing Degree")
  val inDegree = MetricIdAndDescription("in-degree", "Incoming Degree")
  val pageRank = MetricIdAndDescription("pagerank", "Page Rank")
  val eigenvector = MetricIdAndDescription("eigenvector", "Eigenvector Centrality")
  val katz = MetricIdAndDescription("Katz", "Katz Centrality")
  val betweenness = MetricIdAndDescription("betweenness", "Betweenness Centrality")
  val harmonic = MetricIdAndDescription("harmonic", "Harmonic Centrality")
  val combined = MetricIdAndDescription("combined", "All metrics combined")
case class ProjectScoringSummary(
  projectName: String,
  workspace: String,
  stats: List[Statistic],
  graphStats: List[GraphStat]
)

object CrucialNodes:

  def analyze(semanticCodeGraph: SemanticCodeGraph, filePrefix: String): ProjectScoringSummary =
    val jGraphTExporter = new JGraphTAnalyzer(semanticCodeGraph)
    val stats =
      jGraphTExporter.computeStatistics(semanticCodeGraph.project, semanticCodeGraph.projectAndVersion.workspace)
    val outputFile = s"$filePrefix-stats-${semanticCodeGraph.project}.crucial.json"
    JsonUtils.dumpJsonFile(outputFile, stats.asJson.toString)
    println(s"Results exported to: $outputFile")
    stats

object CrucialNodesApp extends App:
  val workspace = args(0)
  val projectName = workspace.split("/").last
  val scg = SemanticCodeGraph.fromZip(ProjectAndVersion(workspace, workspace.split("/").last, ""))
  CrucialNodes.analyze(scg, projectName)

object CrucialNodesAnalyzeAll extends App:
  def analyzeAll() =
    SemanticCodeGraph.readAllProjects().foreach { scg =>
      CrucialNodes.analyze(scg, "all"); println(); Thread.sleep(100)
    }

  analyzeAll()

object CrucialNodesAnalyzeAllZipped extends App:
  def analyzeAll() =
    SemanticCodeGraph.readAllProjects().foreach { scg =>
      CrucialNodes.analyze(scg, "all"); println(); Thread.sleep(100)
    }

  analyzeAll()

class JGraphTAnalyzer(semanticCodeGraph: SemanticCodeGraph):

  val graph = semanticCodeGraph.graph
  val nodes = semanticCodeGraph.nodesMap

  def pickTopN[T](k: Int, iterable: Iterable[T])(implicit ord: Ordering[T]): List[T] =
    val q = collection.mutable.PriorityQueue[T]()
    iterable.foreach { elem =>
      q.enqueue(elem)
      if q.size > k then q.dequeue()
    }
    q.toList.sorted

  def computeStats(
    id: MetricIdAndDescription,
    desc: String,
    stats: Iterable[(String, java.lang.Double)],
    take: Int = 10
  ): Statistic =
    println(s"Computed: $desc")

    val statistics = pickTopN(
      take,
      stats
    )((x: (String, lang.Double), y: (String, lang.Double)) => -x._2.compareTo(y._2))

    Statistic(
      id = id.id,
      description = desc,
      statistics.collect { case (nodeId, score) =>
        val node = nodes(nodeId)
        NodeScore(nodeId, node.displayName, score)
      }
    )

  def computeStatistics(projectName: String, workspace: String): ProjectScoringSummary =
    println(
      s"Nodes size: ${graph.vertexSet().size()}, Edges ${graph.edgeSet().size()}"
    )

    val statistics = List(
      computeStats(
        id = Statistic.loc,
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
        id = Statistic.outDegree,
        desc = "Top by node degree outgoing",
        graph
          .vertexSet()
          .asScala
          .toList
          .map(v => (v, graph.outDegreeOf(v).toDouble))
      ),
      computeStats(
        id = Statistic.inDegree,
        desc = "Top by node degree incoming",
        graph
          .vertexSet()
          .asScala
          .toList
          .map(v => (v, graph.inDegreeOf(v).toDouble))
      ),
      computeStats(
        id = Statistic.pageRank,
        desc = "PageRank - how influential the node is",
        new PageRank(graph, 0.99, 1000, 0.000001).getScores.asScala
      ),
      computeStats(
        id = Statistic.eigenvector,
        desc = "eigenvector Centrality",
        new EigenvectorCentrality(
          graph,
          EigenvectorCentrality.MAX_ITERATIONS_DEFAULT,
          EigenvectorCentrality.TOLERANCE_DEFAULT
        ).getScores.asScala
      ),
      computeStats(
        id = Statistic.katz,
        desc = "Katz Centrality - how influential the node is",
        new KatzCentrality(graph).getScores.asScala
      ),
//      computeStats(
//        id = "codesmell",
//        desc = "Code smell - methods with too many local declarations",
//        nodes.values
//          .filter(_.kind == "METHOD")
//          .toList
//          .map(node => (node.id, node.edges.count(_.`type` == "DECLARATION").toDouble))
//      ),
//      computeStats(
//        id = "clustering_coefficient",
//        desc = "Top clustering coefficient",
//        new ClusteringCoefficient[String, LabeledEdge](graph).getScores.asScala
//      ),
      computeStats(
        id = Statistic.betweenness,
        desc = "Betweenness Centrality",
        JGraphTMetrics.betweennessCentrality(graph).asScala
      ),
      computeStats(
        id = Statistic.harmonic,
        desc = "Top harmonic centrality",
        new HarmonicCentrality[String, LabeledEdge](graph).getScores.asScala
      )
    )

    val averageClusteringCoefficient =
      JGraphTMetrics.averageClusteringCoefficient(JGraphTMetrics.exportUndirected(semanticCodeGraph.nodes))
//    println(
//      s"Average Clustering Coefficient $averageClusteringCoefficient"
//    )
    val clusteringCoefficient =
      GraphStat("clustering_coefficient", "Average Clustering Coefficient", averageClusteringCoefficient)

    val averageNodeDegree = JGraphTMetrics.averageDegree(graph)
//    println(
//      s"Average Degree $averageNodeDegree"
//    )
    val nodesDegree = GraphStat("average_node_degree", "Average Graph Degree", averageNodeDegree)

    ProjectScoringSummary(
      projectName,
      workspace,
      statistics :+ computeCombinedMetrics(statistics),
      List(nodesDegree, clusteringCoefficient)
    )

  def computeCombinedMetrics(stats: List[Statistic]): Statistic =
    val list = stats.flatMap(_.nodes)
    val a = list.groupBy(node => (node.id, node.label)).mapValues(_.size)
    Statistic(
      id = Statistic.combined.id,
      description = "Crucial code elements, combined",
      a.toList.sortBy { case (_, size) => -size }.map { case ((id, label), score) =>
        NodeScore(id, label, score)
      }
    )
