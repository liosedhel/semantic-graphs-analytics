package org.virtuslab.semanticgraphs.analytics.partitions

import com.virtuslab.semanticgraphs.proto.model.graphnode.{Edge, GraphNode}
import org.junit.Assert.{assertEquals, assertTrue}
import org.virtuslab.semanticgraphs.analytics.partitions.gpmetis.SpectralGraphUtils
import org.virtuslab.semanticgraphs.analytics.scg.SemanticCodeGraph

class ExportToSpectralGraphTest {

  def t2(): Unit = {
    val node1 = GraphNode("node1", edges = Seq(Edge(to = "node2"), Edge(to = "node3")))
    val node2 = GraphNode("node2", edges = Seq(Edge(to = "node1")))
    val node3 = GraphNode("node3", edges = Seq(Edge(to = "node2"), Edge(to = "node4"), Edge(to = "node5")))
    val node5 = GraphNode("node5", edges = Seq())
    val (_, nodeAndEdges) = SpectralGraphUtils.toNodeAndEdges(List(node1, node2, node3, node5))
    assertEquals(SpectralGraphUtils.countNodesAndEdges(nodeAndEdges), (5, 5))

    println(s"${nodeAndEdges.size} ${nodeAndEdges.values.foldLeft(0)((result, edges) => result + edges.size) / 2}")
    println(nodeAndEdges)
  }

  def commonsIo(): Unit = {
    val nodes = SemanticCodeGraph.read(SemanticCodeGraph.commonsIO)
    val (_, nodesAndEdges) = SpectralGraphUtils.toNodeAndEdges(nodes.nodes)
    nodesAndEdges.foreach { case (nodeId, edges) =>
      edges.foreach { edge =>
        assertTrue(nodesAndEdges(edge).contains(nodeId))
      }
    }
    val (nodesNumber, edgesNumber) = SpectralGraphUtils.countNodesAndEdges(nodesAndEdges)
    assertEquals(nodesNumber, 2549)
    assertEquals(edgesNumber, 7005)
  }
}

