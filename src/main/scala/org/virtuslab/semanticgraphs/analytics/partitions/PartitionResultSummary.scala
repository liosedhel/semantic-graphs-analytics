package org.virtuslab.semanticgraphs.analytics.partitions

import org.virtuslab.semanticgraphs.analytics.crucial.CrucialNodes.getClass
import org.virtuslab.semanticgraphs.analytics.crucial.CrucialNodesSummary
import io.circe.generic.auto.*
import io.circe.syntax.*

import java.nio.file.{Files, Path, StandardCopyOption}

//|Method |NPart|Modularity|Coefficient|Weighted|Accuracy|Weighted|Accuracy|Variance|Distribution
case class AverageAccuracy(weighted: Int, standard: Int)
case class ShortSummary(
  method: String,
  npart: Int,
  modularity: Double,
  coefficient: Double,
  file: AverageAccuracy,
  `package`: AverageAccuracy,
  variance: Double,
  distribution: String
)

case class PartitionPackageSummary(method: String, part: Int, accuracy: Int, `package`: String, distribution: String)
case class PartitionFileSummary(method: String, part: Int, accuracy: Int, file: String, distribution: String)

case class PartitionResultsSummary(
  nparts: Int,
  byPackage: List[PartitionPackageSummary],
  byFile: List[PartitionFileSummary]
)

case class PartitionResultsWrapper(results: List[PartitionResultsSummary], summary: List[ShortSummary])

object PartitionResultsSummary:

  def apply(partitionResults: List[PartitionResults]): PartitionResultsWrapper = PartitionResultsWrapper(
    results = partitionResults.map { result =>
      val byPackage =
        for partitionResults <- result.packageDistribution.groupPartitionStats
        yield PartitionPackageSummary(
          result.method,
          partitionResults.partition,
          partitionResults.accuracy,
          partitionResults.partitionName,
          partitionResults.distribution.mkString("[", ",", "]")
        )
      val byFile =
        for partitionResults <- result.fileDistribution.groupPartitionStats
        yield PartitionFileSummary(
          result.method,
          partitionResults.partition,
          partitionResults.accuracy,
          partitionResults.partitionName,
          partitionResults.distribution.mkString("[", ",", "]")
        )
      PartitionResultsSummary(result.nparts, byPackage, byFile)
    },
    summary = partitionResults.map { result =>
      ShortSummary(
        result.method,
        result.nparts,
        result.modularityRatio,
        result.clusteringCoefficient,
        AverageAccuracy(
          result.fileDistribution.weightedAverageAccuracy,
          result.fileDistribution.arithmeticAverageAccuracy
        ),
        AverageAccuracy(
          result.packageDistribution.weightedAverageAccuracy,
          result.packageDistribution.arithmeticAverageAccuracy
        ),
        result.distributionVariance,
        result.globalNodesDistribution.map(i => i * 100 / result.nodes.size).mkString("[", ",", "]%")
      )
    }
  )

  def exportHtmlSummary(summary: PartitionResultsWrapper): Unit = {
    exportJsSummary("partition.js", summary)
    copySummaryHtml(Path.of("."))
  }

  private def exportJsSummary(fileName: String, summary: PartitionResultsWrapper): Unit = {
    import java.io._
    val pw = new PrintWriter(new File(fileName))
    val json = s"const partition = ${summary.asJson.spaces2};"
    pw.write(json)
    pw.close()
  }

  private def copySummaryHtml(summaryResultDirectory: Path): Unit = {
    val inputStream = getClass.getClassLoader.getResourceAsStream("partition.html")
    Files.copy(inputStream, summaryResultDirectory.resolve("partition.html"), StandardCopyOption.REPLACE_EXISTING)
    inputStream.close()
  }
