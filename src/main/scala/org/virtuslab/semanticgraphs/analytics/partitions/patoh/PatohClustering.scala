package org.virtuslab.semanticgraphs.analytics.partitions.patoh

import com.virtuslab.semanticgraphs.proto.model.graphnode.GraphNode
import org.virtuslab.semanticgraphs.analytics.partitions.{PartitionHelpers, PartitionResults}
import org.virtuslab.semanticgraphs.analytics.scg.{ProjectAndVersion, SemanticCodeGraph}
import org.virtuslab.semanticgraphs.analytics.utils.MultiPrinter

import java.io.File
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

object PatohClustering extends App {

  val workspace = args(0)
  val nparts = args(1).toInt
  val projectName = workspace.split("/").last
  implicit val (tmpFolder, multiPrinter) = PartitionHelpers.multiPrinter(projectName, "patoh")

  val biggestComponentNodes =
    PartitionHelpers.takeBiggestComponentOnly(SemanticCodeGraph.readOnlyGlobalNodes(ProjectAndVersion(workspace, projectName, "")))(multiPrinter)

  val results = PatohPartitions.partition(biggestComponentNodes, projectName, nparts)

  PartitionResults.print(multiPrinter, results.sortBy(_.packageDistribution.weightedAverageAccuracy)(implicitly[Ordering[Int]].reverse))


  PartitionHelpers.exportAllToGDF(
    nparts,
    biggestComponentNodes,
    s"${tmpFolder.getAbsolutePath}/$projectName-all.gdf",
    results
  )
}

object PatohPartitions {

  def partition(nodes: List[GraphNode], projectName: String, nparts: Int)(implicit multiPrinter: MultiPrinter): List[PartitionResults] = {
    val indexes = PatohPartitions.exportPatohInputGraph(projectName, nodes)
    val result = computePatohPartitioning(nodes, indexes, nparts, projectName)
    new File(s"$projectName.patoh").delete()
    result
  }

  def computePatohPartitioning(nodes: List[GraphNode], indexes: Array[String], nparts: Int, projectName: String)(
    implicit multiPrinter: MultiPrinter
  ): List[PartitionResults] = {
    if (nparts > 1) {
      multiPrinter.println(s"Running patoh parts=$nparts")
      val computing =
        os.proc("patoh", s"$projectName.patoh", nparts, "IB=0.5", "PA=11")
          .call()

      if (computing.exitCode != 0) {
        throw new RuntimeException(s"Computation failed")
      }
      println(computing.out.text())

      val patohPartFile = s"$projectName.patoh.part.$nparts"
      val patohResults = readPatohResults(patohPartFile, indexes)
      new File(patohPartFile).delete()

      computePatohPartitioning(nodes, indexes, nparts - 1, projectName) :+ PartitionResults(
        method = "patoh",
        nodes = nodes,
        nparts = nparts,
        nodeToPart = patohResults,
        comment = computing.out.text()
      )
    } else {
      Nil
    }
  }

  def readPatohResults(file: String, indexes: Array[String]): Map[String, Int] = {
    Source
      .fromFile(file)
      .getLines()
      .toList
      .zipWithIndex
      .map { case (part, index) => (indexes(index), part.toInt) }
      .toMap
  }


  def exportPatohInputGraph(projectName: String, nodes: List[GraphNode]): Array[String] = {
    val (nodeToIndex, networks) = toNodeAndEdges(nodes)
    dumpGraph(projectName, nodeToIndex.size, networks)
    nodeToIndex.toList.sortBy(_._2).map(_._1).toArray
  }

  private def toNodeAndEdges(nodes: List[GraphNode]): (mutable.Map[String, Int], List[Set[Int]]) = {
    var counter = 0
    val nodeAndNumber = scala.collection.mutable.Map.empty[String, Int]
    def getNodeNumber(id: String): Int =
      nodeAndNumber.getOrElseUpdate(id, { counter = counter + 1; counter })

    val networks = ListBuffer.empty[Set[Int]]
    def addNetwork(network: Set[Int]): Unit =
      networks.addOne(network)
    nodes.foreach { currentNode =>
      if (currentNode.edges.nonEmpty) {
        val nodeNumber = getNodeNumber(currentNode.id)

        val callEdges = currentNode.edges.filter(_.`type` == "CALL")
        if (callEdges.nonEmpty) {
          addNetwork(callEdges.map(_.to).map(getNodeNumber).toSet + nodeNumber)
          callEdges.foreach { edge =>
            val to = getNodeNumber(edge.to)
            addNetwork(Set(to, nodeNumber))
          }
        }
        val declarationEdges = currentNode.edges.filter(_.`type` == "DECLARATION")
        if (declarationEdges.nonEmpty) {
          addNetwork(declarationEdges.map(_.to).map(getNodeNumber).toSet + nodeNumber)
        }
        val rest = currentNode.edges.filterNot(x => x.`type` == "DECLARATION" || x.`type` == "CALL")
        if (rest.nonEmpty) {
          addNetwork(rest.map(_.to).map(getNodeNumber).toSet + nodeNumber)
        }
      }
    }

    (nodeAndNumber, networks.toList)
  }

  private def dumpGraph(
    projectName: String,
    vSize: Int,
    networks: List[Set[Int]]
  ): Unit = {
    val nSize = networks.size
    val pinsSize = networks.map(_.size).sum

    val f = new File(s"$projectName.patoh")
    val printer = new java.io.PrintWriter(f)
    printer.println(s"1 $vSize $nSize $pinsSize")

    networks.foreach { network =>
      printer.println(s"${network.mkString(" ")}")
    }

    printer.close()
  }
}
