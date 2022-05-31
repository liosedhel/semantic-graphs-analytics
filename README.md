# Semantic Code Graph analytics

## Metadata

All the metadata analysed is in `./data` folder

## Export to GDF

```bash
sbt "runMain org.virtuslab.semanticgraphs.analytics.exporters.ExportToGdf"
```

## Find crucial elements in the source code

It will analyse the .semanticgraphs files, print and export appropriate .json files:
```bash
sbt "runMain org.virtuslab.semanticgraphs.analytics.crucial.CrucialNodesApp data/akka"
```

or run for all the projects
```bash
sbt "runMain org.virtuslab.semanticgraphs.analytics.crucial.CrucialNodesAnalyzeAll"
```

## Partition the graph

`gpmetis` program has to exist

```bash
sbt "runMain org.virtuslab.semanticgraphs.analytics.partitions.gpmetis.GpmetisPartitionsApp data/akka 4"
```

## Partitioning the hypergraph

`patoh` program has to exist

```bash
sbt "runMain org.virtuslab.semanticgraphs.analytics.partitions.patoh.PatohClustering data/akka 4"
```

## Partitioning comparison

```bash
sbt "runMain org.virtuslab.semanticgraphs.analytics.partitions.comparison.PartitioningComparisonApp data/akka 4"
```


## LOC of original project

```bash
$ find /Users/kborowski/phd/commons-io -type f -not -path '*/.*/*' | egrep --color=never '.*\.(scala|java)$' | egrep --color=never -v '.*/target/.*' | xargs wc -l | tail -n1 | awk '{print $1}'
```
