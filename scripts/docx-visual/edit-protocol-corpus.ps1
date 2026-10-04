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
    What it decides by itself is objective: text kept or lost, a paragraph grown or shrunk and
    still above the block after it, a row held to an exact height that would hide what the edit
    added, the body size following the Normal style, a list continued by Enter, text typed at
    the end of the document readable below everything else, a save that changes nothing. It
    reads letters' size, face and visibility from the text alone, not the paragraph mark, which
    a range's font reports with it. Whether an edited page still looks right is read by a
    person from the PDFs written beside the lengthened copy and the new-page copy.

    Word is not installed by the build. When it is absent the protocol records NOT_RUN and the
    script exits 2; a scenario that does not pass exits 1.

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
$WD_STYLE_NORMAL = -1
$WD_LINE_SPACE_EXACTLY = 4
$WD_HEADER_FOOTER_PRIMARY = 1
$WD_HEADER_FOOTER_FIRST_PAGE = 2
$WD_HEADER_FOOTER_EVEN_PAGES = 3

# An exact line shorter than this share of its letters' size cuts them on Word's screen: Word
# sets a single line of its faces at 1.15 to 1.2 of the size.
$EXACT_LINE_SHARE = 1.1

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

function Get-Text {
    param($Paragraph)
    return ($Paragraph.Range.Text -replace "[`r`a]", '').Trim()
}

# A paragraph's text without its mark, or a cell's end mark: Word does not let an edit touch
# them, and a range's font reports the mark's size and visibility along with the letters'.
function Get-TextRange {
    param($Paragraph)
    $range = $Paragraph.Range.Duplicate
    for ($i = 0; $i -lt 3 -and $range.End -gt $range.Start -and $range.Text -match "[`r`a]$"; $i++) {
        $range.MoveEnd(1, -1) | Out-Null
    }
    return $range
}

# Where a paragraph's last letter stands, as Get-Place counts it.
function Get-EndPlace {
    param($Paragraph)
    $end = Get-TextRange $Paragraph
    $end.Collapse(0)
    return Get-Place $end
}

# How tall a paragraph stands, from its first letter to its last. Word's line statistic reads 0
# inside a table, so the height is measured instead; across a page it counts 10000 a page.
function Get-Extent {
    param($Paragraph)
    return (Get-EndPlace $Paragraph) - (Get-Place $Paragraph.Range)
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

# Every table, nested ones after the table holding them, in one order a reopened document repeats.
function Get-AllTables {
    param($Doc)
    $all = @()
    foreach ($table in $Doc.Tables) {
        $all += $table
        foreach ($nested in $table.Tables) { $all += $nested }
    }
    # The comma keeps a single table an array: PowerShell would hand back the table itself.
    return ,$all
}

# The index, among Get-AllTables, of the table with the most rows: a document's data table.
# A table with cells merged down its rows is left out: Word does not let its rows be reached
# one by one, the way a person adding a row reaches them.
function Find-DataTable {
    param($Doc)
    $tables = Get-AllTables $Doc
    $best = -1; $bestRows = 2
    for ($i = 0; $i -lt $tables.Count; $i++) {
        $rows = $tables[$i].Rows.Count
        if ($rows -le $bestRows) { continue }
        try { $tables[$i].Rows($rows) | Out-Null } catch { continue }
        $best = $i; $bestRows = $rows
    }
    return $best
}

# The row a range sits in, if any, held to an exact height: what an edit adds to it is hidden.
function Test-ExactRow {
    param($Range)
    if (-not $Range.Information($WD_WITHIN_TABLE)) { return $false }
    try { return $Range.Cells(1).HeightRule -eq $WD_ROW_HEIGHT_EXACTLY } catch { return $false }
}

# The letters' size of each paragraph with text, by its index; mixed sizes are left out.
function Get-TextSizes {
    param($Doc)
    $sizes = @{}
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $p = $Doc.Paragraphs($i)
        if ((Get-Text $p).Length -eq 0) { continue }
        $size = [double](Get-TextRange $p).Font.Size
        if ($size -lt 1000) { $sizes[$i] = $size }
    }
    return $sizes
}

# The footer Word shows on a page of a section, digits aside: a page number is not the footer.
function Get-FooterOn {
    param($Section, [int]$Page)
    $kind = $WD_HEADER_FOOTER_PRIMARY
    if ($Page -eq 1 -and $Section.PageSetup.DifferentFirstPageHeaderFooter) {
        $kind = $WD_HEADER_FOOTER_FIRST_PAGE
    } elseif ($Page % 2 -eq 0 -and $Section.PageSetup.OddAndEvenPagesHeaderFooter) {
        $kind = $WD_HEADER_FOOTER_EVEN_PAGES
    }
    return ($Section.Footers($kind).Range.Text -replace "[`r`a0-9]", '').Trim()
}

# An edit Word refuses is a result of that scenario, not the end of the document's protocol.
function Invoke-Scenario {
    param($Word, $Source, [string]$Name, [scriptblock]$Edit, [scriptblock]$Verify, [switch]$Pdf)
    try {
        return Invoke-ScenarioOnce $Word $Source $Name $Edit $Verify -Pdf:$Pdf
    } catch {
        return New-Result $Name 'ERROR' $_.Exception.Message
    }
}

function Invoke-ScenarioOnce {
    param($Word, $Source, [string]$Name, [scriptblock]$Edit, [scriptblock]$Verify, [switch]$Pdf)
    $copy = Join-Path $OutputDir ($Source.BaseName + '-' + $Name + '.docx')
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

    # 1. Lengthen the longest paragraph about twofold: nothing is lost, it grows, no exact row
    #    height hides what was added, and it still ends above the block after it. A paragraph
    #    beside a taller cell grows inside its row, and the row need not grow with it.
    $results += Invoke-Scenario $Word $Source 'lengthen-a-paragraph' -Pdf -Edit {
        param($doc, $state)
        $state.index = Find-BodyParagraph $doc
        if ($state.index -lt 0) { return $false }
        $p = $doc.Paragraphs($state.index)
        $state.target = (Get-Text $p).Substring(0, 30)
        $state.extentBefore = Get-Extent $p
        $state.exactRow = Test-ExactRow $p.Range
        (Get-TextRange $p).InsertAfter($ADDED)
    } -Verify {
        param($doc, $state)
        $p = $doc.Paragraphs($state.index)
        $kept = (Get-Text $p) -like '*whatever stands below it has to move down*'
        $extent = Get-Extent $p
        $next = Get-NextPlace $doc $p
        $above = $null -eq $next -or (Get-EndPlace $p) -le $next
        $detail = "'$($state.target)…' stands $([Math]::Round($state.extentBefore, 1)) -> $([Math]::Round($extent, 1)) tall, ends above the block after it=$above, exact row=$($state.exactRow)"
        if (-not $kept) { return New-Result 'lengthen-a-paragraph' 'FAIL' "the added text was lost; $detail" }
        if ($state.exactRow) { return New-Result 'lengthen-a-paragraph' 'FAIL' "the paragraph sits in a row of exact height, which hides what it grows by; $detail" }
        if ($extent -le $state.extentBefore -or -not $above) { return New-Result 'lengthen-a-paragraph' 'FAIL' $detail }
        New-Result 'lengthen-a-paragraph' 'PASS' $detail
    }

    # 2. Delete the second half of the same paragraph: it stands no taller — a paragraph of one
    #    line stays one — and the block after it comes no lower.
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
        $detail = "removed $($state.removed) characters, $([Math]::Round($state.extentBefore, 1)) -> $([Math]::Round($extent, 1)) tall, block after it not lower=$notLower"
        if ($state.charactersAfter -ge $state.charactersBefore -or $extent -gt $state.extentBefore -or -not $notLower) { return New-Result 'shorten-a-paragraph' 'FAIL' $detail }
        New-Result 'shorten-a-paragraph' 'PASS' $detail
    }

    # 3. Press Enter after it and type: the new paragraph joins the flow in its style, and its
    #    letters in the size and face of the letter typing continues from.
    $results += Invoke-Scenario $Word $Source 'insert-a-paragraph' -Edit {
        param($doc, $state)
        $state.index = Find-BodyParagraph $doc
        if ($state.index -lt 0) { return $false }
        $p = $doc.Paragraphs($state.index)
        $state.style = $p.Style.NameLocal
        $lastLetter = Get-TextRange $p
        $lastLetter.MoveStart(1, $lastLetter.End - $lastLetter.Start - 1) | Out-Null
        $state.size = $lastLetter.Font.Size
        $state.font = $lastLetter.Font.Name
        Send-EnterAndType $doc $p 'A paragraph the protocol inserted to see whether it joins the flow'
    } -Verify {
        param($doc, $state)
        $p = $doc.Paragraphs($state.index + 1)
        $text = Get-TextRange $p
        $present = (Get-Text $p) -like 'A paragraph the protocol inserted*'
        $hidden = $text.Font.Hidden -ne 0
        $detail = "style '$($state.style)' -> '$($p.Style.NameLocal)', size $($state.size) -> $($text.Font.Size), font '$($state.font)' -> '$($text.Font.Name)', hidden=$hidden"
        if (-not $present -or $state.paragraphsAfter -le $state.paragraphsBefore) { return New-Result 'insert-a-paragraph' 'FAIL' "the paragraph did not survive the save; $detail" }
        if ($hidden -or $p.Style.NameLocal -ne $state.style -or $text.Font.Size -ne $state.size -or $text.Font.Name -ne $state.font) { return New-Result 'insert-a-paragraph' 'FAIL' $detail }
        New-Result 'insert-a-paragraph' 'PASS' $detail
    }

    # 4. Restyle the body through Normal by +2pt: the paragraphs whose letters are at the body
    #    size — the size most of the document's letters are set in — grow by those 2pt. A run
    #    with a size of its own swallows the edit.
    $results += Invoke-Scenario $Word $Source 'restyle-body-through-normal' -Edit {
        param($doc, $state)
        $sizes = Get-TextSizes $doc
        $weights = @{}
        foreach ($i in $sizes.Keys) {
            $weights[$sizes[$i]] = $weights[$sizes[$i]] + (Get-Text $doc.Paragraphs($i)).Length
        }
        if ($weights.Count -eq 0) { return $false }
        $state.bodySize = ($weights.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 1).Key
        $state.bodyParagraphs = @($sizes.Keys | Where-Object { $sizes[$_] -eq $state.bodySize })
        $state.normalBefore = $doc.Styles($WD_STYLE_NORMAL).Font.Size
        $doc.Styles($WD_STYLE_NORMAL).Font.Size = $state.normalBefore + 2
    } -Verify {
        param($doc, $state)
        $followed = 0
        foreach ($i in $state.bodyParagraphs) {
            if ([double](Get-TextRange $doc.Paragraphs($i)).Font.Size -eq $state.bodySize + 2) { $followed++ }
        }
        $total = $state.bodyParagraphs.Count
        $share = if ($total -gt 0) { [Math]::Round(100.0 * $followed / $total) } else { 0 }
        $detail = "body size $($state.bodySize)pt (Normal $($state.normalBefore)pt): $followed of $total paragraphs grew with Normal by 2pt ($share%), pages $($state.pagesBefore) -> $($state.pagesAfter)"
        if ($share -lt 90) { return New-Result 'restyle-body-through-normal' 'FAIL' $detail }
        New-Result 'restyle-body-through-normal' 'PASS' $detail
    }

    # 5. Insert a row into the data table and delete one: the grid is a grid. The table is found
    #    again by its place among the document's tables, not by its shape, which the edit changes.
    $results += Invoke-Scenario $Word $Source 'insert-a-table-row' -Edit {
        param($doc, $state)
        $state.table = Find-DataTable $doc
        if ($state.table -lt 0) { return $false }
        $table = (Get-AllTables $doc)[$state.table]
        $state.rows = $table.Rows.Count
        $row = $table.Rows.Add($table.Rows($table.Rows.Count))
        $row.Cells(1).Range.Text = 'Protocol row'
    } -Verify {
        param($doc, $state)
        $present = $doc.Content.Text -like '*Protocol row*'
        $rows = (Get-AllTables $doc)[$state.table].Rows.Count
        $detail = "rows $($state.rows) -> $rows, tables $($state.tablesBefore) -> $($state.tablesAfter)"
        if (-not $present -or $rows -ne $state.rows + 1 -or $state.tablesAfter -ne $state.tablesBefore) { return New-Result 'insert-a-table-row' 'FAIL' $detail }
        New-Result 'insert-a-table-row' 'PASS' $detail
    }
    $results += Invoke-Scenario $Word $Source 'delete-a-table-row' -Edit {
        param($doc, $state)
        $state.table = Find-DataTable $doc
        if ($state.table -lt 0) { return $false }
        $table = (Get-AllTables $doc)[$state.table]
        $state.rows = $table.Rows.Count
        $table.Rows($table.Rows.Count - 1).Delete()
    } -Verify {
        param($doc, $state)
        $rows = (Get-AllTables $doc)[$state.table].Rows.Count
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

    # 7. Type at the end of the document until it runs onto a new page, as a person does: the
    #    caret Word gives the document's end, Enter first where the last paragraph holds text.
    #    What was typed stands below everything else, outside any table, visible, on lines it
    #    fits, and the page count follows. The footer the new page shows is recorded.
    $results += Invoke-Scenario $Word $Source 'add-a-page' -Pdf -Edit {
        param($doc, $state)
        $last = $doc.Paragraphs($doc.Paragraphs.Count)
        $end = $doc.Content
        $end.Collapse(0)
        $end.Select()
        $selection = $doc.ActiveWindow.Selection
        # Ten lines at a time until the page count grows, or 200 lines: a last page with room
        # takes many before it gives way.
        $state.lines = 0
        while ($state.lines -lt 200) {
            foreach ($j in 1..10) {
                $state.lines++
                # Enter between lines, not after the last: an empty paragraph could end a page of its own.
                if ($state.lines -gt 1 -or (Get-Text $last).Length -gt 0) { $selection.TypeParagraph() }
                $selection.TypeText("Protocol line $($state.lines) typed to run the document onto a new page.")
            }
            if ($doc.ComputeStatistics($WD_STATISTIC_PAGES) -gt $state.pagesBefore) { break }
        }
    } -Verify {
        param($doc, $state)
        # Read hidden text too: Word leaves it out of a range's text, and hidden is a finding.
        $typed = $null
        $marker = "Protocol line $($state.lines) typed"
        for ($i = $doc.Paragraphs.Count; $i -ge [Math]::Max(1, $doc.Paragraphs.Count - 210) -and -not $typed; $i--) {
            $range = $doc.Paragraphs($i).Range
            $range.TextRetrievalMode.IncludeHiddenText = $true
            if ($range.Text -match $marker) { $typed = $doc.Paragraphs($i) }
        }
        if (-not $typed) { return New-Result 'add-a-page' 'FAIL' 'the typed lines did not survive the save' }
        if ($typed.Range.Information($WD_WITHIN_TABLE)) {
            return New-Result 'add-a-page' 'FAIL' "what was typed at the end went into a table cell, not below the table; pages $($state.pagesBefore) -> $($state.pagesAfter)"
        }
        $text = Get-TextRange $typed
        $lastPage = $text.Information($WD_ACTIVE_END_PAGE)
        $hidden = $text.Font.Hidden -ne 0
        $size = $text.Font.Size
        $format = $typed.Format
        $squeezed = $format.LineSpacingRule -eq $WD_LINE_SPACE_EXACTLY -and $format.LineSpacing -lt $size * $EXACT_LINE_SHARE
        $footer = Get-FooterOn $doc.Sections($doc.Sections.Count) $lastPage
        $detail = "$($state.lines) lines typed, pages $($state.pagesBefore) -> $($state.pagesAfter), the last on page $lastPage at ${size}pt, hidden=$hidden, exact $($format.LineSpacing)pt lines too short=$squeezed, footer there '$footer'"
        if ($hidden -or $squeezed) { return New-Result 'add-a-page' 'FAIL' "what was typed at the end cannot be read; $detail" }
        if ($state.pagesAfter -le $state.pagesBefore -or $lastPage -ne $state.pagesAfter) { return New-Result 'add-a-page' 'FAIL' $detail }
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
    visualJudgement = 'NOT_RUN: whether an edited page still looks right is read from the PDFs beside the lengthened and new-page copies'
    documents       = $documents
} | ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $OutputDir 'edit-protocol-corpus.json') -Encoding utf8

$failed = @($documents | Where-Object { $_.error -or ($_.scenarios | Where-Object { $_.status -ne 'PASS' }) })
Write-Host "$($documents.Count) documents, $($failed.Count) with a scenario not passing. Protocol: $(Join-Path $OutputDir 'edit-protocol-corpus.json')"
if ($failed.Count -gt 0) { exit 1 }
