[CmdletBinding()]
<#
.SYNOPSIS
Trustworthy A/B benchmark comparison between two branches (default main vs develop).

.DESCRIPTION
Wraps scripts/run-benchmarks.ps1 to produce a *fair* engine-speed comparison that
controls the confounds that wreck single back-to-back runs on a laptop:

  * INTERLEAVES the two branches (A,B,A,B,...) instead of all-A-then-all-B, so
    CPU thermal drift on a mobile chip (e.g. i7-9750H) averages out between them.
  * Repeats each branch N times and compares MEDIANS, not one noisy run.
  * Inserts a cooldown between runs to let the laptop shed heat.
  * Pre-flight warns about the two biggest noise sources: running on battery and
    other JVMs/IDE competing for cores.
  * Temporarily moves the untracked benchmark probes aside (they reference
    develop-only CountingTextMeasurementSystem and break the bench compile on main),
    then restores them.

It then parses every run's JSON + logs and prints a single MAIN-vs-DEVELOP table
(latency per scenario, parallel throughput, scalability, comparative, core/full-cv,
stress) with median values and % delta, and writes a CSV.

Calls run-benchmarks.ps1 with NO parameters for maximum compatibility (the older
copy on main lacks the newer flags). Each branch runs its own committed harness;
only scenarios present on BOTH branches are compared.

.EXAMPLE
  # Close your IDE, plug in AC power, then:
  ./scripts/ab-bench.ps1 -Repeat 3
#>
param(
    [ValidateRange(1, 10)] [int]$Repeat = 3,
    [int]$CooldownSec = 45,
    [string]$BranchA = "main",
    [string]$BranchB = "develop",
    [switch]$KeepProbes,        # don't move/restore the untracked probes (manage them yourself)
    [switch]$SkipPreflight,     # skip the battery/IDE warnings + confirm
    [double]$NoisePct = 3.0     # |delta| below this is reported as ~ (within noise)
)

$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false   # we check $LASTEXITCODE ourselves
}

$repoRoot   = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runner     = Join-Path $repoRoot "scripts\run-benchmarks.ps1"
$probeStash = Join-Path (Split-Path $repoRoot -Parent) "_probe-stash"
$csDir      = Join-Path $repoRoot "target\benchmarks\current-speed"
$cmpDir     = Join-Path $repoRoot "target\benchmarks\comparative"
$runsDir    = Join-Path $repoRoot "target\benchmark-runs"
$outDir     = Join-Path $repoRoot "target\ab-compare"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Fail($msg) { Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }
function Section($t) { Write-Host ""; Write-Host ("=" * 78) -ForegroundColor DarkGray; Write-Host $t -ForegroundColor Cyan; Write-Host ("=" * 78) -ForegroundColor DarkGray }

function Get-Median([double[]]$v) {
    if (-not $v -or $v.Count -eq 0) { return $null }
    $s = $v | Sort-Object
    $n = $s.Count
    if ($n % 2 -eq 1) { return [double]$s[($n - 1) / 2] }
    return ([double]$s[$n / 2 - 1] + [double]$s[$n / 2]) / 2.0
}

# Native git wrappers. git writes informational text to STDERR ("Switched to branch ...");
# under $ErrorActionPreference='Stop' a 2>&1 merge turns that into a TERMINATING error.
# So run git with ErrorActionPreference=Continue, discard stderr, and check $LASTEXITCODE.
function GitTry { param([Parameter(ValueFromRemainingArguments)]$a)
    $old = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    try { & git @a 2>$null | Out-Null } finally { $ErrorActionPreference = $old }
    return $LASTEXITCODE }
function GitOut { param([Parameter(ValueFromRemainingArguments)]$a)
    $old = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    try { $o = & git @a 2>$null } finally { $ErrorActionPreference = $old }
    return $o }
function Get-Git() { $b = GitOut rev-parse --abbrev-ref HEAD; if (-not $b) { Fail "not a git repo" }; return (($b | Select-Object -First 1)).ToString().Trim() }
function Checkout($b) { if ((GitTry checkout $b) -ne 0) { Fail "git checkout $b failed" } }

# ---- parsers (return hashtable metricKey -> value) ---------------------------
function Parse-CurrentSpeed($jsonPath) {
    $o = @{}
    $j = Get-Content $jsonPath -Raw | ConvertFrom-Json
    foreach ($l in $j.latency) {
        $o["latency | $($l.scenario) | avg ms"]   = [double]$l.avgMillis
        $o["latency | $($l.scenario) | p95 ms"]   = [double]$l.p95Millis
        $o["latency | $($l.scenario) | docs/s"]   = [double]$l.docsPerSecond
        $o["latency | $($l.scenario) | heap MB"]  = [double]$l.peakHeapMb
    }
    foreach ($t in $j.throughput) {
        $o["throughput | $($t.scenario) | $($t.threads)t docs/s"] = [double]$t.docsPerSecond
    }
    return $o
}
function Parse-Comparative($jsonPath) {
    $o = @{}
    $j = Get-Content $jsonPath -Raw | ConvertFrom-Json
    foreach ($lib in $j.libraries) { $o["comparative | $($lib.library) | avg ms"] = [double]$lib.avgTimeMs }
    return $o
}
function Parse-Logs($logsDir) {
    $o = @{}
    $scal = Join-Path $logsDir "06-scalability.log"
    if (Test-Path $scal) {
        foreach ($line in (Get-Content $scal)) {
            if ($line -match '^\s*(\d+)\s*\|\s*\d+\s*\|\s*([\d.]+)\s*$') {
                $o["scalability | $($matches[1])t | docs/s"] = [double]$matches[2]
            }
        }
    }
    foreach ($pair in @(@("04-core-engine.log", "core-engine"), @("05-full-cv.log", "full-cv"))) {
        $p = Join-Path $logsDir $pair[0]
        if (Test-Path $p) {
            $txt = Get-Content $p -Raw
            if ($txt -match 'Median[^\r\n]*?:\s*([\d.]+)\s*ms') { $o["$($pair[1]) | median ms"] = [double]$matches[1] }
        }
    }
    $stress = Join-Path $logsDir "07-stress.log"
    if (Test-Path $stress) {
        $txt = Get-Content $stress -Raw
        if ($txt -match 'Time:\s*(\d+)\s*ms') { $o["stress | total ms"] = [double]$matches[1] }
    }
    return $o
}
function Newest-Since($dir, $pattern, [datetime]$since) {
    if (-not (Test-Path $dir)) { return $null }
    Get-ChildItem -Path $dir -Filter $pattern -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -gt $since } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
function Newest-LogsDirSince([datetime]$since) {
    if (-not (Test-Path $runsDir)) { return $null }
    $d = Get-ChildItem -Path $runsDir -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -gt $since } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($d) { return (Join-Path $d.FullName "logs") }
    return $null
}

# ---- pre-flight --------------------------------------------------------------
if (-not (Test-Path $runner)) { Fail "run-benchmarks.ps1 not found at $runner" }
$dirty = GitOut status --porcelain --untracked-files=no
if ($dirty) { Fail ("working tree has uncommitted TRACKED changes; commit/stash them before A/B (branch switching would carry them).`n" + ($dirty -join "`n")) }

if (-not $SkipPreflight) {
    $warn = @()
    $bat = Get-CimInstance Win32_Battery -ErrorAction SilentlyContinue
    if ($bat -and $bat.BatteryStatus -eq 1) { $warn += "On BATTERY power - CPU will throttle. Plug in AC + set High-performance power plan." }
    $jvm = Get-Process -Name java, javaw, idea64, idea, studio64 -ErrorAction SilentlyContinue
    if ($jvm) { $warn += "Other JVM/IDE processes running ($([string]::Join(', ', ($jvm | Select-Object -Expand ProcessName -Unique)))) - they steal cores. Close your IDE." }
    if ($warn.Count -gt 0) {
        Write-Host ""
        foreach ($w in $warn) { Write-Host "  ⚠  $w" -ForegroundColor Yellow }
        $ans = Read-Host "Continue anyway? (y/N)"
        if ($ans -notmatch '^(y|yes)$') { Write-Host "Aborted."; exit 0 }
    }
}

# ---- run ---------------------------------------------------------------------
$startBranch = Get-Git
Write-Host "A/B: $BranchA vs $BranchB | repeats=$Repeat | cooldown=${CooldownSec}s | start branch=$startBranch" -ForegroundColor Green

# Move ALL untracked .java under benchmarks/ aside: they may reference branch-only symbols
# (e.g. develop-only CountingTextMeasurementSystem) and break the bench compile on the other
# branch. Committed benchmarks don't depend on them. Detected via git so new probes are handled.
$moved = New-Object System.Collections.Generic.List[object]
if (-not $KeepProbes) {
    New-Item -ItemType Directory -Force -Path $probeStash | Out-Null
    $untracked = (GitOut status --porcelain --untracked-files=all -- benchmarks) |
        Where-Object { $_ -match '^\?\?\s' -and $_ -match '\.java\s*$' } |
        ForEach-Object { (($_ -replace '^\s*\?\?\s+', '').Trim() -replace '/', '\') }
    $i = 0
    foreach ($rel in $untracked) {
        $full = Join-Path $repoRoot $rel
        if (Test-Path $full) {
            $stashed = Join-Path $probeStash ("{0:000}_{1}" -f $i, (Split-Path $rel -Leaf))
            Move-Item -Force $full $stashed
            $moved.Add([pscustomobject]@{ Rel = $rel; Stashed = $stashed }); $i++
        }
    }
    if ($moved.Count) { Write-Host "Moved $($moved.Count) untracked benchmark .java aside -> $probeStash" -ForegroundColor DarkGray }
}

$samples = New-Object System.Collections.Generic.List[object]   # {Branch, Map}
try {
    foreach ($rep in 1..$Repeat) {
        foreach ($br in @($BranchA, $BranchB)) {
            Section "repeat $rep/$Repeat | branch $br"
            Checkout $br
            $t0 = Get-Date
            $ok = $true
            try { & $runner } catch { $ok = $false; Write-Host "  run-benchmarks.ps1 failed on $br : $($_.Exception.Message)" -ForegroundColor Red }
            if ($ok) {
                $map = @{}
                $csJson  = Newest-Since $csDir  "run-*.json" $t0
                $cmpJson = Newest-Since $cmpDir "run-*.json" $t0
                $logsDir = Newest-LogsDirSince $t0
                if ($csJson)  { (Parse-CurrentSpeed $csJson.FullName).GetEnumerator()  | ForEach-Object { $map[$_.Key] = $_.Value } }
                if ($cmpJson) { (Parse-Comparative $cmpJson.FullName).GetEnumerator()  | ForEach-Object { $map[$_.Key] = $_.Value } }
                if ($logsDir) { (Parse-Logs $logsDir).GetEnumerator()                  | ForEach-Object { $map[$_.Key] = $_.Value } }
                if ($map.Count -gt 0) { $samples.Add([pscustomobject]@{ Branch = $br; Map = $map }) }
                else { Write-Host "  WARN: no fresh outputs parsed for $br (run may have failed early)" -ForegroundColor Yellow }
            }
            if (-not ($rep -eq $Repeat -and $br -eq $BranchB)) {
                Write-Host "  cooldown ${CooldownSec}s..." -ForegroundColor DarkGray
                Start-Sleep -Seconds $CooldownSec
            }
        }
    }
}
finally {
    # restore probes FIRST (filesystem; they're untracked so they survive the checkout),
    # so a checkout hiccup can never strand them in the stash.
    if (-not $KeepProbes -and $moved.Count) {
        foreach ($m in $moved) {
            if (Test-Path $m.Stashed) {
                $dest = Join-Path $repoRoot $m.Rel
                New-Item -ItemType Directory -Force -Path (Split-Path $dest -Parent) | Out-Null
                Move-Item -Force $m.Stashed $dest
            }
        }
        Write-Host "Restored $($moved.Count) probe(s) to working tree" -ForegroundColor DarkGray
    }
    Checkout $startBranch
    Write-Host "Back on branch $startBranch" -ForegroundColor DarkGray
}

# ---- aggregate ---------------------------------------------------------------
$byBranch = @{ $BranchA = @{}; $BranchB = @{} }
foreach ($s in $samples) {
    foreach ($k in $s.Map.Keys) {
        if (-not $byBranch[$s.Branch].ContainsKey($k)) { $byBranch[$s.Branch][$k] = New-Object System.Collections.Generic.List[double] }
        $byBranch[$s.Branch][$k].Add([double]$s.Map[$k])
    }
}
$nA = ($samples | Where-Object Branch -eq $BranchA).Count
$nB = ($samples | Where-Object Branch -eq $BranchB).Count
if ($nA -eq 0 -or $nB -eq 0) { Fail "no parsed runs for one branch (A=$nA B=$nB). Check target\benchmark-runs logs." }

$allKeys = ($byBranch[$BranchA].Keys + $byBranch[$BranchB].Keys) | Sort-Object -Unique
$rows = foreach ($k in $allKeys) {
    if (-not ($byBranch[$BranchA].ContainsKey($k) -and $byBranch[$BranchB].ContainsKey($k))) { continue }  # common-only
    $a = Get-Median $byBranch[$BranchA][$k].ToArray()
    $b = Get-Median $byBranch[$BranchB][$k].ToArray()
    $delta = if ($a -ne 0) { ($b - $a) / $a * 100.0 } else { 0 }
    $higherBetter = ($k -match 'docs/s')
    $better = if ([math]::Abs($delta) -lt $NoisePct) { "~" }
              elseif (($higherBetter -and $delta -gt 0) -or (-not $higherBetter -and $delta -lt 0)) { $BranchB }
              else { $BranchA }
    [pscustomobject]([ordered]@{
        Metric    = $k
        $BranchA  = [math]::Round($a, 2)
        $BranchB  = [math]::Round($b, 2)
        "delta_%" = [math]::Round($delta, 1)
        Better    = $better
    })
}

Section "MEDIAN COMPARISON  ($BranchA n=$nA  vs  $BranchB n=$nB)   [lower=better, except docs/s]"
$rows | Format-Table -AutoSize

$csv = Join-Path $outDir ("ab-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".csv")
$rows | Export-Csv -NoTypeInformation -Encoding UTF8 $csv
Write-Host ""
Write-Host "CSV: $csv"
$improved = ($rows | Where-Object Better -eq $BranchB).Count
$regressed = ($rows | Where-Object Better -eq $BranchA).Count
$neutral = ($rows | Where-Object Better -eq "~").Count
Write-Host ("Summary: {0} metrics  ->  {1} better on {2},  {3} better on {4},  {5} within +/-{6}% (noise)" -f $rows.Count, $improved, $BranchB, $regressed, $BranchA, $neutral, $NoisePct) -ForegroundColor Green
Write-Host "Note: laptop (i7-9750H) results are still noisy; treat <~5-10% deltas as inconclusive." -ForegroundColor DarkGray
