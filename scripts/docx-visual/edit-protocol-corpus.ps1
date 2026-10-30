<#
.SYNOPSIS
    Runs the editing protocol over any exported DOCX through Microsoft Word, choosing what to
    edit from the document itself.

.DESCRIPTION
    edit-protocol-word.ps1 exercises the one document it was written for: it finds its targets
    by their text. This protocol finds them by their shape, so it runs over every document of
    the fidelity corpus: the longest paragraph of body text, the first one, the table with the
    most rows, the first list, the end of the document.

    Each scenario edits a copy, saves it, reopens it from disk and records what it measured.
    What it decides by itself is objective: text kept or lost, a paragraph grown or shrunk and
    still above the block after it, a row held to an exact height that would hide what the edit
    added, the body size following the Normal style with room for it wherever the restyle
    reaches — lines, rows, frames and the text drawings hold — a list continued by Enter, text
    typed at the end of the document readable below everything else, a drawing its paragraph
    carries moving with the text it stands beside and standing still where that text does, a save
    that changes nothing. A drawing nailed to the sheet is placed from the page and by nothing
    else, so it is measured and named with what it did and left out of the verdict; which drawings
    those are is held by DocxDrawingAnchorGuardTest, which names each one and fails closed.
    Where a drawing stands, which page a paragraph is drawn on, and the space above a paragraph
    the geometry draws a box for all come from Word's own page geometry, in points down the page
    Word draws them on; a paragraph's first line and the width its text runs to come from a
    range. It reads letters' size, face and
    visibility from the text alone, not the paragraph mark, which a range's font reports with
    it. Whether an edited page still looks right is read by a person from the PDFs written
    beside the lengthened copy and the new-page copy.

    A scenario a document has nothing to edit for — no table, no list, no drawing — is recorded
    N/A with what the document lacks. Word is not installed by the build. When it is absent the
    protocol records NOT_RUN and the script exits 2; a scenario that neither passes nor is N/A
    exits 1.

    -SelfCheck runs the protocol over probe documents it writes itself instead: it holds where it
    reads a drawing's place to where Word's own PDF draws the same drawing, holds the attribution
    with documents one wrong owner fails, and holds what is asked of whom with a document carrying
    a drawing nailed to the sheet beside one that ought to follow its text and does not.

.PARAMETER Docx
    One or more exported .docx files. All are copied into the output directory before the first
    is edited, and only the copies are opened.

.PARAMETER Scenario
    The scenarios to run, by the names the protocol records them under; all of them when
    left out.

.PARAMETER SelfCheck
    Runs the protocol against probe documents it writes itself, instead of over a corpus. It
    holds where it reads a drawing's place to where Word's own PDF puts the same drawing, holds
    the paragraph a drawing is attributed to with documents one wrong owner fails, and holds what
    is asked of whom with documents that carry a drawing nailed to the sheet. Run it after
    changing how a place is read, how an owner is chosen, or what the verdict is taken over.
    Exits 1 if a check fails, 2 without Word.

.PARAMETER OutputDir
    Where the inputs' copies, the edited copies, their PDFs and edit-protocol-corpus.json are
    written. Keep it outside a module's target directory: a build's clean removes it.

.EXAMPLE
    # Export the corpus once, then run the protocol over it (about a minute a document).
    ./mvnw -B -ntp test -f qa/pom.xml -Dtest=DocxFidelityCorpusTest -Dgraphcompose.docxFidelity=export
    ./scripts/docx-visual/edit-protocol-corpus.ps1 -Docx (Get-ChildItem qa/target/docx-fidelity/engine/*.docx).FullName -OutputDir $env:TEMP/docx-edit

.EXAMPLE
    # Check the protocol itself against the places Word draws a drawing at.
    ./scripts/docx-visual/edit-protocol-corpus.ps1 -SelfCheck -OutputDir $env:TEMP/docx-edit-selfcheck
#>
[CmdletBinding(DefaultParameterSetName = 'Corpus')]
param(
    [Parameter(Mandatory = $true, ParameterSetName = 'Corpus')][string[]]$Docx,
    [Parameter(Mandatory = $true)][string]$OutputDir,
    [Parameter(ParameterSetName = 'Corpus')][string[]]$Scenario,
    [Parameter(Mandatory = $true, ParameterSetName = 'SelfCheck')][switch]$SelfCheck
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
$WD_PRINT_VIEW = 3
$WD_SHAPE_RECTANGLE = 1
$WD_TEXT_RECTANGLE = 0
$WD_MAIN_TEXT_STORY = 1
$WD_RELATIVE_VERTICAL_PAGE = 1
$WD_RELATIVE_VERTICAL_PARAGRAPH = 2

# What Word calls each thing a floating drawing can be placed down from, for a message naming one
# this scenario does not know what to ask of. The export writes only the paragraph and the page.
$RELATIVE_VERTICAL = @{
    0 = "the page's margin"; 1 = 'the page'; 2 = 'its paragraph'; 3 = 'the line'
    4 = 'the top margin'; 5 = 'the bottom margin'; 6 = 'the inner margin'; 7 = 'the outer margin'
}

# An exact line shorter than this share of its letters' size cuts them on Word's screen: Word
# sets a single line of its faces at 1.15 to 1.2 of the size.
$EXACT_LINE_SHARE = 1.1

# A frame Word holds to the height it states, as a row of exact height is held.
$WD_FRAME_EXACT = 2

# The smallest size typed text is read at, in points: a paragraph that holds only pictures has
# its runs and mark a point tall, and text typed on from it would continue at that point.
$READABLE_POINTS = 4

# How far a drawing may stand from the text it follows, in points. Word gives its page geometry
# in whole points, so the place read before an edit and the place read after it can each be half
# a point out, and a drawing that moved with its text exactly can read a whole point adrift. The
# limit must therefore be more than a point. At a point and a half it passes every drawing whose
# own drift is under half a point, and where the rounding runs the other way it can pass one that
# drifted by as much as two and a half: the geometry Word states cannot tell the two apart, and
# measuring drift finer than that wants a reading finer than whole points.
$FOLLOWS_WITHIN_POINTS = 1.5

# How far above the space over a paragraph that space is taken to begin, in points. Word states
# its page geometry in whole points, so a drawing drawn exactly at the top of that space — which
# is the point Word reckons the paragraph's offsets from — can read a point above it.
$SPACE_BEGINS_WITHIN_POINTS = 1

# What a document lacks when a scenario has nothing in it to edit.
$NOT_APPLICABLE = @{
    'lengthen-a-paragraph'        = 'no paragraph of 40 characters or more outside a field or a frame'
    'shorten-a-paragraph'         = 'no paragraph of 40 characters or more outside a field or a frame'
    'insert-a-paragraph'          = 'no paragraph of 40 characters or more outside a field or a frame'
    'restyle-body-through-normal' = 'no paragraph whose letters are all one size'
    'insert-a-table-row'          = 'no table of three rows or more whose rows Word reaches one by one'
    'delete-a-table-row'          = 'no table of three rows or more whose rows Word reaches one by one'
    'continue-a-list'             = 'no list item with text'
    'drawings-follow-text'        = 'no paragraph of 40 characters or more outside a field or a frame, or no floating drawing beside text'
}

$ADDED = ' The editing protocol appended this sentence to make the paragraph about twice as long, so that it has to rewrap and whatever stands below it has to move down to make room for it.'

function New-Result {
    param([string]$Name, [string]$Status, [string]$Detail)
    return [ordered]@{ scenario = $Name; status = $Status; detail = $Detail }
}

# How far something moved, from the difference of two places. A place counts the page it is on as
# 10000 points, so a move onto another page reads as thousands of points: it is written as the
# pages and the points, since "by 9225" will be read as 9225 points by anyone who has not the
# encoding in mind — a page on and 775pt up the next one is what it means.
function Format-Move {
    param([double]$Points)
    $pages = [Math]::Round($Points / 10000)
    $down = [Math]::Round($Points - $pages * 10000, 1)
    if ($pages -eq 0) { return "${down}pt" }
    return "$(if ($pages -gt 0) { '+' })$pages page$(if ([Math]::Abs($pages) -ne 1) { 's' }) and ${down}pt"
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

# A paragraph of body text: 40 characters or more of it, outside a frame and holding no field.
# Both finders below measure one by this, so that "the first" and "the longest" cannot come to mean
# different things.
function Test-BodyParagraph {
    param($Paragraph)
    # A field — a link, a page number — is not cut in half by an edit; Word refuses it.
    if ($Paragraph.Range.Frames.Count -gt 0 -or $Paragraph.Range.Fields.Count -gt 0) { return $false }
    return (Get-Text $Paragraph).Length -gt 39
}

# The longest paragraph of body text of the main story: the one an editor is most likely to
# rewrite. Its index is kept, since a reopened document has new objects.
function Find-BodyParagraph {
    param($Doc)
    $best = -1; $bestLength = 39
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $p = $Doc.Paragraphs($i)
        if (-not (Test-BodyParagraph $p)) { continue }
        $length = (Get-Text $p).Length
        if ($length -gt $bestLength) { $best = $i; $bestLength = $length }
    }
    return $best
}

# The first paragraph of body text, by the same measure as the longest one. Lengthening it moves
# everything the document sets below it, which is
# as much of a document as one edit can move, and which paragraph is edited decides how many
# drawings stand beside text that moved at all: the longest paragraph of a document is often one of
# the last things it sets, so the scenario that measures drawings against the text they stand
# beside takes the first, where the scenarios that measure an edit's own effect take the longest.
#
# The length is what makes it a paragraph of body text rather than merely the first one. Measured
# over the corpus with the first paragraph holding any text at all: in a document whose first text
# is one character — a glyph in a sidebar's own cell — lengthening it by a sentence grew that cell's
# row past the page, and Word, which will not break the row, drew it over the page's edge, putting
# page 1's rectangles at 1006 and 1510pt. Every drawing of that document was then measured
# against a layout that had collapsed, which says nothing about whether a drawing follows its text.
function Find-FirstBodyParagraph {
    param($Doc)
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        if (Test-BodyParagraph $Doc.Paragraphs($i)) { return $i }
    }
    return -1
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

# The text frame of each drawing that holds text, by its place among the drawings: those of the
# main text, then those of the headers and footers, which Word keeps in one collection of their own.
function Get-TextFrames {
    param($Doc)
    $frames = [ordered]@{}
    $stories = @(@('drawing', $Doc.Shapes), @('header drawing', $Doc.Sections(1).Headers(1).Shapes))
    foreach ($story in $stories) {
        for ($k = 1; $k -le $story[1].Count; $k++) {
            # A picture has no text frame to ask, and says so by throwing.
            try { $frame = $story[1].Item($k).TextFrame; if ($frame.HasText) { $frames["$($story[0]) $k"] = $frame } } catch { }
        }
    }
    return $frames
}

# Every paragraph a restyle of Normal reaches, keyed by its story and its place in it, so that the
# reopened document gives each the same key: the main text, each section's own headers and
# footers, and the text drawings hold.
function Get-StoryParagraphs {
    param($Doc)
    $found = [ordered]@{}
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) { $found["body $i"] = $Doc.Paragraphs($i) }
    for ($s = 1; $s -le $Doc.Sections.Count; $s++) {
        $section = $Doc.Sections($s)
        foreach ($kind in 1..3) {
            foreach ($part in @(@('header', $section.Headers($kind)), @('footer', $section.Footers($kind)))) {
                # A part that repeats the section before it holds no paragraphs of its own.
                if (-not $part[1].Exists -or ($s -gt 1 -and $part[1].LinkToPrevious)) { continue }
                $paragraphs = $part[1].Range.Paragraphs
                for ($i = 1; $i -le $paragraphs.Count; $i++) { $found["$($part[0]) $s.$kind $i"] = $paragraphs.Item($i) }
            }
        }
    }
    $frames = Get-TextFrames $Doc
    foreach ($name in $frames.Keys) {
        $paragraphs = $frames[$name].TextRange.Paragraphs
        for ($i = 1; $i -le $paragraphs.Count; $i++) { $found["$name $i"] = $paragraphs.Item($i) }
    }
    return $found
}

# The size of each paragraph's largest letters, by its key in story order, for the paragraphs with text.
function Get-LargestLetterSizes {
    param($Paragraphs)
    $sizes = [ordered]@{}
    foreach ($key in $Paragraphs.Keys) {
        if ((Get-Text $Paragraphs[$key]).Length -eq 0) { continue }
        $sizes[$key] = Get-LargestLetters $Paragraphs[$key]
    }
    return $sizes
}

# What holds a paragraph to a height it cannot grow past, if anything: a row of exact height or
# a frame of exact height.
function Get-HeldBox {
    param($Range)
    if (Test-ExactRow $Range) { return 'a row of exact height' }
    try {
        if ($Range.Frames.Count -gt 0 -and $Range.Frames(1).HeightRule -eq $WD_FRAME_EXACT) { return 'a frame of exact height' }
    } catch { }
    return $null
}

# The drawings whose text no longer fits them, by their place among the drawings.
function Get-OverflowingFrames {
    param($Doc)
    $frames = Get-TextFrames $Doc
    return @($frames.Keys | Where-Object { $frames[$_].Overflowing })
}

# A paragraph as a reader finds it: its first letters, and the story it stands in.
function Format-Paragraph {
    param($Paragraphs, $Key)
    "'$((Get-Text $Paragraphs[$Key]) -replace '^(.{0,24}).*', '$1')' ($Key)"
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

# Word's page geometry for the main story, read in one walk of the pages and handed back as
# @{ shapes; lines }: the rectangles Word draws the floating shapes in, by the character each
# shape's anchor sits on, and the lines it draws the text in, each with the range it holds. Word
# gives both in points from the top-left corner of the page it draws them on, and a shape
# rectangle's range starts exactly where that shape's anchor does.
#
# Read this way a drawing's place is Word's own. Reckoning it from the anchor instead is what
# this protocol had wrong: a stored offset counts from the top of the anchor paragraph's box,
# the space above the paragraph included, while a range reports the top of the paragraph's
# first line — 11.87pt lower wherever a 12pt space stands above it. Measured on 29 probe
# documents of one 20x10pt rectangle each at offset 0, against the same rectangle in Word's own
# PDF: the page geometry is right within 0.41pt in every one, while the paragraph reckoning is
# out by the space above the paragraph, and for a drawing whose row begins on the page before
# the one it is drawn on it names the wrong page altogether. Word gives the geometry in whole
# points, which is why a drawing is held to following its text within a point and a half.
#
# The lines are what the text side is read from: see Get-ParagraphPlaces for what they do and do
# not give.
function Get-PageGeometry {
    param($Doc)
    $byAnchor = @{}
    # A list, not an array: a page of a long document draws hundreds of lines, and `+=` on an
    # array copies the whole of it for each one.
    $lines = New-Object System.Collections.Generic.List[object]
    $window = $Doc.ActiveWindow
    $view = $window.View.Type
    # Word lays a document out in pages only in print view, and the protocol leaves the view as
    # it found it.
    $window.View.Type = $WD_PRINT_VIEW
    try {
        $pane = $window.Panes(1)
        $pages = $pane.Pages
        $pageCount = $pages.Count
        for ($page = 1; $page -le $pageCount; $page++) {
            $rectangles = $pages.Item($page).Rectangles
            $count = $rectangles.Count
            for ($i = 1; $i -le $count; $i++) {
                $rectangle = $rectangles.Item($i)
                $type = [int]$rectangle.RectangleType
                if ($type -ne $WD_SHAPE_RECTANGLE -and $type -ne $WD_TEXT_RECTANGLE) { continue }
                # A character counts from the start of its own story, and what is read here is
                # the main story's: a page's background is written as a shape anchored in a
                # header, which is drawn on every page that header is and whose rectangle sits
                # on character 0 of that header — the character a body drawing that opens the
                # document sits on too. A rectangle whose range cannot be read at all is
                # left out, and the shape it belongs to is then reported as unmeasurable.
                try {
                    if ([int]$rectangle.Range.StoryType -ne $WD_MAIN_TEXT_STORY) { continue }
                    $start = [int]$rectangle.Range.Start
                } catch { continue }
                if ($type -eq $WD_SHAPE_RECTANGLE) {
                    if (-not $byAnchor.ContainsKey($start)) { $byAnchor[$start] = @() }
                    $byAnchor[$start] += @{
                        id     = "$page.$i"
                        place  = $page * 10000 + [double]$rectangle.Top
                        left   = [double]$rectangle.Left
                        width  = [double]$rectangle.Width
                        height = [double]$rectangle.Height
                    }
                    continue
                }
                # A text rectangle holds a block and not a paragraph — a whole table is one, and
                # its lines run one to a row — so the paragraphs are read from its lines, the
                # finest box Word's geometry draws: a paragraph of its own in the body, the row
                # that holds it in a table. A line whose range cannot be read is left out, and a
                # paragraph no line holds is then reported as unplaced.
                try {
                    $drawn = $rectangle.Lines
                    $lineCount = $drawn.Count
                } catch { continue }
                for ($l = 1; $l -le $lineCount; $l++) {
                    try {
                        $line = $drawn.Item($l)
                        $lines.Add(@{
                            page  = $page
                            top   = [double]$line.Top
                            start = [int]$line.Range.Start
                            end   = [int]$line.Range.End
                        })
                    } catch { continue }
                }
            }
        }
    } finally {
        $window.View.Type = $view
    }
    return @{ shapes = $byAnchor; lines = $lines }
}

# The rectangle Word drew one shape in: anchored on the same character, of the shape's own size,
# and not already another shape's — the first such one, where a character carries several. A
# drawing the page geometry does not name is reported rather than measured — a place reckoned
# from its anchor is what this protocol got wrong, so it is no fallback. Nor is a rectangle of
# another size: Word states the size it draws, clipping nothing (measured, a 700pt rectangle on a
# 595pt page: Word reports 700).
function Find-ShapeRectangle {
    param($Shape, $Rectangles, $Taken)
    $start = [int]$Shape.Anchor.Start
    $onIt = @($Rectangles[$start] | Where-Object { $_ })
    $free = @($onIt | Where-Object { -not $Taken.ContainsKey($_.id) })
    # Word rounds the geometry it states to whole points, so a rectangle's size differs from its
    # shape's by half a point at most — every size measured did. The limit is set at a point
    # rather than at that bound, so that a rectangle a shade off still matches its shape instead
    # of a drawing the instrument could have read being refused.
    $sized = @($free | Where-Object {
        [Math]::Abs($_.width - [double]$Shape.Width) -le 1 -and [Math]::Abs($_.height - [double]$Shape.Height) -le 1
    })
    if ($sized.Count -ne 1) {
        $size = "$([Math]::Round([double]$Shape.Width))x$([Math]::Round([double]$Shape.Height))pt"
        $why = if ($sized.Count -gt 1) {
            "stands in $($sized.Count) rectangles of its own $size, so which of them followed the text is not a question with an answer"
        } elseif ($free.Count -gt 0) {
            "stands on a character carrying $($free.Count) rectangles, none of them $size"
        } elseif ($onIt.Count -gt 0) {
            "stands on a character whose $($onIt.Count) rectangles were all matched to other drawings"
        } else {
            "is drawn in no rectangle of Word's page geometry on the character it is anchored on"
        }
        return @{ place = 0; left = 0; unreadable = $why }
    }
    $Taken[$sized[0].id] = $true
    return @{ place = $sized[0].place; left = $sized[0].left; unreadable = $null }
}

# Where each floating drawing of the main story stands, by its name: its place and its left
# edge, both as Word's page geometry gives them, and what Word places it down from. A drawing
# whose rectangle cannot be told apart carries the reason instead of a place.
#
# What a drawing is placed down from is read from Word rather than from the file, and it is the
# same thing the file says. Measured over the 71 documents of the corpus, both ways round: the
# files hold 774 `wp:anchor` in the body and Word names 774 shapes there, with no anchor the one
# reading has and the other has not, and `wp:positionV relativeFrom` agrees with Word's
# RelativeVerticalPosition on every one — 46 from the page, 14 of them page fills, and 728 from
# their paragraph, with no third answer and no name carried twice. The 32 drawings left are the 32
# DocxDrawingAnchorGuardTest holds a named ledger of, document for document and name for name. The
# 18 further anchors the files hold outside the body are page fills in headers, which belong to the
# sheet and which this reading, being of the main story, does not reach.
function Get-DrawingPlaces {
    param($Doc, $Geometry)
    $rectangles = $Geometry.shapes
    $taken = @{}
    $places = @{}
    foreach ($shape in $Doc.Shapes) {
        # A page's background belongs to the page, not to the text on it.
        if ($shape.Name -like 'Page background*') { continue }
        if ($places.ContainsKey($shape.Name)) { $places[$shape.Name] = $null; continue }
        $place = Find-ShapeRectangle $shape $rectangles $taken
        $place.placedDownFrom = [int]$shape.RelativeVerticalPosition
        $places[$shape.Name] = $place
    }
    # No geometry at all, for a document that has drawings to read, is the instrument failing and
    # not every drawing of that document leaving its text behind: it is raised here rather than
    # reported as a finding against each of them.
    if ($rectangles.Count -eq 0 -and $places.Count -gt 0) {
        throw "Word's page geometry named no rectangle for any of the $($places.Count) drawings of this document"
    }
    return $places
}

# Where each paragraph with text stands, by its index: the page Word draws it on, where its own
# first line starts, where the space above it begins, and how far across its text can run — from
# its first letter to the right edge of its cell, or of the page's margin.
#
# The page comes from the geometry, so that a paragraph's page is the page Word draws it on, as a
# drawing's is. Wherever the geometry names one page for a paragraph it is the page a range reports
# as well — 3629 of the 3647 paragraphs of the corpus's documents with drawings, and every probe
# measured — so this settles nothing a document has shown; it reads the page off the lines Word
# draws rather than asking for it, which cannot be wrong about a page.
#
# The space above the paragraph is read from the geometry where the geometry draws this paragraph a
# box and nothing else: that box's top is the point Word reckons a drawing's offset from, the space
# included, so a drawing standing in that space stands at this paragraph. Where the box holds more
# text than the paragraph there is no such reading to be had: a text rectangle holds a block, and
# inside a table its lines run one to a row, so a row of five paragraphs is drawn as one box —
# measured, and 73 of those 3647 paragraphs have a box of their own. A paragraph in a row therefore
# has no space of its own above it here, and keeps the limit that goes with that (see Find-Owner).
#
# The first line, the left edge and the reach across stay a range's. The geometry has no rectangle
# for a cell either: a row's line runs the width of the table, and of the rectangles Word draws on
# a page only text and shape rectangles carry anything of the text. Measured over four documents of
# the corpus: they hold rectangles of type 0 and type 1, the two read here, and of type 7, once or
# a few times a page, which holds no paragraph's text; no rectangle of a cell or of a row is drawn
# at all. So a cell's text column cannot be read from the geometry.
function Get-ParagraphPlaces {
    param($Doc, $Geometry)
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
        $from = [int]$p.Range.Start
        $to = [int]$p.Range.End
        $holding = @($Geometry.lines | Where-Object { $_.start -le $from -and $from -lt $_.end })
        $pages = @($holding | ForEach-Object { $_.page } | Sort-Object -Unique)
        $page = $null
        $why = $null
        $drawnTwice = $false
        if ($pages.Count -eq 0) {
            $why = "Word's page geometry draws no line holding where paragraph $i starts"
        } elseif ($pages.Count -eq 1) {
            $page = $pages[0]
        } else {
            # A row Word draws on more than one page — repeated as a table's head, or with a page
            # break running through it — has a line on each of those pages, and every one of them
            # holds where each of the row's paragraphs starts, the cell beside included. Which
            # page one of those paragraphs is drawn on is not a question those lines answer one
            # paragraph at a time, so the page a range reports is taken there. Measured: the 18
            # paragraphs of the corpus this happens to are all in a repeated head, where a range
            # reports the first page the row is drawn on and the box it is drawn in there holds
            # that paragraph's own line; in a row a page break runs through, a range reports the
            # page the text is drawn on, which is the page the row's line there is tall enough for.
            $drawnTwice = $true
            $reported = [int]$start.Information($WD_ACTIVE_END_PAGE)
            if ($pages -contains $reported) {
                $page = $reported
            } else {
                $why = "paragraph $i starts in a row Word draws on pages $($pages -join ', ') and a range reports it on page $reported, which is none of them"
            }
        }
        if ($null -eq $page) {
            # A paragraph the reading cannot place cannot be held against a drawing, and a drawing
            # beside it then falls to another paragraph as though this one were not there. The
            # reason is carried so that the scenario can name it before the edit; after the edit
            # such a paragraph counts among the drawings it could not find again, which fails.
            $places[$i] = @{ unplaced = $why; drawnTwice = $drawnTwice }
            continue
        }
        $line = [double]$start.Information($WD_VERTICAL_POSITION_ON_PAGE)
        # At least one line holding where the paragraph starts is on this page — the page was taken
        # from those lines — and such a line is a line of this box too, so the box is never empty.
        $box = @($Geometry.lines | Where-Object { $_.page -eq $page -and $_.start -lt $to -and $_.end -gt $from })
        # The box is this paragraph's own only while every line of it draws this paragraph's text
        # and no other's. A row's line reaches from the row's first character to its last, so a
        # paragraph in a cell shares its box with the rest of the row, and the space above it is
        # the row's rather than its own: such a paragraph is left without one, and is then held to
        # its first line alone, exactly as every paragraph was before.
        $shared = @($box | Where-Object { $_.start -lt $from -or $_.end -gt $to })
        $above = $null
        if ($shared.Count -eq 0) {
            # Not Measure-Object -Property: Windows PowerShell does not reach a hashtable's keys
            # that way, and with $ErrorActionPreference at Stop it throws there.
            $above = [double]($box | ForEach-Object { $_.top } | Measure-Object -Minimum).Minimum
        }
        $places[$i] = @{
            place      = $page * 10000 + $line
            above      = $above
            left       = $left
            right      = $right
            page       = $page
            drawnTwice = $drawnTwice
        }
    }
    # No line for any paragraph of a document that has text is the instrument failing, not a
    # document with no text to hold a drawing against: raised here as the shapes half raises Word
    # naming no rectangle for any of a document's drawings, because the scenario would otherwise
    # find no owner for anything and record the document as having nothing to edit.
    if ($places.Count -gt 0 -and @($places.Values | Where-Object { -not $_.unplaced }).Count -eq 0) {
        throw "none of the $($places.Count) paragraphs with text of this document could be placed; the first: $(@($places.Values)[0].unplaced)"
    }
    return $places
}

# The paragraph a drawing belongs beside: of those Word draws on the page it draws the drawing on,
# the nearest to its top-left corner, by how far that corner stands from where the paragraph's text
# begins plus how far it stands outside the width that text can run to. A drawing standing in the
# space above a paragraph counts as standing no distance down the page from it, which is the one
# thing the page geometry adds here; the width still has its say, so a drawing in the space above a
# paragraph of one column can still belong to the text of another. A dot stands at the start of its
# entry, a rail at its first entry's, a skill's bar beside its label.
#
# A drawing placed from its paragraph stands at the top of that paragraph's box, the space above the
# paragraph included, so against a paragraph read at its first line alone it stood that whole space
# away from its own text — and nearer the text above it wherever the space was the wider of the two.
# That was measured in the change that fixed the drawing's own place: a drawing at the box top of a
# paragraph with 12pt above it stood 11.87pt from its own line and 15.98pt from the line above, so
# 4.11pt of margin stood between a right attribution and a wrong one, and a space wider than the
# line above it — a heading over a single line of body text — would have crossed that. Reading the
# space closes it: within the space the drawing is no distance from its own paragraph, while the
# line above is a line away.
#
# Word states its geometry in whole points, so a drawing standing exactly at the top of the space
# can read a point above it, and the space is taken to begin that point higher. Where a drawing is
# as near two paragraphs, the one whose own first line is nearer it takes it.
#
# Only a paragraph the geometry draws a box for has a space to read: every paragraph of a table's
# row shares the row's box, so none of them has one, and they are told apart as every paragraph was
# before — by their first lines and the width their text runs to. The limit this reading lifts
# elsewhere therefore stands inside a row: where the space above such a paragraph is wider than the
# text above it within that row, its drawing is still held against that text. No drawing of the
# corpus stands in the space above a paragraph that has one, so nothing the corpus measures is
# attributed differently by any of this.
function Find-Owner {
    param($Drawing, $Paragraphs)
    $page = [Math]::Floor($Drawing.place / 10000)
    $top = $Drawing.place % 10000
    $best = $null
    $nearest = [double]::MaxValue; $atLine = [double]::MaxValue
    foreach ($index in $Paragraphs.Keys) {
        $p = $Paragraphs[$index]
        if ($p.unplaced -or $p.page -ne $page) { continue }
        $line = $p.place % 10000
        $across = [Math]::Max(0, $p.left - $Drawing.left) + [Math]::Max(0, $Drawing.left - $p.right)
        $fromLine = [Math]::Abs($line - $top)
        $inItsSpace = $null -ne $p.above -and
            $top -ge $p.above - $SPACE_BEGINS_WITHIN_POINTS -and $top -le $line
        $distance = $(if ($inItsSpace) { 0 } else { $fromLine }) + $across
        if ($distance -gt $nearest) { continue }
        if ($distance -lt $nearest -or $fromLine -lt $atLine) {
            $best = $index; $nearest = $distance; $atLine = $fromLine
        }
    }
    return $best
}

# How one set of drawings fared across the edit: how many stood beside text the edit moved and
# moved with it, how many stood beside text it left standing and stood still too, and which of
# them did neither. Reading only the first of those would let a drawing wander off a page while
# the text it belongs beside stays put, and say nothing of it.
#
# Two sets are measured this way and reported apart: the drawings a paragraph carries, which the
# scenario holds to this, and the drawings nailed to the sheet, which it asks nothing of (see the
# scenario). The measuring is the same for both — what differs is whether the answer is a verdict.
#
# A drawing the reading does not find again is counted apart from one whose own text it cannot place
# again, because the two are not the same question and the sets answer for them differently. A
# drawing the reading no longer finds — Word does not name it after the save and the reopen, or
# names it and another alike so that neither can be told from the other — is a drawing gone from
# the document or one that can no longer be told apart, which is not a matter of what it was placed
# from, so it fails for either set. Text that cannot be placed again is the instrument losing the
# paragraph a drawing was held to, which answers nothing about a drawing nothing is asked of.
function Measure-Following {
    param($Doc, $Owners, $Before, $After, $ParagraphsBefore, $ParagraphsAfter)
    $m = @{ moved = 0; followed = 0; stayed = 0; stood = 0; gone = 0; lost = 0; stray = @(); adrift = @(); unreadable = @() }
    foreach ($name in $Owners.Keys) {
        $owner = $Owners[$name]
        if (-not $After.ContainsKey($name) -or $null -eq $After[$name]) { $m.gone++; continue }
        if (-not $ParagraphsAfter.ContainsKey($owner) -or $ParagraphsAfter[$owner].unplaced) { $m.lost++; continue }
        if ($After[$name].unreadable) { $m.unreadable += "$name $($After[$name].unreadable) after the edit"; continue }
        $ownerMoved = $ParagraphsAfter[$owner].place - $ParagraphsBefore[$owner].place
        $drawingMoved = $After[$name].place - $Before[$name].place
        if ([Math]::Abs($ownerMoved) -gt 0.5) {
            $m.moved++
            if ([Math]::Abs($drawingMoved - $ownerMoved) -le $FOLLOWS_WITHIN_POINTS) { $m.followed++; continue }
            $beside = (Get-Text $Doc.Paragraphs($owner)) -replace '^(.{0,24}).*', '$1'
            $m.stray += "$name by $(Format-Move $drawingMoved) beside '$beside' by $(Format-Move $ownerMoved)"
        } else {
            $m.stayed++
            if ([Math]::Abs($drawingMoved) -le $FOLLOWS_WITHIN_POINTS) { $m.stood++; continue }
            $beside = (Get-Text $Doc.Paragraphs($owner)) -replace '^(.{0,24}).*', '$1'
            $m.adrift += "$name by $(Format-Move $drawingMoved) beside '$beside' that stayed"
        }
    }
    return $m
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
    # Only the false an edit returns for it: PowerShell holds 0 equal to $false, and an edit that
    # ends on a call returning 0 must not read as having nothing to edit.
    if ($applies -is [bool] -and -not $applies) {
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
    #    with a size of its own swallows the edit. The larger letters still have room, wherever
    #    the restyle reached them — the main text, the headers and footers, the text drawings
    #    hold: no paragraph whose letters grew stands on exact lines shorter than its letters
    #    take (the share add-a-page holds typed text to), none sits in a row or frame of exact
    #    height, and no drawing's text runs past it. Word cuts all three on its screen and not
    #    in its PDF, so they are read from the document, not from a picture of it. A paragraph
    #    the restyle did not grow is not judged: the page sets many exact lines tighter than
    #    that share for their own letters, and that is its leading.
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
        $state.lettersBefore = Get-LargestLetterSizes (Get-StoryParagraphs $doc)
        $state.overflowingBefore = Get-OverflowingFrames $doc
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
        # Paragraphs are compared by their place in their story: an edit to the styles adds and
        # removes none, so a different count is a finding of its own.
        if ($state.paragraphsAfter -ne $state.paragraphsBefore) {
            return New-Result 'restyle-body-through-normal' 'FAIL' "the paragraphs changed across the restyle and its reopening, $($state.paragraphsBefore) -> $($state.paragraphsAfter); $detail"
        }
        $paragraphs = Get-StoryParagraphs $doc
        $letters = Get-LargestLetterSizes $paragraphs
        $grown = @($letters.Keys | Where-Object { $state.lettersBefore.Contains($_) -and $letters[$_] -gt $state.lettersBefore[$_] })
        $short = @(); $held = @()
        foreach ($key in $grown) {
            $format = $paragraphs[$key].Format
            if ($format.LineSpacingRule -eq $WD_LINE_SPACE_EXACTLY -and $format.LineSpacing -lt $letters[$key] * $EXACT_LINE_SHARE) {
                $short += "$(Format-Paragraph $paragraphs $key) $($state.lettersBefore[$key]) -> $($letters[$key])pt letters on $([Math]::Round($format.LineSpacing, 2))pt exact lines"
            }
            $box = Get-HeldBox $paragraphs[$key].Range
            if ($box) { $held += "$(Format-Paragraph $paragraphs $key) grew in $box" }
        }
        $overflowing = @(Get-OverflowingFrames $doc | Where-Object { $_ -notin $state.overflowingBefore } | ForEach-Object { "the text of $_ runs past it" })
        $stories = @($grown | ForEach-Object { $_ -replace ' [0-9. ]+$', '' } | Select-Object -Unique)
        $detail += ", $($grown.Count) paragraphs grew in $($stories -join ', '): $($short.Count) on exact lines shorter than $EXACT_LINE_SHARE of their letters, $($held.Count) in a row or frame of exact height, $($overflowing.Count) drawings' text past them"
        $findings = @($short | Select-Object -First 3) + @($held | Select-Object -First 3) + @($overflowing | Select-Object -First 3)
        if ($findings.Count -gt 0) { $detail += '; ' + ($findings -join '; ') }
        if ($share -lt 90 -or $short.Count -gt 0 -or $held.Count -gt 0 -or $overflowing.Count -gt 0) { return New-Result 'restyle-body-through-normal' 'FAIL' $detail }
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

    # 8. Lengthen the first paragraph of body text, and see that each drawing beside text the
    #    edit moved — a timeline's dot, a rail, an icon, a badge — moved with it, by as much and
    #    onto the same page, while each drawing beside text the edit left where it was stayed
    #    there too. A drawing is held to the paragraph nearest its top-left corner when the
    #    document opens — nearest down the page from where that paragraph's text begins, and
    #    across from the width that text runs to — and a drawing standing in the space above a
    #    paragraph counts as standing at its text. That corner, the pages, and the space above a
    #    paragraph the geometry draws a box for are Word's page geometry; the first line, the left
    #    edge and the width come from a range.
    #
    #    Only a drawing a paragraph carries is asked to follow text. One nailed to the sheet —
    #    `wp:positionV relativeFrom="page"` — is placed from the top of the page it is drawn on and
    #    by nothing else, so the text beside it has no claim on where it stands: measured on the
    #    self-check's two alike probe documents, with the paragraph above lengthened by two lines,
    #    such a drawing moved 0.00pt in Word 16 and 0.00pt in LibreOffice 24.2 while its own text
    #    moved 32.04 and 52.50pt, where the same drawing carried by that text's paragraph in the
    #    other document moved 31.96 and 52.50pt with it. Holding one to
    #    the text it does not belong to measures the export's choice of anchor, which is not this
    #    scenario's question and is held elsewhere: DocxDrawingAnchorGuardTest names every drawing
    #    of the corpus still placed from the page, one by one with the reason, and fails closed when
    #    one more appears. So the nailed ones are measured here as the carried ones are, named and
    #    counted in the detail with what they did, and left out of the verdict — a drawing that
    #    ought to follow stays in it, nailed neighbours or none, since what is read is each
    #    drawing's own anchor and not the document it stands in.
    $results += Invoke-Scenario $Word $Source 'drawings-follow-text' -Edit {
        param($doc, $state)
        $state.index = Find-FirstBodyParagraph $doc
        if ($state.index -lt 0 -or $doc.Shapes.Count -eq 0) { return $false }
        $geometry = Get-PageGeometry $doc
        $state.drawings = Get-DrawingPlaces $doc $geometry
        $state.paragraphs = Get-ParagraphPlaces $doc $geometry
        # What the reading could not do before the edit, named rather than left silent: the
        # paragraphs it could not place at all, and the paragraphs of a row Word draws on more than
        # one page, whose own page none of that row's lines answers for.
        $state.unplaced = @($state.paragraphs.Values | Where-Object { $_.unplaced } | ForEach-Object { $_.unplaced })
        $state.inASplitRow = @($state.paragraphs.Values | Where-Object { $_.drawnTwice }).Count
        $state.owners = @{}
        $state.unreadable = @()
        # Drawings this scenario says nothing about: one standing beside no text on its page has
        # nothing to follow, and the count is reported rather than left out, so that a document
        # measuring one drawing of twelve cannot read as a document of one.
        $state.beside = @()
        # The drawings nailed to the sheet, which are asked nothing: those with text beside them,
        # measured as the carried ones are, and those the reading has nothing to say of either way.
        $state.onTheSheet = @{}
        $state.onTheSheetSilent = @()
        # A drawing placed down from something that is neither: nothing is asked of a nailed
        # drawing, so a drawing must not be able to fall out of the reading by carrying a name this
        # scenario does not know. The export writes only the paragraph and the page, and the corpus
        # holds nothing else, so this is a hole closed rather than a case seen.
        $state.placedFromElsewhere = @()
        foreach ($name in $state.drawings.Keys) {
            $place = $state.drawings[$name]
            # Two or more drawings Word names alike are read as nothing at all, and what one of
            # them is placed from cannot be told either, so such a name is named rather than
            # passed over: a drawing that ought to follow must not be able to leave the reading by
            # sharing a name. No document of the corpus carries a name twice.
            if ($null -eq $place) {
                $state.unreadable += "$name is the name of more than one drawing, so neither can be told from the other"
                continue
            }
            $nailed = $place.placedDownFrom -eq $WD_RELATIVE_VERTICAL_PAGE
            if (-not $nailed -and $place.placedDownFrom -ne $WD_RELATIVE_VERTICAL_PARAGRAPH) {
                $called = $RELATIVE_VERTICAL[$place.placedDownFrom]
                if (-not $called) { $called = "what Word calls $($place.placedDownFrom)" }
                $state.placedFromElsewhere += "$name is placed down from $called"
                continue
            }
            # A drawing whose place Word's page geometry does not give is named, not guessed at:
            # the protocol can say nothing about whether it follows its text.
            if ($place.unreadable) {
                if ($nailed) { $state.onTheSheetSilent += "$name $($place.unreadable)" }
                else { $state.unreadable += "$name $($place.unreadable)" }
                continue
            }
            $owner = Find-Owner $place $state.paragraphs
            if ($nailed) {
                if ($null -ne $owner) { $state.onTheSheet[$name] = $owner } else { $state.onTheSheetSilent += "$name stands beside no text on its page" }
                continue
            }
            if ($null -ne $owner) { $state.owners[$name] = $owner } else { $state.beside += $name }
        }
        # Where no drawing found text to stand beside and the reading could not place some of the
        # paragraphs, it is the reading that left them without one — a page whose text lines could
        # not be read, say — and the document must not be recorded as having nothing to measure.
        # A drawing nailed to the sheet found its text as well as a carried one would, so it
        # answers this: it is the reading that is in question here, not what is asked of whom.
        if ($state.owners.Count -eq 0 -and $state.onTheSheet.Count -eq 0 -and $state.unplaced.Count -gt 0) {
            throw "no drawing of this document found text to stand beside, and $($state.unplaced.Count) of its paragraphs could not be placed; the first: $($state.unplaced[0])"
        }
        # Drawings beside no text — a page's backgrounds alone — leave nothing to follow, unless
        # one could not be read at all, which is a finding of its own.
        #
        # A document whose drawings are all nailed to the sheet is not one of those: this scenario
        # asks nothing of them, but it did read them, and recording it N/A would say the document
        # has nothing this scenario edits, which is not so. It is measured, and its detail names
        # them beside the nought carried drawings it holds to the verdict. Nothing is hidden by
        # that: a reading that called every drawing of the corpus nailed would read as green here
        # either way, and what it is held by is DocxDrawingAnchorGuardTest, whose named ledger of
        # the drawings still placed from the page fails closed when one is added to it.
        if ($state.owners.Count -eq 0 -and $state.onTheSheet.Count -eq 0 -and
            $state.onTheSheetSilent.Count -eq 0 -and $state.unreadable.Count -eq 0 -and
            $state.placedFromElsewhere.Count -eq 0) { return $false }
        $p = $doc.Paragraphs($state.index)
        $state.target = (Get-Text $p).Substring(0, 30)
        (Get-TextRange $p).InsertAfter($ADDED)
    } -Verify {
        param($doc, $state)
        $geometry = Get-PageGeometry $doc
        $drawings = Get-DrawingPlaces $doc $geometry
        $paragraphs = Get-ParagraphPlaces $doc $geometry
        $carried = Measure-Following $doc $state.owners $state.drawings $drawings $state.paragraphs $paragraphs
        $moved = $carried.moved; $followed = $carried.followed; $stayed = $carried.stayed
        $stood = $carried.stood; $gone = $carried.gone; $lost = $carried.lost
        $stray = $carried.stray; $adrift = $carried.adrift
        $unreadable = @($state.unreadable) + $carried.unreadable
        $detail = "'$($state.target)…' lengthened; $($state.owners.Count) drawings beside text, $moved beside text that moved, $followed of them followed it, " +
            "$stayed beside text that stayed, $stood of them stood still, $gone the reading no longer finds, $lost whose own text could not be placed again"
        if ($state.beside.Count -gt 0) { $detail += ", $($state.beside.Count) beside no text on their page" }
        # The drawings nailed to the sheet, named one by one and with what each of them did: the
        # verdict leaves them out, so the reader is given the number, which they are, every one that
        # left its text, and the measurement the old denominator was made of. The carried ones are
        # named in the order their numbers run, since they are enumerated from a hashtable and
        # "Drawing 10" would otherwise come before "Drawing 8"; the rest carry their reason with
        # them and are ordered by the same key.
        $byItsNumber = { ($_ -replace '[^0-9]', '') -as [int] }
        $nailedCount = $state.onTheSheet.Count + $state.onTheSheetSilent.Count
        $nailedGone = 0
        if ($nailedCount -gt 0) {
            $onTheSheet = Measure-Following $doc $state.onTheSheet $state.drawings $drawings $state.paragraphs $paragraphs
            $nailedGone = $onTheSheet.gone
            $named = @($state.onTheSheet.Keys | Sort-Object $byItsNumber, { $_ }) +
                @($state.onTheSheetSilent | Sort-Object $byItsNumber, { $_ })
            $detail += "; $nailedCount nailed to the sheet, which this scenario asks nothing of: " + ($named -join '; ')
            $detail += " — $($onTheSheet.moved) of them beside text that moved, $($onTheSheet.followed) followed it, " +
                "$($onTheSheet.stayed) beside text that stayed, $($onTheSheet.stood) stood still, " +
                "$($onTheSheet.gone) the reading no longer finds, $($onTheSheet.lost) whose own text could not be placed again, " +
                "$($onTheSheet.unreadable.Count) whose place the geometry no longer gives"
            # Each nailed drawing that left its text, by name, as the carried ones are named: the
            # count alone would not say which of thirteen tiles were the eleven over moved text.
            $left = @($onTheSheet.stray) + @($onTheSheet.adrift)
            if ($left.Count -gt 0) { $detail += '; of them, ' + ($left -join '; ') }
        }
        if ($state.inASplitRow -gt 0) {
            $detail += ", $($state.inASplitRow) paragraphs in a row Word draws on more than one page"
        }
        # A paragraph the reading could not place is named, so that a drawing held to another
        # paragraph for want of it can be seen for what it is. It is not a failure of the
        # scenario: what the scenario answers for is the drawings, and a paragraph without a
        # place has not been shown to carry one.
        if ($state.unplaced.Count -gt 0) {
            $detail += "; could not place " + (($state.unplaced | Select-Object -First 3) -join '; ')
        }
        if ($stray.Count -gt 0) { $detail += "; not following: " + (($stray | Select-Object -First 3) -join '; ') }
        if ($adrift.Count -gt 0) { $detail += "; moving from text that did not: " + (($adrift | Select-Object -First 3) -join '; ') }
        if ($unreadable.Count -gt 0) {
            $detail += "; $($unreadable.Count) whose place Word's page geometry does not give: " +
                (($unreadable | Select-Object -First 3) -join '; ')
        }
        if ($state.placedFromElsewhere.Count -gt 0) {
            $detail += '; ' + (($state.placedFromElsewhere | Sort-Object $byItsNumber, { $_ }) -join '; ') +
                ", and this scenario does not know what to ask of a drawing placed down from that"
        }
        # A drawing Word no longer names is gone from the document, whichever set it was in: that is
        # content lost over a save and a reopen, and no other scenario counts the shapes.
        if ($stray.Count -gt 0 -or $adrift.Count -gt 0 -or $gone -gt 0 -or $nailedGone -gt 0 -or
            $lost -gt 0 -or $unreadable.Count -gt 0 -or $state.placedFromElsewhere.Count -gt 0) {
            return New-Result 'drawings-follow-text' 'FAIL' $detail
        }
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

# --- The self-check -------------------------------------------------------------------------
# Probe documents of one 20x10pt red rectangle each, written here so the check needs nothing on
# disk. Four hold a rectangle whose place was measured once in Word's own PDF, and the protocol is
# held to that place; each of the four puts it at offset 0, where the place Word draws it at *is*
# the point Word reckons the offset from. The other three run the scenario itself: the same
# rectangle beside the same line of text, anchored to that line's paragraph where it must follow
# the text and nailed to the sheet at the very place it already stands where it is asked nothing,
# and one in a row Word repeats, where its place cannot be read at all. Three more came with the
# reading of the space above a paragraph: a paragraph with more space above it than the line above
# is tall, which must keep its own drawing; a drawing that moves while the text it stands beside
# stays, which the standing-text check must catch; and a row a page break runs through, whose
# paragraph the geometry alone cannot put on a page. Four came with setting the nailed drawings
# outside the verdict: one document holding a nailed drawing and a carried one that leaves its
# text, where the carried one must still fail the scenario; a drawing nailed to a page with no text
# on it, the one disposition the corpus does not reach; a nailed drawing whose place the geometry
# cannot give, which is the same document as the carried one that fails for it; and one drawing
# placed down from the page's margin, which is neither answer and must not pass for either.

$PROBE_CONTENT_TYPES = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>
'@

$PROBE_PACKAGE_RELS = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
'@

$PROBE_DOCUMENT_RELS = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
'@

# For the one probe that needs a header of its own, to put a drawing in a story that is not the
# body's.
$PROBE_HEADER_TYPE = '<Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>'

$PROBE_HEADER_RELS = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/>
</Relationships>
'@

$PROBE_NS = 'xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"' +
    ' xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"' +
    ' xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"' +
    ' xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape"'
$PROBE_RPR = '<w:rPr><w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr>'
$PROBE_SECT = '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>' +
    '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="0" w:footer="0" w:gutter="0"/></w:sectPr>'
# 20x10pt in EMU, and a point in EMU.
$PROBE_WIDTH = 254000
$PROBE_HEIGHT = 127000
$EMU_A_POINT = 12700

function New-ProbeDrawing {
    param([string]$From = 'paragraph', [double]$OffsetPoints = 0, [switch]$InCell, [int]$Id = 1)
    $cell = [int]$InCell.IsPresent
    $across = if ($InCell) { 'column' } else { 'page' }
    $offset = [long]($OffsetPoints * $EMU_A_POINT)
    return '<w:drawing><wp:anchor distT="0" distB="0" distL="0" distR="0" simplePos="0"' +
        ' relativeHeight="251658240" behindDoc="0" locked="0" layoutInCell="' + $cell + '" allowOverlap="1">' +
        '<wp:simplePos x="0" y="0"/>' +
        '<wp:positionH relativeFrom="' + $across + '"><wp:posOffset>0</wp:posOffset></wp:positionH>' +
        '<wp:positionV relativeFrom="' + $From + '"><wp:posOffset>' + $offset + '</wp:posOffset></wp:positionV>' +
        '<wp:extent cx="' + $PROBE_WIDTH + '" cy="' + $PROBE_HEIGHT + '"/>' +
        '<wp:effectExtent l="0" t="0" r="0" b="0"/><wp:wrapNone/>' +
        '<wp:docPr id="' + $Id + '" name="Drawing ' + $Id + '"/><wp:cNvGraphicFramePr/>' +
        '<a:graphic><a:graphicData uri="http://schemas.microsoft.com/office/word/2010/wordprocessingShape">' +
        '<wps:wsp><wps:cNvSpPr/><wps:spPr>' +
        '<a:xfrm><a:off x="0" y="0"/><a:ext cx="' + $PROBE_WIDTH + '" cy="' + $PROBE_HEIGHT + '"/></a:xfrm>' +
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom>' +
        '<a:solidFill><a:srgbClr val="FF0000"/></a:solidFill><a:ln><a:noFill/></a:ln>' +
        '</wps:spPr><wps:bodyPr/></wps:wsp></a:graphicData></a:graphic></wp:anchor></w:drawing>'
}

function New-ProbeParagraph {
    param([string]$Text = '', [int]$Before = 0, [string]$Drawing = '', [switch]$PageBreak)
    $runs = ''
    if ($PageBreak) { $runs += '<w:r>' + $PROBE_RPR + '<w:br w:type="page"/></w:r>' }
    if ($Drawing) { $runs += '<w:r>' + $PROBE_RPR + $Drawing + '</w:r>' }
    if ($Text) { $runs += '<w:r>' + $PROBE_RPR + '<w:t xml:space="preserve">' + $Text + '</w:t></w:r>' }
    return '<w:p><w:pPr><w:spacing w:before="' + $Before + '" w:after="0"/>' + $PROBE_RPR + '</w:pPr>' + $runs + '</w:p>'
}

function New-ProbeCell {
    param([string[]]$Paragraphs)
    return '<w:tc><w:tcPr><w:tcW w:w="4000" w:type="dxa"/></w:tcPr>' + ($Paragraphs -join '') + '</w:tc>'
}

function New-ProbeRow {
    param([string[]]$Cells, [int]$Height = 0, [switch]$Header)
    $props = ''
    if ($Height -gt 0) { $props += '<w:trHeight w:val="' + $Height + '" w:hRule="atLeast"/>' }
    if ($Header) { $props += '<w:tblHeader/>' }
    if ($props) { $props = '<w:trPr>' + $props + '</w:trPr>' }
    return '<w:tr>' + $props + ($Cells -join '') + '</w:tr>'
}

function New-ProbeTable {
    param([string[]]$Rows)
    return '<w:tbl><w:tblPr><w:tblW w:w="8000" w:type="dxa"/><w:tblInd w:w="0" w:type="dxa"/>' +
        '<w:tblLayout w:type="fixed"/><w:tblCellMar>' +
        '<w:top w:w="0" w:type="dxa"/><w:left w:w="0" w:type="dxa"/>' +
        '<w:bottom w:w="0" w:type="dxa"/><w:right w:w="0" w:type="dxa"/>' +
        '</w:tblCellMar></w:tblPr>' +
        '<w:tblGrid><w:gridCol w:w="4000"/><w:gridCol w:w="4000"/></w:tblGrid>' +
        ($Rows -join '') + '</w:tbl>'
}

function New-ProbeDocument {
    param([string]$Path, [string]$Body, [string]$Header = '')
    if (-not ('System.IO.Compression.ZipFile' -as [type])) {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
    }
    if (Test-Path $Path) { Remove-Item $Path -Force }
    $namespaces = $PROBE_NS
    $sect = $PROBE_SECT
    $types = $PROBE_CONTENT_TYPES
    $rels = $PROBE_DOCUMENT_RELS
    if ($Header) {
        $namespaces += ' xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"'
        $sect = $PROBE_SECT -replace '<w:sectPr>', '<w:sectPr><w:headerReference w:type="default" r:id="rId1"/>'
        $types = $PROBE_CONTENT_TYPES -replace '</Types>', ($PROBE_HEADER_TYPE + "`n</Types>")
        $rels = $PROBE_HEADER_RELS
    }
    $document = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document ' + $namespaces +
        '><w:body>' + $Body + $sect + '</w:body></w:document>'
    $parts = @(
        @{ name = '[Content_Types].xml'; text = $types }
        @{ name = '_rels/.rels'; text = $PROBE_PACKAGE_RELS }
        @{ name = 'word/_rels/document.xml.rels'; text = $rels }
        @{ name = 'word/document.xml'; text = $document }
    )
    if ($Header) {
        $parts += @{
            name = 'word/header1.xml'
            text = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:hdr ' + $PROBE_NS + '>' +
                   $Header + '</w:hdr>'
        }
    }
    $zip = [System.IO.Compression.ZipFile]::Open($Path, 'Create')
    try {
        foreach ($part in $parts) {
            $writer = New-Object System.IO.StreamWriter($zip.CreateEntry($part.name).Open())
            try { $writer.Write($part.text) } finally { $writer.Dispose() }
        }
    } finally {
        $zip.Dispose()
    }
    return Get-Item $Path
}

# The body text the protocol lengthens, and the short line a drawing stands beside.
$PROBE_LONG = 'A paragraph of body text long enough for the protocol to pick it out and lengthen it, so that everything below it has to move down to make room.'
$PROBE_SHORT = 'Delivery timeline for the programme.'

function New-ProbePlaces {
    # place is the page times 10000 plus the points down the page, as Get-DrawingPlaces counts
    # it, and left is from the page's left edge: both are where Word's own PDF puts the
    # rectangle's top-left corner, the page carried in the place so that a wrong page cannot be
    # taken for a place half a point out.
    return @(
        @{
            name  = 'space-above-a-paragraph'
            what  = 'a drawing at offset 0 in a body paragraph with 12pt above it'
            place = 10087.98
            left  = 0.00
            body  = (New-ProbeParagraph -Text 'A first line above the one that holds the drawing.') +
                   (New-ProbeParagraph -Text 'Body paragraph that holds the drawing.' -Before 240 -Drawing (New-ProbeDrawing))
        }
        @{
            name  = 'the-second-paragraph-of-a-cell'
            what  = 'a drawing at offset 0 in the second paragraph of a cell, a line below the top of the row'
            place = 10087.98
            left  = 72.00
            body  = New-ProbeTable @((New-ProbeRow @(
                       (New-ProbeCell @((New-ProbeParagraph -Text 'First paragraph of the cell.'),
                                        (New-ProbeParagraph -Text 'Second paragraph holds the drawing.' -Drawing (New-ProbeDrawing -InCell)))),
                       (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))))
        }
        @{
            # A character counts from the start of its own story, so a drawing of a header sits on
            # character 0 of that header — as a page's background does, which is how the export
            # writes one — and a body drawing that opens the document sits on character 0 too. Of
            # the same size, the two are told apart only by the story their character belongs to.
            name   = 'a-header-drawing-on-the-same-character'
            what   = 'a body drawing anchored on character 0 beside a header drawing of its size on character 0 of the header'
            place  = 10072.00
            left   = 0.00
            header = New-ProbeParagraph -Drawing (New-ProbeDrawing -From 'page' -OffsetPoints 20 -Id 2)
            body   = New-ProbeParagraph -Text 'Body text that opens the document with a drawing.' -Drawing (New-ProbeDrawing)
        }
        @{
            name  = 'a-row-that-opens-a-page'
            what  = 'a drawing in the first paragraph of a cell of a row Word opens page 2 with, while the row begins on page 1'
            place = 20072.00
            left  = 72.00
            body  = New-ProbeTable @(
                       (New-ProbeRow -Height 13600 -Cells @(
                           (New-ProbeCell @((New-ProbeParagraph -Text 'A tall first row.'))),
                           (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))),
                       (New-ProbeRow @(
                           (New-ProbeCell @((New-ProbeParagraph -Text 'Second row cell holding the drawing.' -Before 240 -Drawing (New-ProbeDrawing -InCell)))),
                           (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))))
        }
    )
}

function New-ProbeFollows {
    return @(
        @{
            name   = 'a-drawing-anchored-to-its-paragraph'
            status = 'PASS'
            detail = '*1 beside text that moved, 1 of them followed it*'
            what   = 'a drawing anchored to the paragraph it stands beside follows it down'
            body   = (New-ProbeParagraph -Text $PROBE_LONG) +
                     (New-ProbeParagraph -Text $PROBE_SHORT -Drawing (New-ProbeDrawing))
        }
        @{
            name   = 'a-drawing-anchored-to-the-page'
            status = 'PASS'
            # Named, and with what it did: it stood beside text that moved and did not move with
            # it, which is what a drawing placed from the page does and what this scenario does
            # not ask of it. Measured on this very document: 0.00pt in Word 16 and 0.00pt in
            # LibreOffice 24.2 while its text moved 32.04 and 52.50pt.
            detail = '*1 nailed to the sheet, which this scenario asks nothing of: Drawing 1*1 of them beside text that moved, 0 followed it*; of them, Drawing 1 by *'
            # 103.95pt down page 1 is where the probe above it draws the same rectangle — the top
            # of the short paragraph's box, two lines of $PROBE_LONG below the margin, measured in
            # Word. The two documents must look alike, so that what tells them apart is the edit
            # and not the place: a page-anchored drawing elsewhere could be caught for standing
            # beside nothing, which is not what this probe is for.
            what   = 'the same drawing nailed to the sheet at the place it already stands is asked nothing'
            body   = (New-ProbeParagraph -Text $PROBE_LONG) +
                     (New-ProbeParagraph -Text $PROBE_SHORT -Drawing (New-ProbeDrawing -From 'page' -OffsetPoints 103.95))
        }
        @{
            name   = 'a-carried-drawing-beside-a-nailed-one'
            status = 'FAIL'
            # Drawing 2's own name, not the count: the verdict has to come from the carried
            # drawing, and a pattern that any failure would meet would pass a document whose
            # nailed drawing had been let into the verdict again.
            detail = '*nailed to the sheet, which this scenario asks nothing of: Drawing 1*moving from text that did not: Drawing 2 by*'
            # Setting a nailed drawing outside the verdict must not set its neighbours outside it
            # too: the reading is each drawing's own anchor, and a false FAIL traded for a false
            # PASS would be the worse of the two. So one document holds both — Drawing 1 nailed to
            # the sheet at the place it already stands, as the probe above, and Drawing 2 carried
            # by the short paragraph 20pt above its box, where it stands beside the lengthened
            # paragraph's text and so leaves text that did not move. The two are anchored in
            # different paragraphs because both are 20x10pt: on one character Word would draw two
            # rectangles of a size neither could be told apart by.
            what   = 'a drawing that ought to follow is still held to it beside one that need not'
            body   = (New-ProbeParagraph -Text $PROBE_LONG -Drawing (New-ProbeDrawing -From 'page' -OffsetPoints 103.95)) +
                     (New-ProbeParagraph -Text $PROBE_SHORT -Drawing (New-ProbeDrawing -OffsetPoints -20 -Id 2))
        }
        @{
            name   = 'a-nailed-drawing-beside-no-text'
            status = 'PASS'
            detail = '*1 nailed to the sheet, which this scenario asks nothing of: Drawing 1 stands beside no text on its page*0 of them beside text that moved*'
            # The third way a nailed drawing can turn out: no text on its page to stand beside at
            # all. The corpus reaches none of it — all 32 of its nailed drawings found text beside
            # them. A body filling page 1 pushes a row of 300pt onto the page after, both its cells
            # hold a paragraph with no text, and the drawing is nailed to the top of that page from
            # one of them: no paragraph of that page has any text, so none can be placed from and
            # the drawing stands beside none.
            #
            # A paragraph holding only a page break was measured and refused for this: the break is
            # a character of that paragraph's text, so the paragraph is placed, its line is drawn on
            # the page before the break, and Word draws the drawing nailed from it on that page too
            # — where the probe found the body's own paragraph to stand beside.
            what   = 'a drawing nailed to a page with no text on it'
            body   = (New-ProbeParagraph -Text (($PROBE_LONG + ' ') * 23)) +
                     (New-ProbeTable @((New-ProbeRow -Height 6000 -Cells @(
                         (New-ProbeCell @((New-ProbeParagraph -Drawing (New-ProbeDrawing -From 'page' -InCell)))),
                         (New-ProbeCell @((New-ProbeParagraph)))))))
        }
        @{
            name   = 'a-nailed-drawing-in-two-rectangles'
            status = 'PASS'
            detail = '*1 nailed to the sheet, which this scenario asks nothing of: Drawing 1 stands in 2 rectangles of its own 20x10pt*'
            # The same document as one-drawing-in-two-rectangles below, with the drawing nailed to
            # the sheet instead of carried by the row's text. A place the geometry cannot give fails
            # the scenario for a carried drawing, since the instrument cannot then say whether it
            # followed; for a nailed one there was no question to leave unanswered, so it is named
            # with the same reason and fails nothing. The pair is what holds the two apart.
            what   = 'a nailed drawing whose place cannot be read is named and fails nothing'
            body   = (New-ProbeParagraph -Text $PROBE_LONG) +
                     (New-ProbeTable @(
                         (New-ProbeRow -Header -Cells @(
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Repeated head holding the drawing.' -Drawing (New-ProbeDrawing -From 'page' -InCell)))),
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))),
                         (New-ProbeRow -Height 6000 -Cells @(
                             (New-ProbeCell @((New-ProbeParagraph -Text 'A tall row.'))),
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))),
                         (New-ProbeRow -Height 6000 -Cells @(
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Another tall row.'))),
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))),
                         (New-ProbeRow -Height 6000 -Cells @(
                             (New-ProbeCell @((New-ProbeParagraph -Text 'A third tall row, on the page after.'))),
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.')))))))
        }
        @{
            name   = 'two-drawings-of-one-name'
            status = 'FAIL'
            detail = '*Drawing 1 is the name of more than one drawing*'
            # A name Word gives to more than one drawing is read as no drawing at all, so what it
            # is placed from cannot be told either. Were such a name passed over, a drawing that
            # ought to follow could leave the reading without being counted or named anywhere —
            # the same hole this change closes for a drawing placed down from a third thing. No
            # document of the corpus carries a name twice, so the case is written here.
            what   = 'two drawings Word names alike are named, not passed over'
            body   = (New-ProbeParagraph -Text $PROBE_LONG -Drawing (New-ProbeDrawing)) +
                     (New-ProbeParagraph -Text $PROBE_SHORT -Drawing (New-ProbeDrawing))
        }
        @{
            name   = 'a-drawing-placed-from-the-margin'
            status = 'FAIL'
            detail = '*Drawing 1 is placed down from the page*margin*does not know what to ask*'
            # Nothing is asked of a drawing nailed to the sheet, so a drawing must not be able to
            # leave the verdict by carrying a name this scenario does not know either. The export
            # writes only the paragraph and the page and the corpus holds nothing else, so this
            # document is written here to reach the case at all.
            what   = 'a drawing placed down from something that is neither the paragraph nor the page'
            body   = (New-ProbeParagraph -Text $PROBE_LONG) +
                     (New-ProbeParagraph -Text $PROBE_SHORT -Drawing (New-ProbeDrawing -From 'margin' -OffsetPoints 103.95))
        }
        @{
            name   = 'a-paragraph-with-space-above-it-keeps-its-drawing'
            status = 'PASS'
            detail = '*1 beside text that moved, 1 of them followed it*'
            # The drawing stands at the top of the second paragraph's box, which is where the line
            # above it ends: 24pt of space above that paragraph puts its own first line further
            # from the drawing than the line above is tall, so a paragraph read at its first line
            # loses it to the paragraph above. The edit lengthens the first paragraph, whose own
            # top does not move, so the wrong reading leaves the drawing held against text that
            # stands still while the drawing moves — which the standing-text check fails it for.
            # Before that check the two readings would have passed alike.
            what   = 'a drawing in a paragraph with more space above it than the line above is tall'
            body   = (New-ProbeParagraph -Text 'A single line of body text standing above it.') +
                     (New-ProbeParagraph -Text $PROBE_SHORT -Before 480 -Drawing (New-ProbeDrawing))
        }
        @{
            name   = 'a-drawing-that-leaves-text-that-stays'
            status = 'FAIL'
            detail = '*moving from text that did not: Drawing 1 by*'
            # A drawing anchored to the paragraph below the edited one, 20pt above its own box, so
            # that it stands beside the edited paragraph's text. The edit lengthens that text
            # without moving where it starts, and the drawing moves with the paragraph it is
            # anchored to: it leaves the text it stands beside standing. Read only for drawings
            # beside text that moved, this one is not read at all.
            what   = 'a drawing that moves while the text it stands beside stays where it is'
            body   = (New-ProbeParagraph -Text $PROBE_LONG) +
                     (New-ProbeParagraph -Text $PROBE_SHORT -Drawing (New-ProbeDrawing -OffsetPoints -20))
        }
        @{
            name   = 'a-row-the-page-breaks-through'
            status = 'PASS'
            # The count of paragraphs in a row Word draws on more than one page is in the detail
            # so that this probe can be held to having reached that case: without it the probe
            # would pass on any row that happens not to break, and the reading it is written for
            # would go untried. Twelve paragraphs of one cell, after a body that fills page 1 down
            # to its last lines, break over it: the row wants far more lines than the body leaves
            # it, and it keeps breaking until the body leaves it none at all — at which point Word
            # moves the whole row to the next page, and then this probe fails on that count rather
            # than passing quietly on a row that never broke.
            detail = '*1 beside text that moved, 1 of them followed it*in a row Word draws on more than one page*'
            # Every line of a row a page break runs through holds where each of that row's
            # paragraphs starts, so the geometry alone does not say which page one of them is
            # drawn on. The drawing here stands beside the part of the row drawn on the page
            # after, and the scenario has to measure it rather than lose it: a paragraph left
            # without a page is no candidate, and its drawing then stands beside no text at all.
            what   = 'a drawing beside the part of a split row drawn on the page after'
            body   = (New-ProbeParagraph -Text (($PROBE_LONG + ' ') * 23)) +
                     (New-ProbeTable @((New-ProbeRow -Cells @(
                         (New-ProbeCell @(
                             (New-ProbeParagraph -Text 'A cell of twelve paragraphs the break runs through.'),
                             (New-ProbeParagraph -Text 'The second of them.'),
                             (New-ProbeParagraph -Text 'The third of them.'),
                             (New-ProbeParagraph -Text 'The fourth of them.'),
                             (New-ProbeParagraph -Text 'The fifth of them.'),
                             (New-ProbeParagraph -Text 'The sixth of them.'),
                             (New-ProbeParagraph -Text 'The seventh of them.'),
                             (New-ProbeParagraph -Text 'The eighth of them.'),
                             (New-ProbeParagraph -Text 'The ninth of them.'),
                             (New-ProbeParagraph -Text 'The tenth of them.'),
                             (New-ProbeParagraph -Text 'The eleventh of them.'),
                             (New-ProbeParagraph -Text 'The last of them, below the break.' -Drawing (New-ProbeDrawing -InCell)))),
                         (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.')))))))
        }
        @{
            name   = 'one-drawing-in-two-rectangles'
            status = 'FAIL'
            # Not the phrase every refusal carries, but this refusal's own reason: any other would
            # pass a document meant to fail for this one.
            detail = '*stands in 2 rectangles of its own 20x10pt*'
            # A row Word repeats as a table's head is drawn on every page the table reaches, and
            # the drawing it holds with it, so one shape stands in two rectangles anchored on the
            # one character. Which of them followed the text is not a question with an answer, and
            # the protocol says so instead of measuring one of the two.
            what   = 'a drawing in a row Word repeats stands in a rectangle on every page'
            body   = (New-ProbeParagraph -Text $PROBE_LONG) +
                     (New-ProbeTable @(
                         (New-ProbeRow -Header -Cells @(
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Repeated head holding the drawing.' -Drawing (New-ProbeDrawing -InCell)))),
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))),
                         (New-ProbeRow -Height 6000 -Cells @(
                             (New-ProbeCell @((New-ProbeParagraph -Text 'A tall row.'))),
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))),
                         (New-ProbeRow -Height 6000 -Cells @(
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Another tall row.'))),
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.'))))),
                         (New-ProbeRow -Height 6000 -Cells @(
                             (New-ProbeCell @((New-ProbeParagraph -Text 'A third tall row, on the page after.'))),
                             (New-ProbeCell @((New-ProbeParagraph -Text 'Neighbour.')))))))
        }
    )
}

function Invoke-SelfCheck {
    param($Word, [string]$Directory)
    New-Item -ItemType Directory -Force $Directory | Out-Null
    $failures = @()
    $checked = 0
    # Each probe is counted before it is measured and its own errors are caught: with
    # $ErrorActionPreference at Stop, one failing COM call would otherwise leave the whole check
    # with nothing said about any of them.
    foreach ($probe in New-ProbePlaces) {
        $checked++
        try {
            $file = New-ProbeDocument (Join-Path $Directory ($probe.name + '.docx')) $probe.body $probe.header
            $doc = $Word.Documents.Open($file.FullName, [ref]$false, [ref]$true)
            try {
                $drawing = (Get-DrawingPlaces $doc (Get-PageGeometry $doc))['Drawing 1']
                if (-not $drawing) {
                    $failures += "$($probe.name): $($probe.what) — Word names no drawing in the probe"
                } elseif ($drawing.unreadable) {
                    $failures += "$($probe.name): $($probe.what) — no place read: the drawing $($drawing.unreadable)"
                # Half a point: Word states its page geometry in whole points, and the places
                # below were measured in its PDF, which does not round.
                } elseif ([Math]::Abs($drawing.place - $probe.place) -gt 0.5 -or [Math]::Abs($drawing.left - $probe.left) -gt 0.5) {
                    $failures += ("$($probe.name): $($probe.what) — read page {0} at {1:F2}pt, {2:F2}pt across; Word's PDF draws it on page {3} at {4:F2}pt, {5:F2}pt across" -f
                        [Math]::Floor($drawing.place / 10000), ($drawing.place % 10000), $drawing.left,
                        [Math]::Floor($probe.place / 10000), ($probe.place % 10000), $probe.left)
                }
            } finally {
                $doc.Close([ref]$false)
                [Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
            }
        } catch {
            $failures += "$($probe.name): $($probe.what) — the check itself failed: $($_.Exception.Message)"
        }
    }
    # The scenario itself, on the one scenario these documents are for. Nothing runs after the
    # self-check, so the narrowing is not put back.
    $script:Scenario = @('drawings-follow-text')
    foreach ($probe in New-ProbeFollows) {
        $checked++
        try {
            $file = New-ProbeDocument (Join-Path $Directory ($probe.name + '.docx')) $probe.body
            $result = @(Invoke-Protocol $Word $file) | Where-Object { $_.scenario -eq 'drawings-follow-text' }
            # The status alone would be met by any breakage at all where the status wanted is
            # FAIL, so what the protocol measured is read too.
            if ($result.status -ne $probe.status) {
                $failures += "$($probe.name): $($probe.what) — the protocol says $($result.status), not $($probe.status): $($result.detail)"
            } elseif ($result.detail -notlike $probe.detail) {
                $failures += "$($probe.name): $($probe.what) — $($probe.status) for another reason: $($result.detail)"
            }
        } catch {
            $failures += "$($probe.name): $($probe.what) — the check itself failed: $($_.Exception.Message)"
        }
    }
    foreach ($failure in $failures) { Write-Host "SELF-CHECK FAIL $failure" }
    # The probes are written to the first directory; the copies the scenario edits, which is what
    # a person opens to see why a check failed, are written to the output directory itself.
    Write-Host "$checked checks, $($failures.Count) failed. Probes: $Directory, edited copies: $OutputDir"
    return $failures.Count
}

if ($SelfCheck) {
    try {
        $word = New-Object -ComObject Word.Application
    } catch {
        Write-Warning 'NOT_RUN: Microsoft Word is not available'
        exit 2
    }
    $word.Visible = $false
    $word.DisplayAlerts = 0
    try {
        $failed = Invoke-SelfCheck $word (Join-Path $OutputDir 'selfcheck')
    } finally {
        $word.Quit()
        [Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
        [GC]::Collect(); [GC]::WaitForPendingFinalizers()
    }
    if ($failed -gt 0) { exit 1 }
    exit 0
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
