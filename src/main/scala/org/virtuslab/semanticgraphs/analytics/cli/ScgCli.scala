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
import org.virtuslab.semanticgraphs.analytics.exporters.JupyterNotebook
import org.virtuslab.semanticgraphs.analytics.partitions.{EdgeDTO, GraphNodeDTO, LocationDTO, PartitionResults, PartitionResultsSummary}
import picocli.CommandLine
import picocli.CommandLine.{Command, HelpCommand, Option, Parameters}
import picocli.CommandLine.Model.CommandSpec

import java.io.{File, PrintWriter}
import java.util.Locale
import upickle.default.*
import upickle.default.{macroRW, ReadWriter as RW}

import java.nio.file.{Files, Path}

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
    workspace: String,
    @Option(
      names = Array("-o", "--output"),
      description = Array("Output format: html, json, txt"),
      arity = "0..1",
      defaultValue = "html"
    )
    output: String,
    @Option(
      names = Array("-m", "--mode"),
      description = Array("Analysis mode: quick, full"),
      arity = "0..1",
      defaultValue = "quick"
    )
    mode: String
  ): Unit =
    val scg = SemanticCodeGraph.read(ProjectAndVersion(workspace, workspace.split("/").last, ""))
    val summary = CrucialNodes.analyze(scg, mode == "quick")
    output match {
      case "html" =>
        CrucialNodes.exportHtmlSummary(summary)
      case "json" =>
        val outputFile = s"${scg.projectName}.crucial.json"
        JsonUtils.dumpJsonFile(outputFile, summary.asJson.toString)
        println(s"Results exported to: $outputFile")
      case "txt" =>
        println(summary.asJson.spaces2)
    }

  case class PartitionResult(results: List[PartitionResults])
  object PartitionResult:
    given RW[EdgeDTO] = macroRW
    given RW[LocationDTO] = macroRW
    given RW[GraphNodeDTO] = macroRW
    given RW[PartitionResults] = macroRW
    given RW[PartitionResult] = macroRW

  @Command(name = "partition", description = Array("Suggest project partitioning."))
  def partition(
    @Parameters(
      paramLabel = "<workspace>",
      description = Array("Workspace where SCG proto files are located in .semanticgraphs directory or zipped archive")
    )
    workspace: String,
    @Parameters(paramLabel = "<nparts>", description = Array("Up to how many partitions split the project"))
    nparts: Int,
    @Option(
      names = Array("-o", "--output"),
      description = Array("Output format: html, json, txt, default: ${DEFAULT-VALUE}"),
      arity = "0..1",
      defaultValue = "html"
    )
    output: String,
    @Option(
      names = Array("--use-docker"),
      description = Array("Use partition gpmetis/patoh programs through docker image, default: ${DEFAULT-VALUE}"),
      arity = "0..1",
      defaultValue = "false"
    )
    useDocker: Boolean
  ): Unit =
    val projectAndVersion = ProjectAndVersion(workspace, workspace.split("/").last, "")
    val results = PartitioningComparisonApp.runPartitionComparison(
      projectAndVersion,
      nparts,
      useDocker
    )
    output match {
      case "html" =>
        PartitionResultsSummary.exportHtmlSummary(PartitionResultsSummary(results))
      case "json" =>
        val outputFile = s"${projectAndVersion.projectName}.partition.json"
        JsonUtils.dumpJsonFile(outputFile, write(PartitionResult(results)))
        println(s"Results exported to: $outputFile")
      case "txt" =>
        PartitionResults.print(
          new MultiPrinter(
            new PrintWriter(System.out),
            new PrintWriter(new PrintWriter(new File(s"${workspace.replace("/", "-")}.partition.txt")))
          ),
          results
        )
      case "tex" =>
        println(PartitionResultsSummary.exportTex(PartitionResultsSummary(results)))
    }

  @Command(name = "export", description = Array("Export SCG metadata for further analysis"))
  def `export`(
    @Parameters(
      paramLabel = "<workspace>",
      description = Array("Workspace where SCG proto files are located in .semanticgraphs directory or zipped archive")
    )
    workspace: String,
    @Option(
      names = Array("-o", "--output"),
      description = Array("Output format: html, json, txt, default: ${DEFAULT-VALUE}"),
      arity = "0..1",
      defaultValue = "jupyter"
    )
    output: String
  ): Unit =
    val projectAndVersion = ProjectAndVersion(workspace, workspace.split("/").last, "")
    output match {
      case "jupyter" =>
        JupyterNotebook.runJupyterNotebook(projectAndVersion.workspace)
    }


object ScgCli:
  def main(args: Array[String]): Unit =
    // Files.createDirectories(Path.of(".scg"))
    val exitCode = new CommandLine(new ScgCli).execute(args: _*)
    System.exit(exitCode)
