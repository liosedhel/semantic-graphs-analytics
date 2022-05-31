package org.virtuslab.semanticgraphs.analytics.scg

import com.virtuslab.semanticgraphs.proto.model.graphnode.{Edge, GraphNode, SemanticGraphFile}
import org.apache.commons.compress.archivers.zip.ZipFile
import org.jgrapht.Graph
import org.virtuslab.semanticgraphs.analytics.metrics.JGraphTMetrics.LabeledEdge
import org.virtuslab.semanticgraphs.analytics.metrics.JGraphTMetrics
import org.virtuslab.semanticgraphs.analytics.utils.PathHelpers._

import java.nio.file.Files

class SemanticCodeGraph(
  val projectAndVersion: ProjectAndVersion,
  val nodesMap: Map[String, GraphNode]
) {
  def project: String = projectAndVersion.projectName
  def version: String = projectAndVersion.version

  lazy val nodes: Iterable[GraphNode] = nodesMap.values

  lazy val graph: Graph[String, LabeledEdge] =
    buildGraph(JGraphTMetrics.emptyGraph())

  private def buildGraph(graph: Graph[String, LabeledEdge]): Graph[String, LabeledEdge] = {
    nodes.foreach { node =>
      graph.addVertex(node.id)
      node.edges.foreach(edge => addEdge(graph, node.id, edge.to, edge.`type`))
    }
    graph
  }

  private def addEdge(graph: Graph[String, LabeledEdge], parentId: String, childId: String, `type`: String): Unit = {
    if (parentId != childId) {
      graph.addVertex(childId)
      graph.addVertex(parentId)
      graph.addEdge(
        parentId,
        childId,
        LabeledEdge(parentId, childId, `type`)
      )
    }
  }

  def withoutZeroDegreeNodes() =
    new SemanticCodeGraph(projectAndVersion, nodesMap.filter { case (id, _) => graph.degreeOf(id) > 0 })

}

case class ProjectAndVersion(workspace: String, projectName: String, version: String)

object SemanticCodeGraph {

  val commonsIO = ProjectAndVersion("data/commons-io", "commons-io", "2.12.0")
  val metals = ProjectAndVersion("data/metals", "metals", "0.10.3")
  val springBoot = ProjectAndVersion("data/spring-boot", "spring-boot", "2.7.5")
  val akka = ProjectAndVersion("data/akka", "akka", "2.7.0")
  val spark = ProjectAndVersion("data/spark", "spark", "3.3.0")

  val allProjects = List(commonsIO, metals, springBoot, akka, spark)

  def readAllProjects(): List[SemanticCodeGraph] =
    List(commonsIO, metals, springBoot, akka, spark).map(SemanticCodeGraph.fromZip(_))

  def readAllProjectsCallGraph(): List[SemanticCodeGraph] =
    List(commonsIO, metals, springBoot, akka, spark).map(fetchCallGraph)

  def readAllProjectsFullCallGraph(): List[SemanticCodeGraph] =
    List(commonsIO, metals, springBoot, akka, spark).map(fetchFullCallGraph)

  def isNodeDefinedInProject(node: GraphNode): Boolean =
    node.kind.nonEmpty && node.location.isDefined && !node.kind.contains("FILE") && node.kind != "PACKAGE_OBJECT"

  def notLocal(node: GraphNode): Boolean =
    !node.id.startsWith("local")

  def isEdgeDefinedInProject(edge: Edge): Boolean =
    edge.location.isDefined

  /**
    * If we want to take into account only call graphs
    *
    * @param workspace
    */
  def fetchCallGraph(projectAndVersion: ProjectAndVersion) = {
    SemanticCodeGraph.fromZip(
      projectAndVersion,
      node => isNodeDefinedInProject(node) && node.kind == "METHOD",
      edge => isEdgeDefinedInProject(edge) && edge.`type` == "CALL"
    )
  }

  def fetchFullCallGraph(projectAndVersion: ProjectAndVersion) = {
    SemanticCodeGraph.fromZip(
      projectAndVersion,
      node =>
        isNodeDefinedInProject(
          node
        ) && node.kind != "CLASS" && node.kind != "OBJECT" && node.kind != "TRAIT" && node.kind != "INTERFACE",
      edge => isEdgeDefinedInProject(edge) && edge.`type` == "CALL"
    )
  }

  def readOnlyGlobalNodes(projectAndVersion: ProjectAndVersion): SemanticCodeGraph = {
    val semanticCodeGraph = SemanticCodeGraph.fromZip(
      projectAndVersion,
      nodeFilter = node => SemanticCodeGraph.isNodeDefinedInProject(node) && SemanticCodeGraph.notLocal(node)
    )
    semanticCodeGraph.withoutZeroDegreeNodes()
  }

  def fromDir(
    projectAndVersion: ProjectAndVersion,
    nodeFilter: GraphNode => Boolean = SemanticCodeGraph.isNodeDefinedInProject,
    edgeFilter: Edge => Boolean = SemanticCodeGraph.isEdgeDefinedInProject
  ): SemanticCodeGraph = {
    lazy val nodesMap: scala.collection.mutable.Map[String, GraphNode] = {
      val map = scala.collection.mutable.Map.empty[String, GraphNode]
      val dir = projectAndVersion.workspace.resolve(".semanticgraphs")
      Files
        .walk(dir)
        .iterator()
        .forEachRemaining { path =>
          if (Files.isRegularFile(path) && path.toString.endsWith(".semanticgraphdb")) {
            val graphFile = SemanticGraphFile.parseFrom(Files.readAllBytes(path))
            graphFile.nodes.foreach { node =>
              if (nodeFilter(node)) map.update(node.id, node.copy(edges = node.edges.filter(edgeFilter)))
            }
          }
        }
      map.map { case (id, node) =>
        (id, node.copy(edges = node.edges.filter(edge => map.isDefinedAt(edge.to))))
      } // filter out edges pointing to outside nodes
    }
    new SemanticCodeGraph(projectAndVersion, nodesMap.toMap)
  }

  def fromZip(
    projectAndVersion: ProjectAndVersion,
    nodeFilter: GraphNode => Boolean = SemanticCodeGraph.isNodeDefinedInProject,
    edgeFilter: Edge => Boolean = SemanticCodeGraph.isEdgeDefinedInProject
  ): SemanticCodeGraph = {
    lazy val nodesMap: scala.collection.mutable.Map[String, GraphNode] = {
      val map = scala.collection.mutable.Map.empty[String, GraphNode]
      val zipFile = new ZipFile(s"${projectAndVersion.workspace}.zip")
      val entries = zipFile.getEntries
      entries.asIterator().forEachRemaining { entry =>
        if (!entry.isDirectory && entry.getName.endsWith(".semanticgraphdb")) {
          val graphFile = SemanticGraphFile.parseFrom(zipFile.getInputStream(entry))
          graphFile.nodes.foreach { node =>
            if (nodeFilter(node)) map.update(node.id, node.copy(edges = node.edges.filter(edgeFilter)))
          }
        }
      }
      zipFile.close()
      map.map { case (id, node) =>
        (id, node.copy(edges = node.edges.filter(edge => map.isDefinedAt(edge.to))))
      } // filter out edges pointing to outside nodes
    }
    new SemanticCodeGraph(projectAndVersion, nodesMap.toMap)
  }

  def readLOCFromDir(
    projectAndVersion: ProjectAndVersion
  ): Long = {
    var loc = 0L
    val dir = projectAndVersion.workspace.resolve(".semanticgraphs")

    Files
      .walk(dir)
      .iterator()
      .forEachRemaining { path =>
        if (Files.isRegularFile(path) && path.toString.endsWith(".semanticgraphdb")) {
          val graphFile = SemanticGraphFile.parseFrom(Files.readAllBytes(path))
          graphFile.nodes.foreach { node =>
            if (node.kind.contains("FILE")) loc += node.properties.get("LOC").map(_.toInt).getOrElse(0)
          }
        }
      }
    loc
  }

  def readLOCFromZip(
    projectAndVersion: ProjectAndVersion
  ): Long = {
    var loc = 0L
    val dir = projectAndVersion.workspace.resolve(".semanticgraphs")

    val zipFile = new ZipFile(s"${projectAndVersion.workspace}.zip")
    val entries = zipFile.getEntries
    entries.asIterator().forEachRemaining { entry =>
      if (!entry.isDirectory && entry.getName.endsWith(".semanticgraphdb")) {
        val graphFile = SemanticGraphFile.parseFrom(zipFile.getInputStream(entry))
        graphFile.nodes.foreach { node =>
          if (node.kind.contains("FILE")) loc += node.properties.get("LOC").map(_.toInt).getOrElse(0)
        }
      }
    }
    zipFile.close()
    loc
  }

}
