<#
.SYNOPSIS
    Holds the DOCX export of every document of the DOCX fidelity corpus — each
    template preset, and each document built for a shape no preset has — as
    Microsoft Word sets it, to the corpus's Word baseline.

.DESCRIPTION
    Word is the editor the DOCX export answers to, and CI cannot run it. This is
    the same gate DocxFidelityCorpusTest runs with LibreOffice in CI, run locally
    with Word:

      1. the corpus test in `export` mode writes each document's PDF and DOCX to
         qa/target/docx-fidelity/engine;
      2. convert-with-word.ps1 converts the DOCX through a private, hidden Word
         instance into qa/target/docx-fidelity/word, recording Word's version;
      3. the corpus test in `word` mode checks, by the SHA-256 the conversion
         recorded, that Word converted the DOCX this tree exports, measures Word's
         PDFs line by line against the engine's, and holds them to
         qa/src/test/resources/docx-fidelity/word-windows.tsv.

    The `export` run also writes corpus documents changed in each way the gate
    is there to fail on, with a copy of each left unchanged, to
    qa/target/docx-fidelity/tampered; Word converts those into
    qa/target/docx-fidelity/word-tampered, and the `word` run fails unless each
    unchanged copy stands where the corpus's conversion set it, within the
    gate's half-point slack, and the gate fails on every change, losing lines,
    or coming to another page count, where the change costs that. Word failing
    to convert them is reported by that run, beside the corpus's result.

    Run it from PowerShell (pwsh), not through powershell.exe under Git Bash: Word
    driven through COM from a process Git Bash started has stalled on its first
    document. Run the install the qa module needs first, as for any standalone qa
    run.

.PARAMETER Update
    Rewrites the Word baseline with what was measured, which is how a change
    that moves documents — nearer the page included — records the move. What it
    writes over is printed first.

.EXAMPLE
    .\mvnw.cmd -B -ntp -DskipTests install -pl :graph-compose-qa -am
    .\scripts\docx-visual\word-fidelity.ps1

.EXAMPLE
    .\scripts\docx-visual\word-fidelity.ps1 -Update
#>
[CmdletBinding()]
param(
    [switch]$Update
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$mvnw = Join-Path $root 'mvnw.cmd'
$qa = Join-Path $root 'qa\pom.xml'
$work = Join-Path $root 'qa\target\docx-fidelity'

function Invoke-Corpus {
    param([string]$Mode, [string[]]$Extra = @())
    $arguments = @('-B', '-ntp', 'test', '-f', $qa, '-Dtest=DocxFidelityCorpusTest',
        '-Dsurefire.failIfNoSpecifiedTests=false', "-Dgraphcompose.docxFidelity=$Mode") + $Extra
    & $mvnw @arguments
    if ($LASTEXITCODE -ne 0) { throw "the corpus test in $Mode mode failed (exit $LASTEXITCODE)" }
}

Invoke-Corpus -Mode 'export'

$pdfs = Join-Path $work 'word'
if (Test-Path $pdfs) { Remove-Item -Recurse -Force $pdfs }
# The converter sets an exit code only when it stops early (2, NOT_RUN); clear the build's.
$global:LASTEXITCODE = 0
& (Join-Path $PSScriptRoot 'convert-with-word.ps1') -Path (Join-Path $work 'engine') -OutputDir $pdfs
if ($LASTEXITCODE -ne 0) { throw "Word did not convert the corpus (exit $LASTEXITCODE)" }

# The changed documents are converted apart, and a failure there does not stop the corpus being
# measured: the word run reports what it lacks of them beside the corpus's result.
$tampered = Join-Path $work 'word-tampered'
if (Test-Path $tampered) { Remove-Item -Recurse -Force $tampered }
try {
    $global:LASTEXITCODE = 0
    & (Join-Path $PSScriptRoot 'convert-with-word.ps1') -Path (Join-Path $work 'tampered') -OutputDir $tampered
    if ($LASTEXITCODE -ne 0) { Write-Warning "Word did not convert the changed documents (exit $LASTEXITCODE)" }
} catch {
    Write-Warning "Word did not convert the changed documents: $_"
}

$extra = if ($Update) { @('-Dgraphcompose.docxFidelity.update=true') } else { @() }
Invoke-Corpus -Mode 'word' -Extra $extra
