package org.virtuslab.semanticgraphs.analytics.partitions.comparison

import org.virtuslab.semanticgraphs.analytics.partitions.gpmetis.GpmetisPartitions
import org.virtuslab.semanticgraphs.analytics.partitions.patoh.PatohPartitions
import org.virtuslab.semanticgraphs.analytics.partitions.{PartitionHelpers, PartitionResults}
import org.virtuslab.semanticgraphs.analytics.scg.{ProjectAndVersion, SemanticCodeGraph}
import org.virtuslab.semanticgraphs.analytics.utils.MultiPrinter

object PartitioningComparisonApp:

  def runAndPrinterPartitionComparison(projectAndVersion: ProjectAndVersion, nparts: Int) =
    val projectName = projectAndVersion.projectName
    val (comparisonTmp, comparisonPrinter) = PartitionHelpers.multiPrinter(projectName, "comparison")
    comparisonPrinter.println(s"Computing graph ${projectAndVersion.workspace}, $nparts with project name $projectName")
    val results = runPartitionComparison(projectAndVersion, nparts)
    PartitionResults.print(comparisonPrinter, results)

  def runPartitionComparison(projectAndVersion: ProjectAndVersion, nparts: Int) =
    val projectName = projectAndVersion.projectName

    val biggestComponentNodes =
      PartitionHelpers.takeBiggestComponentOnly(SemanticCodeGraph.readOnlyGlobalNodes(projectAndVersion))

    val gpmetisResults = GpmetisPartitions.partition(biggestComponentNodes, projectName, nparts)
    val patohResults = PatohPartitions.partition(biggestComponentNodes, projectName, nparts)

    (gpmetisResults ::: patohResults).sortBy(x => (x.nparts, x.method))
