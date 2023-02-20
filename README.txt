# Semantic Code Graph analytics

## Using scg-cli

Build the `scg-cli` with:
```bash
$ sbt stage
```

and follow the `scg-cli` help page:

```bash
$ ./target/universal/stage/bin/scg-cli help  
Usage: scg-cli [COMMAND]
CLI to analyse projects based on SCG data
Commands:
  help       Display help information about the specified command.
  crucial    Find crucial code entities
  generate   Generate SCG metadata
  partition  Suggest project partitioning.
  summary    Summarize the project
```

### Example 

In `data` folder you can find extracted and zipped `*.semanticgraphs` files. You can try to analyse them with:

```bash
$ ./target/universal/stage/bin/scg-cli summary data/metals.zip 
```

## Using on your java project

First the metadata for the project has to be generated
```bash
$ ./target/universal/stage/bin/scg-cli generate path/to/project
```

It will take a moment to generate the metadata. Then you can start to analyse your project:
```bash
$ ./target/universal/stage/bin/scg-cli summary path/to/project
```