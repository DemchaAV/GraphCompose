# Engine Performance Baseline (pre-optimization)

Captured: 2026-04-27 09:44 UTC, branch `graphcompose-v2-engine` @ 513187a

## CurrentSpeedBenchmark (smoke profile, 2 warmup + 5 measurement)

| Scenario | Avg ms | p50 ms | p95 ms | Max ms | Docs/sec | Avg KB | Peak MB |
|---|---:|---:|---:|---:|---:|---:|---:|
| engine-simple | 5.86 | 5.57 | 7.19 | 7.19 | 170.64 | 1.08 | 40.08 |
| invoice-template | 35.21 | 35.95 | 38.64 | 38.64 | 28.40 | 10.15 | 84.08 |
| cv-template | 22.45 | 23.65 | 25.45 | 25.45 | 44.54 | 4.23 | 103.30 |
| proposal-template | 35.84 | 35.17 | 38.83 | 38.83 | 27.90 | 12.26 | 149.75 |
| feature-rich | 49.52 | 50.10 | 53.86 | 53.86 | 20.19 | 6.37 | 204.85 |

JSON report: `target/benchmarks/current-speed/run-20260427-094410.json`

## GraphComposeBenchmark (500 iterations, simple one-page doc)

| Stat | Value (ms) |
|---|---:|
| Min | 0.57 |
| Avg | 1.00 |
| p50 | 0.93 |
| p95 | 1.61 |
| p99 | 1.97 |
| Max | 3.52 |

## FullCvBenchmark (500 iterations, full CV)

| Stat | Value (ms) |
|---|---:|
| Min | 2.62 |
| Avg | 4.21 |
| p50 | 4.04 |
| p95 | 5.95 |
| p99 | 8.19 |
| Max | 11.60 |

## What we expect to move

The optimizations target hot paths used in the **template** scenarios
(invoice, cv, proposal, feature-rich, FullCv) since they go through the
legacy ECS engine pipeline (`engine.*` package):

1. **UUID bit-comparison in `PageBreaker.paginationPriority`** — removes
   `UUID::toString` allocation per `PriorityQueue` compare. Should help
   pagination of larger templates (proposal, feature-rich).
2. **`isDebugEnabled` guards in `Entity.getComponent`** — `getComponent`
   is called on every component lookup during layout/render. Removing
   trace-style allocations even when DEBUG is off should improve all
   template scenarios moderately.
3. **Empty pptx/word handler packages** — pure cleanup, no perf impact
   expected.

Engine-simple and the simple GraphComposeBenchmark are dominated by
PDF I/O and font-cache warmup, so they're a control: they should not
regress.
