<#
.SYNOPSIS
    Runs the editing protocol over any exported DOCX through Microsoft Word, choosing what to
    edit from the document itself.

.DESCRIPTION
    edit-protocol-word.ps1 exercises the one document it was written for: it finds its targets
    by their text. This protocol finds them by their shape, so it runs over every document of
    the fidelity corpus: the longest paragraph of body text, the table with the most rows, the
    first list, the end of the document.

    Each scenario edits a copy, saves it, reopens it from disk and records what it measured.
    What it decides by itself is objective: text kept or lost, a paragraph rewrapped, the block
    after it moved, a row held to an exact height that would hide what the edit added, the body
    size following the Normal style, a list continued by Enter, a new page carrying the
    document's footer, a save that changes nothing. Whether the edited page still looks right is
    read from the PDFs it writes beside each copy, by a person.

    Word is not installed by the build. When it is absent every document is recorded NOT_RUN
    and the script exits non-zero.

.PARAMETER Docx
    One or more exported .docx files. All are copied into the output directory before the first
    is edited, and only the copies are opened.

.PARAMETER OutputDir
    Where the inputs' copies, the edited copies, their PDFs and edit-protocol-corpus.json are
    written. Keep it outside a module's target directory: a build's clean removes it.

.EXAMPLE
    # Export the corpus once, then run the protocol over it (about a minute a document).
    ./mvnw -B -ntp test -f qa/pom.xml -Dtest=DocxFidelityCorpusTest -Dgraphcompose.docxFidelity=export
    ./scripts/docx-visual/edit-protocol-corpus.ps1 -Docx (Get-ChildItem qa/target/docx-fidelity/engine/*.docx).FullName -OutputDir $env:TEMP/docx-edit
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string[]]$Docx,
    [Parameter(Mandatory = $true)][string]$OutputDir
)

$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force $OutputDir | Out-Null
$OutputDir = (Resolve-Path $OutputDir).Path

# Word constants, numeric so the script needs no type library.
$WD_STATISTIC_PAGES = 2
$WD_ACTIVE_END_PAGE = 3
$WD_VERTICAL_POSITION_ON_PAGE = 6
$WD_WITHIN_TABLE = 12
$WD_ROW_HEIGHT_EXACTLY = 2
$WD_EXPORT_PDF = 17
$WD_HEADER_FOOTER_PRIMARY = 1

$ADDED = ' The editing protocol appended this sentence to make the paragraph about twice as long, so that it has to rewrap and whatever stands below it has to move down to make room for it.'

function New-Result {
    param([string]$Name, [string]$Status, [string]$Detail)
    return [ordered]@{ scenario = $Name; status = $Status; detail = $Detail }
}

# Where a range starts, as one number that grows down the document: page, then points from the top.
function Get-Place {
    param($Range)
    $start = $Range.Duplicate
    $start.Collapse(1)
    return [double]$start.Information($WD_ACTIVE_END_PAGE) * 10000 + [double]$start.Information($WD_VERTICAL_POSITION_ON_PAGE)
}

# How tall a paragraph stands, from its first letter to its last, in points across pages.
# Word's line statistic reads 0 inside a table, so the height is measured instead.
function Get-Extent {
    param($Paragraph)
    $end = Get-TextRange $Paragraph
    $end.Collapse(0)
    return (Get-Place $end) - (Get-Place $Paragraph.Range)
}

# Where the block after a paragraph starts: after its row, for a paragraph in a table, since
# the paragraph after it there may stand beside it in the next cell.
function Get-NextPlace {
    param($Doc, $Paragraph)
    $end = $Paragraph.Range.End
    if ($Paragraph.Range.Information($WD_WITHIN_TABLE)) {
        $end = $Paragraph.Range.Rows(1).Range.End
    }
    if ($end -ge $Doc.Content.End - 1) { return $null }
    return Get-Place $Doc.Range($end, $end + 1)
}

function Get-Text {
    param($Paragraph)
    return ($Paragraph.Range.Text -replace "[`r`a]", '').Trim()
}

# A paragraph's text without its mark, or a cell's end mark, which Word does not let an edit touch.
function Get-TextRange {
    param($Paragraph)
    $range = $Paragraph.Range.Duplicate
    for ($i = 0; $i -lt 3 -and $range.End -gt $range.Start -and $range.Text -match "[`r`a]$"; $i++) {
        $range.MoveEnd(1, -1) | Out-Null
    }
    return $range
}

# Puts the caret after a paragraph's last letter and types as a person does: Enter, then text.
# The new paragraph takes what Word carries forward from the caret, not what a script sets.
function Send-EnterAndType {
    param($Doc, $Paragraph, [string]$Text)
    $end = Get-TextRange $Paragraph
    $end.Collapse(0)
    $end.Select()
    $selection = $Doc.ActiveWindow.Selection
    $selection.TypeParagraph()
    $selection.TypeText($Text)
}

# The longest paragraph of the main story with real text, outside a frame: the one an editor
# is most likely to rewrite. Its index is kept, since a reopened document has new objects.
function Find-BodyParagraph {
    param($Doc)
    $best = -1; $bestLength = 39
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $p = $Doc.Paragraphs($i)
        # A field — a link, a page number — is not cut in half by an edit; Word refuses it.
        if ($p.Range.Frames.Count -gt 0 -or $p.Range.Fields.Count -gt 0) { continue }
        $length = (Get-Text $p).Length
        if ($length -gt $bestLength) { $best = $i; $bestLength = $length }
    }
    return $best
}

function Find-ListParagraph {
    param($Doc)
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $p = $Doc.Paragraphs($i)
        if ($p.Range.ListFormat.ListType -ne 0 -and (Get-Text $p).Length -gt 0) { return $i }
    }
    return -1
}

# The table with the most rows, nested tables included: a document's data table.
function Find-DataTable {
    param($Doc)
    $best = $null; $bestRows = 2
    foreach ($table in $Doc.Tables) {
        foreach ($candidate in @($table) + @($table.Tables)) {
            if ($candidate.Rows.Count -gt $bestRows) { $best = $candidate; $bestRows = $candidate.Rows.Count }
        }
    }
    return $best
}

# The row a range sits in, if any, held to an exact height: what an edit adds to it is hidden.
function Test-ExactRow {
    param($Range)
    if (-not $Range.Information($WD_WITHIN_TABLE)) { return $false }
    try { return $Range.Cells(1).HeightRule -eq $WD_ROW_HEIGHT_EXACTLY } catch { return $false }
}

# An edit Word refuses is a result of that scenario, not the end of the document's protocol.
function Invoke-Scenario {
    param($Word, $Source, [string]$Suffix, [scriptblock]$Edit, [scriptblock]$Verify, [switch]$Pdf)
    try {
        return Invoke-ScenarioOnce $Word $Source $Suffix $Edit $Verify -Pdf:$Pdf
    } catch {
        return New-Result $Suffix 'ERROR' $_.Exception.Message
    }
}

function Invoke-ScenarioOnce {
    param($Word, $Source, [string]$Suffix, [scriptblock]$Edit, [scriptblock]$Verify, [switch]$Pdf)
    $copy = Join-Path $OutputDir ($Source.BaseName + '-' + $Suffix + '.docx')
    Copy-Item $Source.FullName $copy -Force
    $state = @{}
    $doc = $Word.Documents.Open($copy, [ref]$false, [ref]$false)
    try {
        $state.pagesBefore = $doc.ComputeStatistics($WD_STATISTIC_PAGES)
        $state.charactersBefore = $doc.Content.Text.Length
        $state.paragraphsBefore = $doc.Paragraphs.Count
        $state.tablesBefore = $doc.Tables.Count
        $applies = & $Edit $doc $state
        $doc.Save()
    } finally {
        $doc.Close([ref]$true)
        [Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
    }
    if ($applies -eq $false) {
        return $null
    }
    $reopened = $Word.Documents.Open($copy, [ref]$false, [ref]$true)
    try {
        $state.pagesAfter = $reopened.ComputeStatistics($WD_STATISTIC_PAGES)
        $state.charactersAfter = $reopened.Content.Text.Length
        $state.paragraphsAfter = $reopened.Paragraphs.Count
        $state.tablesAfter = $reopened.Tables.Count
        $result = & $Verify $reopened $state
        if ($Pdf) {
            $reopened.ExportAsFixedFormat(($copy -replace '\.docx$', '.pdf'), $WD_EXPORT_PDF)
        }
    } finally {
        $reopened.Close([ref]$false)
        [Runtime.InteropServices.Marshal]::ReleaseComObject($reopened) | Out-Null
    }
    $result.file = Split-Path $copy -Leaf
    return $result
}

function Invoke-Protocol {
    param($Word, $Source)
    $results = @()

    # 1. Lengthen the longest paragraph about twofold: it rewraps, nothing is lost, the block
    #    after it moves down, and no exact row height hides what was added.
    $results += Invoke-Scenario $Word $Source 'lengthen-a-paragraph' -Pdf -Edit {
        param($doc, $state)
        $state.index = Find-BodyParagraph $doc
        if ($state.index -lt 0) { return $false }
        $p = $doc.Paragraphs($state.index)
        $state.target = (Get-Text $p).Substring(0, 30)
        $state.extentBefore = Get-Extent $p
        $state.exactRow = Test-ExactRow $p.Range
        $state.nextBefore = Get-NextPlace $doc $p
        (Get-TextRange $p).InsertAfter($ADDED)
    } -Verify {
        param($doc, $state)
        $p = $doc.Paragraphs($state.index)
        $kept = (Get-Text $p) -like '*whatever stands below it has to move down*'
        $extent = Get-Extent $p
        $next = Get-NextPlace $doc $p
        $moved = $null -eq $state.nextBefore -or $next -gt $state.nextBefore
        $detail = "'$($state.target)…' stands $([Math]::Round($state.extentBefore, 1)) -> $([Math]::Round($extent, 1))pt tall, block after it moved=$moved, exact row=$($state.exactRow)"
        if (-not $kept) { return New-Result 'lengthen-a-paragraph' 'FAIL' "the added text was lost; $detail" }
        if ($state.exactRow) { return New-Result 'lengthen-a-paragraph' 'FAIL' "the paragraph sits in a row of exact height, which hides what it grows by; $detail" }
        if ($extent -le $state.extentBefore -or -not $moved) { return New-Result 'lengthen-a-paragraph' 'FAIL' $detail }
        New-Result 'lengthen-a-paragraph' 'PASS' $detail
    }

    # 2. Delete the second half of the same paragraph: it shrinks, and the block after it
    #    does not stay where a taller paragraph would have left it.
    $results += Invoke-Scenario $Word $Source 'shorten-a-paragraph' -Edit {
        param($doc, $state)
        $state.index = Find-BodyParagraph $doc
        if ($state.index -lt 0) { return $false }
        $p = $doc.Paragraphs($state.index)
        $state.extentBefore = Get-Extent $p
        $state.nextBefore = Get-NextPlace $doc $p
        $half = Get-TextRange $p
        $half.MoveStart(1, [int][Math]::Floor(($half.End - $half.Start) / 2)) | Out-Null
        $state.removed = $half.Text.Length
        $half.Delete() | Out-Null
    } -Verify {
        param($doc, $state)
        $p = $doc.Paragraphs($state.index)
        $extent = Get-Extent $p
        $next = Get-NextPlace $doc $p
        $notLower = $null -eq $state.nextBefore -or $next -le $state.nextBefore
        $detail = "removed $($state.removed) characters, $([Math]::Round($state.extentBefore, 1)) -> $([Math]::Round($extent, 1))pt tall, block after it not lower=$notLower"
        if ($state.charactersAfter -ge $state.charactersBefore -or -not $notLower) { return New-Result 'shorten-a-paragraph' 'FAIL' $detail }
        New-Result 'shorten-a-paragraph' 'PASS' $detail
    }

    # 3. Insert a paragraph after it: it joins the flow in the same style and size.
    $results += Invoke-Scenario $Word $Source 'insert-a-paragraph' -Edit {
        param($doc, $state)
        $state.index = Find-BodyParagraph $doc
        if ($state.index -lt 0) { return $false }
        $p = $doc.Paragraphs($state.index)
        $state.style = $p.Style.NameLocal
        # What the text ends in is what typing after it continues: the last letter's size and face.
        $lastLetter = Get-TextRange $p
        $lastLetter.MoveStart(1, $lastLetter.End - $lastLetter.Start - 1) | Out-Null
        $state.size = $lastLetter.Font.Size
        $state.font = $lastLetter.Font.Name
        Send-EnterAndType $doc $p 'A paragraph the protocol inserted to see whether it joins the flow'
    } -Verify {
        param($doc, $state)
        $p = $doc.Paragraphs($state.index + 1)
        $present = (Get-Text $p) -like 'A paragraph the protocol inserted*'
        $hidden = $p.Range.Font.Hidden -ne 0
        $detail = "style '$($state.style)' -> '$($p.Style.NameLocal)', size $($state.size) -> $($p.Range.Font.Size), font '$($state.font)' -> '$($p.Range.Font.Name)', hidden=$hidden"
        if (-not $present -or $state.paragraphsAfter -le $state.paragraphsBefore) { return New-Result 'insert-a-paragraph' 'FAIL' "the paragraph did not survive the save; $detail" }
        if ($hidden -or $p.Style.NameLocal -ne $state.style -or $p.Range.Font.Size -ne $state.size -or $p.Range.Font.Name -ne $state.font) { return New-Result 'insert-a-paragraph' 'FAIL' $detail }
        New-Result 'insert-a-paragraph' 'PASS' $detail
    }

    # 4. Restyle the body through Normal: the body size — the most common size of the
    #    document's text — has to follow. A run with a direct size swallows the edit.
    $results += Invoke-Scenario $Word $Source 'restyle-body-through-normal' -Edit {
        param($doc, $state)
        $sizes = @{}
        for ($i = 1; $i -le $doc.Paragraphs.Count; $i++) {
            $p = $doc.Paragraphs($i)
            $length = (Get-Text $p).Length
            if ($length -eq 0) { continue }
            $size = [double]$p.Range.Font.Size
            if ($size -gt 1000) { continue }
            $sizes[$size] = $sizes[$size] + $length
        }
        $state.bodySize = ($sizes.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 1).Key
        $state.normalBefore = $doc.Styles('Normal').Font.Size
        $state.bodyParagraphs = @()
        for ($i = 1; $i -le $doc.Paragraphs.Count; $i++) {
            $p = $doc.Paragraphs($i)
            if ((Get-Text $p).Length -gt 0 -and [double]$p.Range.Font.Size -eq $state.bodySize) { $state.bodyParagraphs += $i }
        }
        $doc.Styles('Normal').Font.Size = $state.normalBefore + 2
    } -Verify {
        param($doc, $state)
        $followed = 0
        foreach ($i in $state.bodyParagraphs) {
            if ([double]$doc.Paragraphs($i).Range.Font.Size -ne $state.bodySize) { $followed++ }
        }
        $total = $state.bodyParagraphs.Count
        $share = if ($total -gt 0) { [Math]::Round(100.0 * $followed / $total) } else { 0 }
        $detail = "body size $($state.bodySize)pt (Normal $($state.normalBefore)pt): $followed of $total paragraphs followed Normal (+2pt) ($share%), pages $($state.pagesBefore) -> $($state.pagesAfter)"
        if ($share -lt 90) { return New-Result 'restyle-body-through-normal' 'FAIL' $detail }
        New-Result 'restyle-body-through-normal' 'PASS' $detail
    }

    # 5. Insert a row into the data table and delete one: the grid is a grid.
    $results += Invoke-Scenario $Word $Source 'insert-a-table-row' -Edit {
        param($doc, $state)
        $table = Find-DataTable $doc
        if (-not $table) { return $false }
        $state.rows = $table.Rows.Count
        $row = $table.Rows.Add($table.Rows($table.Rows.Count))
        $row.Cells(1).Range.Text = 'Protocol row'
    } -Verify {
        param($doc, $state)
        $present = $doc.Content.Text -like '*Protocol row*'
        $table = Find-DataTable $doc
        $detail = "rows $($state.rows) -> $($table.Rows.Count), tables $($state.tablesBefore) -> $($state.tablesAfter)"
        if (-not $present -or $table.Rows.Count -ne $state.rows + 1 -or $state.tablesAfter -ne $state.tablesBefore) { return New-Result 'insert-a-table-row' 'FAIL' $detail }
        New-Result 'insert-a-table-row' 'PASS' $detail
    }
    $results += Invoke-Scenario $Word $Source 'delete-a-table-row' -Edit {
        param($doc, $state)
        $table = Find-DataTable $doc
        if (-not $table) { return $false }
        $state.rows = $table.Rows.Count
        $table.Rows($table.Rows.Count - 1).Delete()
    } -Verify {
        param($doc, $state)
        $table = Find-DataTable $doc
        $rows = if ($table) { $table.Rows.Count } else { 0 }
        $detail = "rows $($state.rows) -> $rows, tables $($state.tablesBefore) -> $($state.tablesAfter)"
        if ($rows -ne $state.rows - 1 -or $state.tablesAfter -ne $state.tablesBefore) { return New-Result 'delete-a-table-row' 'FAIL' $detail }
        New-Result 'delete-a-table-row' 'PASS' $detail
    }

    # 6. Continue a list with Enter: the new paragraph is the next item, not a plain line.
    $results += Invoke-Scenario $Word $Source 'continue-a-list' -Edit {
        param($doc, $state)
        $state.index = Find-ListParagraph $doc
        if ($state.index -lt 0) { return $false }
        $p = $doc.Paragraphs($state.index)
        $state.listType = $p.Range.ListFormat.ListType
        Send-EnterAndType $doc $p 'An item the protocol added by continuing the list'
    } -Verify {
        param($doc, $state)
        $p = $doc.Paragraphs($state.index + 1)
        $present = (Get-Text $p) -like 'An item the protocol added*'
        $type = $p.Range.ListFormat.ListType
        $detail = "list type $($state.listType) -> $type"
        if (-not $present -or $type -eq 0) { return New-Result 'continue-a-list' 'FAIL' $detail }
        New-Result 'continue-a-list' 'PASS' $detail
    }

    # 7. Type at the end of the document until it runs onto a new page: what was typed is
    #    visible text on lines it fits, the page count follows, and the new page carries the
    #    document's footer — its page numbers aside.
    $results += Invoke-Scenario $Word $Source 'add-a-page' -Pdf -Edit {
        param($doc, $state)
        $section = $doc.Sections($doc.Sections.Count)
        $state.footer = ($section.Footers($WD_HEADER_FOOTER_PRIMARY).Range.Text -replace "[`r`a0-9]", '').Trim()
        $end = $doc.Content
        $end.Collapse(0)
        $end.Select()
        $selection = $doc.ActiveWindow.Selection
        foreach ($i in 1..70) {
            # Enter between lines, not after the last: an empty paragraph could end a page of its own.
            if ($i -gt 1) { $selection.TypeParagraph() }
            $selection.TypeText("Protocol line $i typed to run the document onto a new page.")
        }
    } -Verify {
        param($doc, $state)
        # Read hidden text too: Word leaves it out of a range's text, and hidden is a finding.
        $typed = $null
        for ($i = $doc.Paragraphs.Count; $i -ge [Math]::Max(1, $doc.Paragraphs.Count - 80) -and -not $typed; $i--) {
            $range = $doc.Paragraphs($i).Range
            $range.TextRetrievalMode.IncludeHiddenText = $true
            if ($range.Text -like 'Protocol line 70 typed*') { $typed = $doc.Paragraphs($i) }
        }
        if (-not $typed) { return New-Result 'add-a-page' 'FAIL' 'the typed lines did not survive the save' }
        $lastPage = $typed.Range.Information($WD_ACTIVE_END_PAGE)
        $hidden = $typed.Range.Font.Hidden -ne 0
        $size = $typed.Range.Font.Size
        $format = $typed.Format
        # wdLineSpaceExactly is 4: an exact line shorter than its text cuts the letters.
        $squeezed = $format.LineSpacingRule -eq 4 -and $format.LineSpacing -lt $size
        $section = $doc.Sections($doc.Sections.Count)
        $footer = ($section.Footers($WD_HEADER_FOOTER_PRIMARY).Range.Text -replace "[`r`a0-9]", '').Trim()
        $detail = "pages $($state.pagesBefore) -> $($state.pagesAfter), last typed line on page $lastPage at ${size}pt, hidden=$hidden, squeezed into $($format.LineSpacing)pt lines=$squeezed, footer kept=$($footer -eq $state.footer)"
        if ($hidden -or $squeezed) { return New-Result 'add-a-page' 'FAIL' "what was typed at the end cannot be read; $detail" }
        if ($state.pagesAfter -le $state.pagesBefore -or $lastPage -ne $state.pagesAfter -or $footer -ne $state.footer) { return New-Result 'add-a-page' 'FAIL' $detail }
        New-Result 'add-a-page' 'PASS' $detail
    }

    # 8. Save and reopen with no edit.
    $results += Invoke-Scenario $Word $Source 'round-trip' -Edit {
        param($doc, $state)
    } -Verify {
        param($doc, $state)
        $same = $state.paragraphsAfter -eq $state.paragraphsBefore -and $state.tablesAfter -eq $state.tablesBefore `
            -and $state.charactersAfter -eq $state.charactersBefore -and $state.pagesAfter -eq $state.pagesBefore
        $detail = "paragraphs $($state.paragraphsBefore) -> $($state.paragraphsAfter), pages $($state.pagesBefore) -> $($state.pagesAfter)"
        if (-not $same) { return New-Result 'round-trip' 'FAIL' $detail }
        New-Result 'round-trip' 'PASS' $detail
    }
    return @($results | Where-Object { $_ })
}

# The inputs are copied first: a build that exports the corpus again while the protocol runs
# would otherwise pull them out from under it.
$inputs = Join-Path $OutputDir 'input'
New-Item -ItemType Directory -Force $inputs | Out-Null
$sources = @($Docx | ForEach-Object { Copy-Item $_ $inputs -Force -PassThru })
try {
    $word = New-Object -ComObject Word.Application
} catch {
    @{ status = 'NOT_RUN'; reason = 'Microsoft Word is not available through COM on this machine' } |
        ConvertTo-Json | Set-Content -Path (Join-Path $OutputDir 'edit-protocol-corpus.json') -Encoding utf8
    Write-Warning 'NOT_RUN: Microsoft Word is not available'
    exit 2
}
$wordVersion = "$($word.Version) ($($word.Build))"
$word.Visible = $false
$word.DisplayAlerts = 0

$documents = @()
try {
    foreach ($source in $sources) {
        try {
            $scenarios = Invoke-Protocol $word $source
            $documents += [ordered]@{ document = $source.BaseName; scenarios = $scenarios }
        } catch {
            $documents += [ordered]@{ document = $source.BaseName; error = $_.Exception.Message; scenarios = @() }
        }
        $line = ($documents[-1].scenarios | ForEach-Object { "$($_.scenario)=$($_.status)" }) -join ' '
        Write-Host ("{0,-36} {1} {2}" -f $source.BaseName, $line, $documents[-1].error)
    }
} finally {
    $word.Quit()
    [Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
    [GC]::Collect(); [GC]::WaitForPendingFinalizers()
}

[ordered]@{
    editor          = 'Microsoft Word'
    version         = $wordVersion
    os              = [System.Environment]::OSVersion.VersionString
    ranAt           = (Get-Date).ToString('o')
    visualJudgement = 'NOT_RUN — whether an edited page still looks right is read from the PDFs beside the copies'
    documents       = $documents
} | ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $OutputDir 'edit-protocol-corpus.json') -Encoding utf8

$failed = @($documents | Where-Object { $_.error -or ($_.scenarios | Where-Object { $_.status -ne 'PASS' }) })
Write-Host "$($documents.Count) documents, $($failed.Count) with a scenario not passing. Protocol: $(Join-Path $OutputDir 'edit-protocol-corpus.json')"
if ($failed.Count -gt 0) { exit 1 }
