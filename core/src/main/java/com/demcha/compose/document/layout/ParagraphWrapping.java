package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.*;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.engine.components.content.text.TextDataBody;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.engine.text.TextControlSanitizer;
import com.demcha.compose.engine.text.bidi.BidiParagraphResolver;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.text.markdown.MarkDownParser;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.*;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;

/**
 * Greedy inline-paragraph line wrapping, pulled out of {@code TextFlowSupport}
 * so the three wrap loops (plain / inline-run / markdown) and their tokenization,
 * trim, and indent helpers are a focused unit. Pure and stateless: every input is
 * a parameter and the output is a fresh list of laid-out lines; the session
 * orchestrator calls in through the wrap* entry points.
 *
 * @author Artem Demchyshyn
 */
final class ParagraphWrapping {

    private ParagraphWrapping() {
    }

    static ParagraphLine emptyParagraphLine(TextMeasurementSystem.LineMetrics metrics) {
        return new ParagraphLine(
                "",
                0.0,
                metrics.lineHeight(),
                metrics.lineHeight(),
                metrics.ascent(),
                metrics.baselineOffsetFromBottom(),
                List.of());
    }

    static List<String> wrapParagraph(List<String> logicalLines,
                                              TextStyle style,
                                              double maxWidth,
                                              String bulletOffset,
                                              TextIndentStrategy indentStrategy,
                                              TextMeasurementSystem measurement) {
        List<String> result = new ArrayList<>();
        ParagraphIndentSpec indentSpec = ParagraphIndentSpec.from(bulletOffset, style, measurement);

        for (int logicalLineIndex = 0; logicalLineIndex < logicalLines.size(); logicalLineIndex++) {
            String logicalLine = logicalLines.get(logicalLineIndex);
            if (logicalLine.isEmpty()) {
                result.add("");
                continue;
            }
            if (maxWidth <= EPS) {
                result.add("");
                continue;
            }

            String initialPrefix = "";
            if (logicalLineIndex == 0) {
                if (indentStrategy.indentFirstLine()) {
                    initialPrefix = indentSpec.firstLinePrefix();
                }
            } else if (indentStrategy.indentWrappedLines()) {
                initialPrefix = indentSpec.continuationPrefix();
            }

            String continuationPrefix = indentStrategy.indentWrappedLines()
                    ? indentSpec.continuationPrefix()
                    : "";

            List<String> tokens = tokenize(logicalLine);
            String currentPrefix = initialPrefix;
            // currentLine is assembled in a reused StringBuilder: appending a
            // token is amortised O(1), whereas concatenating Strings re-copied
            // the whole growing line on every token (O(chars^2) char copies plus
            // a fresh throwaway String each step). The character sequence is
            // identical to the old `+` assembly, so wrapping stays byte-for-byte
            // the same; we only materialise a String via toString() when a line
            // is emitted (which the result list needs anyway).
            StringBuilder currentLine = new StringBuilder(initialPrefix);
            // Running width of currentLine. The greedy fit only needs the width
            // of the line built so far plus the next token, not a fresh
            // measurement of the whole growing prefix on every token (which made
            // wrapping O(chars per line x tokens) measured characters). PDFBox
            // glyph advances are additive here (no kerning), so accumulating
            // per-token widths matches measuring the full string to well within
            // the EPS the fit test already tolerates; each new line re-measures
            // its (short) start to pin any floating-point drift.
            double currentWidth = measurement.textWidth(style, initialPrefix);
            boolean hasContent = false;

            for (String token : tokens) {
                String nextToken = hasContent ? token : token.stripLeading();
                if (nextToken.isEmpty()) {
                    continue;
                }

                double nextTokenWidth = measurement.textWidth(style, nextToken);
                if (currentWidth + nextTokenWidth <= maxWidth + EPS) {
                    currentLine.append(nextToken);
                    currentWidth += nextTokenWidth;
                    hasContent = true;
                    continue;
                }

                // Does not fit. If the line already has content, flush it and retry
                // the token on a fresh line before resorting to a break.
                String strippedToken = nextToken.stripLeading();
                double strippedTokenWidth = measurement.textWidth(style, strippedToken);
                if (hasContent) {
                    result.add(trimTrailingSpaces(currentLine.toString()));
                    currentPrefix = continuationPrefix;
                    currentLine.setLength(0);
                    currentLine.append(continuationPrefix);
                    currentWidth = measurement.textWidth(style, continuationPrefix);
                    hasContent = false;

                    if (currentWidth + strippedTokenWidth <= maxWidth + EPS) {
                        currentLine.setLength(0);
                        currentLine.append(currentPrefix).append(strippedToken);
                        currentWidth += strippedTokenWidth;
                        hasContent = true;
                        continue;
                    }
                }

                // Over-wide on a fresh (or already empty) line: break it at soft seams,
                // char-splitting as a last resort.
                double availableWidth = availableWidthForPrefix(maxWidth, currentPrefix, style, measurement);
                List<String> chunks = TokenBreaking.breakLongToken(strippedToken, style, availableWidth, measurement);
                if (chunks.isEmpty()) {
                    continue;
                }

                for (int index = 0; index < chunks.size() - 1; index++) {
                    result.add(currentPrefix + chunks.get(index));
                    currentPrefix = continuationPrefix;
                }
                currentLine.setLength(0);
                currentLine.append(currentPrefix).append(chunks.get(chunks.size() - 1));
                currentWidth = measurement.textWidth(style, currentLine.toString());
                hasContent = true;
            }

            result.add(trimTrailingSpaces(currentLine.toString()));
        }

        return List.copyOf(result);
    }

    static List<ParagraphLine> toParagraphLines(List<String> wrappedLines,
                                                        TextStyle style,
                                                        TextMeasurementSystem.LineMetrics metrics,
                                                        TextMeasurementSystem measurement) {
        return toParagraphLines(wrappedLines, style, metrics, measurement,
                BidiParagraphResolver.BaseDirection.LEFT_TO_RIGHT);
    }

    static List<ParagraphLine> toParagraphLines(List<String> wrappedLines,
                                                        TextStyle style,
                                                        TextMeasurementSystem.LineMetrics metrics,
                                                        TextMeasurementSystem measurement,
                                                        BidiParagraphResolver.BaseDirection baseDirection) {
        List<ParagraphLine> result = new ArrayList<>(wrappedLines.size());
        double textLineHeight = metrics.lineHeight();
        for (String line : wrappedLines) {
            String safeLine = line == null ? "" : line;
            // Checked before resolving, not inside it: a line with nothing to reorder must
            // not pay for a copy of itself, a run record and a list, on every line of every
            // document. It is the difference between a scan and an allocation per line.
            if (needsDirectionalLayout(safeLine, baseDirection)) {
                List<BidiParagraphResolver.DirectionalRun> runs =
                        BidiParagraphResolver.resolve(safeLine, baseDirection);
                if (runs.size() > 1 || (runs.size() == 1 && runs.get(0).isRightToLeft())) {
                    result.add(directionalLine(runs, style, metrics, measurement, textLineHeight));
                    continue;
                }
            }

            // One left-to-right run: the line every document had before direction
            // existed. Built exactly as it was, so its geometry cannot drift.
            double width = measurement.textWidth(style, safeLine);
            result.add(new ParagraphLine(
                    safeLine,
                    width,
                    textLineHeight,
                    textLineHeight,
                    metrics.ascent(),
                    metrics.baselineOffsetFromBottom(),
                    List.of(new ParagraphTextSpan(safeLine, style, width, textLineHeight))));
        }
        return List.copyOf(result);
    }

    /**
     * Builds a line whose text changes direction, as one span per directional run.
     *
     * <p>A single span is drawn with one show-text operation, so a mixed-direction line
     * cannot be reordered by moving spans around unless each run is a span of its own.
     * The spans stay in logical order and carry a permutation describing how they are
     * drawn; the steering characters are dropped here, having been read.</p>
     */
    private static ParagraphLine directionalLine(List<BidiParagraphResolver.DirectionalRun> runs,
                                                 TextStyle style,
                                                 TextMeasurementSystem.LineMetrics metrics,
                                                 TextMeasurementSystem measurement,
                                                 double textLineHeight) {
        List<ParagraphSpan> spans = new ArrayList<>(runs.size());
        int[] levels = new int[runs.size()];
        StringBuilder lineText = new StringBuilder();
        double lineWidth = 0.0;

        for (int index = 0; index < runs.size(); index++) {
            BidiParagraphResolver.DirectionalRun run = runs.get(index);
            String runText = TextControlSanitizer.removeDirectionMarks(run.text());
            double runWidth = measurement.textWidth(style, runText);
            spans.add(new ParagraphTextSpan(runText, style, runWidth, textLineHeight,
                    null, null, run.isRightToLeft()));
            levels[index] = run.embeddingLevel();
            lineText.append(runText);
            lineWidth += runWidth;
        }

        List<Integer> visualOrder = new ArrayList<>();
        for (int position : BidiParagraphResolver.visualOrder(levels)) {
            visualOrder.add(position);
        }

        return new ParagraphLine(
                lineText.toString(),
                lineWidth,
                textLineHeight,
                textLineHeight,
                metrics.ascent(),
                metrics.baselineOffsetFromBottom(),
                spans,
                visualOrder);
    }

    static List<ParagraphLine> wrapInlineParagraph(List<InlineRun> runs,
                                                           TextStyle defaultStyle,
                                                           TextMeasurementSystem.LineMetrics defaultMetrics,
                                                           double maxWidth,
                                                           String bulletOffset,
                                                           TextIndentStrategy indentStrategy,
                                                           TextMeasurementSystem measurement) {
        return wrapInlineParagraph(runs, defaultStyle, defaultMetrics, maxWidth, bulletOffset,
                indentStrategy, measurement, BidiParagraphResolver.BaseDirection.LEFT_TO_RIGHT);
    }

    static List<ParagraphLine> wrapInlineParagraph(List<InlineRun> runs,
                                                           TextStyle defaultStyle,
                                                           TextMeasurementSystem.LineMetrics defaultMetrics,
                                                           double maxWidth,
                                                           String bulletOffset,
                                                           TextIndentStrategy indentStrategy,
                                                           TextMeasurementSystem measurement,
                                                           BidiParagraphResolver.BaseDirection baseDirection) {
        List<ParagraphLine> result = new ArrayList<>();
        ParagraphIndentSpec indentSpec = ParagraphIndentSpec.from(bulletOffset, defaultStyle, measurement);
        List<List<InlineLayoutToken>> logicalLines = tokenizeInlineRuns(runs, defaultStyle, measurement);

        for (int logicalLineIndex = 0; logicalLineIndex < logicalLines.size(); logicalLineIndex++) {
            List<InlineLayoutToken> logicalLine = logicalLines.get(logicalLineIndex);
            if (logicalLine.isEmpty() || maxWidth <= EPS) {
                result.add(emptyParagraphLine(defaultMetrics));
                continue;
            }

            String initialPrefix = "";
            if (logicalLineIndex == 0) {
                if (indentStrategy.indentFirstLine()) {
                    initialPrefix = indentSpec.firstLinePrefix();
                }
            } else if (indentStrategy.indentWrappedLines()) {
                initialPrefix = indentSpec.continuationPrefix();
            }

            String continuationPrefix = indentStrategy.indentWrappedLines()
                    ? indentSpec.continuationPrefix()
                    : "";

            List<InlineLayoutToken> currentLine = new ArrayList<>();
            if (!initialPrefix.isEmpty()) {
                currentLine.add(InlineTextToken.of(initialPrefix, defaultStyle, null, measurement));
            }
            double currentWidth = inlineLineWidth(currentLine);

            for (InlineLayoutToken token : logicalLine) {
                InlineLayoutToken sanitizedToken = trimLeadingIfInlineLineStart(token, currentLine, measurement);
                if (sanitizedToken == null) {
                    continue;
                }

                double tokenWidth = sanitizedToken.wrapWidth();
                if (currentWidth + tokenWidth <= maxWidth + EPS) {
                    currentLine.add(sanitizedToken);
                    currentWidth += tokenWidth;
                    continue;
                }

                // Does not fit. If the line already has content, flush it and retry
                // the token on a fresh line before resorting to a break.
                if (!currentLine.isEmpty()) {
                    result.add(toInlineParagraphLine(currentLine, defaultMetrics, measurement, baseDirection));
                    currentLine = new ArrayList<>();
                    if (!continuationPrefix.isEmpty()) {
                        currentLine.add(InlineTextToken.of(continuationPrefix, defaultStyle, null, measurement));
                    }
                    currentWidth = inlineLineWidth(currentLine);

                    sanitizedToken = trimLeadingIfInlineLineStart(token, currentLine, measurement);
                    if (sanitizedToken == null) {
                        continue;
                    }
                    tokenWidth = sanitizedToken.wrapWidth();
                    if (currentWidth + tokenWidth <= maxWidth + EPS) {
                        currentLine.add(sanitizedToken);
                        currentWidth += tokenWidth;
                        continue;
                    }
                }

                // Still over-wide on a fresh (or already empty) line. Text tokens —
                // plain OR highlight chip — are broken within the column; a graphic
                // (image / shape / SVG) has no break point and is emitted as-is.
                if (!(sanitizedToken instanceof InlineTextToken textToken)) {
                    currentLine.add(sanitizedToken);
                    currentWidth += sanitizedToken.wrapWidth();
                    continue;
                }

                boolean chip = textToken.highlightGroup() != null;
                // A chip fragment paints leadPad + glyphs + trailPad, but the break
                // budgets glyphs only, so reserve the run's padding here to keep the
                // coalesced fill inside the column. Interior fragments under-fill by
                // at most that padding — cosmetic, and only on an over-wide chip.
                double reserve = chip ? textToken.leadPad() + textToken.trailPad() : 0.0;
                List<String> pieces = TokenBreaking.breakLongToken(
                        textToken.text(),
                        textToken.textStyle(),
                        Math.max(1.0, maxWidth - currentWidth - reserve),
                        measurement);
                for (int pieceIndex = 0; pieceIndex < pieces.size(); pieceIndex++) {
                    String piece = pieces.get(pieceIndex);
                    if (piece.isEmpty()) {
                        continue;
                    }
                    // Chip pieces keep the run's group + background so the coalescer
                    // paints one fill per fragment; the run's outer pad sits on the
                    // first/last piece only, leaving the break seams open.
                    InlineTextToken chunkToken = chip
                            ? InlineTextToken.ofHighlight(
                                    piece,
                                    textToken.textStyle(),
                                    textToken.linkTarget(),
                                    textToken.background(),
                                    textToken.highlightGroup(),
                                    pieceIndex == 0 ? textToken.leadPad() : 0.0,
                                    pieceIndex == pieces.size() - 1 ? textToken.trailPad() : 0.0,
                                    measurement)
                            : InlineTextToken.of(
                                    piece,
                                    textToken.textStyle(),
                                    textToken.linkTarget(),
                                    measurement);
                    currentLine.add(chunkToken);
                    currentWidth += chunkToken.wrapWidth();

                    if (pieceIndex < pieces.size() - 1) {
                        result.add(toInlineParagraphLine(currentLine, defaultMetrics, measurement, baseDirection));
                        currentLine = new ArrayList<>();
                        if (!continuationPrefix.isEmpty()) {
                            currentLine.add(InlineTextToken.of(continuationPrefix, defaultStyle, null, measurement));
                        }
                        currentWidth = inlineLineWidth(currentLine);
                    }
                }
            }

            result.add(toInlineParagraphLine(currentLine, defaultMetrics, measurement, baseDirection));
        }

        return List.copyOf(result);
    }

    static boolean containsMarkdownSyntax(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.indexOf('*') >= 0
               || value.indexOf('_') >= 0
               || value.indexOf('`') >= 0;
    }

    static List<ParagraphLine> wrapMarkdownParagraph(List<String> logicalLines,
                                                             TextStyle style,
                                                             TextMeasurementSystem.LineMetrics metrics,
                                                             double maxWidth,
                                                             String bulletOffset,
                                                             TextIndentStrategy indentStrategy,
                                                             TextMeasurementSystem measurement) {
        return wrapMarkdownParagraph(logicalLines, style, metrics, maxWidth, bulletOffset,
                indentStrategy, measurement, BidiParagraphResolver.BaseDirection.LEFT_TO_RIGHT);
    }

    static List<ParagraphLine> wrapMarkdownParagraph(List<String> logicalLines,
                                                             TextStyle style,
                                                             TextMeasurementSystem.LineMetrics metrics,
                                                             double maxWidth,
                                                             String bulletOffset,
                                                             TextIndentStrategy indentStrategy,
                                                             TextMeasurementSystem measurement,
                                                     BidiParagraphResolver.BaseDirection baseDirection) {
        List<ParagraphLine> result = new ArrayList<>();
        ParagraphIndentSpec indentSpec = ParagraphIndentSpec.from(bulletOffset, style, measurement);
        MarkDownParser parser = new MarkDownParser();

        for (int logicalLineIndex = 0; logicalLineIndex < logicalLines.size(); logicalLineIndex++) {
            String logicalLine = logicalLines.get(logicalLineIndex);
            if (logicalLine.isEmpty() || maxWidth <= EPS) {
                result.add(emptyParagraphLine(metrics));
                continue;
            }

            String initialPrefix = "";
            if (logicalLineIndex == 0) {
                if (indentStrategy.indentFirstLine()) {
                    initialPrefix = indentSpec.firstLinePrefix();
                }
            } else if (indentStrategy.indentWrappedLines()) {
                initialPrefix = indentSpec.continuationPrefix();
            }

            String continuationPrefix = indentStrategy.indentWrappedLines()
                    ? indentSpec.continuationPrefix()
                    : "";

            List<TextDataBody> tokens = tokenizeMarkdownLine(logicalLine, style, parser);
            List<TextDataBody> currentLine = new ArrayList<>();
            if (!initialPrefix.isEmpty()) {
                currentLine.add(new TextDataBody(initialPrefix, style));
            }
            double currentWidth = lineWidth(currentLine, measurement);

            for (TextDataBody token : tokens) {
                TextDataBody sanitizedToken = trimLeadingIfLineStart(token, currentLine);
                if (sanitizedToken == null || sanitizedToken.text().isEmpty()) {
                    continue;
                }

                double tokenWidth = measurement.textWidth(sanitizedToken.textStyle(), sanitizedToken.text());
                if (currentWidth + tokenWidth <= maxWidth + EPS) {
                    currentLine.add(sanitizedToken);
                    currentWidth += tokenWidth;
                    continue;
                }

                // Does not fit. If the line already has content, flush it and retry
                // the token on a fresh line before resorting to a break.
                if (!currentLine.isEmpty()) {
                    result.add(toParagraphLine(currentLine, metrics, measurement, baseDirection));
                    currentLine = new ArrayList<>();
                    if (!continuationPrefix.isEmpty()) {
                        currentLine.add(new TextDataBody(continuationPrefix, style));
                    }
                    currentWidth = lineWidth(currentLine, measurement);

                    sanitizedToken = trimLeadingIfLineStart(token, currentLine);
                    if (sanitizedToken == null || sanitizedToken.text().isEmpty()) {
                        continue;
                    }
                    tokenWidth = measurement.textWidth(sanitizedToken.textStyle(), sanitizedToken.text());
                    if (currentWidth + tokenWidth <= maxWidth + EPS) {
                        currentLine.add(sanitizedToken);
                        currentWidth += tokenWidth;
                        continue;
                    }
                }

                List<String> chunks = TokenBreaking.breakLongToken(
                        sanitizedToken.text(),
                        sanitizedToken.textStyle(),
                        Math.max(1.0, maxWidth - currentWidth),
                        measurement);
                if (chunks.isEmpty()) {
                    continue;
                }

                for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                    String chunk = chunks.get(chunkIndex);
                    if (chunk.isEmpty()) {
                        continue;
                    }
                    TextDataBody chunkBody = new TextDataBody(chunk, sanitizedToken.textStyle());
                    currentLine.add(chunkBody);
                    currentWidth += measurement.textWidth(chunkBody.textStyle(), chunkBody.text());

                    if (chunkIndex < chunks.size() - 1) {
                        result.add(toParagraphLine(currentLine, metrics, measurement, baseDirection));
                        currentLine = new ArrayList<>();
                        if (!continuationPrefix.isEmpty()) {
                            currentLine.add(new TextDataBody(continuationPrefix, style));
                        }
                        currentWidth = lineWidth(currentLine, measurement);
                    }
                }
            }

            result.add(toParagraphLine(currentLine, metrics, measurement, baseDirection));
        }

        return List.copyOf(result);
    }

    // ------------------------------------------------------------------
    // Tokenisation + measurement utilities
    // ------------------------------------------------------------------

    private static double availableWidthForPrefix(double maxWidth,
                                                  String prefix,
                                                  TextStyle style,
                                                  TextMeasurementSystem measurement) {
        return Math.max(1.0, maxWidth - measurement.textWidth(style, prefix == null ? "" : prefix));
    }

    private static String normalizeBulletPrefix(String bulletOffset) {
        if (bulletOffset == null || bulletOffset.isEmpty()) {
            return "";
        }
        char last = bulletOffset.charAt(bulletOffset.length() - 1);
        return Character.isWhitespace(last) ? bulletOffset : bulletOffset + " ";
    }

    private static String computeIndentFromPrefix(TextMeasurementSystem measurement,
                                                  TextStyle style,
                                                  String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        double targetWidth = measurement.textWidth(style, prefix);
        double spaceWidth = measurement.textWidth(style, " ");
        if (spaceWidth <= EPS) {
            return "";
        }
        int spaces = (int) Math.ceil(targetWidth / spaceWidth);
        return " ".repeat(Math.max(0, spaces));
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean whitespace = Character.isWhitespace(text.charAt(0));

        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            boolean currentWhitespace = Character.isWhitespace(ch);
            if (currentWhitespace != whitespace && !current.isEmpty()) {
                tokens.add(current.toString());
                current.setLength(0);
            }
            current.append(ch);
            whitespace = currentWhitespace;
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return List.copyOf(tokens);
    }

    private static List<TextDataBody> tokenizeMarkdownLine(String text,
                                                           TextStyle style,
                                                           MarkDownParser parser) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int firstNonWhitespace = 0;
        while (firstNonWhitespace < text.length() && Character.isWhitespace(text.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }

        if (firstNonWhitespace + 1 < text.length()) {
            char marker = text.charAt(firstNonWhitespace);
            boolean listMarker = marker == '-' || marker == '*' || marker == '+';
            boolean hasSpaceAfter = Character.isWhitespace(text.charAt(firstNonWhitespace + 1));
            if (listMarker && hasSpaceAfter) {
                List<TextDataBody> bodies = new ArrayList<>();
                if (firstNonWhitespace > 0) {
                    bodies.add(new TextDataBody(text.substring(0, firstNonWhitespace), style));
                }
                bodies.add(new TextDataBody(String.valueOf(marker), style));
                bodies.add(new TextDataBody(" ", style));
                bodies.addAll(parser.getBody(text.substring(firstNonWhitespace + 2), style));
                return List.copyOf(bodies);
            }
        }

        return List.copyOf(parser.getBody(text, style));
    }

    private static ParagraphLine toParagraphLine(List<TextDataBody> bodies,
                                                 TextMeasurementSystem.LineMetrics metrics,
                                                 TextMeasurementSystem measurement,
                                                 BidiParagraphResolver.BaseDirection baseDirection) {
        List<TextDataBody> trimmedBodies = trimTrailingWhitespaceBodies(bodies);
        if (trimmedBodies.isEmpty()) {
            return emptyParagraphLine(metrics);
        }

        double textLineHeight = metrics.lineHeight();
        List<ParagraphSpan> spans = new ArrayList<>(trimmedBodies.size());
        StringBuilder text = new StringBuilder();
        double width = 0.0;
        for (TextDataBody body : trimmedBodies) {
            TextStyle style = body.textStyle() == null ? TextStyle.DEFAULT_STYLE : body.textStyle();
            double bodyWidth = measurement.textWidth(style, body.text());
            spans.add(new ParagraphTextSpan(body.text(), style, bodyWidth, textLineHeight));
            text.append(body.text());
            width += bodyWidth;
        }

        return new ParagraphLine(
                text.toString(),
                width,
                textLineHeight,
                textLineHeight,
                metrics.ascent(),
                metrics.baselineOffsetFromBottom(),
                spans,
                directionOf(spans, baseDirection));
    }

    private static List<List<InlineLayoutToken>> tokenizeInlineRuns(List<InlineRun> runs,
                                                                    TextStyle defaultStyle,
                                                                    TextMeasurementSystem measurement) {
        List<List<InlineLayoutToken>> lines = new ArrayList<>();
        List<InlineLayoutToken> currentLine = new ArrayList<>();

        for (InlineRun run : runs) {
            if (run == null) {
                continue;
            }
            if (run instanceof InlineTextRun textRun) {
                if (textRun.text().isEmpty()) {
                    continue;
                }
                TextStyle style = textRun.textStyle() == null ? defaultStyle : toTextStyle(textRun.textStyle());
                String normalized = TextControlSanitizer.remove(textRun.text().replace("\r\n", "\n").replace('\r', '\n'));
                String[] parts = normalized.split("\n", -1);
                for (int partIndex = 0; partIndex < parts.length; partIndex++) {
                    if (partIndex > 0) {
                        lines.add(List.copyOf(currentLine));
                        currentLine = new ArrayList<>();
                    }
                    if (parts[partIndex].isEmpty()) {
                        continue;
                    }
                    for (String token : tokenize(parts[partIndex])) {
                        currentLine.add(InlineTextToken.of(token, style, textRun.linkTarget(), measurement));
                    }
                }
            } else if (run instanceof InlineImageRun imageRun) {
                currentLine.add(InlineImageToken.of(imageRun));
            } else if (run instanceof InlineShapeRun shapeRun) {
                currentLine.add(InlineShapeToken.of(shapeRun));
            } else if (run instanceof InlineSvgRun svgRun) {
                currentLine.add(InlineSvgToken.of(svgRun));
            } else if (run instanceof InlineHighlightRun highlight) {
                if (highlight.text().isEmpty()) {
                    continue;
                }
                TextStyle style = highlight.textStyle() == null
                        ? defaultStyle : toTextStyle(highlight.textStyle());
                // A chip stays on one logical line (newlines collapse to spaces) but
                // its text tokenizes into words, all tagged with the same group, so
                // it wraps with the surrounding line. Horizontal padding sits on the
                // run's outer edges — lead pad on the first word, trail pad on the
                // last — and toInlineParagraphLine coalesces the same-group tokens on
                // each visual line back into one rounded fill.
                String normalized = TextControlSanitizer.remove(
                        highlight.text().replace("\r\n", " ").replace('\r', ' ').replace('\n', ' '));
                if (normalized.isEmpty()) {
                    continue;
                }
                List<String> words = tokenize(normalized);
                DocumentInsets pad = highlight.background().padding();
                for (int wordIndex = 0; wordIndex < words.size(); wordIndex++) {
                    currentLine.add(InlineTextToken.ofHighlight(
                            words.get(wordIndex), style, highlight.linkTarget(),
                            highlight.background(), highlight,
                            wordIndex == 0 ? pad.left() : 0.0,
                            wordIndex == words.size() - 1 ? pad.right() : 0.0,
                            measurement));
                }
            }
        }

        lines.add(List.copyOf(currentLine));
        return List.copyOf(lines);
    }

    private static ParagraphLine toInlineParagraphLine(List<InlineLayoutToken> tokens,
                                                       TextMeasurementSystem.LineMetrics defaultMetrics,
                                                       TextMeasurementSystem measurement,
                                                       BidiParagraphResolver.BaseDirection baseDirection) {
        List<InlineLayoutToken> trimmedTokens = trimTrailingWhitespaceTokens(tokens);
        if (trimmedTokens.isEmpty()) {
            return emptyParagraphLine(defaultMetrics);
        }

        double dominantTextLineHeight = 0.0;
        double dominantAscent = 0.0;
        double dominantBaselineFromBottom = defaultMetrics.baselineOffsetFromBottom();
        boolean sawText = false;
        for (InlineLayoutToken token : trimmedTokens) {
            if (token instanceof InlineTextToken textToken) {
                TextMeasurementSystem.LineMetrics metrics = measurement.lineMetrics(textToken.textStyle());
                double textLineHeight = metrics.lineHeight();
                if (textLineHeight > dominantTextLineHeight) {
                    dominantTextLineHeight = textLineHeight;
                    dominantAscent = metrics.ascent();
                    dominantBaselineFromBottom = metrics.baselineOffsetFromBottom();
                    sawText = true;
                }
            }
        }
        if (!sawText) {
            dominantTextLineHeight = defaultMetrics.lineHeight();
            dominantAscent = defaultMetrics.ascent();
            dominantBaselineFromBottom = defaultMetrics.baselineOffsetFromBottom();
        }

        double maxInlineGraphicHeight = 0.0;
        for (InlineLayoutToken token : trimmedTokens) {
            if (token instanceof InlineImageToken imageToken) {
                if (imageToken.height() > maxInlineGraphicHeight) {
                    maxInlineGraphicHeight = imageToken.height();
                }
            } else if (token instanceof InlineShapeToken shapeToken) {
                if (shapeToken.height() > maxInlineGraphicHeight) {
                    maxInlineGraphicHeight = shapeToken.height();
                }
            } else if (token instanceof InlineSvgToken svgToken) {
                if (svgToken.height() > maxInlineGraphicHeight) {
                    maxInlineGraphicHeight = svgToken.height();
                }
            }
        }
        double resolvedLineHeight = Math.max(dominantTextLineHeight, maxInlineGraphicHeight);

        List<ParagraphSpan> spans = new ArrayList<>(trimmedTokens.size());
        StringBuilder text = new StringBuilder();
        double width = 0.0;
        int tokenIndex = 0;
        while (tokenIndex < trimmedTokens.size()) {
            InlineLayoutToken token = trimmedTokens.get(tokenIndex);
            if (token instanceof InlineTextToken chipStart && chipStart.highlightGroup() != null) {
                // Coalesce every consecutive token of the same chip run on this
                // visual line into ONE span, so a multi-word (or wrapped) chip paints
                // a single rounded fill per line-fragment. Padding sits on the
                // fragment's outer edges — the lead pad of the first token consumed
                // and the trail pad of the last — so a wrapped fragment is open on
                // the inner break edge.
                Object group = chipStart.highlightGroup();
                InlineBackground source = chipStart.background();
                List<InlineTextToken> parts = new ArrayList<>();
                while (tokenIndex < trimmedTokens.size()
                        && trimmedTokens.get(tokenIndex) instanceof InlineTextToken part
                        && part.highlightGroup() == group) {
                    parts.add(part);
                    tokenIndex++;
                }
                // Collapse a soft-wrap space at a wrap seam: a continuation fragment
                // can begin or end with an inter-word space token (which carries no
                // lead/trail pad). Drop those so the fill hugs the visible glyphs and
                // the seam space stays out of line width. The run's AUTHORED outer
                // spaces keep their pad (leadPad/trailPad > 0) and are preserved.
                // tokenize() coalesces consecutive whitespace, so at most one token is
                // trimmed per side; the guard keeps at least one token regardless.
                int start = 0;
                int end = parts.size();
                while (end - start > 1 && parts.get(end - 1).text().isBlank() && parts.get(end - 1).trailPad() == 0.0) {
                    end--;
                }
                while (end - start > 1 && parts.get(start).text().isBlank() && parts.get(start).leadPad() == 0.0) {
                    start++;
                }
                double leftPad = parts.get(start).leadPad();
                double trailPad = parts.get(end - 1).trailPad();
                double glyphs = 0.0;
                StringBuilder chip = new StringBuilder();
                for (int partIndex = start; partIndex < end; partIndex++) {
                    chip.append(parts.get(partIndex).text());
                    glyphs += parts.get(partIndex).width();
                }
                double spanWidth = leftPad + glyphs + trailPad;
                DocumentInsets basePad = source.padding();
                InlineBackground fragment = new InlineBackground(source.fill(), source.cornerRadius(),
                        new DocumentInsets(basePad.top(), trailPad, basePad.bottom(), leftPad));
                spans.add(new ParagraphTextSpan(
                        chip.toString(),
                        chipStart.textStyle(),
                        spanWidth,
                        measurement.lineMetrics(chipStart.textStyle()).lineHeight(),
                        chipStart.linkTarget(),
                        fragment));
                text.append(chip);
                width += spanWidth;
            } else if (token instanceof InlineTextToken textToken) {
                // wrapWidth folds in the chip's horizontal padding (zero for plain
                // text), so the span width and the line width both account for it.
                double spanWidth = textToken.wrapWidth();
                spans.add(new ParagraphTextSpan(
                        textToken.text(),
                        textToken.textStyle(),
                        spanWidth,
                        measurement.lineMetrics(textToken.textStyle()).lineHeight(),
                        textToken.linkTarget(),
                        textToken.background()));
                text.append(textToken.text());
                width += spanWidth;
                tokenIndex++;
            } else if (token instanceof InlineImageToken imageToken) {
                spans.add(new ParagraphImageSpan(
                        imageToken.imageData(),
                        imageToken.width(),
                        imageToken.height(),
                        imageToken.alignment(),
                        imageToken.baselineOffset(),
                        imageToken.linkTarget()));
                width += imageToken.width();
                tokenIndex++;
            } else if (token instanceof InlineShapeToken shapeToken) {
                spans.add(new ParagraphShapeSpan(
                        shapeToken.layers(),
                        shapeToken.width(),
                        shapeToken.height(),
                        shapeToken.alignment(),
                        shapeToken.baselineOffset(),
                        shapeToken.linkTarget()));
                width += shapeToken.width();
                tokenIndex++;
            } else if (token instanceof InlineSvgToken svgToken) {
                spans.add(new ParagraphSvgSpan(
                        svgToken.layers(),
                        svgToken.width(),
                        svgToken.height(),
                        svgToken.alignment(),
                        svgToken.baselineOffset(),
                        svgToken.linkTarget()));
                width += svgToken.width();
                tokenIndex++;
            } else {
                tokenIndex++;
            }
        }

        return new ParagraphLine(
                text.toString(),
                width,
                resolvedLineHeight,
                dominantTextLineHeight,
                dominantAscent,
                dominantBaselineFromBottom,
                spans,
                directionOf(spans, baseDirection));
    }

    /**
     * Works out how a line's already-split spans are drawn, and marks the ones that run
     * right to left.
     *
     * <p>Inline runs arrive as one span per word, so a direction change lands on a span
     * boundary and each span only needs to be told which way it goes — no second split.
     * The line is resolved over a probe string where an inline graphic stands in as an
     * object replacement character, which keeps a span's position in the probe equal to
     * its position in the line and lets an image sit inside right-to-left text as the
     * neutral object it is.</p>
     *
     * <p>Returns an empty order for a line that is entirely left to right, leaving both
     * the spans and the drawing untouched.</p>
     */
    private static List<Integer> directionOf(List<ParagraphSpan> spans,
                                             BidiParagraphResolver.BaseDirection baseDirection) {
        if (spans.isEmpty() || !spansNeedDirectionalLayout(spans, baseDirection)) {
            return List.of();
        }

        StringBuilder probe = new StringBuilder();
        int[] offsets = new int[spans.size()];
        for (int index = 0; index < spans.size(); index++) {
            offsets[index] = probe.length();
            if (spans.get(index) instanceof ParagraphTextSpan textSpan) {
                probe.append(textSpan.text());
            } else {
                probe.append(OBJECT_REPLACEMENT);
            }
        }

        int[] levels = BidiParagraphResolver.levelsFor(probe.toString(), baseDirection);
        if (levels.length == 0) {
            return List.of();
        }

        int[] spanLevels = new int[spans.size()];
        for (int index = 0; index < spans.size(); index++) {
            int offset = Math.min(offsets[index], levels.length - 1);
            spanLevels[index] = levels[offset];
            if (spans.get(index) instanceof ParagraphTextSpan textSpan
                    && BidiParagraphResolver.isRightToLeftLevel(spanLevels[index])
                    && !textSpan.rightToLeft()) {
                spans.set(index, new ParagraphTextSpan(
                        textSpan.text(),
                        textSpan.textStyle(),
                        textSpan.width(),
                        textSpan.height(),
                        textSpan.linkTarget(),
                        textSpan.background(),
                        true));
            }
        }

        List<Integer> visualOrder = new ArrayList<>();
        for (int position : BidiParagraphResolver.visualOrder(spanLevels)) {
            visualOrder.add(position);
        }
        return visualOrder;
    }

    /** Stands in for an inline graphic while a line's direction is resolved. */
    private static final char OBJECT_REPLACEMENT = '￼';

    /**
     * Whether a line has to go through directional layout at all. An explicit
     * right-to-left paragraph always does; otherwise only text that holds something to
     * reorder does, which is a scan rather than a copy.
     */
    private static boolean needsDirectionalLayout(String text,
                                                  BidiParagraphResolver.BaseDirection baseDirection) {
        return baseDirection == BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT
                || BidiParagraphResolver.requiresBidi(text);
    }

    /** The same question for a line that is already split into spans. */
    private static boolean spansNeedDirectionalLayout(List<ParagraphSpan> spans,
                                                      BidiParagraphResolver.BaseDirection baseDirection) {
        if (baseDirection == BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT) {
            return true;
        }
        for (ParagraphSpan span : spans) {
            if (span instanceof ParagraphTextSpan textSpan
                    && BidiParagraphResolver.requiresBidi(textSpan.text())) {
                return true;
            }
        }
        return false;
    }

    private static double inlineLineWidth(List<InlineLayoutToken> tokens) {
        double width = 0.0;
        for (InlineLayoutToken token : tokens) {
            width += token.wrapWidth();
        }
        return width;
    }

    private static List<InlineLayoutToken> trimTrailingWhitespaceTokens(List<InlineLayoutToken> tokens) {
        int end = tokens.size();
        while (end > 0) {
            InlineLayoutToken candidate = tokens.get(end - 1);
            if (candidate == null) {
                end--;
                continue;
            }
            if (candidate instanceof InlineTextToken textToken
                && textToken.highlightGroup() == null
                && (textToken.text() == null || textToken.text().isBlank())) {
                end--;
                continue;
            }
            break;
        }
        return end <= 0 ? List.of() : List.copyOf(tokens.subList(0, end));
    }

    private static InlineLayoutToken trimLeadingIfInlineLineStart(InlineLayoutToken token,
                                                                  List<InlineLayoutToken> currentLine,
                                                                  TextMeasurementSystem measurement) {
        if (token == null) {
            return null;
        }
        if (!(token instanceof InlineTextToken textToken)) {
            return token;
        }
        if (textToken.highlightGroup() != null) {
            // A chip token carries the run's background/group/padding; never strip
            // its leading whitespace or rebuild it via the plain factory (that would
            // drop the fill). The chip's words reassemble in toInlineParagraphLine,
            // which collapses the soft-wrap space at a wrap seam.
            return textToken;
        }
        if (!inlineLineHasVisibleContent(currentLine)) {
            String trimmed = textToken.text() == null ? "" : textToken.text().stripLeading();
            if (trimmed.isEmpty()) {
                return null;
            }
            if (trimmed.equals(textToken.text())) {
                return textToken;
            }
            return InlineTextToken.of(trimmed, textToken.textStyle(), textToken.linkTarget(), measurement);
        }
        return textToken;
    }

    private static boolean inlineLineHasVisibleContent(List<InlineLayoutToken> tokens) {
        for (InlineLayoutToken token : tokens) {
            if (token == null) {
                continue;
            }
            if (token instanceof InlineTextToken textToken) {
                if (textToken.highlightGroup() != null) {
                    // A chip is visible content (it carries a fill) even when its
                    // text is blank — e.g. a colour-swatch badge.
                    return true;
                }
                if (textToken.text() != null && !textToken.text().isBlank()) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private static List<TextDataBody> trimTrailingWhitespaceBodies(List<TextDataBody> bodies) {
        int end = bodies.size();
        while (end > 0) {
            TextDataBody candidate = bodies.get(end - 1);
            if (candidate == null || candidate.text() == null || candidate.text().isBlank()) {
                end--;
                continue;
            }
            break;
        }
        return end <= 0 ? List.of() : List.copyOf(bodies.subList(0, end));
    }

    private static TextDataBody trimLeadingIfLineStart(TextDataBody body,
                                                       List<TextDataBody> currentLine) {
        if (body == null) {
            return null;
        }
        if (!lineHasVisibleContent(currentLine)) {
            String trimmed = body.text() == null ? "" : body.text().stripLeading();
            if (trimmed.isEmpty()) {
                return null;
            }
            return new TextDataBody(trimmed, body.textStyle());
        }
        return body;
    }

    private static boolean lineHasVisibleContent(List<TextDataBody> bodies) {
        for (TextDataBody body : bodies) {
            if (body != null && body.text() != null && !body.text().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static double lineWidth(List<TextDataBody> bodies,
                                    TextMeasurementSystem measurement) {
        double width = 0.0;
        for (TextDataBody body : bodies) {
            if (body == null || body.text() == null || body.text().isEmpty()) {
                continue;
            }
            width += measurement.textWidth(
                    body.textStyle() == null ? TextStyle.DEFAULT_STYLE : body.textStyle(),
                    body.text());
        }
        return width;
    }

    private static String trimTrailingSpaces(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private record ParagraphIndentSpec(String firstLinePrefix, String continuationPrefix) {
        private static ParagraphIndentSpec from(String bulletOffset,
                                                TextStyle style,
                                                TextMeasurementSystem measurement) {
            String raw = bulletOffset == null ? "" : bulletOffset;
            boolean hasVisibleChars = raw.chars().anyMatch(ch -> !Character.isWhitespace(ch));
            if (hasVisibleChars) {
                String normalizedPrefix = normalizeBulletPrefix(raw);
                return new ParagraphIndentSpec(
                        normalizedPrefix,
                        computeIndentFromPrefix(measurement, style, normalizedPrefix));
            }
            return new ParagraphIndentSpec(raw, raw);
        }
    }
}
