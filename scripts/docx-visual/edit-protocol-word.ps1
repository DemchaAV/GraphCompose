<#
.SYNOPSIS
    Runs the editing protocol against an exported DOCX through Microsoft Word.

.DESCRIPTION
    Looking identical on open is half the contract; the other half is that the file
    behaves like a Word document once somebody types in it. This drives Word through
    COM over a copy of the export, performs the edits the contract names, saves,
    reopens, and records what survived.

    What it can decide by itself is whether text was lost, whether structure held and
    whether the file still opens after a save — all of which are objective. What it
    cannot decide is whether the result still looks right; that stays a human reading
    of the rendered PDFs, and the protocol says so rather than scoring itself.

    Word is not installed by the build. When it is absent every scenario is recorded
    NOT_RUN and the script exits non-zero; an absent editor never becomes a pass.

.PARAMETER Docx
    The exported .docx to exercise. It is copied first and never modified in place.

.PARAMETER OutputDir
    Where the edited copies and the protocol JSON are written.
    Defaults to an `edit` directory beside the input.

.EXAMPLE
    ./scripts/docx-visual/edit-protocol-word.ps1 -Docx render-docx/target/docx-probe/mixed-two-pager.docx
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Docx,
    [string]$OutputDir
)

$ErrorActionPreference = 'Stop'

$source = Get-Item $Docx
if (-not $OutputDir) { $OutputDir = Join-Path $source.DirectoryName 'edit' }
New-Item -ItemType Directory -Force $OutputDir | Out-Null
$OutputDir = (Resolve-Path $OutputDir).Path

# wdStatisticPages; numeric so the script does not need the Word type library.
$WD_STATISTIC_PAGES = 2

$scenarios = @()

function New-Result {
    param([string]$Name, [string]$Status, [string]$Detail, $Observed = $null)
    return @{ scenario = $Name; status = $Status; detail = $Detail; observed = $Observed }
}

try {
    $word = New-Object -ComObject Word.Application
} catch {
    $report = @{
        status    = 'NOT_RUN'
        reason    = 'Microsoft Word is not available through COM on this machine'
        source    = $source.Name
        scenarios = @()
    }
    $report | ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $OutputDir 'edit-protocol.json') -Encoding utf8
    Write-Warning 'NOT_RUN: Microsoft Word is not available'
    exit 2
}

$wordVersion = "$($word.Version) ($($word.Build))"
$word.Visible = $false
$word.DisplayAlerts = 0

function Invoke-Scenario {
    param([string]$Name, [string]$Suffix, [scriptblock]$Edit, [scriptblock]$Verify)

    $copy = Join-Path $OutputDir ($source.BaseName + '-' + $Suffix + '.docx')
    Copy-Item $source.FullName $copy -Force

    $doc = $word.Documents.Open($copy, [ref]$false, [ref]$false)
    try {
        $before = @{
            pages      = $doc.ComputeStatistics($WD_STATISTIC_PAGES)
            paragraphs = $doc.Paragraphs.Count
            tables     = $doc.Tables.Count
            characters = $doc.Content.Text.Length
        }
        & $Edit $doc
        $doc.Save()
    } finally {
        $doc.Close([ref]$true)
        [Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
    }

    # Reopen from disk: an edit that only holds while the document is in memory has
    # not survived anything. This is also where a package Word had to repair shows up.
    $reopened = $word.Documents.Open($copy, [ref]$false, [ref]$true)
    try {
        $after = @{
            pages      = $reopened.ComputeStatistics($WD_STATISTIC_PAGES)
            paragraphs = $reopened.Paragraphs.Count
            tables     = $reopened.Tables.Count
            characters = $reopened.Content.Text.Length
        }
        $result = & $Verify $reopened $before $after
    } finally {
        $reopened.Close([ref]$false)
        [Runtime.InteropServices.Marshal]::ReleaseComObject($reopened) | Out-Null
    }
    $result['observed'] = @{ before = $before; after = $after; file = (Split-Path $copy -Leaf) }
    return $result
}

try {
    # 1. Lengthen a sentence roughly twofold. The paragraph must rewrap, nothing may
    #    be lost, and the blocks below it must move rather than be overwritten.
    $added = ' This sentence was appended by the editing protocol to make the paragraph roughly twice as long as it was, so the block has to rewrap and everything below it has to move down the page.'
    $scenarios += Invoke-Scenario -Name 'lengthen-a-sentence' -Suffix 'lengthened' -Edit {
        param($doc)
        $target = $doc.Paragraphs | Where-Object { $_.Range.Text -like '*Lengthen this sentence*' } | Select-Object -First 1
        if (-not $target) { throw 'the card paragraph was not found in the export' }
        $target.Range.InsertAfter($added)
    } -Verify {
        param($doc, $before, $after)
        $text = $doc.Content.Text
        $keptOriginal = $text -like '*Lengthen this sentence in Word*'
        $keptAddition = $text -like '*roughly twice as long*'
        if ($keptOriginal -and $keptAddition -and $after.characters -gt $before.characters) {
            New-Result 'lengthen-a-sentence' 'PASS' 'the paragraph absorbed the new text and both the original and the addition survived the save'
        } else {
            New-Result 'lengthen-a-sentence' 'FAIL' "original kept=$keptOriginal, addition kept=$keptAddition"
        }
    }

    # 2. Insert a whole new paragraph into the flow.
    $scenarios += Invoke-Scenario -Name 'insert-a-paragraph' -Suffix 'inserted' -Edit {
        param($doc)
        $anchor = $doc.Paragraphs | Where-Object { $_.Range.Text -like '*Lengthen this sentence*' } | Select-Object -First 1
        if (-not $anchor) { throw 'the card paragraph was not found in the export' }
        $range = $anchor.Range
        $range.InsertParagraphAfter()
        $range.InsertAfter('A paragraph the protocol inserted, to see whether it joins the flow.')
    } -Verify {
        param($doc, $before, $after)
        $present = $doc.Content.Text -like '*a paragraph the protocol inserted*'
        if ($present -and $after.paragraphs -gt $before.paragraphs) {
            New-Result 'insert-a-paragraph' 'PASS' 'the new paragraph joined the body flow and survived the save'
        } else {
            New-Result 'insert-a-paragraph' 'FAIL' "present=$present, paragraphs $($before.paragraphs) -> $($after.paragraphs)"
        }
    }

    # 3. Change the body size globally, through the Normal style — the way a person
    #    restyles a document. The question is not whether the style object accepts the
    #    new value; it is whether the body text actually changes. Runs carrying a direct
    #    size override the style and swallow the edit silently, which looks like a
    #    working document until somebody tries to restyle it.
    $scenarios += Invoke-Scenario -Name 'restyle-body-through-normal' -Suffix 'resized' -Edit {
        param($doc)
        $sample = $doc.Paragraphs | Where-Object { $_.Range.Text -like '*Lengthen this sentence*' } | Select-Object -First 1
        if (-not $sample) { throw 'the card paragraph was not found in the export' }
        $script:bodySizeBefore = $sample.Range.Font.Size
        $doc.Styles('Normal').Font.Size = 14
    } -Verify {
        param($doc, $before, $after)
        $styleSize = $doc.Styles('Normal').Font.Size
        $sample = $doc.Paragraphs | Where-Object { $_.Range.Text -like '*Lengthen this sentence*' } | Select-Object -First 1
        $bodySizeAfter = if ($sample) { $sample.Range.Font.Size } else { $null }
        $kept = $doc.Content.Text -like '*Quarterly service report*'
        if (-not $kept) {
            return New-Result 'restyle-body-through-normal' 'FAIL' 'text was lost when the Normal style changed'
        }
        if ($bodySizeAfter -eq 14) {
            New-Result 'restyle-body-through-normal' 'PASS' "the Normal style reached the body text: ${script:bodySizeBefore}pt became ${bodySizeAfter}pt, and pagination went from $($before.pages) to $($after.pages) pages"
        } else {
            New-Result 'restyle-body-through-normal' 'FAIL' "Normal was set to ${styleSize}pt but the body text stayed at ${bodySizeAfter}pt — the runs carry a direct size that overrides the style, so a global restyle does nothing"
        }
    }

    # 4. Insert a table row, through Word's own table editing. The grid has to be a
    #    grid, not a picture of one.
    $scenarios += Invoke-Scenario -Name 'insert-a-table-row' -Suffix 'row-added' -Edit {
        param($doc)
        if ($doc.Tables.Count -lt 1) { throw 'the export contains no Word table' }
        $table = $doc.Tables($doc.Tables.Count)
        $row = $table.Rows.Add()
        $row.Cells(1).Range.Text = 'Protocol row'
    } -Verify {
        param($doc, $before, $after)
        $present = $doc.Content.Text -like '*Protocol row*'
        $stillTables = $after.tables -eq $before.tables
        if ($present -and $stillTables) {
            New-Result 'insert-a-table-row' 'PASS' 'a row was added through Word table editing and survived the save with the table count unchanged'
        } else {
            New-Result 'insert-a-table-row' 'FAIL' "row present=$present, tables $($before.tables) -> $($after.tables)"
        }
    }

    # 5. Delete a row again. Added and removed are different operations against a
    #    merged grid, so a pass on one is not a pass on the other.
    $scenarios += Invoke-Scenario -Name 'delete-a-table-row' -Suffix 'row-deleted' -Edit {
        param($doc)
        if ($doc.Tables.Count -lt 1) { throw 'the export contains no Word table' }
        $table = $doc.Tables($doc.Tables.Count)
        $script:rowsBeforeDelete = $table.Rows.Count
        $script:deletedText = $table.Rows($table.Rows.Count).Range.Text
        $table.Rows($table.Rows.Count).Delete()
    } -Verify {
        param($doc, $before, $after)
        $table = $doc.Tables($doc.Tables.Count)
        $rowsNow = $table.Rows.Count
        $stillTables = $after.tables -eq $before.tables
        if ($rowsNow -eq ($script:rowsBeforeDelete - 1) -and $stillTables) {
            New-Result 'delete-a-table-row' 'PASS' "the last row was removed through Word table editing: $($script:rowsBeforeDelete) rows became $rowsNow, and the grid survived the save"
        } else {
            New-Result 'delete-a-table-row' 'FAIL' "rows $($script:rowsBeforeDelete) -> $rowsNow, tables $($before.tables) -> $($after.tables)"
        }
    }

    # 6. Continue a list with Enter. This is a question about the mechanism, not the
    #    look: marker characters typed into ordinary paragraphs look exactly like a
    #    list until somebody presses Enter and gets a blank line instead of an item.
    #    wdListNoNumbering is 0, so anything else means Word sees a real list.
    $scenarios += Invoke-Scenario -Name 'continue-a-list' -Suffix 'list-continued' -Edit {
        param($doc)
        $item = $doc.Paragraphs | Where-Object { $_.Range.Text -like '*Storage growth reviewed*' } | Select-Object -First 1
        if (-not $item) { throw 'the checklist item was not found in the export' }
        $script:listTypeBefore = $item.Range.ListFormat.ListType
        $range = $item.Range
        $range.InsertParagraphAfter()
        $range.InsertAfter('An item the protocol added by continuing the list.')
    } -Verify {
        param($doc, $before, $after)
        $added = $doc.Paragraphs | Where-Object { $_.Range.Text -like '*continuing the list*' } | Select-Object -First 1
        if (-not $added) {
            return New-Result 'continue-a-list' 'FAIL' 'the new item did not survive the save'
        }
        $listTypeAfter = $added.Range.ListFormat.ListType
        if ($script:listTypeBefore -ne 0 -and $listTypeAfter -ne 0) {
            New-Result 'continue-a-list' 'PASS' "Word reads the block as a real list (ListType $($script:listTypeBefore)) and the new paragraph joined it as an item (ListType $listTypeAfter)"
        } elseif ($script:listTypeBefore -eq 0) {
            New-Result 'continue-a-list' 'FAIL' 'Word sees no list here: the markers are characters in ordinary paragraphs, so Enter produces a plain paragraph rather than the next item'
        } else {
            New-Result 'continue-a-list' 'FAIL' "the block is a list (ListType $($script:listTypeBefore)) but the new paragraph did not join it (ListType $listTypeAfter)"
        }
    }

    # 7. Save and reopen with no edit at all. A package that needs repairing fails
    #    here before any editing question is asked.
    $scenarios += Invoke-Scenario -Name 'round-trip-without-editing' -Suffix 'roundtrip' -Edit {
        param($doc)
    } -Verify {
        param($doc, $before, $after)
        $same = $after.paragraphs -eq $before.paragraphs -and $after.tables -eq $before.tables `
            -and $after.characters -eq $before.characters
        if ($same) {
            New-Result 'round-trip-without-editing' 'PASS' 'saving and reopening changed neither the text nor the structure'
        } else {
            New-Result 'round-trip-without-editing' 'FAIL' 'the save/reopen cycle altered the document'
        }
    }
} finally {
    $word.Quit()
    [Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
    [GC]::Collect(); [GC]::WaitForPendingFinalizers()
}

$report = @{
    status          = 'OK'
    editor          = 'Microsoft Word'
    version         = $wordVersion
    os              = [System.Environment]::OSVersion.VersionString
    ranAt           = (Get-Date).ToString('o')
    source          = $source.Name
    visualJudgement = 'NOT_RUN — whether the edited document still looks right is read from the PDFs by a person, not scored here'
    scenarios       = $scenarios
}
$protocol = Join-Path $OutputDir 'edit-protocol.json'
$report | ConvertTo-Json -Depth 6 | Set-Content -Path $protocol -Encoding utf8

$failed = @($scenarios | Where-Object { $_.status -ne 'PASS' })
foreach ($s in $scenarios) { Write-Host ("{0,-32} {1}" -f $s.scenario, $s.status) }
Write-Host "Protocol: $protocol"
if ($failed.Count -gt 0) { exit 1 }
