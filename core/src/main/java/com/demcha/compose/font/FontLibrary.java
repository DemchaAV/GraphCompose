package com.demcha.compose.font;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Document-scoped font registry and resolver.
 *
 * <p>{@code FontLibrary} maps logical {@link FontName} values to concrete
 * backend font objects. Public authoring code deals only with logical names;
 * measurement and rendering phases resolve those names through typed backend
 * lookups keyed by the backend's own font {@code Class} token.</p>
 *
 * <p>The library supports both eagerly registered fonts and lazily created
 * fonts through factories.</p>
 *
 * @author Artem Demchyshyn
 */
public class FontLibrary {

    private static final Logger LOG = LoggerFactory.getLogger(FontLibrary.class);

    /**
     * Face aliases already reported, so the warning costs one line per name, not per
     * lookup. Package-private so the guard covering the warning can start from a known
     * state — a static cache is otherwise order-dependent across a test class.
     */
    static final Set<FontName> WARNED_FACE_ALIASES = ConcurrentHashMap.newKeySet();

    private static final Map<FontName, FontName> FONT_ALIASES = Map.ofEntries(
            Map.entry(FontName.HELVETICA_BOLD, FontName.HELVETICA),
            Map.entry(FontName.HELVETICA_OBLIQUE, FontName.HELVETICA),
            Map.entry(FontName.HELVETICA_BOLD_OBLIQUE, FontName.HELVETICA),
            Map.entry(FontName.TIMES_BOLD, FontName.TIMES_ROMAN),
            Map.entry(FontName.TIMES_ITALIC, FontName.TIMES_ROMAN),
            Map.entry(FontName.TIMES_BOLD_ITALIC, FontName.TIMES_ROMAN),
            Map.entry(FontName.COURIER_BOLD, FontName.COURIER),
            Map.entry(FontName.COURIER_OBLIQUE, FontName.COURIER),
            Map.entry(FontName.COURIER_BOLD_OBLIQUE, FontName.COURIER));

    private final Map<FontName, Map<Class<?>, Object>> fonts = new ConcurrentHashMap<>();
    private final Map<FontName, Map<Class<?>, Supplier<?>>> fontFactories = new ConcurrentHashMap<>();

    /**
     * Resolves a font family to a concrete backend font object.
     *
     * @param fontName logical family requested by styles or templates
     * @param fontClass backend font type to resolve
     * @param <F> backend font type
     * @return the matching backend font when available
     */
    public <F> Optional<F> getFont(FontName fontName, Class<F> fontClass) {
        Objects.requireNonNull(fontClass, "fontClass");
        FontName resolvedName = resolveBaseFont(fontName);

        Map<Class<?>, Object> fontRegistry = fonts.get(resolvedName);
        if (fontRegistry != null) {
            Object registered = fontRegistry.get(fontClass);
            if (fontClass.isInstance(registered)) {
                return Optional.of(fontClass.cast(registered));
            }
        }

        Map<Class<?>, Supplier<?>> factoryRegistry = fontFactories.get(resolvedName);
        if (factoryRegistry != null) {
            Supplier<?> factory = factoryRegistry.get(fontClass);
            if (factory != null) {
                Object created = Objects.requireNonNull(
                        factory.get(),
                        "Font factory returned null for " + resolvedName + " and " + fontClass.getName());
                F typed = fontClass.cast(created);
                addFont(resolvedName, fontClass, typed);
                return Optional.of(typed);
            }
        }

        return Optional.empty();
    }

    /**
     * Registers an already created backend font object under a logical family.
     *
     * @param name logical font family
     * @param fontClass backend font type
     * @param font backend font instance
     * @param <F> backend font type
     */
    public <F> void addFont(FontName name, Class<F> fontClass, F font) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fontClass, "fontClass");
        Objects.requireNonNull(font, "font");
        if (!fontClass.isInstance(font)) {
            throw new IllegalArgumentException("Font instance does not match the provided class type.");
        }
        fonts.computeIfAbsent(name, key -> new ConcurrentHashMap<>())
                .put(fontClass, font);
    }

    /**
     * Registers a lazy factory for a backend font object.
     *
     * @param name logical font family
     * @param fontClass backend font type
     * @param factory lazy factory that creates the backend font object
     * @param <F> backend font type
     */
    public <F> void addFontFactory(FontName name, Class<F> fontClass, Supplier<? extends F> factory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fontClass, "fontClass");
        Objects.requireNonNull(factory, "factory");
        fontFactories.computeIfAbsent(name, key -> new ConcurrentHashMap<>())
                .put(fontClass, factory);
    }

    /**
     * Convenience overload that registers a font under its runtime class.
     *
     * @param name logical font family
     * @param font backend font object
     * @param <F> backend font type
     */
    @SuppressWarnings("unchecked")
    public <F> void addFont(FontName name, F font) {
        Objects.requireNonNull(font, "font");
        addFont(name, (Class<F>) font.getClass(), font);
    }

    /**
     * Registers a font from a logical family plus concrete font pair.
     *
     * @param set font registration tuple
     * @param <F> backend font type
     */
    public <F> void addFont(FontSet<F> set) {
        Objects.requireNonNull(set, "set");
        addFont(set.name(), set.fontClass(), set.font());
    }

    /**
     * Returns the set of logical families currently known to this library.
     *
     * @return available logical font names
     */
    public Set<FontName> availableFonts() {
        Set<FontName> all = new LinkedHashSet<>();
        all.addAll(fonts.keySet());
        all.addAll(fontFactories.keySet());
        return Collections.unmodifiableSet(all);
    }

    /**
     * Returns the family a declared font name is actually laid out in.
     *
     * <p>Two kinds of name resolve to something other than themselves. {@code DEFAULT}
     * (and {@code null}) become {@code HELVETICA}. A standard-14 <em>face</em> such as
     * {@code HELVETICA_BOLD} becomes its family, because the face is chosen later from
     * the style's decoration — so a style that names the bold face and sets no
     * decoration renders regular.</p>
     *
     * <p>Both rewrites lay out and draw without error, which is what makes them
     * expensive: the authoring code is correct, the document is not, and nothing in the
     * output says so. Exposing the rule lets the layout snapshot report the declared and
     * the resolved name side by side.</p>
     *
     * <p>Pure and side-effect free, unlike the internal path it backs: a diagnostic
     * projection has no business emitting warnings while it looks.</p>
     *
     * @param fontName declared font name, or {@code null}
     * @return the font family the text is laid out in
     * @since 2.2.2
     */
    public static FontName resolveFamily(FontName fontName) {
        if (fontName == null || FontName.DEFAULT.equals(fontName)) {
            return FontName.HELVETICA;
        }
        FontName base = FONT_ALIASES.get(fontName);
        return base == null ? fontName : base;
    }

    private FontName resolveBaseFont(FontName fontName) {
        FontName resolved = resolveFamily(fontName);
        // DEFAULT is not a face alias — it is the absence of a choice, and warning
        // about it would fire on every document that never named a font at all.
        if (fontName != null && !FontName.DEFAULT.equals(fontName) && !resolved.equals(fontName)) {
            warnOnceAboutFaceAlias(fontName, resolved);
        }
        return resolved;
    }

    /**
     * Warns the first time a style selects a standard-14 face by name.
     *
     * <p>A name like {@code HELVETICA_BOLD} is an alias of its family: it is rewritten
     * to {@code HELVETICA} here, and the face is chosen later from the style's
     * decoration. Naming the face therefore contributes nothing, and a style that names
     * it and sets no decoration renders regular — silently, since the text still lays
     * out and still draws. One line per distinct alias, so a document that uses the form
     * a thousand times says so once.</p>
     */
    private static void warnOnceAboutFaceAlias(FontName requested, FontName base) {
        if (WARNED_FACE_ALIASES.add(requested)) {
            LOG.warn("fontName({}) selects the {} family, not a face — the face comes from the "
                            + "style's decoration. Name the family and set decoration(...) instead; "
                            + "as written this renders {} unless a decoration says otherwise.",
                    requested.name(), base.name(), base.name());
        }
    }
}
