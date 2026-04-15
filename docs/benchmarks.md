# Benchmark Workflow

This document explains the local and CI benchmark flow used in GraphCompose.

The short version is:

- `scripts/run-benchmarks.ps1` is the normal local entry point
- `CurrentSpeedBenchmark` has two profiles: `smoke` and `full`
- current-speed diffs are only valid between reports from the same profile
- repeated local runs should be compared via median aggregation, not by eyeballing one lucky run

If you are changing layout, pagination, render ordering, PDF session lifetime, or benchmark tooling, read this file together with [README.md](./../README.md), [architecture.md](./architecture.md), and [CONTRIBUTING.md](./../CONTRIBUTING.md).

## Core terms

- `suite`: one benchmark family such as `current-speed` or `comparative`
- `profile`: a current-speed mode. Today that means `smoke` or `full`
- `run`: one timestamped JSON/CSV result written as `run-<timestamp>.json`
- `aggregate`: a median report built from several repeated local runs
- `compatible pair`: two reports that can be diffed safely. For `current-speed`, compatibility means the same profile

## The local benchmark entry point

The default local workflow is:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1
```

That wrapper is intentionally opinionated. It does more than just invoke one Java main class.

### Pipeline stages

The script prints numbered sections so you can map console output to the pipeline:

1. `01-build-classpath`
   Builds the test classpath once and writes `target/benchmark.classpath`.
2. `02-current-speed`
   Runs `CurrentSpeedBenchmark` in the selected profile.
3. `03-comparative`
   Runs the low-level GraphCompose vs iText 5 vs JasperReports comparison.
4. `04-core-engine`
   Runs `GraphComposeBenchmark`.
5. `05-full-cv`
   Runs `FullCvBenchmark`.
6. `06-scalability`
   Runs the thread-scaling throughput benchmark.
7. `07-stress`
   Runs the concurrent stability stress test.
8. `08-endurance`
   Optional. Runs only when `-IncludeEndurance` is provided.
9. `09-diff-current-speed`
   Diffs the newest compatible current-speed reports.
10. `10-diff-comparative`
   Diffs the two newest comparative reports.

Each step writes a dedicated log file under `target/benchmark-runs/<timestamp>/logs/`, and the wrapper mirrors that log back to the console after the step finishes.

## Current-speed profiles

`CurrentSpeedBenchmark` supports two intended usage modes:

- `smoke`
  Bounded latency-oriented checks for pull requests and quick local spot checks.
- `full`
  Wider warmup and measurement windows plus throughput coverage for local investigation and scheduled runs.

Use the same profile when comparing results. A `smoke` report and a `full` report are different experiments, not two samples of the same one.

Examples:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1 -CurrentSpeedProfile smoke
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1 -CurrentSpeedProfile full
```

## Diff selection rules

### Current-speed diffs

For current-speed reports, the wrapper now selects the newest pair that matches the profile of the latest run.

That means:

- if the newest run is `full`, the script looks for the newest previous `full` run
- if the newest run is `smoke`, the script looks for the newest previous `smoke` run
- if there is no second run with that profile yet, the diff step is skipped instead of failing the whole benchmark run

This mirrors the rule enforced by `BenchmarkDiffTool`: current-speed reports with different profiles must not be diffed.

### Comparative diffs

Comparative reports do not have the same profile split, so the wrapper simply diffs the two newest comparative runs.

### Repeated local runs

When you pass `-Repeat N`, the wrapper reruns:

- `current-speed`
- `comparative`

After that, it writes median aggregate reports and diffs median-vs-median on later runs. This is the preferred mode for local decision-making because it reduces noise from GC, background processes, JIT warmup differences, and filesystem activity.

Example:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1 -CurrentSpeedProfile full -Repeat 5
```

## Recommended local workflows

### Quick spot check before a small change

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1 -CurrentSpeedProfile smoke
```

### Normal local investigation

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1 -CurrentSpeedProfile full
```

### Safer local comparison after a performance-sensitive change

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1 -CurrentSpeedProfile full -Repeat 5
```

### Run benchmarks but skip diffs

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1 -SkipDiff
```

### Open the generated summary and benchmark folder after the run

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-benchmarks.ps1 -OpenResults
```

## Artifact layout

The wrapper writes two groups of artifacts.

### Run-level logs and summaries

- `target/benchmark-runs/<timestamp>/SUMMARY.md`
- `target/benchmark-runs/<timestamp>/logs/*.log`

These are the best place to look when one numbered step fails.

### Persistent benchmark reports

- `target/benchmarks/current-speed/`
- `target/benchmarks/comparative/`
- `target/benchmarks/diffs/`
- `target/benchmarks/aggregates/`

Typical contents:

- `run-<timestamp>.json`
- suite-specific CSV exports
- `latest.json` convenience copies
- median aggregate reports under `aggregates/...`

## Running the Java entry points directly

The PowerShell wrapper is preferred, but direct runs are still useful when debugging one suite in isolation.

Build the classpath first:

```powershell
mvn --% -B -ntp -DskipTests test-compile dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=target/benchmark.classpath
$cp = (Get-Content 'target/benchmark.classpath' -Raw).Trim()
```

Then run the suite you care about:

```powershell
java -cp "target\test-classes;target\classes;$cp" com.demcha.compose.CurrentSpeedBenchmark
java -Dgraphcompose.benchmark.profile=smoke -cp "target\test-classes;target\classes;$cp" com.demcha.compose.CurrentSpeedBenchmark
java -cp "target\test-classes;target\classes;$cp" com.demcha.compose.ComparativeBenchmark
java -cp "target\test-classes;target\classes;$cp" com.demcha.compose.BenchmarkDiffTool current-speed
java -cp "target\test-classes;target\classes;$cp" com.demcha.compose.BenchmarkDiffTool comparative
```

Use the suite shortcut when possible. `BenchmarkDiffTool current-speed` already knows how to select the newest compatible pair for the current-speed suite.

## Troubleshooting

### Why did `09-diff-current-speed` skip?

Because there were not yet two current-speed reports with the same profile as the latest run.

Example:

- latest run is `full`
- historical reports contain only one `full` run and several `smoke` runs
- result: the diff is skipped because there is no compatible pair yet

### Why do I see a scary `sun.misc.Unsafe` warning during `01-build-classpath`?

Today that warning comes from Lombok on newer JDKs. If the Maven section still ends with `BUILD SUCCESS`, treat it as noisy stderr, not as a benchmark failure.

### Why did one local run get much slower even though the code did not obviously change?

Local benchmark numbers are sensitive to machine conditions:

- background CPU load
- OneDrive or antivirus activity
- thermal throttling
- JVM warmup differences
- GC timing

Do not call a one-off slowdown a code regression until repeated runs show the same direction.

### Which numbers should I cite in docs or release notes?

Prefer rerunning the relevant suite on the current checkout. For local claims, median-based repeated runs are safer than one-off results.

## Maintenance rules

When changing the benchmark pipeline:

- keep `README.md` aligned with the supported command line
- update this file when the wrapper flow, artifact layout, or diff rules change
- keep current-speed profile semantics explicit in user-facing docs
- preserve the rule that incompatible current-speed profiles must never be diffed
