# GraphCompose Benchmarks Module

> **What this is.** A **manual performance harness** for GraphCompose —
> a small set of Java programs that render representative documents
> repeatedly and report rough numbers (latency, throughput, byte size,
> peak memory) to a JSON / CSV / text report.
>
> **What this is _not_.** A JMH-grade benchmark. There is no warmup
> control, no forked JVM, no per-measurement reset, no GC profiling
> beyond what JFR / `-verbose:gc` can pick up out-of-band. Numbers
> produced here are **rough local comparisons** suitable for "did this
> change regress something obviously?" — not for public marketing
> claims, not for cross-machine performance comparisons, and not for
> answering "how does GraphCompose compare to iText / openHTMLToPDF /
> JasperReports?" with rigour.
>
> A separate JMH layer (sibling chain Track C: B3 → B4 → B5 → B6 in the
> 1.7.0 plan) sits alongside this harness — the infrastructure and first
> benchmark have landed (B4); see [Strict JMH layer](#strict-jmh-layer).
> Treat the **manual** harness numbers below as **smoke-test fidelity, not
> benchmark fidelity**; quote the JMH layer for rigorous claims.

## When to use the harness

- **Smoke check before a release** — `CurrentSpeedBenchmark -Dgraphcompose.benchmark.profile=smoke`
  takes ~15 s, exercises the canonical render path through 5 fixture
  scenarios, and prints a single-page latency / throughput table.
  CI runs this on every PR (the `perf-smoke` job); the goal is "did
  this PR make a representative render visibly slower?" — *not* "is
  this number a publishable performance claim".

- **Pre/post comparison on a single machine** — render a fixture
  before and after a layout change, run `BenchmarkDiffTool` against
  the two JSON reports, eyeball the delta. Variance per run is in
  single-digit percent; treat deltas inside ±5 % as noise on the
  default machine and tighten the threshold only when comparing on a
  quiescent system with a fixed CPU frequency.

- **Stress / endurance check** — `GraphComposeStressTest` and
  `EnduranceTest` drive higher-cardinality fixtures over longer
  windows to catch GC pressure spikes or memory leaks that a single
  smoke run wouldn't surface. Run by hand; not on CI by default.

## When **not** to use the harness

- For a **published "X% faster than Y" claim** of any kind — the
  numbers are not statistically rigorous and the comparison setup is
  not reproducible across machines / JDKs.
- For **deciding between two architecturally different approaches** —
  pick the right invariant (allocation count, big-O of the algorithm,
  layout-pass count) and reason about it; the harness is a sanity
  check after you've already chosen, not a decision tool before.
- For **comparing GraphCompose to another PDF library** —
  `ComparativeBenchmark` does render the same fixture through iText /
  openHTMLToPDF / JasperReports for rough sizing, but the comparison
  is a manual smoke test: each library has different defaults
  (compression, font embedding, image resampling) and reading too much
  into a single number is the wrong call.

## Files in this module

| File | Role |
|---|---|
| `CurrentSpeedBenchmark` | Default scenario runner — what CI's `perf-smoke` job exercises. Takes a `-Dgraphcompose.benchmark.profile=smoke\|full\|stress` switch. |
| `ComparativeBenchmark` | Renders the same fixtures through GraphCompose, iText, openHTMLToPDF, JasperReports. **Rough local comparison only** — see "When not to use" above. |
| `FullCvBenchmark`, `ScalabilityBenchmark` | Fixture-specific runners for CV and table-heavy scenarios. |
| `CanonicalBenchmarkSupport`, `BenchmarkSupport` | Shared fixture builders + measurement helpers. |
| `BenchmarkReportWriter` | Writes JSON / CSV / text reports under `benchmarks/target/benchmarks/`. |
| `BenchmarkDiffTool` | Compares two JSON reports and prints a delta table. Useful for pre/post comparisons. |
| `BenchmarkMedianTool` | Median + dispersion across N runs of the same scenario. |
| `GraphComposeStressTest`, `EnduranceTest` | Long-running stress / endurance harnesses. |
| `GraphComposeBenchmark` | Legacy entry point preserved for one downstream caller. New work should target `CurrentSpeedBenchmark`. |

## Running

From the repo root:

```bash
# Smoke profile (~15s) — what CI runs on every PR
./mvnw -B -ntp -f benchmarks/pom.xml -DskipTests \
    exec:java \
    -Dexec.mainClass=com.demcha.compose.CurrentSpeedBenchmark \
    -Dgraphcompose.benchmark.profile=smoke

# Diff two existing report runs under the same scenario
./mvnw -B -ntp -f benchmarks/pom.xml -DskipTests \
    exec:java \
    -Dexec.mainClass=com.demcha.compose.BenchmarkDiffTool \
    -Dexec.args="current-speed"
```

Reports land in `benchmarks/target/benchmarks/<scenario>/`. The CI
`perf-smoke` job uploads the smoke directory as an artifact for every
PR run, so a regression can be diffed against the previous PR's run
without reproducing locally.

## How to read a report

The JSON shape is intentionally simple — a top-level run record with
per-scenario sub-records. Each sub-record carries:

- `avgMs`, `p50Ms`, `p95Ms`, `maxMs` — latency distribution across
  iterations within the run.
- `docsPerSec` — rough throughput; **not statistically rigorous**,
  intended only as a relative number against a sibling scenario or a
  previous run on the same machine.
- `avgKB` — average output byte size. Stable across runs on the same
  fixture; useful for catching content corruption (size shifts by
  > a few hundred bytes are usually a bug, not a benchmark fluctuation).
- `peakMB` — peak heap as observed by `MemoryMXBean`; coarse, do not
  use for memory-budget enforcement.

## Strict JMH layer

The Track C JMH layer (forked JVM, warmup + measurement, JIT-stable numbers)
lives alongside this manual harness. JMH benchmarks are annotated classes under
`com.demcha.compose.jmh`; the shade plugin builds a self-contained runner jar so
forked benchmark JVMs inherit the full classpath. Present benchmarks:
`CanonicalRender` (bare-DSL multi-section render), `TemplateCv` (the
`ModernProfessional` layered template), and `PaginatedDocument` (a multi-page
document parameterised by section count).

```bash
# Build the runner jar
./mvnw -B -ntp -f benchmarks/pom.xml clean package -DskipTests

# Run all JMH benchmarks (real config: forked, warmup + measurement)
java -jar benchmarks/target/benchmarks.jar

# Run one benchmark with a quick ad-hoc config
java -jar benchmarks/target/benchmarks.jar CanonicalRender -f 1 -wi 2 -i 3
```

An `exec:java` run **cannot** fork (the child JVM loses the project classpath),
so always run JMH through the jar. Quote the JMH numbers — not the manual
harness numbers — for any public performance claim.

## Roadmap

The 1.7.0 plan (Track C, B3 → B4 → B5 → B6) introduces a sibling JMH
layer:

- **B3** — pull fixtures into a `fixtures/` package with deterministic
  seeds so the JMH layer can reuse them.
- ✅ **B4** — JMH infrastructure (`jmh-core`, `jmh-generator-annprocess`,
  shade runner jar) + first benchmark (`CanonicalRenderJmhBenchmark`). **Landed.**
- 🟡 **B5** — JMH benchmarks landed: `CanonicalRender`, `TemplateCv`,
  `PaginatedDocument`. Invoice / large-table coverage to follow.
- ✅ **B6** — CI job ([`benchmarks-jmh.yml`](../.github/workflows/benchmarks-jmh.yml))
  runs the JMH layer on a `workflow_dispatch` / weekly cadence and uploads the
  `*.json` report as an artifact. Decoupled from the main CI pipeline so forked
  runs never gate PRs. **Landed.**

Once that chain is in place, any *public* performance claim should
quote the JMH layer's numbers, with explicit warmup / measurement /
fork configuration in the source. This manual harness will stay for
the smoke / diff / endurance roles described above.

---

*This page is the source of truth for what the manual benchmark layer
is and is not. When in doubt — and especially before quoting a number
in a public communication — re-read the "When not to use" section.*
