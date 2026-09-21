<#
.SYNOPSIS
    Converts DOCX files to PDF through an installed desktop editor, for the
    editable-export visual comparison.

.DESCRIPTION
    The DOCX side of the comparison has to be rendered by the program people
    actually open the file in. Exporting the GraphCompose PDF a second time and
    calling it the DOCX render would compare the engine against itself and pass
    no matter what the exporter wrote.

    Microsoft Word is driven through COM. LibreOffice Writer is driven through
    `soffice --convert-to pdf`. Neither is installed by the build, so a missing
    editor is reported as NOT_RUN and the script exits non-zero: an absent editor
    changes how a result is obtained, never whether it is required.

    Every run writes a sidecar JSON next to the PDFs recording which editor and
    version produced them, because a rendering difference between two Word builds
    is a real finding and an unlabelled PDF cannot carry it.

.PARAMETER Path
    A .docx file, or a directory that is searched (non-recursively) for .docx files.

.PARAMETER OutputDir
    Where the PDFs and the sidecar are written. Defaults to a `word` or
    `libreoffice` subdirectory beside the input.

.PARAMETER Editor
    `word` (default) or `libreoffice`.

.EXAMPLE
    ./scripts/docx-visual/convert-with-word.ps1 -Path render-docx/target/docx-probe

.EXAMPLE
    ./scripts/docx-visual/convert-with-word.ps1 -Path out.docx -Editor libreoffice
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Path,
    [string]$OutputDir,
    [ValidateSet('word', 'libreoffice')][string]$Editor = 'word'
)

$ErrorActionPreference = 'Stop'

function Resolve-Inputs {
    param([string]$Target)
    if (Test-Path -PathType Leaf $Target) { return @((Get-Item $Target)) }
    if (Test-Path -PathType Container $Target) {
        return @(Get-ChildItem -Path $Target -Filter *.docx -File)
    }
    throw "Input not found: $Target"
}

# wdExportFormatPDF; the numeric constant is used rather than the named enum so
# the script does not depend on the Word type library being registered for the
# PowerShell host.
$WD_EXPORT_FORMAT_PDF = 17

function Convert-WithWord {
    param([System.IO.FileInfo[]]$Files, [string]$Destination)

    try {
        $word = New-Object -ComObject Word.Application
    } catch {
        return @{ status = 'NOT_RUN'; reason = 'Microsoft Word is not available through COM on this machine'; results = @() }
    }

    $version = $word.Version
    $build = $word.Build
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $results = @()
    try {
        foreach ($file in $Files) {
            $out = Join-Path $Destination ($file.BaseName + '.pdf')
            $doc = $word.Documents.Open($file.FullName, [ref]$false, [ref]$true)
            try {
                $doc.ExportAsFixedFormat($out, $WD_EXPORT_FORMAT_PDF)
                $results += @{ source = $file.Name; pdf = (Split-Path $out -Leaf); pages = $doc.ComputeStatistics(2) }
            } finally {
                $doc.Close([ref]$false)
                [Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
            }
        }
    } finally {
        $word.Quit()
        [Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
        [GC]::Collect(); [GC]::WaitForPendingFinalizers()
    }
    return @{ status = 'OK'; editor = 'Microsoft Word'; version = "$version ($build)"; results = $results }
}

function Convert-WithLibreOffice {
    param([System.IO.FileInfo[]]$Files, [string]$Destination)

    $candidates = @(
        'C:\Program Files\LibreOffice\program\soffice.exe',
        'C:\Program Files (x86)\LibreOffice\program\soffice.exe'
    )
    $exe = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $exe) { $exe = (Get-Command soffice -ErrorAction SilentlyContinue).Source }
    if (-not $exe) {
        return @{ status = 'NOT_RUN'; reason = 'LibreOffice (soffice) was not found on this machine'; results = @() }
    }

    $version = (& $exe --version 2>&1 | Select-Object -First 1)
    $results = @()
    foreach ($file in $Files) {
        & $exe --headless --convert-to pdf --outdir $Destination $file.FullName | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "soffice failed on $($file.Name) with exit code $LASTEXITCODE" }
        $results += @{ source = $file.Name; pdf = ($file.BaseName + '.pdf'); pages = $null }
    }
    return @{ status = 'OK'; editor = 'LibreOffice Writer'; version = "$version"; results = $results }
}

$files = Resolve-Inputs -Target $Path
if ($files.Count -eq 0) { throw "No .docx files under $Path" }

if (-not $OutputDir) {
    $base = if (Test-Path -PathType Container $Path) { $Path } else { Split-Path $Path -Parent }
    $OutputDir = Join-Path $base $Editor
}
New-Item -ItemType Directory -Force $OutputDir | Out-Null
$OutputDir = (Resolve-Path $OutputDir).Path

$report = if ($Editor -eq 'word') {
    Convert-WithWord -Files $files -Destination $OutputDir
} else {
    Convert-WithLibreOffice -Files $files -Destination $OutputDir
}

$report['os'] = [System.Environment]::OSVersion.VersionString
$report['convertedAt'] = (Get-Date).ToString('o')
$report['requestedEditor'] = $Editor
$sidecar = Join-Path $OutputDir 'conversion.json'
$report | ConvertTo-Json -Depth 5 | Set-Content -Path $sidecar -Encoding utf8

if ($report.status -eq 'NOT_RUN') {
    Write-Warning "NOT_RUN: $($report.reason)"
    Write-Host "Recorded in $sidecar"
    exit 2
}

Write-Host "Converted $($report.results.Count) file(s) with $($report.editor) $($report.version)"
Write-Host "Output: $OutputDir"
