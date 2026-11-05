#!/usr/bin/env python3
"""Runs the editing protocol over exported DOCX through LibreOffice.

What edit-protocol-corpus.ps1 does through Microsoft Word, this does through LibreOffice: it opens
each document, makes one edit on a copy, saves the copy as DOCX, opens what was saved and has
LibreOffice set it as a PDF, so what the protocol holds is what a person sees after their own save.
The save with no edit is read against the PDF LibreOffice sets from the document as exported; every
edit is read against the PDF of that unedited save, so a scenario answers for its edit alone and
what the save itself changes is the round trip's to report:

  round-trip               save with no edit: the same pages, every line found and none moved
                           more than 0.525pt down or across, every link still a link
  lengthen-in-a-panel      add a sentence to the longest paragraph in a table cell (a panel, a
                           card, a column of a row)
  lengthen-in-a-drawing    add a sentence to the longest text a drawing holds
                           — both: the whole sentence readable, nothing else lost, no text over text
  edit-the-footer          put a few words at the start of the footer the first page shows: the
                           edit on every page that showed that footer's text, nothing else lost,
                           no text over text
  add-a-page               type at the end until a new page opens: the saved document has grown,
                           the last line typed stands on its last page, typed lines stand at least
                           their size apart, nothing else lost, no text over text, and every page
                           whose header or footer read "Page i of n" or "i / n" before, and every
                           page added, reads its own number and the new total there
  replace-a-picture        give the largest picture a 300 x 60 image: the new image drawn, every
                           picture drawn where it was and as many of them, nothing else lost
  edit-a-link              type inside the first link the save with no edit keeps at its address:
                           that address kept, every link still a link, nothing else lost

"Nothing else lost" holds every line of text from before the edit to the text of its own page or
the next one after it — an edit moves text down, never up — read without whitespace and with every
digit kept but a page number's and its total's, which the edit may change. The link edit is undone
in that text before the search, so the line holding the link is held too. A short line can still be
found in other words of the same two pages; a lost figure cannot hide that way. Page numbers are
read from the top and bottom 15% of each page, where headers and footers stand, so an "a / b" in
the body is not taken for one. Text over text means two lines whose letters, taken from their
baseline up to 0.7 of their size, cross by more than a point both ways, counted as many times as
they cross, and more often than before the edit. Text a PDF clips away is not read as text: the
script tries PyMuPDF on a clipped probe before it reads anything, and does not run where the hidden
text comes back — as it does from Ubuntu 24.04's python3-fitz 1.23.7, and not from PyMuPDF 1.27
or 2.1 from pip. Typed lines are found with their spaces ignored: typing on from a
letter-spaced line, LibreOffice spaces the typed letters too. An address is compared without a
trailing slash, which LibreOffice adds to a bare host on its save. The edited footer is held to
every page that showed its text, another section's pages included: LibreOffice gives each section a
footer of its own when it opens a file, even where the file names one footer part for every
section, and a person editing the footer they see expects it to change wherever it reads the same.

A document with nothing a scenario edits — no table cell with a paragraph of 40 characters or more,
no drawing on the page holding text, no footer with text on the first page, no picture, no link in
the text or its tables that a save keeps — records that scenario N/A with what it lacks.

A move is read within 0.525 of a point rather than 0.5: LibreOffice writes positions on a grid of
0.05 of a point, so a limit of 0.5 is a difference two readings can produce exactly, and the verdict
would then turn on the float noise under the grid instead of on where LibreOffice set a line.

The script needs a Python that imports both LibreOffice's UNO bridge (`uno`) and PyMuPDF, and starts
`soffice` itself. On Ubuntu 24.04 — the LibreOffice the DOCX fidelity job runs:
  apt-get install libreoffice-writer python3-uno python3-venv
  python3 -m venv --system-site-packages /opt/lo-edit && /opt/lo-edit/bin/pip install pymupdf
and run it with /opt/lo-edit/bin/python. Without the bridge, PyMuPDF that leaves clipped text out, or
`soffice`, it records NOT_RUN and exits 2; a scenario that neither passes nor is N/A, a usage
error, or a run that stops, exits 1. The JSON is written after every document. A soffice that dies
is started again for the next document; one that hangs is not caught.

--self-check holds every verdict to probe PDFs it writes with PyMuPDF: each rule is broken once, in
a probe only that rule can fail, and each limit is held from both sides. It needs a PyMuPDF that
leaves clipped text out, and no LibreOffice; without one it exits 2. Run it after changing how a verdict is read or a limit it is read by. Exits 1 if a
check fails.

Examples:
  # Export the corpus once, then run the protocol over it.
  ./mvnw -B -ntp test -f qa/pom.xml -Dtest=DocxFidelityCorpusTest -Dgraphcompose.docxFidelity=export
  /opt/lo-edit/bin/python scripts/docx-visual/edit-protocol-libreoffice.py --output-dir /tmp/lo-edit \\
      qa/target/docx-fidelity/engine/*.docx

  # Check the verdicts against probe PDFs.
  python3 scripts/docx-visual/edit-protocol-libreoffice.py --self-check
"""
import argparse
import collections
import datetime
import difflib
import json
import os
import platform
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import time
import zlib

try:
    import pymupdf as fitz
except ImportError:
    try:
        import fitz
    except ImportError:
        fitz = None

SCENARIOS = ["round-trip", "lengthen-in-a-panel", "lengthen-in-a-drawing", "edit-the-footer",
             "add-a-page", "replace-a-picture", "edit-a-link"]

NOT_APPLICABLE = {
    "lengthen-in-a-panel": "no table cell holds a paragraph of 40 characters or more",
    "lengthen-in-a-drawing": "no drawing on the page holds text",
    "edit-the-footer": "the first page shows no footer with text in it",
    "replace-a-picture": "the document has no picture",
    "edit-a-link": ("the text and its tables hold no link with four characters or more of text that a save"
                    " with no edit keeps at its address"),
}

ADDED = (" The protocol added this sentence so the text grows, and whatever stands below it has to"
         " move down to make room.")
FOOTER_EDIT = "Edited footer. "
LINK_EDIT = "EDIT"
TYPED = "Protocol line {} typed to run the document onto a new page."
REPLACEMENT_PIXELS = (300, 60)

# The grid LibreOffice writes positions on, and the limits read off it.
GRID = 0.05
MOVE_LIMIT = 0.525
# Letters are taken from the baseline up to this share of their size; two lines cross when these
# boxes overlap by more than CROSS_LIMIT both ways.
LETTER_SHARE = 0.7
CROSS_LIMIT = 1.0
# A picture, unlike an icon's dot of colour, covers more than this many square points.
PICTURE_AREA = 1.0
# Headers and footers stand in the top and bottom share of a page this size.
BAND = 0.15
PAGE_NUMBER = re.compile(r"Page (\d+) (?:of|/) (\d+)|(?<![\d/])(\d+) / (\d+)(?![\d/])")


# --- Reading a PDF ------------------------------------------------------------------------------

def clip_flag():
    """PyMuPDF's flag for leaving out text a clip hides, or None where this PyMuPDF has none."""
    return getattr(fitz, "TEXT_CLIP", None) if fitz else None


def clip_honoured():
    """Whether this PyMuPDF leaves out text a clip hides: tried on a probe, not taken from a flag's
    name, since a reader that keeps hidden text would pass every edit whose text was cut off."""
    if clip_flag() is None:
        return False
    try:
        probe = probe_pdf(1, [(0, 72, 100, "Shown on the page.", 10)], clipped=[(0, 72, 140, "Hidden by a clip.", 10)])
        texts = [l["text"] for l in read_lines(probe)] + page_texts(probe)
    except Exception:
        return False
    return any("Shown" in t for t in texts) and not any("Hidden" in t for t in texts)


def read_lines(pdf):
    """Every line of text a person can see: page, baseline y, left x, right x, letter size, text."""
    out = []
    for pno, page in enumerate(pdf):
        for block in page.get_text("dict", flags=fitz.TEXTFLAGS_DICT | clip_flag())["blocks"]:
            for line in block.get("lines", []):
                text = " ".join("".join(s["text"] for s in line["spans"]).split())
                if not text:
                    continue
                spans = [s for s in line["spans"] if s["text"].strip()]
                size = max(s["size"] for s in spans)
                out.append({"page": pno, "y": line["spans"][0]["origin"][1], "x": line["bbox"][0],
                            "right": line["bbox"][2], "size": size, "text": text})
    return out


def page_texts(pdf):
    return [page.get_text("text", flags=fitz.TEXTFLAGS_TEXT | clip_flag()) for page in pdf]


def read_links(pdf):
    """Every link, by address, as many times as the document links to it."""
    return collections.Counter(address(link.get("uri")) or "page %s" % link.get("page")
                               for page in pdf for link in page.get_links())


def read_pictures(pdf):
    """Every picture drawn: page, box, and the pixels of the image drawn there."""
    out = []
    for pno, page in enumerate(pdf):
        for image in page.get_image_info():
            x0, y0, x1, y1 = image["bbox"]
            if (x1 - x0) * (y1 - y0) > PICTURE_AREA:
                out.append((pno, (x0, y0, x1, y1), (image["width"], image["height"])))
    return out


def squash(text):
    """Text without whitespace or digits: a footer read across tab stops, its page number left out."""
    return re.sub(r"[\s\d]", "", text)


def flat(text):
    return re.sub(r"\s", "", text)


def address(url):
    return url.rstrip("/") if url else url


def in_band(pdf, line):
    """Whether a line stands where headers and footers do: the top or bottom BAND of its page."""
    height = pdf[line["page"]].rect.height
    return line["y"] < BAND * height or line["y"] > (1 - BAND) * height


def crossings(lines):
    """Pairs of lines on one page whose letters stand on one another, as many as there are."""
    found = collections.Counter()
    by_page = {}
    for line in lines:
        by_page.setdefault(line["page"], []).append(line)
    for page_lines in by_page.values():
        boxes = [(l["x"], l["y"] - LETTER_SHARE * l["size"], l["right"], l["y"], l["text"]) for l in page_lines]
        for i in range(len(boxes)):
            for j in range(i + 1, len(boxes)):
                a, b = boxes[i], boxes[j]
                across = min(a[2], b[2]) - max(a[0], b[0])
                down = min(a[3], b[3]) - max(a[1], b[1])
                if across > CROSS_LIMIT and down > CROSS_LIMIT:
                    found[tuple(sorted((a[4][:30], b[4][:30])))] += 1
    return found


def new_crossings(reference, edited):
    """Crossings the edit made: a pair already crossing before it, on whichever page, is the export's."""
    return sorted((crossings(read_lines(edited)) - crossings(read_lines(reference))).elements())


def comparable(text):
    """Text as an edit should leave it: whitespace out, and the digits of a page number and total
    out, since the edit may change them; every other digit kept."""
    text = PAGE_NUMBER.sub(lambda m: re.sub(r"\d+", "", m.group(0)), " ".join(text.split()))
    return flat(text)


def lost_lines(reference, edited, undo=None):
    """Lines of text from before the edit not found after it, on their own page or the next one:
    an edit moves text down, never up. A line of a header or footer band made only of a page
    number's parts — "Page", "of", "/", digits — is not held, since the total is meant to change.
    undo is the edit's own text and what it replaced, put back before the search."""
    pages = [comparable(t) for t in page_texts(edited)]
    if undo and undo[0]:
        pages = [p.replace(flat(undo[0]), flat(undo[1]), 1) for p in pages]
    lost = []
    for line in read_lines(reference):
        key = comparable(line["text"])
        if not key or (in_band(reference, line) and re.fullmatch(r"(Page|of|/|\d)+", key)):
            continue
        if key not in "".join(pages[line["page"]:line["page"] + 2]):
            lost.append(line["text"])
    return lost


def page_numbers(pdf):
    """Each page's "Page i of n" and "i / n" readings, taken from its header and footer bands only:
    an "a / b" in the body is no page number."""
    bands = [[] for _ in range(len(pdf))]
    for line in read_lines(pdf):
        if in_band(pdf, line):
            bands[line["page"]].append(line)
    readings = []
    for band in bands:
        text = " ".join(l["text"] for l in sorted(band, key=lambda l: (l["y"], l["x"])))
        readings.append([(int(a or c), int(b or d)) for a, b, c, d in PAGE_NUMBER.findall(text)])
    return readings


def numbering(readings):
    """The pages that read a number when each reads its own and the total; None where no page reads
    one; False where a page reads another number than its own."""
    total = len(readings)
    pages = [i for i, r in enumerate(readings) if r]
    if not pages:
        return None
    if any(pair != (i + 1, total) for i in pages for pair in readings[i]):
        return False
    return pages


# --- Verdicts -----------------------------------------------------------------------------------

def result(scenario, status, detail):
    return {"scenario": scenario, "status": status, "detail": detail}


NUMBERING = {None: "none", False: "not each page's own, so not held"}


def judge_round_trip(reference, edited):
    ref, new = read_lines(reference), read_lines(edited)
    match = difflib.SequenceMatcher(a=[l["text"] for l in ref], b=[l["text"] for l in new], autojunk=False)
    matched = moved = 0
    furthest = 0.0
    for tag, i1, i2, j1, j2 in match.get_opcodes():
        if tag != "equal":
            continue
        for k in range(i2 - i1):
            a, b = ref[i1 + k], new[j1 + k]
            matched += 1
            if a["page"] != b["page"]:
                moved += 1
                continue
            move = max(abs(a["y"] - b["y"]), abs(a["x"] - b["x"]))
            furthest = max(furthest, move)
            if move > MOVE_LIMIT:
                moved += 1
    lost_links = sorted((read_links(reference) - read_links(edited)).elements())
    detail = ("pages %d -> %d, %d lines, %d not found, %d new, %d moved more than %.3fpt (furthest %.2fpt), links lost %s"
              % (len(reference), len(edited), len(ref), len(ref) - matched, len(new) - matched, moved, MOVE_LIMIT,
                 furthest, lost_links))
    held = len(reference) == len(edited) and matched == len(ref) == len(new) and moved == 0 and not lost_links
    return result("round-trip", "PASS" if held else "FAIL", detail)


def judge_lengthened(scenario, reference, edited):
    text = " ".join(" ".join(l["text"] for l in read_lines(edited)).split())
    whole = ADDED.strip() in text
    words = ADDED.split()
    seen = sum(1 for w in words if w in text)
    lost = lost_lines(reference, edited)
    new = new_crossings(reference, edited)
    detail = ("pages %d -> %d, the added sentence readable whole=%s (%d of %d words), lines lost %s, text over text %s"
              % (len(reference), len(edited), whole, seen, len(words), lost[:3], new[:3]))
    return result(scenario, "PASS" if whole and not lost and not new else "FAIL", detail)


def judge_footer(reference, edited, footer):
    marker = squash(footer)
    before, after = page_texts(reference), page_texts(edited)
    showing = [i for i, t in enumerate(before) if marker in squash(t)]
    if not showing:
        return result("edit-the-footer", "FAIL", "the footer's text '%s' was found on no page before the edit" % footer)
    edit = flat(FOOTER_EDIT)
    missing = [i + 1 for i in showing if i >= len(after) or marker not in squash(after[i]) or edit not in flat(after[i])]
    lost = lost_lines(reference, edited)
    new = new_crossings(reference, edited)
    detail = ("the footer showed on pages %s; pages without it and the edit %s; lines lost %s; text over text %s"
              % ([i + 1 for i in showing], missing, lost[:3], new[:3]))
    held = not missing and not lost and not new
    return result("edit-the-footer", "PASS" if held else "FAIL", detail)


def judge_add_a_page(reference, edited, typed, pages_typed=None):
    lines = read_lines(edited)
    # Looked for in the page's whole text: lines set on one another are read as one, and cut where
    # the reader cuts them.
    last = flat("Protocol line %d typed" % typed)
    last_page = max((i for i, t in enumerate(page_texts(edited)) if last in flat(t)), default=None)
    ours = [l for l in lines if "Protocolline" in flat(l["text"]) or "ontoanewpage" in flat(l["text"])]
    squeezed = []
    for a, b in zip(ours, ours[1:]):
        if a["page"] == b["page"] and b["y"] - a["y"] + GRID / 2 < min(a["size"], b["size"]):
            squeezed.append(round(b["y"] - a["y"], 2))
    new = new_crossings(reference, edited)
    lost = lost_lines(reference, edited)
    numbered = numbering(page_numbers(reference))
    renumbered = None
    if numbered:
        held_pages = numbering(page_numbers(edited)) or []
        wanted = set(numbered) | set(range(len(reference), len(edited)))
        renumbered = bool(held_pages) and wanted <= set(held_pages)
    detail = ("pages %d -> %d (%s while typing), the last typed line on page %s, typed lines closer than their size %s,"
              " lines lost %s, text over text %s, pages numbered before %s, numbered after=%s"
              % (len(reference), len(edited), pages_typed, None if last_page is None else last_page + 1, squeezed[:3],
                 lost[:3], new[:3], NUMBERING.get(numbered, None) if not numbered else [i + 1 for i in numbered], renumbered))
    held = (len(edited) > len(reference) and last_page == len(edited) - 1 and not squeezed and not new and not lost
            and renumbered is not False)
    return result("add-a-page", "PASS" if held else "FAIL", detail)


def judge_picture(reference, edited, facts):
    ref, new = read_pictures(reference), read_pictures(edited)
    unplaced = [(p, r) for p, r, _ in ref
                if not any(q == p and all(abs(u - v) <= MOVE_LIMIT for u, v in zip(r, s)) for q, s, _ in new)]
    drawn = sum(1 for _, _, pixels in new if pixels == REPLACEMENT_PIXELS)
    lost = lost_lines(reference, edited)
    detail = ("pictures drawn %d -> %d, the new image drawn %d times, not drawn where they were %s, lines lost %s;"
              " '%s' sized %s -> %s (1/100 mm)"
              % (len(ref), len(new), drawn, [(p + 1, [round(v, 1) for v in r]) for p, r in unplaced[:3]], lost[:3],
                 facts.get("name"), facts.get("sizeBefore"), facts.get("sizeAfter")))
    held = len(ref) == len(new) and not unplaced and drawn > 0 and not lost
    return result("replace-a-picture", "PASS" if held else "FAIL", detail)


def judge_link(reference, edited, facts):
    lost_links = sorted((read_links(reference) - read_links(edited)).elements())
    kept = bool(facts.get("urlBefore")) and address(facts.get("urlAfter")) == address(facts.get("urlBefore"))
    lost = lost_lines(reference, edited, undo=(facts.get("textAfter"), facts.get("textBefore") or ""))
    detail = ("'%s' -> '%s' links to %s -> %s; links lost %s; lines lost %s"
              % (facts.get("textBefore"), facts.get("textAfter"), facts.get("urlBefore"), facts.get("urlAfter"),
                 lost_links, lost[:3]))
    return result("edit-a-link", "PASS" if kept and not lost_links and not lost else "FAIL", detail)


# --- LibreOffice --------------------------------------------------------------------------------

class Office:
    """A headless soffice of the script's own, reached through a pipe."""

    def __init__(self, soffice):
        import uno
        self.uno = uno
        self.profile = tempfile.mkdtemp(prefix="lo-edit-profile-")
        self.pipe = "lo-edit-%d-%d" % (os.getpid(), int(time.time() * 1000))
        self.process = subprocess.Popen(
            [soffice, "--headless", "--invisible", "--norestore", "--nologo", "--nodefault",
             "-env:UserInstallation=" + uno.systemPathToFileUrl(self.profile),
             "--accept=pipe,name=%s;urp;" % self.pipe],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        try:
            local = uno.getComponentContext()
            resolver = local.ServiceManager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", local)
            for _ in range(240):
                try:
                    self.context = resolver.resolve("uno:pipe,name=%s;urp;StarOffice.ComponentContext" % self.pipe)
                    break
                except Exception:
                    if self.process.poll() is not None:
                        raise RuntimeError("soffice exited with %s before it could be reached" % self.process.returncode)
                    time.sleep(0.5)
            else:
                raise RuntimeError("soffice did not answer within two minutes")
            self.desktop = self.service("com.sun.star.frame.Desktop")
        except Exception:
            self.process.kill()
            shutil.rmtree(self.profile, ignore_errors=True)
            raise

    def alive(self):
        try:
            self.desktop.getComponents()
            return self.process.poll() is None
        except Exception:
            return False

    def service(self, name):
        return self.context.ServiceManager.createInstanceWithContext(name, self.context)

    def prop(self, name, value):
        from com.sun.star.beans import PropertyValue
        p = PropertyValue()
        p.Name = name
        p.Value = value
        return p

    def version(self):
        provider = self.service("com.sun.star.configuration.ConfigurationProvider")
        node = provider.createInstanceWithArguments("com.sun.star.configuration.ConfigurationAccess",
                                                    (self.prop("nodepath", "/org.openoffice.Setup/Product"),))
        return "%s %s" % (node.getByName("ooName"), node.getByName("ooSetupVersionAboutBox"))

    def open(self, path):
        return self.desktop.loadComponentFromURL(self.uno.systemPathToFileUrl(os.path.abspath(path)), "_blank", 0,
                                                 (self.prop("Hidden", True),))

    def save(self, doc, path, pdf=False):
        url = self.uno.systemPathToFileUrl(os.path.abspath(path))
        if not pdf:
            doc.storeToURL(url, (self.prop("FilterName", "MS Word 2007 XML"),))
            return
        # Images at the pixels they have: by default the PDF export scales an image down to 300 DPI
        # of the frame it fills, and a replaced picture could then not be told by its pixels.
        data = self.uno.Any("[]com.sun.star.beans.PropertyValue",
                            (self.prop("ReduceImageResolution", False), self.prop("UseLosslessCompression", True)))
        self.uno.invoke(doc, "storeToURL", (url, (self.prop("FilterName", "writer_pdf_Export"),
                                                  self.prop("FilterData", data))))

    def close(self):
        try:
            self.desktop.terminate()
        except Exception:
            pass
        try:
            self.process.wait(timeout=30)
        except Exception:
            self.process.kill()
        shutil.rmtree(self.profile, ignore_errors=True)


def page_count(doc):
    return doc.getCurrentController().getPropertyValue("PageCount")


def paragraphs(text):
    walk = text.createEnumeration()
    while walk.hasMoreElements():
        p = walk.nextElement()
        if p.supportsService("com.sun.star.text.Paragraph"):
            yield p


def cell_paragraphs(doc):
    tables = doc.getTextTables()
    for t in range(tables.getCount()):
        table = tables.getByIndex(t)
        for name in table.getCellNames():
            yield from paragraphs(table.getCellByName(name).getText())


def text_drawings(doc):
    page = doc.getDrawPage()
    for i in range(page.getCount()):
        shape = page.getByIndex(i)
        try:
            if shape.supportsService("com.sun.star.drawing.Text") and shape.getString().strip():
                yield shape
        except Exception:
            pass


def first_page_footer(doc):
    """The footer the first page shows: its own where the first page has one, else the page style's."""
    name = next(paragraphs(doc.getText())).getPropertyValue("PageStyleName") or "Standard"
    style = doc.getStyleFamilies().getByName("PageStyles").getByName(name)
    if not style.getPropertyValue("FooterIsOn"):
        return None
    try:
        own_first = not style.getPropertyValue("FirstIsShared")
    except Exception:
        own_first = False
    return style.getPropertyValue("FooterTextFirst") if own_first else style.getPropertyValue("FooterText")


def pictures(doc):
    found = []
    graphics = doc.getGraphicObjects()
    for i in range(graphics.getCount()):
        found.append(graphics.getByIndex(i))
    page = doc.getDrawPage()
    for i in range(page.getCount()):
        shape = page.getByIndex(i)
        if shape.supportsService("com.sun.star.drawing.GraphicObjectShape"):
            found.append(shape)
    return found


def size_of(shape):
    s = shape.getSize()
    return [s.Width, s.Height]


def links(doc):
    """Each run of linked text, with four characters or more of it, in the order the text has them."""
    for p in list(paragraphs(doc.getText())) + list(cell_paragraphs(doc)):
        portions = p.createEnumeration()
        while portions.hasMoreElements():
            portion = portions.nextElement()
            try:
                if portion.getPropertyValue("HyperLinkURL") and len(portion.getString().strip()) >= 4:
                    yield portion
            except Exception:
                pass


def link_key(portion):
    return portion.getString().strip(), address(portion.getPropertyValue("HyperLinkURL"))


def first_link(doc, kept):
    """The first link whose text the save with no edit still links to the same address, counting
    alike links one by one: where that save already drops an address, the round trip reports it,
    and an edit could add nothing."""
    seen = collections.Counter()
    for portion in links(doc):
        key = link_key(portion)
        seen[key] += 1
        if kept[key] >= seen[key]:
            return portion
    return None


def link_holding(doc, before):
    """The run that holds the edited link's text with the edit typed into it."""
    want = flat(before)
    for p in list(paragraphs(doc.getText())) + list(cell_paragraphs(doc)):
        portions = p.createEnumeration()
        while portions.hasMoreElements():
            portion = portions.nextElement()
            text = portion.getString()
            if LINK_EDIT in text and flat(text.replace(LINK_EDIT, "", 1)) == want:
                return portion
    return None


def write_png(path, width, height, rgb):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
    raw = b"".join(b"\x00" + rgb * width for _ in range(height))
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


class Protocol:
    def __init__(self, office, output):
        self.office = office
        self.output = output
        self.png = os.path.join(output, "replacement.png")
        write_png(self.png, *REPLACEMENT_PIXELS, b"\x20\x60\xc0")
        self.kept_links = collections.Counter()

    def edit(self, scenario, doc, facts):
        """Makes the scenario's edit; False when the document has nothing it edits."""
        if scenario == "round-trip":
            return True
        if scenario == "lengthen-in-a-panel":
            candidates = [p for p in cell_paragraphs(doc) if len(p.getString()) >= 40]
            if not candidates:
                return False
            p = max(candidates, key=lambda q: len(q.getString()))
            facts["target"] = p.getString()[:30]
            p.getText().insertString(p.getEnd(), ADDED, False)
            return True
        if scenario == "lengthen-in-a-drawing":
            shapes = list(text_drawings(doc))
            if not shapes:
                return False
            shape = max(shapes, key=lambda s: len(s.getString()))
            facts["target"] = shape.getString()[:30]
            shape.getText().insertString(shape.getText().getEnd(), ADDED, False)
            return True
        if scenario == "edit-the-footer":
            footer = first_page_footer(doc)
            if footer is None or not squash(footer.getString()):
                return False
            facts["footer"] = footer.getString()
            footer.insertString(footer.getStart(), FOOTER_EDIT, False)
            return True
        if scenario == "add-a-page":
            text = doc.getText()
            before = page_count(doc)
            typed = 0
            while typed < 300 and page_count(doc) <= before:
                for _ in range(10):
                    typed += 1
                    text.insertControlCharacter(text.getEnd(), 0, False)  # a paragraph break: Enter
                    text.insertString(text.getEnd(), TYPED.format(typed), False)
            facts.update(typed=typed, pagesTyped=page_count(doc))
            return True
        if scenario == "replace-a-picture":
            found = pictures(doc)
            if not found:
                return False
            picture = max(found, key=lambda s: size_of(s)[0] * size_of(s)[1])
            provider = self.office.service("com.sun.star.graphic.GraphicProvider")
            graphic = provider.queryGraphic((self.office.prop("URL", self.office.uno.systemPathToFileUrl(self.png)),))
            facts.update(name=picture.getName(), sizeBefore=size_of(picture))
            picture.setPropertyValue("Graphic", graphic)
            return True
        if scenario == "edit-a-link":
            portion = first_link(doc, self.kept_links)
            if portion is None:
                return False
            facts.update(urlBefore=portion.getPropertyValue("HyperLinkURL"), textBefore=portion.getString())
            cursor = portion.getText().createTextCursorByRange(portion.getStart())
            cursor.goRight(len(portion.getString()) // 2, False)
            portion.getText().insertString(cursor, LINK_EDIT, False)
            return True
        raise ValueError(scenario)

    def reread(self, scenario, doc, facts):
        """What the saved copy holds, read back through LibreOffice."""
        if scenario == "replace-a-picture":
            facts["sizeAfter"] = next((size_of(p) for p in pictures(doc) if p.getName() == facts["name"]), None)
        if scenario == "edit-a-link":
            portion = link_holding(doc, facts["textBefore"])
            facts["textAfter"] = portion.getString() if portion is not None else None
            facts["urlAfter"] = portion.getPropertyValue("HyperLinkURL") if portion is not None else None

    def judge(self, scenario, reference, edited, facts):
        if scenario in ("lengthen-in-a-panel", "lengthen-in-a-drawing"):
            return judge_lengthened(scenario, reference, edited)
        if scenario == "edit-the-footer":
            return judge_footer(reference, edited, facts["footer"])
        if scenario == "add-a-page":
            return judge_add_a_page(reference, edited, facts["typed"], facts.get("pagesTyped"))
        if scenario == "replace-a-picture":
            return judge_picture(reference, edited, facts)
        return judge_link(reference, edited, facts)

    def copy(self, source, name, scenario, facts):
        """Edits a copy, saves it, reopens it and sets it as a PDF; the PDF's path, or None when the
        document has nothing the scenario edits."""
        copy = os.path.join(self.output, "%s-%s.docx" % (name, scenario))
        doc = self.office.open(source)
        try:
            applies = self.edit(scenario, doc, facts)
            if applies:
                self.office.save(doc, copy)
        finally:
            doc.close(True)
        if not applies:
            return None
        doc = self.office.open(copy)
        try:
            self.reread(scenario, doc, facts)
            self.office.save(doc, copy[:-5] + ".pdf", pdf=True)
        finally:
            doc.close(True)
        return copy[:-5] + ".pdf"

    def run(self, source, scenarios):
        name = os.path.splitext(os.path.basename(source))[0]
        exported_pdf = os.path.join(self.output, name + ".pdf")
        try:
            doc = self.office.open(source)
            try:
                self.office.save(doc, exported_pdf, pdf=True)
            finally:
                doc.close(True)
            # The unedited save comes first, every time: it is what each edit is read against.
            saved = fitz.open(self.copy(source, name, "round-trip", {}))
            doc = self.office.open(os.path.join(self.output, "%s-round-trip.docx" % name))
            try:
                self.kept_links = collections.Counter(link_key(p) for p in links(doc))
            finally:
                doc.close(True)
        except Exception as e:
            why = "the document could not be set, or saved with no edit, to read the edit against: %r" % e
            return [result(s, "ERROR", why[:400]) for s in scenarios]
        results = []
        for scenario in scenarios:
            if scenario == "round-trip":
                try:
                    verdict = judge_round_trip(fitz.open(exported_pdf), saved)
                    verdict["file"] = "%s-round-trip.docx" % name
                except Exception as e:
                    verdict = result(scenario, "ERROR", repr(e)[:400])
                results.append(verdict)
                continue
            facts = {}
            try:
                pdf = self.copy(source, name, scenario, facts)
                if pdf is None:
                    results.append(result(scenario, "N/A", NOT_APPLICABLE[scenario]))
                    continue
                verdict = self.judge(scenario, saved, fitz.open(pdf), facts)
                verdict["file"] = os.path.basename(pdf)[:-4] + ".docx"
                results.append(verdict)
            except Exception as e:
                results.append(result(scenario, "ERROR", repr(e)[:400]))
        return results


# --- Self-check ---------------------------------------------------------------------------------

def probe_pdf(pages, lines=(), links=(), pictures_at=(), clipped=()):
    """A PDF of letter-size pages: lines as (page, x, baseline y, text, size), links as (page, rect,
    uri), pictures as (page, rect, png path), and lines drawn under a clip that hides them all."""
    doc = fitz.open()
    for _ in range(pages):
        doc.new_page(width=612, height=792)
    for page, x, y, text, size in clipped:
        doc[page].insert_text((x, y), text, fontsize=size, fontname="helv")
        doc[page].clean_contents()
        xref = doc[page].get_contents()[0]
        doc.update_stream(xref, b"q 0 0 1 1 re W n\n" + doc.xref_stream(xref) + b"\nQ")
    for page, x, y, text, size in lines:
        doc[page].insert_text((x, y), text, fontsize=size, fontname="helv")
    for page, rect, uri in links:
        doc[page].insert_link({"kind": fitz.LINK_URI, "from": fitz.Rect(rect), "uri": uri})
    for page, rect, png in pictures_at:
        doc[page].insert_image(fitz.Rect(rect), filename=png, keep_proportion=False)
    return fitz.open("pdf", doc.tobytes())


def self_check():
    scratch = tempfile.mkdtemp(prefix="lo-edit-selfcheck-")
    try:
        return run_self_check(scratch)
    finally:
        shutil.rmtree(scratch, ignore_errors=True)


def run_self_check(scratch):
    old_png, new_png = os.path.join(scratch, "old.png"), os.path.join(scratch, "new.png")
    write_png(old_png, 50, 50, b"\xc0\x20\x20")
    write_png(new_png, *REPLACEMENT_PIXELS, b"\x20\x60\xc0")
    body = [(0, 72, 100, "A paragraph of body text the probe sets first.", 10),
            (0, 72, 114, "A second line under it, fourteen points down.", 10)]
    link = (0, (72, 90, 200, 102), "mailto:a@example.com")
    checks = []

    def check(name, verdict, expected):
        checks.append((name, verdict["status"], expected, verdict["detail"]))

    checks.append(("the move limit sits half a grid step off the grid",
                   "PASS" if abs(MOVE_LIMIT / GRID - round(MOVE_LIMIT / GRID)) > 0.25 else "FAIL", "PASS", MOVE_LIMIT))
    checks.append(("this PyMuPDF leaves clipped text out", "PASS" if clip_honoured() else "FAIL", "PASS", fitz.VersionBind))

    # The round trip.
    ref = probe_pdf(1, body, links=[link])
    shifted = lambda dx, dy: probe_pdf(1, [(0, 72 + dx, 100 + dy, body[0][3], 10), body[1]], links=[link])
    check("round trip, nothing changed", judge_round_trip(ref, shifted(0, 0)), "PASS")
    check("round trip, a line 0.5pt lower", judge_round_trip(ref, shifted(0, 0.5)), "PASS")
    check("round trip, a line 0.55pt lower", judge_round_trip(ref, shifted(0, 0.55)), "FAIL")
    check("round trip, a line 0.5pt across", judge_round_trip(ref, shifted(0.5, 0)), "PASS")
    check("round trip, a line 0.55pt across", judge_round_trip(ref, shifted(0.55, 0)), "FAIL")
    check("round trip, a line lost", judge_round_trip(ref, probe_pdf(1, body[:1], links=[link])), "FAIL")
    check("round trip, a line more", judge_round_trip(ref, probe_pdf(1, body + [(0, 72, 128, "A line the save added.", 10)], links=[link])), "FAIL")
    check("round trip, a page more", judge_round_trip(ref, probe_pdf(2, body, links=[link])), "FAIL")
    check("round trip, the link lost", judge_round_trip(ref, probe_pdf(1, body)), "FAIL")
    twice = probe_pdf(1, body, links=[link, (0, (72, 104, 200, 116), "mailto:a@example.com")])
    check("round trip, one of two links to one address lost", judge_round_trip(twice, probe_pdf(1, body, links=[link])), "FAIL")

    # Lengthening.
    added = ADDED.strip()
    half = added.rfind(" ", 0, len(added) // 2)  # broken at a space, as LibreOffice breaks it
    sentence = [(0, 72, 128, added[:half], 10), (0, 72, 142, added[half:], 10)]
    lengthen = lambda before, after: judge_lengthened("lengthen-in-a-panel", before, after)
    check("lengthened, the sentence whole", lengthen(ref, probe_pdf(1, body + sentence)), "PASS")
    check("lengthened, the sentence cut short", lengthen(ref, probe_pdf(1, body + sentence[:1])), "FAIL")
    # The clipped half is drawn first, so a reader that keeps clipped text reads the sentence whole.
    check("lengthened, half the sentence clipped away", lengthen(probe_pdf(1), probe_pdf(1, sentence[1:], clipped=sentence[:1])), "FAIL")
    check("lengthened, a line of the page lost", lengthen(ref, probe_pdf(1, body[:1] + sentence)), "FAIL")
    over = [(0, 90, 117, added[:half], 10), (0, 72, 131, added[half:], 10)]
    check("lengthened, the sentence over the next line", lengthen(ref, probe_pdf(1, body + over)), "FAIL")
    crossing = lambda by: [(0, 72, 93 + by, added[:half], 10), (0, 72, 142, added[half:], 10)]
    check("lengthened, letters crossing by 0.9pt", lengthen(ref, probe_pdf(1, body + crossing(0.9))), "PASS")
    check("lengthened, letters crossing by 1.1pt", lengthen(ref, probe_pdf(1, body + crossing(1.1))), "FAIL")
    figure = lambda amount: (0, 72, 300, "Total due %s" % amount, 10)
    check("lengthened, a figure changed", lengthen(probe_pdf(1, body + [figure("1,250.00")]), probe_pdf(1, body + [figure("2,000.00")] + sentence)), "FAIL")
    closing = "Closing words of the page."
    check("lengthened, a line found only two pages on", lengthen(
        probe_pdf(3, body + [(0, 72, 400, closing, 10)]), probe_pdf(3, body + sentence + [(2, 72, 400, closing, 10)])), "FAIL")
    pushed = [(0, 72, 101, "Two lines the export already set on one another.", 10)]
    check("lengthened, a crossing the export had pushed onto the next page",
          lengthen(probe_pdf(2, body + pushed), probe_pdf(2, body + sentence + [(1, 72, 101, pushed[0][3], 10), (1, 72, 100, body[0][3], 10)])), "PASS")
    old = (0, 72, 101, "Two lines the export already set on one another.", 10)
    check("lengthened, text over text the reference already had",
          lengthen(probe_pdf(1, body + [old]), probe_pdf(1, body + [old] + sentence)), "PASS")
    tight = [(0, 72, 100, "Ten point letters on lines ten points apart.", 10),
             (0, 72, 110, "The next line, as tight as a line can be set.", 10)]
    check("lengthened, lines set tight are not text over text",
          lengthen(probe_pdf(1, tight), probe_pdf(1, tight + [(0, 72, 120, added[:half], 10), (0, 72, 130, added[half:], 10)])), "PASS")

    # The footer.
    footer = "Northwind Ltd\tPage 1 of 2"
    foot = lambda p, edited: (p, 72, 760, (FOOTER_EDIT if edited else "") + "Northwind Ltd     Page %d of 2" % (p + 1), 9)
    two = probe_pdf(2, body + [foot(0, False), foot(1, False)])
    check("footer edited on every page", judge_footer(two, probe_pdf(2, body + [foot(0, True), foot(1, True)]), footer), "PASS")
    check("footer edited on one page of two", judge_footer(two, probe_pdf(2, body + [foot(0, True), foot(1, False)]), footer), "FAIL")
    # Three pages, so the third page's footer keeps the second one's text within reach: only the
    # footer rule, and not the search for lost lines, can see the second page lose it.
    foot3 = lambda p, edited: (p, 72, 760, (FOOTER_EDIT if edited else "") + "Northwind Ltd     Page %d of 3" % (p + 1), 9)
    three = probe_pdf(3, body + [foot3(p, False) for p in range(3)])
    check("footer gone from the second page", judge_footer(three, probe_pdf(3, body + [foot3(0, True), foot3(2, True)]), footer), "FAIL")
    check("footer edited over the body", judge_footer(two, probe_pdf(2, body + [foot(0, True), foot(1, True), (0, 72, 755, "Body text above the footer.", 9)]), footer), "FAIL")
    check("footer edited, a line of the page lost", judge_footer(two, probe_pdf(2, body[:1] + [foot(0, True), foot(1, True)]), footer), "FAIL")
    check("footer not found before the edit", judge_footer(two, probe_pdf(2, body + [foot(0, True), foot(1, True)]), "Southwind Ltd"), "FAIL")

    # Typing onto a new page.
    numbered = lambda pages, total, form="Page %d of %d": [(p, 72, 760, form % (p + 1, total), 9) for p in range(pages)]
    typed = lambda pitch: [(1, 72, 100 + i * pitch, TYPED.format(i + 1), 10) for i in range(3)]
    one = probe_pdf(1, body + numbered(1, 1))
    add = lambda before, after, n=3: judge_add_a_page(before, after, n)
    check("a page added, typed lines clear, pages renumbered", add(one, probe_pdf(2, body + numbered(2, 2) + typed(14))), "PASS")
    check("a page added, typed lines a point apart", add(one, probe_pdf(2, body + numbered(2, 2) + typed(1))), "FAIL")
    check("a page added, typed lines their size apart", add(one, probe_pdf(2, body + numbered(2, 2) + typed(10))), "PASS")
    check("a page added, typed lines 0.05pt closer than their size", add(one, probe_pdf(2, body + numbered(2, 2) + typed(9.95))), "FAIL")
    check("a page added, the typed lines short of it",
          add(one, probe_pdf(2, body + numbered(2, 2) + [(0, 72, 140 + i * 14, TYPED.format(i + 1), 10) for i in range(3)])), "FAIL")
    check("a page added, a typed line over the body",
          add(one, probe_pdf(2, body + numbered(2, 2) + [(0, 72, 116, TYPED.format(1), 10)]
                             + [(1, 72, 100 + i * 14, TYPED.format(i + 2), 10) for i in range(2)])), "FAIL")
    check("a page added, a line of the page lost", add(one, probe_pdf(2, body[:1] + numbered(2, 2) + typed(14))), "FAIL")
    check("a page added, the total not updated", add(one, probe_pdf(2, body + numbered(2, 1) + typed(14))), "FAIL")
    slash = lambda pages, total: numbered(pages, total, "%d / %d")
    check("a page added, 'i / n' renumbered", add(probe_pdf(1, body + slash(1, 1)), probe_pdf(2, body + slash(2, 2) + typed(14))), "PASS")
    check("a page added, 'i / n' not updated", add(probe_pdf(1, body + slash(1, 1)), probe_pdf(2, body + slash(2, 1) + typed(14))), "FAIL")
    parts = lambda p, n, total: [(p, 72, 760, "Page", 9), (p, 110, 760, str(n), 9), (p, 130, 760, "of", 9), (p, 150, 760, str(total), 9)]
    # Typed lines numbered 7 to 9, so no digit of theirs stands in for the footer's.
    typed_late = [(1, 72, 100 + i * 14, TYPED.format(i + 7), 10) for i in range(3)]
    check("a page added, a footer's number set as words of their own",
          add(probe_pdf(1, body + parts(0, 1, 1)), probe_pdf(2, body + parts(0, 1, 2) + parts(1, 2, 2) + typed_late), 9), "PASS")
    rating = (0, 72, 400, "Rating 4 / 5", 10)
    check("a page added, a body 'a / b' and the footers left as they were",
          add(probe_pdf(1, body + [rating] + numbered(1, 1)), probe_pdf(2, body + [rating] + numbered(2, 1) + typed(14))), "FAIL")
    cover = probe_pdf(2, body + [(1, 72, 760, "Page 2 of 2", 9)])
    check("a page added after an unnumbered cover, renumbered",
          add(cover, probe_pdf(3, body + [(1, 72, 760, "Page 2 of 3", 9), (2, 72, 760, "Page 3 of 3", 9)]
                               + [(2, 72, 100 + i * 14, TYPED.format(i + 1), 10) for i in range(3)])), "PASS")
    check("a page added after an unnumbered cover, the new page unnumbered",
          add(cover, probe_pdf(3, body + [(1, 72, 760, "Page 2 of 3", 9)]
                               + [(2, 72, 100 + i * 14, TYPED.format(i + 1), 10) for i in range(3)])), "FAIL")
    check("no page added", add(one, probe_pdf(1, body + numbered(1, 1) + [(0, 72, 140, TYPED.format(1), 10)]), 1), "FAIL")
    check("a page added to a document that numbers no pages", add(probe_pdf(1, body), probe_pdf(2, body + typed(14))), "PASS")
    spaced = [(1, 72, 100 + i * 14, " ".join(TYPED.format(i + 1)), 10) for i in range(3)]
    check("a page added, typed on from a letter-spaced line", add(one, probe_pdf(2, body + numbered(2, 2) + spaced)), "PASS")

    # Replacing a picture.
    at = (72, 200, 172, 300)
    facts = {"name": "Picture 1", "sizeBefore": [100, 100], "sizeAfter": [100, 100]}
    before = probe_pdf(1, body, pictures_at=[(0, at, old_png)])
    pic = lambda *placed, lines=body: judge_picture(before, probe_pdf(1, lines, pictures_at=list(placed)), facts)
    check("picture replaced in place", pic((0, at, new_png)), "PASS")
    check("picture replaced, 0.5pt lower", pic((0, (72, 200.5, 172, 300.5), new_png)), "PASS")
    check("picture replaced, 0.55pt lower", pic((0, (72, 200.55, 172, 300.55), new_png)), "FAIL")
    check("picture replaced, one more drawn", pic((0, at, new_png), (0, (300, 200, 400, 300), new_png)), "FAIL")
    check("picture replaced, the old image still drawn", pic((0, at, old_png)), "FAIL")
    check("picture replaced, a line of the page lost", pic((0, at, new_png), lines=body[:1]), "FAIL")

    # Editing a link.
    linked = {"urlBefore": "mailto:a@example.com", "urlAfter": "mailto:a@example.com",
              "textBefore": "a@example.com", "textAfter": "a@exEDITample.com"}
    line = lambda text: [(0, 72, 100, text, 10), body[1]]
    link_ref = probe_pdf(1, line("Write to a@example.com today."), links=[link])
    edited = lambda links=(link,), text="Write to a@exEDITample.com today.", lines=None: probe_pdf(
        1, lines if lines is not None else line(text), links=list(links))
    check("link edited, every link kept", judge_link(link_ref, edited(), linked), "PASS")
    check("link edited, a link lost", judge_link(link_ref, edited(links=()), linked), "FAIL")
    check("link edited, one of two links to one address lost",
          judge_link(probe_pdf(1, line("Write to a@example.com today."), links=[link, (0, (72, 104, 200, 116), "mailto:a@example.com")]),
                     edited(), linked), "FAIL")
    check("link edited, its address gone", judge_link(link_ref, edited(), dict(linked, urlAfter=None)), "FAIL")
    check("link edited, a line of the page lost", judge_link(link_ref, edited(lines=line("Write to a@exEDITample.com today.")[:1]), linked), "FAIL")
    here = {"urlBefore": "https://example.com/a", "urlAfter": "https://example.com/a", "textBefore": "here", "textAfter": "heEDITre"}
    check("link edited, another line holding its text lost", judge_link(
        probe_pdf(1, [(0, 72, 300, "Click here to read.", 10), (0, 72, 314, "Somewhere over there is a line.", 10)],
                  links=[(0, (100, 290, 130, 302), "https://example.com/a")]),
        probe_pdf(1, [(0, 72, 300, "Click heEDITre to read.", 10)], links=[(0, (100, 290, 140, 302), "https://example.com/a")]), here), "FAIL")
    wrapped = dict(linked, textAfter="a@examEDITple.com")
    check("link edited, its text wrapped over two lines", judge_link(
        probe_pdf(1, [(0, 72, 300, "Write to a@ex", 10), (0, 72, 314, "ample.com today.", 10)], links=[link]),
        probe_pdf(1, [(0, 72, 300, "Write to a@examEDIT", 10), (0, 72, 314, "ple.com today.", 10)], links=[link]), wrapped), "PASS")
    host = {"urlBefore": "https://example.com", "urlAfter": "https://example.com/", "textBefore": "example.com", "textAfter": "examEDITple.com"}
    check("link edited, a slash put after a bare host", judge_link(
        probe_pdf(1, line("See example.com"), links=[(0, (72, 90, 200, 102), "https://example.com")]),
        probe_pdf(1, line("See examEDITple.com"), links=[(0, (72, 90, 200, 102), "https://example.com/")]), host), "PASS")

    failed = 0
    for name, status, expected, detail in checks:
        ok = status == expected
        failed += 0 if ok else 1
        print("SELF-CHECK %s %s (expected %s, got %s): %s" % ("ok  " if ok else "FAIL", name, expected, status, detail))
    print("%d checks, %d failed" % (len(checks), failed))
    return failed


# --- Main ---------------------------------------------------------------------------------------

class Parser(argparse.ArgumentParser):
    def error(self, message):
        # Exit 2 means NOT_RUN to a reader of this protocol, as it does for the Word one.
        self.print_usage(sys.stderr)
        print("%s: error: %s" % (self.prog, message), file=sys.stderr)
        sys.exit(1)


def main():
    parser = Parser(description=__doc__.split("\n\n")[0])
    parser.add_argument("docx", nargs="*", help="exported .docx files; copies are opened, never the files themselves")
    parser.add_argument("--output-dir", help="where the copies, their PDFs and edit-protocol-libreoffice.json go")
    parser.add_argument("--scenario", action="append", help="a scenario to run, by name; all of them when left out")
    parser.add_argument("--soffice", default="soffice", help="the soffice to start (default: soffice on the PATH)")
    parser.add_argument("--self-check", action="store_true", help="check the verdicts against probe PDFs and exit")
    args = parser.parse_args()

    if args.self_check:
        if fitz is None or not clip_honoured():
            print("NOT_RUN: this Python has no PyMuPDF that leaves clipped text out (TEXT_CLIP)")
            return 2
        return 1 if self_check() else 0

    if not args.docx or not args.output_dir:
        parser.error("give the .docx files and --output-dir, or --self-check")
    scenarios = [s.strip() for item in (args.scenario or SCENARIOS) for s in item.split(",") if s.strip()]
    if not scenarios:
        parser.error("no scenario named: the scenarios are %s" % ", ".join(SCENARIOS))
    unknown = [s for s in scenarios if s not in SCENARIOS]
    if unknown:
        parser.error("no scenario is called %s; the scenarios are %s" % (", ".join(unknown), ", ".join(SCENARIOS)))
    os.makedirs(args.output_dir, exist_ok=True)
    output = os.path.abspath(args.output_dir)
    report = os.path.join(output, "edit-protocol-libreoffice.json")

    def not_run(reason):
        with open(report, "w", encoding="utf-8") as f:
            json.dump({"status": "NOT_RUN", "reason": reason}, f, indent=1)
        print("NOT_RUN: " + reason)
        return 2

    try:
        import uno  # noqa: F401
    except ImportError:
        return not_run("this Python cannot import LibreOffice's UNO bridge (uno)")
    if fitz is None or not clip_honoured():
        return not_run("this Python has no PyMuPDF that leaves clipped text out (TEXT_CLIP)")
    if shutil.which(args.soffice) is None and not os.path.exists(args.soffice):
        return not_run("no soffice at %s" % args.soffice)

    # The inputs are copied first: an export run while the protocol works would pull them out
    # from under it.
    inputs = os.path.join(output, "input")
    os.makedirs(inputs, exist_ok=True)
    sources = [shutil.copy(path, inputs) for path in args.docx]

    try:
        office = Office(args.soffice)
    except Exception as e:
        return not_run("LibreOffice could not be started: %s" % e)
    documents = []
    header = {"editor": "LibreOffice", "version": None, "pymupdf": fitz.VersionBind, "os": platform.platform(),
              "ranAt": datetime.datetime.now(datetime.timezone.utc).isoformat()}

    def write():
        # Written after every document, so a run that stops half way keeps what it measured.
        with open(report, "w", encoding="utf-8") as f:
            json.dump(dict(header, documents=documents), f, indent=1)

    try:
        header["version"] = office.version()
        protocol = Protocol(office, output)
        for source in sources:
            name = os.path.splitext(os.path.basename(source))[0]
            documents.append({"document": name, "scenarios": protocol.run(source, scenarios)})
            line = " ".join("%s=%s" % (s["scenario"], s["status"]) for s in documents[-1]["scenarios"])
            print("%-38s %s" % (name, line), flush=True)
            if not office.alive():
                # A soffice that died takes every later document with it: start another.
                documents[-1]["note"] = "soffice died during this document and was started again"
                office.close()
                office = Office(args.soffice)
                protocol.office = office
            write()
    except Exception as e:
        header["stopped"] = "the run stopped after %d documents: %r" % (len(documents), e)
        print("STOPPED: " + header["stopped"])
    finally:
        office.close()
        write()

    failed = [d for d in documents if any(s["status"] not in ("PASS", "N/A") for s in d["scenarios"])]
    print("%d documents, %d with a scenario not passing. Protocol: %s" % (len(documents), len(failed), report))
    return 1 if failed or "stopped" in header else 0


if __name__ == "__main__":
    sys.exit(main())
