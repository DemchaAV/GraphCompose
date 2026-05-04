# Performance — v1.4 numbers

All numbers below come from `scripts/run-benchmarks.ps1` — the full local
benchmark workflow that builds the test classpath once and runs
`current-speed`, `comparative`, `core-engine`, `full-cv`, `scalability`,
and `stress` suites in sequence. They were captured on a developer
laptop; CI machines are typically 1.5–2× slower. The benchmark
methodology, profiles, GC stabilisation, percentile rule, and how to
compare two runs locally are documented in
[docs/benchmarks.md](./benchmarks.md).

## End-to-end latency

`current-speed` full profile — 12 warmup + 40 measurement iterations.

| Scenario          | Avg ms | p50 ms | p95 ms | Docs/sec |
|-------------------|-------:|-------:|-------:|---------:|
| engine-simple     |   3.00 |   2.73 |   4.86 |   333.83 |
| invoice-template  |  17.74 |  17.44 |  25.13 |    56.38 |
| cv-template       |  10.16 |   9.91 |  14.08 |    98.46 |
| proposal-template |  18.21 |  16.93 |  23.57 |    54.91 |
| feature-rich      |  36.02 |  34.18 |  41.79 |    27.76 |

### Per-stage breakdown (median ms per stage)

| Scenario          | Compose | Layout | Render | Total |
|-------------------|--------:|-------:|-------:|------:|
| invoice-template  |   0.33  |  2.55  |  5.76  |  8.63 |
| cv-template       |   0.27  |  2.77  |  1.60  |  4.72 |
| proposal-template |   0.34  |  9.54  |  5.66  | 15.65 |

Render time is dominated by PDFBox serialization (36–67 % of total),
so engine-side optimisations look smaller in the end-to-end avg than
they do in the layout column. Page-background injection is a constant
1 fragment per page; column spans, layer stacks, and themes do not
change the number of fragments emitted.

## Parallel throughput

Invoice template, 12 docs per thread.

| Threads | Total docs | Throughput | Avg doc ms |
|--------:|-----------:|-----------:|-----------:|
| 1       |        12  |    89.56/s |     11.17  |
| 2       |        24  |   143.53/s |      6.97  |
| 4       |        48  |   245.26/s |      4.08  |
| 8       |        96  |   328.78/s |      3.04  |

Near-linear scaling through 4 cores, ~2.7× throughput by 8 threads on
a hyper-threaded CPU.

## Linear scalability

`scalability` suite, simple docs.

| Threads | Total docs | Throughput   |
|--------:|-----------:|-------------:|
| 1       |       100  |     807.41/s |
| 2       |       200  |   1,960.75/s |
| 4       |       400  |   3,839.64/s |
| 8       |       800  |   7,394.56/s |
| 16      |     1,600  |  11,164.76/s |

13.8× throughput at 16 threads — the engine has no global
synchronisation in the hot path.

## Stress test

50-thread pool, 5,000 documents, single run:

```text
Successful: 5000
Errors:     0
Time:       2499 ms
```

~2,000 docs/sec sustained under contention, **zero failures**.

## Comparative benchmark

Simple invoice-class document, 100 measurement iterations.

| Library                | Avg ms | Avg heap MB | Notes                       |
|------------------------|-------:|------------:|-----------------------------|
| iText 5                |   1.57 |        0.16 | low-level page primitives   |
| **GraphCompose v1.4**  |   2.45 |        0.16 | **semantic DSL + pagination** |
| JasperReports          |   4.45 |        0.19 | XML-template based engine   |

GraphCompose sits between low-level PDF generators (iText 5) and
template engines (JasperReports): close to iText latency on a per-doc
basis while exposing a fully semantic Java DSL with deterministic
snapshots.

## Engine-only timings

- `GraphComposeBenchmark` (engine-only, no PDF render): avg **1.04 ms**, p50 **0.97 ms**, p95 **1.64 ms**.
- `FullCvBenchmark` (full CV template, including render): avg **4.14 ms**, p50 **3.80 ms**, p95 **6.37 ms**.
