package org.virtuslab.semanticgraphs.analytics.exporters

import com.virtuslab.semanticgraphs.proto.model.graphnode._
import org.virtuslab.semanticgraphs.analytics.utils.PathHelpers.StringResolver

import java.io.File
import java.nio.file.{FileSystems, Files}
import scala.collection.mutable
import org.virtuslab.semanticgraphs.analytics.utils._

object ExportToGdf extends App {
  import Helpers._
  def exportToGdf(workspace: String): Unit = {
    val output = "fullgraph.gdf"

    val dir = workspace.resolve(Common.SEMANTIC_GRAPHS_DIR)

    val nodes = scala.collection.mutable.ListBuffer.empty[GraphNode]
    Files
      .walk(dir)
      .forEach { path =>
        if (
          Files
            .isRegularFile(path) && path.toString.endsWith(
            Common.SEMANTIC_GRAPH_FILE_SUFFIX
          )
        ) {
          val graphFile = SemanticGraphFile.parseFrom(Files.readAllBytes(path))
          nodes ++= graphFile.nodes.filter(_.location.isDefined)
        }

      }
    dumpGraph(output, nodes)
  }
}

object ExportCallGraph extends App {
  import Helpers._

  def exportCallGraph(workspace: String): Unit = {
    val output = "callgraph.gdf"

    val dir = workspace.resolve(Common.SEMANTIC_GRAPHS_DIR)

    val nodes = scala.collection.mutable.ListBuffer.empty[GraphNode]

    Files
      .walk(dir)
      .forEach { path =>
        if (
          Files
            .isRegularFile(path) && path.toString.endsWith(
            Common.SEMANTIC_GRAPH_FILE_SUFFIX
          )
        ) {
          val graphFile = SemanticGraphFile.parseFrom(Files.readAllBytes(path))
          nodes ++= graphFile.nodes
            .filter(node =>
              node.kind.nonEmpty && (node.kind == "METHOD" || node.edges
                .exists(_.`type` == "CALL"))
            )
        }

      }
    dumpGraph(output, nodes)
  }
}

object Helpers {

  /**
    * Export graph to standard .GDF format, see: https://gephi.org/users/supported-graph-formats/gdf-format/
    */
  def dumpGraph(
    outputFileName: String,
    nodes: mutable.ListBuffer[GraphNode]
  ): Unit = {
    val f = new File(outputFileName)
    val printer = new java.io.PrintWriter(f)

    printer.println(
      "nodedef> name VARCHAR, label VARCHAR, kind VARCHAR, uri VARCHAR, loc INTEGER"
    )
    nodes.filter(node => node.kind.nonEmpty && node.location.isDefined).foreach { node =>
      import node._
      printer.println(
        s"$id, $displayName, $kind, ${location
          .map(_.uri)
          .getOrElse("")}, ${properties.get("LOC").map(_.toInt).getOrElse(0)}"
      )
    }
    printer.println(
      "edgedef> source VARCHAR, target VARCHAR, type VARCHAR, directed BOOLEAN, uri VARCHAR, label VARCHAR"
    )
    nodes.foreach { node =>
      node.edges.filter(_.location.isDefined).foreach { edge =>
        import edge._
        printer.println(
          s"${node.id}, $to, ${`type`}, true, ${location.map(_.uri).getOrElse("")}, ${`type`}"
        )
      }
    }

    printer.close()
  }

}
