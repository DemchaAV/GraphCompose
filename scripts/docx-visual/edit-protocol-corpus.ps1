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
    added, the body size following the Normal style on lines and rows that still have room for
    it, a list continued by Enter, text typed at the end of the document readable below
    everything else, a drawing moving with the text it stands beside, a save that changes
    nothing. It reads letters' size, face and visibility from the text alone, not the paragraph
    mark, which a range's font reports with it. Whether an edited page still looks right is
    read by a person from the PDFs written beside the lengthened copy and the new-page copy.

    A scenario a document has nothing to edit for — no table, no list, no drawing — is recorded
    N/A with what the document lacks. Word is not installed by the build. When it is absent the
    protocol records NOT_RUN and the script exits 2; a scenario that neither passes nor is N/A
    exits 1.

.PARAMETER Docx
    One or more exported .docx files. All are copied into the output directory before the first
    is edited, and only the copies are opened.

.PARAMETER Scenario
    The scenarios to run, by the names the protocol records them under; all of them when
    left out.

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
    [Parameter(Mandatory = $true)][string]$OutputDir,
    [string[]]$Scenario
)

$ErrorActionPreference = 'Stop'

$SCENARIOS = 'lengthen-a-paragraph', 'shorten-a-paragraph', 'insert-a-paragraph', 'restyle-body-through-normal',
    'insert-a-table-row', 'delete-a-table-row', 'continue-a-list', 'add-a-page', 'drawings-follow-text', 'round-trip'
# `-File` hands "a,b" over as one string: split it, and refuse a name no scenario has rather
# than run nothing and report an empty protocol.
$Scenario = @($Scenario | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$unknown = @($Scenario | Where-Object { $_ -notin $SCENARIOS })
if ($unknown.Count -gt 0) {
    throw "no scenario is called $($unknown -join ', '); the scenarios are $($SCENARIOS -join ', ')"
}

New-Item -ItemType Directory -Force $OutputDir | Out-Null
$OutputDir = (Resolve-Path $OutputDir).Path

# Word constants, numeric so the script needs no type library.
$WD_STATISTIC_PAGES = 2
$WD_ACTIVE_END_PAGE = 3
$WD_HORIZONTAL_POSITION_ON_PAGE = 5
$WD_VERTICAL_POSITION_ON_PAGE = 6
$WD_WITHIN_TABLE = 12
$WD_ROW_HEIGHT_EXACTLY = 2
$WD_EXPORT_PDF = 17
$WD_STYLE_NORMAL = -1
$WD_LINE_SPACE_EXACTLY = 4
$WD_HEADER_FOOTER_PRIMARY = 1
$WD_HEADER_FOOTER_FIRST_PAGE = 2
$WD_HEADER_FOOTER_EVEN_PAGES = 3
$WD_RELATIVE_TO_MARGIN = 0
$WD_RELATIVE_TO_PAGE = 1

# An exact line shorter than this share of its letters' size cuts them on Word's screen: Word
# sets a single line of its faces at 1.15 to 1.2 of the size.
$EXACT_LINE_SHARE = 1.1

# The smallest share of its letters' size Word sets a single line of its faces at. An exact line
# the page set for its letters can be tighter — the page's leading is its own — so a restyle is
# judged by what it changes: letters that grew on an exact line that kept its height, now below
# this share of them, stand on a line too short for them.
$NATURAL_LINE_SHARE = 1.15

# The smallest size typed text is read at, in points: a paragraph that holds only pictures has
# its runs and mark a point tall, and text typed on from it would continue at that point.
$READABLE_POINTS = 4

# What a document lacks when a scenario has nothing in it to edit.
$NOT_APPLICABLE = @{
    'lengthen-a-paragraph'        = 'no paragraph of 40 letters or more outside a field or a frame'
    'shorten-a-paragraph'         = 'no paragraph of 40 letters or more outside a field or a frame'
    'insert-a-paragraph'          = 'no paragraph of 40 letters or more outside a field or a frame'
    'restyle-body-through-normal' = 'no paragraph whose letters are all one size'
    'insert-a-table-row'          = 'no table of three rows or more whose rows Word reaches one by one'
    'delete-a-table-row'          = 'no table of three rows or more whose rows Word reaches one by one'
    'continue-a-list'             = 'no list item with text'
    'drawings-follow-text'        = 'no paragraph to lengthen, or no floating drawing'
}

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

# The size of a paragraph's largest letters: its text's size, or, where its runs differ, the
# largest of its words' — a word of mixed sizes read letter by letter.
function Get-LargestLetters {
    param($Paragraph)
    $text = Get-TextRange $Paragraph
    $size = [double]$text.Font.Size
    if ($size -lt 1000) { return $size }
    $largest = 0
    foreach ($word in $text.Words) {
        $size = [double]$word.Font.Size
        if ($size -lt 1000) { $largest = [Math]::Max($largest, $size); continue }
        foreach ($letter in $word.Characters) {
            $size = [double]$letter.Font.Size
            if ($size -lt 1000) { $largest = [Math]::Max($largest, $size) }
        }
    }
    return $largest
}

# The size of each paragraph's largest letters, by its index, for the paragraphs with text.
function Get-LargestLetterSizes {
    param($Doc)
    $sizes = @{}
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $p = $Doc.Paragraphs($i)
        if ((Get-Text $p).Length -eq 0) { continue }
        $sizes[$i] = Get-LargestLetters $p
    }
    return $sizes
}

# The height of each exact line, by paragraph index, for the paragraphs given.
function Get-ExactLines {
    param($Doc, $Indexes)
    $lines = @{}
    foreach ($i in $Indexes) {
        $format = $Doc.Paragraphs($i).Format
        if ($format.LineSpacingRule -eq $WD_LINE_SPACE_EXACTLY) { $lines[$i] = [double]$format.LineSpacing }
    }
    return $lines
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

# Where each floating drawing of the main story stands, by its name: its place as Get-Place
# counts it, and its left edge. One placed from the page stands at its offset on the page its
# anchor lands on; one placed from its paragraph or its column, at its offset from them.
function Get-DrawingPlaces {
    param($Doc)
    $places = @{}
    foreach ($shape in $Doc.Shapes) {
        # A page's background belongs to the page, not to the text on it.
        if ($shape.Name -like 'Page background*') { continue }
        if ($places.ContainsKey($shape.Name)) { $places[$shape.Name] = $null; continue }
        $anchor = $shape.Anchor.Paragraphs(1).Range.Duplicate
        $anchor.Collapse(1)
        $page = [double]$anchor.Information($WD_ACTIVE_END_PAGE)
        $place = switch ($shape.RelativeVerticalPosition) {
            $WD_RELATIVE_TO_PAGE { $page * 10000 + $shape.Top }
            $WD_RELATIVE_TO_MARGIN { $page * 10000 + $Doc.Sections(1).PageSetup.TopMargin + $shape.Top }
            default { (Get-Place $anchor) + $shape.Top }
        }
        $left = if ($shape.RelativeHorizontalPosition -eq $WD_RELATIVE_TO_PAGE) { $shape.Left } `
            else { [double]$anchor.Information($WD_HORIZONTAL_POSITION_ON_PAGE) + $shape.Left }
        $places[$shape.Name] = @{ place = $place; left = $left }
    }
    return $places
}

# Where each paragraph with text starts, by its index: its place, and how far across its text
# can run — from its first letter to the right edge of its cell, or of the page's margin.
function Get-ParagraphPlaces {
    param($Doc)
    $places = @{}
    $setup = $Doc.Sections(1).PageSetup
    $marginRight = $setup.PageWidth - $setup.RightMargin
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $p = $Doc.Paragraphs($i)
        if ((Get-Text $p).Length -eq 0) { continue }
        $start = $p.Range.Duplicate
        $start.Collapse(1)
        $left = [double]$start.Information($WD_HORIZONTAL_POSITION_ON_PAGE)
        $right = $marginRight
        if ($start.Information($WD_WITHIN_TABLE)) {
            try { $right = $left - $p.LeftIndent + $p.Range.Cells(1).Width } catch { }
        }
        $places[$i] = @{ place = Get-Place $start; left = $left; right = $right }
    }
    return $places
}

# The paragraph a drawing belongs beside: of those with text starting on its page, the nearest
# to its top-left corner, up or down from the paragraph's start and across from its text. A dot stands
# at the start of its entry, a rail at its first entry's, a skill's bar beside its label.
function Find-Owner {
    param($Drawing, $Paragraphs)
    $page = [Math]::Floor($Drawing.place / 10000)
    $best = $null; $nearest = [double]::MaxValue
    foreach ($index in $Paragraphs.Keys) {
        $p = $Paragraphs[$index]
        if ([Math]::Floor($p.place / 10000) -ne $page) { continue }
        $across = [Math]::Max(0, $p.left - $Drawing.left) + [Math]::Max(0, $Drawing.left - $p.right)
        $distance = [Math]::Abs($p.place - $Drawing.place) + $across
        if ($distance -lt $nearest) { $best = $index; $nearest = $distance }
    }
    return $best
}

# An edit Word refuses is a result of that scenario, not the end of the document's protocol.
function Invoke-Scenario {
    param($Word, $Source, [string]$Name, [scriptblock]$Edit, [scriptblock]$Verify, [switch]$Pdf)
    if ($Scenario -and $Name -notin $Scenario) { return $null }
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
    # A document with nothing the scenario edits is recorded so, not left out: a result missing
    # from the protocol reads as one that passed.
    if ($applies -eq $false) {
        return New-Result $Name 'N/A' $NOT_APPLICABLE[$Name]
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
        $lastLetter.Start = $lastLetter.End - 1
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
    #    with a size of its own swallows the edit. The larger letters still have room: no
    #    paragraph whose letters grew keeps an exact line that did not grow with them and now
    #    stands below a natural line for them, and none sits in a row of exact height. Word cuts
    #    both on its screen and not in its PDF, so they are read from the lines, not a picture.
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
        $state.lettersBefore = Get-LargestLetterSizes $doc
        $state.linesBefore = Get-ExactLines $doc $state.lettersBefore.Keys
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
        # The paragraphs are compared by index: a restyle adds and removes none.
        if ($state.paragraphsAfter -ne $state.paragraphsBefore) {
            return New-Result 'restyle-body-through-normal' 'FAIL' "the restyle changed the paragraphs $($state.paragraphsBefore) -> $($state.paragraphsAfter); $detail"
        }
        $letters = Get-LargestLetterSizes $doc
        $grown = @($letters.Keys | Where-Object { $state.lettersBefore.ContainsKey($_) -and $letters[$_] -gt $state.lettersBefore[$_] } | Sort-Object)
        $lines = Get-ExactLines $doc $grown
        # An exact line that kept the share of its letters it was set at — a line the restyle
        # grew too — is the page's leading; one that kept its height under larger letters is not.
        $squeezed = @($grown | Where-Object {
            $lines.ContainsKey($_) -and $state.linesBefore.ContainsKey($_) -and
            $lines[$_] + 0.5 -lt $state.linesBefore[$_] * $letters[$_] / $state.lettersBefore[$_] -and
            $lines[$_] -lt $letters[$_] * $NATURAL_LINE_SHARE })
        $hidden = @($grown | Where-Object { Test-ExactRow $doc.Paragraphs($_).Range })
        $detail += ", $($grown.Count) grew: $($squeezed.Count) on exact lines that did not grow with their letters, $($hidden.Count) in rows of exact height"
        $examples = @($squeezed | Select-Object -First 3 | ForEach-Object {
            "'$((Get-Text $doc.Paragraphs($_)) -replace '^(.{0,24}).*', '$1')' $($state.lettersBefore[$_]) -> $($letters[$_])pt letters on $([Math]::Round($lines[$_], 2))pt lines" })
        if ($examples.Count -gt 0) { $detail += '; ' + ($examples -join '; ') }
        if ($share -lt 90 -or $squeezed.Count -gt 0 -or $hidden.Count -gt 0) { return New-Result 'restyle-body-through-normal' 'FAIL' $detail }
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
    #    What was typed stands below everything else, outside any table, visible, and the page
    #    count follows. Typing on from a paragraph's text, the letters are in that text's size,
    #    on the lines the page set it on; typing in an empty paragraph, they fit its lines. The
    #    footer the new page shows is recorded.
    $results += Invoke-Scenario $Word $Source 'add-a-page' -Pdf -Edit {
        param($doc, $state)
        $last = $doc.Paragraphs($doc.Paragraphs.Count)
        if ((Get-Text $last).Length -gt 0) {
            $lastLetter = Get-TextRange $last
            $lastLetter.Start = $lastLetter.End - 1
            $state.continues = [double]$lastLetter.Font.Size
        }
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
        # A paragraph's exact lines are set for its own text; typed on at another size, the
        # letters stand on lines meant for others. An empty one's lines are judged against the
        # letters alone.
        $squeezed = [double]$size -lt $READABLE_POINTS -or $(if ($null -ne $state.continues) { [double]$size -ne $state.continues } else {
            $format.LineSpacingRule -eq $WD_LINE_SPACE_EXACTLY -and $format.LineSpacing -lt $size * $EXACT_LINE_SHARE })
        $footer = Get-FooterOn $doc.Sections($doc.Sections.Count) $lastPage
        $detail = "$($state.lines) lines typed, pages $($state.pagesBefore) -> $($state.pagesAfter), the last on page $lastPage at ${size}pt continuing $($state.continues)pt text, hidden=$hidden, on exact $($format.LineSpacing)pt lines not set for it=$squeezed, footer there '$footer'"
        if ($hidden -or $squeezed) { return New-Result 'add-a-page' 'FAIL' "what was typed at the end cannot be read as the page set it; $detail" }
        if ($state.pagesAfter -le $state.pagesBefore -or $lastPage -ne $state.pagesAfter) { return New-Result 'add-a-page' 'FAIL' $detail }
        New-Result 'add-a-page' 'PASS' $detail
    }

    # 8. Lengthen the same paragraph as the first scenario, and see that each drawing beside
    #    text the edit moved — a timeline's dot, a rail, an icon, a badge — moved with it, by as
    #    much and onto the same page. A drawing is held to the paragraph nearest its top-left
    #    corner when the document opens.
    $results += Invoke-Scenario $Word $Source 'drawings-follow-text' -Edit {
        param($doc, $state)
        $state.index = Find-BodyParagraph $doc
        if ($state.index -lt 0 -or $doc.Shapes.Count -eq 0) { return $false }
        $state.drawings = Get-DrawingPlaces $doc
        $state.paragraphs = Get-ParagraphPlaces $doc
        $state.owners = @{}
        foreach ($name in $state.drawings.Keys) {
            if ($null -eq $state.drawings[$name]) { continue }
            $owner = Find-Owner $state.drawings[$name] $state.paragraphs
            if ($null -ne $owner) { $state.owners[$name] = $owner }
        }
        $p = $doc.Paragraphs($state.index)
        $state.target = (Get-Text $p).Substring(0, 30)
        (Get-TextRange $p).InsertAfter($ADDED)
    } -Verify {
        param($doc, $state)
        $drawings = Get-DrawingPlaces $doc
        $paragraphs = Get-ParagraphPlaces $doc
        $moved = 0; $followed = 0; $lost = 0; $stray = @()
        foreach ($name in $state.owners.Keys) {
            $owner = $state.owners[$name]
            if (-not $drawings.ContainsKey($name) -or $null -eq $drawings[$name] -or -not $paragraphs.ContainsKey($owner)) { $lost++; continue }
            $ownerMoved = $paragraphs[$owner].place - $state.paragraphs[$owner].place
            if ([Math]::Abs($ownerMoved) -le 0.5) { continue }
            $moved++
            $drawingMoved = $drawings[$name].place - $state.drawings[$name].place
            if ([Math]::Abs($drawingMoved - $ownerMoved) -le 1) { $followed++; continue }
            $stray += "$name by $([Math]::Round($drawingMoved, 1)) beside '$((Get-Text $doc.Paragraphs($owner)) -replace '^(.{0,24}).*', '$1')' by $([Math]::Round($ownerMoved, 1))"
        }
        $detail = "'$($state.target)…' lengthened; $($state.owners.Count) drawings, $moved beside text that moved, $followed of them followed it, $lost not found again"
        if ($stray.Count -gt 0) { $detail += "; not following: " + (($stray | Select-Object -First 3) -join '; ') }
        if ($stray.Count -gt 0 -or $lost -gt 0) { return New-Result 'drawings-follow-text' 'FAIL' $detail }
        New-Result 'drawings-follow-text' 'PASS' $detail
    }

    # 9. Save and reopen with no edit.
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
            # PowerShell hands a one-element array back as its element: a run of one scenario would
            # write that scenario's result where the JSON holds a list.
            $scenarios = @(Invoke-Protocol $word $source)
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

$failed = @($documents | Where-Object { $_.error -or ($_.scenarios | Where-Object { $_.status -notin 'PASS', 'N/A' }) })
Write-Host "$($documents.Count) documents, $($failed.Count) with a scenario not passing. Protocol: $(Join-Path $OutputDir 'edit-protocol-corpus.json')"
if ($failed.Count -gt 0) { exit 1 }
