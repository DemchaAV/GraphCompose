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
  takes ~15 s, exercises the canonical render path through 7 fixture
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
  `ComparativeBenchmark` does render equivalent content through iText /
  JasperReports for rough sizing (two tiers: a tiny single-page invoice
  for fixed overhead, and a multi-page report — title + 40-row table +
  prose — for realistic work), but the comparison is a manual smoke test:
  each library has different defaults (compression, font embedding, image
  resampling) and reading too much into a single number is the wrong call.
  Note one boundary asymmetry: the JasperReports figure measures fill +
  PDF export with the design compiled once outside the loop, while the
  GraphCompose and iText figures include per-iteration document
  construction — so the Jasper number excludes work the other two pay.
  `openHTMLtoPDF` is intentionally absent: its current release (1.0.10)
  targets PDFBox 2.x and fails at runtime against the PDFBox 3.x this
  project uses (no PDFBox-3-compatible openhtmltopdf release exists yet),
  so it cannot share GraphCompose's classpath.

## What runs on a PR — and what is on-demand (by design)

The per-PR CI gate is deliberately light and deterministic:

- **`perf-smoke` job** — `CurrentSpeedBenchmark` in the `smoke` profile with
  absolute latency / heap thresholds (a gross-regression tripwire), plus the
  module's deterministic gate tests (`mvnw -f benchmarks/pom.xml test`:
  image-cache reuse, render-operator coalescing, scenario/threshold coverage).

These are intentionally **not** on the per-PR path:

- **The JMH benches** (`*JmhBenchmark`) are full / on-demand only. A forked,
  warmed JMH run of the whole suite takes minutes; running it per PR is too
  expensive for the signal. Run them by hand (or on a schedule) before a release
  and quote those numbers for rigorous claims.
- **The relative `BenchmarkVerdictTool` gate** (±% vs a committed baseline) runs
  locally only, and no static `smoke` baseline is committed: absolute timings are
  machine-specific, so a baseline captured on one machine would false-positive on
  another. Use a local same-machine A/B (a `-Repeat` median before/after) for
  relative comparison; the absolute smoke thresholds are the CI safety net.

## Files in this module

| File | Role |
|---|---|
| `CurrentSpeedBenchmark` | Default scenario runner — what CI's `perf-smoke` job exercises. Takes a `-Dgraphcompose.benchmark.profile=smoke\|full\|stress` switch. |
| `ComparativeBenchmark` | Renders equivalent content through GraphCompose, iText, JasperReports — two tiers (small invoice + multi-page report), and dumps a sample PDF per library/scenario. **Rough local comparison only** — see "When not to use" above. |
| `CanonicalBenchmarkSupport`, `BenchmarkSupport` | Shared fixture builders + measurement helpers. |
| `BenchmarkReportWriter` | Writes JSON / CSV / text reports under `benchmarks/target/benchmarks/`. |
| `BenchmarkDiffTool` | Compares two JSON reports and prints a delta table. Useful for pre/post comparisons. |
| `BenchmarkMedianTool` | Median + dispersion across N runs of the same scenario. |
| `GraphComposeStressTest`, `EnduranceTest` | Long-running stress / endurance harnesses. |

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
per-scenario sub-records. The latency rows carry these fields (the JSON
keys are camelCase; the CSV columns are the snake_case equivalents):

- `avgMillis`, `p50Millis`, `p95Millis`, `maxMillis` — latency distribution
  across iterations within the run.
- `docsPerSecond` — a **derived** figure, `1000 / avgMillis`: the reciprocal of
  average latency, **not** a measured throughput rate. Real parallel throughput
  lives in the separate `throughput[]` section (full profile only). Treat it as
  a relative number against a sibling scenario or a previous run on the same
  machine, not a publishable rate.
- `avgKilobytes` — average output byte size. Stable across runs on the same
  fixture; useful for catching content corruption (size shifts by more than a
  few hundred bytes are usually a bug, not a benchmark fluctuation).
- `peakHeapMb` — used-heap **delta** over the post-warmup baseline (closer to
  per-iteration allocation pressure than to absolute live heap). GC-timing
  noisy, so **advisory only** — for a deterministic memory signal use the
  allocation bytes from `MeasurementCountBenchmark` or the alloc probes.

A `stages[]` array carries the per-template-scenario compose / layout / render
median split (`composeMillis` / `layoutMillis` / `renderMillis` / `totalMillis`),
present when the run has enough measurement iterations.

## Strict JMH layer

The Track C JMH layer (forked JVM, warmup + measurement, JIT-stable numbers)
lives alongside this manual harness. JMH benchmarks are annotated classes under
`com.demcha.compose.jmh`; the shade plugin builds a self-contained runner jar so
forked benchmark JVMs inherit the full classpath. The suite spans steady-state
render benches (`CanonicalRender`, `TemplateCv`, `Chart`, `ChartVariant`, `Image`,
`MixedShowcase`), parameterised scaling ramps (`IconRamp`, `LargeTable`,
`SparklineRamp`, `PaginatedDocument`, `VectorPaint`), the SVG-import micro-benches
(`Svg`), and a single-shot cold-start bench (`ColdStart`).

Every steady-state JMH bench uses `@Fork(1)` with a 3×2s warmup / 5×2s measurement
window — a deliberately fast default for on-demand local iteration (a single fork,
so the reported `Error` column is blank). For a number you intend to quote, pass
more forks on the CLI (e.g. `-f 5`) for a cross-fork error estimate. The exception
is `ColdStart`, which is single-shot (`Mode.SingleShotTime`, `@Warmup(0)`,
`@Fork(10)`) — it deliberately measures the JIT-cold first render across ten fresh
JVMs.

The measured region differs per benchmark: `TemplateCv` hoists fixture
construction into `@Setup` and times the render only, while `CanonicalRender` and
`PaginatedDocument` build the document inside the benchmark, so their scores also
include `DocumentSession` creation and DSL construction. Compare each benchmark
against its own history, not across the three.

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
