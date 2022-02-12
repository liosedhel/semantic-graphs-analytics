package org.virtuslab.semanticgraphs.analytics.spark

import com.virtuslab.semanticgraphs.proto.model.graphnode.SemanticGraphFile

import java.nio.file.Files
import org.virtuslab.semanticgraphs.analytics.utils._

class SparkBasedStats {

  import org.apache.spark._
  import org.apache.spark.graphx._
  // To make some of the examples work we will also need RDD
  import org.apache.spark.rdd.RDD

  val spark = SparkSession.builder.appName("Simple Application").getOrCreate()

  def fetch(workspace: String): Unit = {
    val dir = workspace.resolve(".semanticgraphs")
    Files
      .walk(dir)
      .iterator()
      .forEachRemaining { path =>
        if (
          Files
            .isRegularFile(path) && path.toString.endsWith(".semanticgraphdb")
        ) {
          val graphFile = SemanticGraphFile.parseFrom(Files.readAllBytes(path))
          graphFile.nodes
            .filter(node => node.kind.nonEmpty && node.location.isDefined)
            .foreach { node =>
              //addVertex(node)
              node.edges
                .filter(_.location.isDefined)
                .foreach(edge => edge)
            }
        }
      }
  }

}
