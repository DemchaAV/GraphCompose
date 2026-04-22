[CmdletBinding()]
<#
.SYNOPSIS
Runs the local GraphCompose benchmark pipeline and stores timestamped logs and reports.

.DESCRIPTION
The wrapper performs a staged local run:
01 build classpath, 02 current-speed, 03 comparative, 04 core engine, 05 full CV, 06 scalability,
07 stress, optional 08 endurance, then 09/10 diff steps.

Current-speed diffs are profile-aware. The wrapper only compares reports
from the same current-speed profile (`smoke` or `full`) and skips the
diff gracefully when no compatible historical pair exists yet.

Use `-Repeat` to generate repeated current-speed/comparative runs and median
aggregates for more stable local comparisons.
#>
param(
    [switch]$IncludeEndurance,
    [switch]$OpenResults,
    [switch]$SkipDiff,
    [ValidateSet("full", "smoke")]
    [string]$CurrentSpeedProfile = "full",
    [ValidateRange(1, 10)]
    [int]$Repeat = 1,
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
        Get-Content $logPath | Out-Host
        Add-SummaryLine(("- ``{0}``: FAILED" -f $Name))
        Add-SummaryLine(("  - Log: ``{0}``" -f $logPath))
        throw
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    Get-Content $logPath | Out-Host

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

    return @(Get-RunFiles -SuiteName $SuiteName).Count
}

function Get-RunFiles {
    param([string]$SuiteName)

    $suiteDir = Join-Path $repoRoot "target\benchmarks\$SuiteName"
    if (-not (Test-Path $suiteDir)) {
        return @()
    }

    return @(
        Get-ChildItem -Path $suiteDir -Filter "run-*.json" -File -ErrorAction SilentlyContinue |
            Sort-Object Name |
            ForEach-Object { $_.FullName }
    )
}

function Get-RunPair {
    param([string]$SuiteName)

    $runFiles = @(Get-RunFiles -SuiteName $SuiteName)
    if ($runFiles.Count -lt 2) {
        return $null
    }

    return @($runFiles[$runFiles.Count - 2], $runFiles[$runFiles.Count - 1])
}

function Get-CurrentSpeedRunProfile {
    param([string]$RunPath)

    if (-not (Test-Path $RunPath)) {
        return "full"
    }

    try {
        $report = Get-Content -Path $RunPath -Raw | ConvertFrom-Json
        $profile = [string]$report.profile
        if (-not [string]::IsNullOrWhiteSpace($profile)) {
            return $profile
        }
    } catch {
    }

    return "full"
}

function Get-CurrentSpeedCompatibleRunPair {
    $runFiles = @(Get-RunFiles -SuiteName "current-speed")
    if ($runFiles.Count -lt 2) {
        return $null
    }

    $latestRun = $runFiles[$runFiles.Count - 1]
    $latestProfile = Get-CurrentSpeedRunProfile -RunPath $latestRun
    $matchingRuns = @(
        $runFiles |
            Where-Object { (Get-CurrentSpeedRunProfile -RunPath $_) -eq $latestProfile }
    )

    if ($matchingRuns.Count -lt 2) {
        return $null
    }

    return @($matchingRuns[$matchingRuns.Count - 2], $matchingRuns[$matchingRuns.Count - 1])
}

function Invoke-RepeatedBenchmark {
    param(
        [string]$NamePrefix,
        [string]$SuiteName,
        [string]$Classpath,
        [string]$MainClass,
        [string[]]$SystemProperties = @(),
        [string[]]$JavaOptions = @(),
        [string[]]$Arguments = @(),
        [int]$RepeatCount = 1
    )

    $createdRuns = @()
    for ($index = 1; $index -le $RepeatCount; $index++) {
        $beforeRuns = @(Get-RunFiles -SuiteName $SuiteName)
        $stepName = if ($RepeatCount -gt 1) {
            "{0}-{1:d2}-of-{2:d2}" -f $NamePrefix, $index, $RepeatCount
        } else {
            $NamePrefix
        }

        Invoke-JavaMain -Name $stepName `
            -Classpath $Classpath `
            -MainClass $MainClass `
            -SystemProperties $SystemProperties `
            -JavaOptions $JavaOptions `
            -Arguments $Arguments | Out-Null

        $afterRuns = @(Get-RunFiles -SuiteName $SuiteName)
        $newRuns = @($afterRuns | Where-Object { $_ -notin $beforeRuns })
        if ($newRuns.Count -gt 0) {
            $createdRuns += ($newRuns | Sort-Object | Select-Object -Last 1)
        } elseif ($afterRuns.Count -gt 0) {
            $createdRuns += $afterRuns[$afterRuns.Count - 1]
        } else {
            throw "Benchmark '$NamePrefix' did not produce any run-*.json files in suite '$SuiteName'"
        }
    }

    return @($createdRuns)
}

function Invoke-MedianAggregation {
    param(
        [string]$Name,
        [string]$SuiteName,
        [string]$AggregateSuiteName,
        [string]$Classpath,
        [string[]]$InputPaths
    )

    if ($InputPaths.Count -lt 2) {
        return $null
    }

    $beforeRuns = @(Get-RunFiles -SuiteName $AggregateSuiteName)
    $arguments = @($SuiteName) + $InputPaths
    Invoke-JavaMain -Name $Name -Classpath $Classpath -MainClass "com.demcha.compose.BenchmarkMedianTool" -Arguments $arguments | Out-Null

    $afterRuns = @(Get-RunFiles -SuiteName $AggregateSuiteName)
    $newRuns = @($afterRuns | Where-Object { $_ -notin $beforeRuns })
    if ($newRuns.Count -gt 0) {
        return ($newRuns | Sort-Object | Select-Object -Last 1)
    }
    if ($afterRuns.Count -gt 0) {
        return $afterRuns[$afterRuns.Count - 1]
    }

    throw "Median aggregation '$Name' did not produce any run-*.json files in suite '$AggregateSuiteName'"
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
    ("- Current speed profile: ``{0}``" -f $CurrentSpeedProfile),
    ("- Repeat current-speed/comparative: ``{0}``" -f $Repeat),
    ("- Logs folder: ``{0}``" -f $logRoot),
    ""
)

Push-Location $repoRoot
try {
    $currentSpeedAggregateSuite = $null
    $comparativeAggregateSuite = $null

    Invoke-LoggedCommand -Name "01-build-classpath" -Command {
        & $mavenWrapper "-B" "-ntp" "-DskipTests" "test-compile" "dependency:build-classpath" "-DincludeScope=test" "-Dmdep.outputFile=target/benchmark.classpath"
    }

    $resolvedClasspathFile = (Resolve-Path $benchmarkClasspathFile).Path
    $dependencyClasspath = (Get-Content $resolvedClasspathFile -Raw).Trim()
    $javaClasspath = "target\test-classes;target\classes;$dependencyClasspath"

        $currentSpeedProperties = @()
        $currentSpeedProperties += "-Dgraphcompose.benchmark.profile=$CurrentSpeedProfile"
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

        $currentSpeedRuns = Invoke-RepeatedBenchmark `
            -NamePrefix "02-current-speed" `
            -SuiteName "current-speed" `
            -Classpath $javaClasspath `
            -MainClass "com.demcha.compose.CurrentSpeedBenchmark" `
            -SystemProperties $currentSpeedProperties `
            -RepeatCount $Repeat
        $currentSpeedAggregateSuite = "aggregates/current-speed/$CurrentSpeedProfile"
        if ($Repeat -gt 1) {
            Invoke-MedianAggregation `
                -Name "02a-current-speed-median" `
                -SuiteName "current-speed" `
                -AggregateSuiteName $currentSpeedAggregateSuite `
                -Classpath $javaClasspath `
                -InputPaths $currentSpeedRuns | Out-Null
        }

        $comparativeRuns = Invoke-RepeatedBenchmark `
            -NamePrefix "03-comparative" `
            -SuiteName "comparative" `
            -Classpath $javaClasspath `
            -MainClass "com.demcha.compose.ComparativeBenchmark" `
            -RepeatCount $Repeat
        $comparativeAggregateSuite = "aggregates/comparative"
        if ($Repeat -gt 1) {
            Invoke-MedianAggregation `
                -Name "03a-comparative-median" `
                -SuiteName "comparative" `
                -AggregateSuiteName $comparativeAggregateSuite `
                -Classpath $javaClasspath `
                -InputPaths $comparativeRuns | Out-Null
        }

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
            if ($Repeat -gt 1) {
                $currentSpeedDiffPair = Get-RunPair -SuiteName $currentSpeedAggregateSuite
            } else {
                # Current-speed diffs are only valid within the same profile.
                # Use the newest previous run that matches the latest run's profile.
                $currentSpeedDiffPair = Get-CurrentSpeedCompatibleRunPair
            }

            if ($null -ne $currentSpeedDiffPair) {
                $currentSpeedDiffName = if ($Repeat -gt 1) { "09-diff-current-speed-median" } else { "09-diff-current-speed" }
                Invoke-JavaMain -Name $currentSpeedDiffName -Classpath $javaClasspath -MainClass "com.demcha.compose.BenchmarkDiffTool" -Arguments $currentSpeedDiffPair
            } else {
                Add-SummaryLine("- ``09-diff-current-speed``: skipped")
                if ($Repeat -gt 1) {
                    Add-SummaryLine("  - Reason: need at least two current-speed median aggregates")
                } else {
                    Add-SummaryLine("  - Reason: need at least two current-speed runs with the same profile as the latest run")
                }
            }

            if ($Repeat -gt 1) {
                $comparativeDiffPair = Get-RunPair -SuiteName $comparativeAggregateSuite
            } else {
                $comparativeDiffPair = Get-RunPair -SuiteName "comparative"
            }

            if ($null -ne $comparativeDiffPair) {
                $comparativeDiffName = if ($Repeat -gt 1) { "10-diff-comparative-median" } else { "10-diff-comparative" }
                Invoke-JavaMain -Name $comparativeDiffName -Classpath $javaClasspath -MainClass "com.demcha.compose.BenchmarkDiffTool" -Arguments $comparativeDiffPair
            } else {
                Add-SummaryLine("- ``10-diff-comparative``: skipped")
                if ($Repeat -gt 1) {
                    Add-SummaryLine("  - Reason: need at least two comparative median aggregates")
                } else {
                    Add-SummaryLine("  - Reason: need at least two comparative runs")
                }
            }
        } else {
            Add-SummaryLine("- ``09-10-diff``: skipped")
            Add-SummaryLine("  - Reason: ``-SkipDiff`` was provided")
        }

    $currentSpeedLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\current-speed\latest.json")
    $comparativeLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\comparative\latest.json")
    $currentSpeedDiffLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\diffs\current-speed\latest.json")
    $comparativeDiffLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\diffs\comparative\latest.json")
    if ($Repeat -gt 1) {
        $currentSpeedMedianLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\$currentSpeedAggregateSuite\latest.json")
        $comparativeMedianLatest = Get-IfExists (Join-Path $repoRoot "target\benchmarks\$comparativeAggregateSuite\latest.json")
    }

    Add-SummaryLine("")
    Add-SummaryLine("## Artifacts")
    if ($currentSpeedLatest) {
        Add-SummaryLine(("- Current speed latest: ``{0}``" -f $currentSpeedLatest))
    }
    if ($Repeat -gt 1 -and $currentSpeedMedianLatest) {
        Add-SummaryLine(("- Current speed median latest: ``{0}``" -f $currentSpeedMedianLatest))
    }
    if ($comparativeLatest) {
        Add-SummaryLine(("- Comparative latest: ``{0}``" -f $comparativeLatest))
    }
    if ($Repeat -gt 1 -and $comparativeMedianLatest) {
        Add-SummaryLine(("- Comparative median latest: ``{0}``" -f $comparativeMedianLatest))
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
