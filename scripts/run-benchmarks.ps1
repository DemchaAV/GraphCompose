[CmdletBinding()]
param(
    [switch]$IncludeEndurance,
    [switch]$OpenResults,
    [switch]$SkipDiff,
    [int]$Warmup = -1,
    [int]$Iterations = -1,
    [int]$DocsPerThread = -1,
    [string]$Threads = "",
    [string]$EnduranceHeap = "128m"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$mavenWrapper = Join-Path $repoRoot "mvnw.cmd"
$benchmarkClasspathFile = Join-Path $repoRoot "target\benchmark.classpath"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runRoot = Join-Path $repoRoot "target\benchmark-runs\$timestamp"
$logRoot = Join-Path $runRoot "logs"
$summaryPath = Join-Path $runRoot "SUMMARY.md"

New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

function Write-Section {
    param([string]$Text)

    Write-Host ""
    Write-Host ("=" * 78) -ForegroundColor DarkGray
    Write-Host $Text -ForegroundColor Cyan
    Write-Host ("=" * 78) -ForegroundColor DarkGray
}

function Add-SummaryLine {
    param([string]$Text)

    Add-Content -Path $summaryPath -Value $Text
}

function Invoke-LoggedCommand {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    $logPath = Join-Path $logRoot ($Name + ".log")
    Write-Section $Name
    $global:LASTEXITCODE = 0
    $previousErrorActionPreference = $ErrorActionPreference

    try {
        $ErrorActionPreference = "Continue"
        & $Command *> $logPath
        $exitCode = $LASTEXITCODE
    } catch {
        $_ | Out-File -FilePath $logPath -Append
        Get-Content $logPath
        Add-SummaryLine(("- ``{0}``: FAILED" -f $Name))
        Add-SummaryLine(("  - Log: ``{0}``" -f $logPath))
        throw
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    Get-Content $logPath

    if ($exitCode -ne 0) {
        Add-SummaryLine(("- ``{0}``: FAILED" -f $Name))
        Add-SummaryLine(("  - Log: ``{0}``" -f $logPath))
        throw "Command '$Name' failed with exit code $exitCode. See $logPath"
    }

    Add-SummaryLine(("- ``{0}``: OK" -f $Name))
    Add-SummaryLine(("  - Log: ``{0}``" -f $logPath))
}

function Invoke-JavaMain {
    param(
        [string]$Name,
        [string]$Classpath,
        [string]$MainClass,
        [string[]]$SystemProperties = @(),
        [string[]]$JavaOptions = @(),
        [string[]]$Arguments = @()
    )

    Invoke-LoggedCommand -Name $Name -Command {
        $javaArgs = @()
        if ($JavaOptions.Count -gt 0) {
            $javaArgs += $JavaOptions
        }
        if ($SystemProperties.Count -gt 0) {
            $javaArgs += $SystemProperties
        }
        $javaArgs += "-cp"
        $javaArgs += $Classpath
        $javaArgs += $MainClass
        if ($Arguments.Count -gt 0) {
            $javaArgs += $Arguments
        }
        & java @javaArgs
    }
}

function Get-RunCount {
    param([string]$SuiteName)

    $suiteDir = Join-Path $repoRoot "target\benchmarks\$SuiteName"
    if (-not (Test-Path $suiteDir)) {
        return 0
    }

    return @(
        Get-ChildItem -Path $suiteDir -Filter "run-*.json" -File -ErrorAction SilentlyContinue
    ).Count
}

function Get-IfExists {
    param([string]$PathText)

    if (Test-Path $PathText) {
        return (Resolve-Path $PathText).Path
    }

    return $null
}

Add-Content -Path $summaryPath -Value @(
    "# Benchmark Run",
    "",
    ("- Timestamp: ``{0}``" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss")),
    ("- Repository: ``{0}``" -f $repoRoot),
    ("- Include endurance: ``{0}``" -f $IncludeEndurance),
    ("- Skip diff: ``{0}``" -f $SkipDiff),
    ("- Logs folder: ``{0}``" -f $logRoot),
    ""
)

Push-Location $repoRoot
try {
    Invoke-LoggedCommand -Name "01-build-classpath" -Command {
        & $mavenWrapper "-B" "-ntp" "-DskipTests" "test-compile" "dependency:build-classpath" "-DincludeScope=test" "-Dmdep.outputFile=target/benchmark.classpath"
    }

    $resolvedClasspathFile = (Resolve-Path $benchmarkClasspathFile).Path
    $dependencyClasspath = (Get-Content $resolvedClasspathFile -Raw).Trim()
    $javaClasspath = "target\test-classes;target\classes;$dependencyClasspath"

    $currentSpeedProperties = @()
    if ($Warmup -gt 0) {
        $currentSpeedProperties += "-Dgraphcompose.benchmark.warmup=$Warmup"
    }
    if ($Iterations -gt 0) {
        $currentSpeedProperties += "-Dgraphcompose.benchmark.iterations=$Iterations"
    }
    if ($DocsPerThread -gt 0) {
        $currentSpeedProperties += "-Dgraphcompose.benchmark.docsPerThread=$DocsPerThread"
    }
    if (-not [string]::IsNullOrWhiteSpace($Threads)) {
        $currentSpeedProperties += "-Dgraphcompose.benchmark.threads=$Threads"
    }

    Invoke-JavaMain -Name "02-current-speed" -Classpath $javaClasspath -MainClass "com.demcha.compose.CurrentSpeedBenchmark" -SystemProperties $currentSpeedProperties
    Invoke-JavaMain -Name "03-comparative" -Classpath $javaClasspath -MainClass "com.demcha.compose.ComparativeBenchmark"
    Invoke-JavaMain -Name "04-core-engine" -Classpath $javaClasspath -MainClass "com.demcha.compose.GraphComposeBenchmark"
    Invoke-JavaMain -Name "05-full-cv" -Classpath $javaClasspath -MainClass "com.demcha.compose.FullCvBenchmark"
    Invoke-JavaMain -Name "06-scalability" -Classpath $javaClasspath -MainClass "com.demcha.compose.ScalabilityBenchmark"
    Invoke-JavaMain -Name "07-stress" -Classpath $javaClasspath -MainClass "com.demcha.compose.GraphComposeStressTest"

    if ($IncludeEndurance) {
        Invoke-JavaMain -Name "08-endurance" -Classpath $javaClasspath -MainClass "com.demcha.compose.EnduranceTest" -JavaOptions @("-Xmx$EnduranceHeap")
    } else {
        Add-SummaryLine("- ``08-endurance``: skipped")
        Add-SummaryLine("  - Reason: use ``-IncludeEndurance`` to enable the 100,000 document soak run")
    }

    if (-not $SkipDiff) {
        if ((Get-RunCount "current-speed") -ge 2) {
            Invoke-JavaMain -Name "09-diff-current-speed" -Classpath $javaClasspath -MainClass "com.demcha.compose.BenchmarkDiffTool" -Arguments @("current-speed")
        } else {
            Add-SummaryLine("- ``09-diff-current-speed``: skipped")
            Add-SummaryLine("  - Reason: need at least two current-speed runs")
        }

        if ((Get-RunCount "comparative") -ge 2) {
            Invoke-JavaMain -Name "10-diff-comparative" -Classpath $javaClasspath -MainClass "com.demcha.compose.BenchmarkDiffTool" -Arguments @("comparative")
        } else {
            Add-SummaryLine("- ``10-diff-comparative``: skipped")
            Add-SummaryLine("  - Reason: need at least two comparative runs")
        }
    } else {
        Add-SummaryLine("- ``09-10-diff``: skipped")
        Add-SummaryLine("  - Reason: ``-SkipDiff`` was provided")
    }

    $currentSpeedLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\current-speed\latest.json")
    $comparativeLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\comparative\latest.json")
    $currentSpeedDiffLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\diffs\current-speed\latest.json")
    $comparativeDiffLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\diffs\comparative\latest.json")

    Add-SummaryLine("")
    Add-SummaryLine("## Artifacts")
    if ($currentSpeedLatest) {
        Add-SummaryLine(("- Current speed latest: ``{0}``" -f $currentSpeedLatest))
    }
    if ($comparativeLatest) {
        Add-SummaryLine(("- Comparative latest: ``{0}``" -f $comparativeLatest))
    }
    if ($currentSpeedDiffLatest) {
        Add-SummaryLine(("- Current speed diff latest: ``{0}``" -f $currentSpeedDiffLatest))
    }
    if ($comparativeDiffLatest) {
        Add-SummaryLine(("- Comparative diff latest: ``{0}``" -f $comparativeDiffLatest))
    }
    Add-SummaryLine(("- Benchmarks folder: ``{0}``" -f (Join-Path $repoRoot "target\benchmarks")))

    Write-Section "Benchmark run completed"
    Write-Host "Summary: $summaryPath" -ForegroundColor Green
    Write-Host "Benchmarks: $(Join-Path $repoRoot 'target\benchmarks')" -ForegroundColor Green
    Write-Host "Logs: $logRoot" -ForegroundColor Green

    if ($OpenResults) {
        Invoke-Item $summaryPath
        Invoke-Item (Join-Path $repoRoot "target\benchmarks")
    }
} finally {
    Pop-Location
}
