package org.virtuslab.semanticgraphs.analytics.cli

import ch.qos.logback.classic.Logger
import com.virtuslab.semanticgraphs.javaparser.JavaParserMain
import com.virtuslab.semanticgraphs.parsercommon.logger.LoggerFactory
import org.virtuslab.semanticgraphs.analytics.partitions.comparison.PartitioningComparisonApp
import org.virtuslab.semanticgraphs.analytics.scg.{ProjectAndVersion, SemanticCodeGraph}
import org.virtuslab.semanticgraphs.analytics.utils.{JsonUtils, MultiPrinter}
import org.virtuslab.semanticgraphs.analytics.summary.SCGProjectSummary
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.virtuslab.semanticgraphs.analytics.crucial.CrucialNodes
import org.virtuslab.semanticgraphs.analytics.partitions.PartitionResults
import picocli.CommandLine
import picocli.CommandLine.{Command, HelpCommand, Parameters}
import picocli.CommandLine.Model.CommandSpec

import java.io.{File, PrintWriter}
import java.util.Locale

@Command(
  name = "scg-cli",
  description = Array("CLI to analyse projects based on SCG data"),
  subcommands = Array(classOf[HelpCommand])
)
class ScgCli:

  @Command(name = "generate", description = Array("Generate SCG metadata"))
  def generate(
     @Parameters(
       paramLabel = "<workspace>",
       description = Array("Workspace where SCG proto files are located in .semanticgraphs directory or zipped archive")
     )
     workspace: String
   ): Unit =
    println(s"Generating SCG metadata for $workspace")
    JavaParserMain.generateSemanticGraphFiles(workspace)
    println(s"SCG was generated to $workspace/.semanticgraphs")

  @Command(name = "summary", description = Array("Summarize the project"))
  def summary(
    @Parameters(
      paramLabel = "<workspace>",
      description = Array("Workspace where SCG proto files are located in .semanticgraphs directory or zipped archive")
    )
    workspace: String
  ): Unit =
    val scg = SemanticCodeGraph.read(ProjectAndVersion(workspace, workspace.split("/").last, ""))
    val summary = SCGProjectSummary.summary(scg)
    SCGProjectSummary.exportHtmlSummary(summary)

  @Command(name = "crucial", description = Array("Find crucial code entities"))
  def crucial(
    @Parameters(
      paramLabel = "<workspace>",
      description = Array("Workspace where SCG proto files are located in .semanticgraphs directory or zipped archive")
    )
    workspace: String
  ): Unit =
    val scg = SemanticCodeGraph.read(ProjectAndVersion(workspace, workspace.split("/").last, ""))
    val summary = CrucialNodes.analyze(scg)
    val outputFile = s"${scg.projectName}.crucial.json"
    JsonUtils.dumpJsonFile(outputFile, summary.asJson.toString)
    println(s"Results exported to: $outputFile")
    CrucialNodes.exportHtmlSummary(summary)

  @Command(name = "partition", description = Array("Suggest project partitioning."))
  def partition(
    @Parameters(
      paramLabel = "<workspace>",
      description = Array("Workspace where SCG proto files are located in .semanticgraphs directory or zipped archive")
    )
    workspace: String,
    @Parameters(paramLabel = "<nparts>", description = Array("Up to how many partitions split the project"))
    nparts: Int
  ): Unit =
    val results = PartitioningComparisonApp.runPartitionComparison(
      ProjectAndVersion(workspace, workspace.split("/").last, ""),
      nparts
    )
    PartitionResults.print(
      new MultiPrinter(
        new PrintWriter(System.out),
        new PrintWriter(new PrintWriter(new File(s"${workspace.replace("/", "-")}.partition.txt")))
      ),
      results
    )

object ScgCli:
  def main(args: Array[String]): Unit =
    val exitCode = new CommandLine(new ScgCli).execute(args: _*)
    System.exit(exitCode)
