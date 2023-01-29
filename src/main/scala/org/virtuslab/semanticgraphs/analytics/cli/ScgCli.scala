package org.virtuslab.semanticgraphs.analytics.cli

import org.virtuslab.semanticgraphs.analytics.partitions.comparison.PartitioningComparisonApp
import org.virtuslab.semanticgraphs.analytics.scg.{ProjectAndVersion, SemanticCodeGraph}
import org.virtuslab.semanticgraphs.analytics.utils.JsonUtils
import org.virtuslab.semanticgraphs.analytics.summary.SCGProjectSummary
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.virtuslab.semanticgraphs.analytics.crucial.CrucialNodes
import picocli.CommandLine
import picocli.CommandLine.{Command, HelpCommand, Parameters}
import picocli.CommandLine.Model.CommandSpec

import java.util.Locale

@Command(
  name = "iso-code-resolver",
  subcommands = Array(classOf[SubcommandAsClass], classOf[HelpCommand]),
  description = Array("Resolves ISO country codes (ISO-3166-1) or language codes (ISO 639-1/-2)")
)
class ISOCodeResolver:
  val spec: CommandSpec = null

  @Command(name = "country", description = Array("Resolves ISO country codes (ISO-3166-1)"))
  def subCommandViaMethod(
    @Parameters(
      arity = "1..*",
      paramLabel = "<countryCode>",
      description = Array("country code(s) to be resolved")
    ) countryCodes: Array[String]
  ): Unit =
    for code <- countryCodes do println(s"${code.toUpperCase()}: ".concat(new Locale("", code).getDisplayCountry))

@Command(name = "language", description = Array("Resolves language codes (ISO-639-1/-2)"))
class SubcommandAsClass extends Runnable:
  @Parameters(arity = "1..*", paramLabel = "<languageCode>", description = Array("language code(s)"))
  private val languageCodes = new Array[String](0)

  override def run(): Unit =
    for code <- languageCodes do println(s"${code.toUpperCase()}: ".concat(new Locale(code).getDisplayLanguage))

object ISOCodeResolver:

  def main(args: Array[String]): Unit =
    val exitCode = new CommandLine(new ISOCodeResolver).execute(args: _*)
    System.exit(exitCode)

@Command(
  name = "scg-cli",
  description = Array("CLI to analyse projects based on SCG data"),
  subcommands = Array(classOf[HelpCommand])
)
class ScgCli:

  @Command(name = "summary", description = Array("Summarize the project"))
  def summary(
    @Parameters(
      paramLabel = "<workspace>",
      description = Array("Workspace where SCG proto files are located in .semanticgraphs directory or zipped archive")
    )
    workspace: String
  ): Unit =
    val summary = SCGProjectSummary.summary(
      SemanticCodeGraph.read(ProjectAndVersion(workspace, workspace.split("/").last, ""))
    )
    println(summary.asJson.spaces2)

  @Command(name = "crucial", description = Array("Find crucial code entities"))
  def crucial(
     @Parameters(
       paramLabel = "<workspace>",
       description = Array("Workspace where SCG proto files are located in .semanticgraphs directory or zipped archive")
     )
     workspace: String
   ): Unit =
    val scg = SemanticCodeGraph.read(ProjectAndVersion(workspace, workspace.split("/").last, ""))
    val projectScoringSummary = CrucialNodes.analyze(scg)
    val outputFile = s"${scg.projectName}.crucial.json"
    JsonUtils.dumpJsonFile(outputFile, projectScoringSummary.asJson.toString)
    println(s"Results exported to: $outputFile")

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
    PartitioningComparisonApp.runPartitionComparison(
      ProjectAndVersion(workspace, workspace.split("/").last, ""),
      nparts
    )

object ScgCli:
  def main(args: Array[String]): Unit =
    val exitCode = new CommandLine(new ScgCli).execute(args: _*)
    System.exit(exitCode)
