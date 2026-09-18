package com.demcha.documentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a JSON document into maps, lists, strings, doubles, booleans and nulls, refusing
 * anything RFC 8259 does not allow.
 *
 * <p>A guard that reads a manifest a page loads needs the browser's verdict, not a lenient
 * one: a document {@code Response.json()} rejects must fail the build rather than read as
 * a shorter catalogue. So a raw control character in a string, {@code +1}, {@code 01}, a
 * trailing comma and whitespace other than space, tab, line feed and carriage return are
 * all errors. The core test classpath carries no JSON library, which is why this exists.</p>
 */
final class StrictJsonReader {

    private static final Pattern NUMBER = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");
    private static final Pattern HEX_DIGITS = Pattern.compile("[0-9a-fA-F]{4}");

    private final String text;
    private int at;

    private StrictJsonReader(String text) {
        this.text = text;
    }

    /**
     * Reads one JSON document.
     *
     * @param text the document
     * @return the value it holds
     * @throws IllegalStateException if the text is not exactly one valid JSON document
     */
    static Object read(String text) {
        StrictJsonReader reader = new StrictJsonReader(text);
        Object value = reader.readValue();
        reader.skipWhitespace();
        if (reader.at != text.length()) {
            throw reader.error("content after the document");
        }
        return value;
    }

    private Object readValue() {
        skipWhitespace();
        if (at >= text.length()) {
            throw error("the document ends early");
        }
        return switch (text.charAt(at)) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> members = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (consume('}')) {
            return members;
        }
        do {
            skipWhitespace();
            String name = readString();
            skipWhitespace();
            expect(':');
            members.put(name, readValue());
            skipWhitespace();
        } while (consume(','));
        expect('}');
        return members;
    }

    private List<Object> readArray() {
        List<Object> items = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (consume(']')) {
            return items;
        }
        do {
            items.add(readValue());
            skipWhitespace();
        } while (consume(','));
        expect(']');
        return items;
    }

    private String readString() {
        expect('"');
        StringBuilder value = new StringBuilder();
        while (at < text.length()) {
            char c = text.charAt(at++);
            if (c == '"') {
                return value.toString();
            }
            if (c < 0x20) {
                throw error("a raw control character in a string");
            }
            if (c != '\\') {
                value.append(c);
                continue;
            }
            if (at >= text.length()) {
                break;
            }
            char escape = text.charAt(at++);
            switch (escape) {
                case '"', '\\', '/' -> value.append(escape);
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> value.append(readUnicodeEscape());
                default -> throw error("an unknown escape \\" + escape);
            }
        }
        throw error("an unterminated string");
    }

    private char readUnicodeEscape() {
        if (at + 4 > text.length() || !HEX_DIGITS.matcher(text).region(at, at + 4).matches()) {
            throw error("a malformed \\u escape");
        }
        char c = (char) Integer.parseInt(text, at, at + 4, 16);
        at += 4;
        return c;
    }

    private Object readLiteral(String literal, Object value) {
        if (!text.startsWith(literal, at)) {
            throw error("an unknown literal");
        }
        at += literal.length();
        return value;
    }

    private Double readNumber() {
        Matcher number = NUMBER.matcher(text).region(at, text.length());
        if (!number.lookingAt()) {
            throw error("an unexpected character");
        }
        at = number.end();
        return Double.valueOf(number.group());
    }

    private void skipWhitespace() {
        while (at < text.length() && " \t\n\r".indexOf(text.charAt(at)) >= 0) {
            at++;
        }
    }

    private boolean consume(char expected) {
        if (at < text.length() && text.charAt(at) == expected) {
            at++;
            return true;
        }
        return false;
    }

    private void expect(char expected) {
        if (!consume(expected)) {
            throw error("'" + expected + "' expected");
        }
    }

    private IllegalStateException error(String problem) {
        return new IllegalStateException("not valid JSON: " + problem + " at offset " + at);
    }
}
