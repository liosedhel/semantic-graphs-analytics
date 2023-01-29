package org.virtuslab.semanticgraphs.analytics.partitions.comparison

import org.virtuslab.semanticgraphs.analytics.partitions.gpmetis.GpmetisPartitions
import org.virtuslab.semanticgraphs.analytics.partitions.patoh.PatohPartitions
import org.virtuslab.semanticgraphs.analytics.partitions.{PartitionHelpers, PartitionResults}
import org.virtuslab.semanticgraphs.analytics.scg.{ProjectAndVersion, SemanticCodeGraph}

object PartitioningComparisonApp:

  def runPartitionComparison(projectAndVersion: ProjectAndVersion, nparts: Int) =
    val projectName = projectAndVersion.projectName

    val (comparisonTmp, comparisonPrinter) = PartitionHelpers.multiPrinter(projectName, "comparison")
    comparisonPrinter.println(s"Computing graph ${projectAndVersion.workspace}, $nparts with project name $projectName")

    val biggestComponentNodes =
      PartitionHelpers.takeBiggestComponentOnly(
        SemanticCodeGraph.readOnlyGlobalNodes(projectAndVersion)
      )(comparisonPrinter)

    val gpmetisResults = GpmetisPartitions.partition(biggestComponentNodes, projectName, nparts)(comparisonPrinter)
    val patohResults = PatohPartitions.partition(biggestComponentNodes, projectName, nparts)(comparisonPrinter)

    val results = (gpmetisResults ::: patohResults).sortBy(x => (x.nparts, x.method))
    PartitionResults.print(comparisonPrinter, results)
