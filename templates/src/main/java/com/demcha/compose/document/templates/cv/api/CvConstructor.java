package com.demcha.compose.document.templates.cv.api;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.ModuleSection;

import java.util.Objects;

/**
 * The constructor contract: one method per module <em>shape</em>, not per
 * CV meaning.
 *
 * <p>A template that implements this does not know whether a section is
 * Experience, Projects, or a heading the author invented. It knows how
 * to draw prose, a bullet list, an inline list, and a timeline — with
 * and without dates. JSON (or any runtime mapper) picks the kind; the
 * template implements the kind.</p>
 *
 * <p>There are no defaults on the kind methods. Adding a {@link CvKind}
 * constant adds a method here, and every {@link ModularCvTemplate}
 * fails to compile until it draws the new shape. That is the point: a
 * constructor surface that grows in one place and pulls every template
 * with it, rather than a shared renderer that can absorb a new kind
 * without the templates noticing.</p>
 *
 * <p>{@link #render(SectionBuilder, ModuleSection, BrandTheme)} is the
 * dispatcher, and it is a default because it is not a shape. Its
 * {@code switch} is exhaustive over {@link CvKind}, so a new constant
 * without a method is a compile error here too.</p>
 *
 * @since 2.3.0
 */
public interface CvConstructor {

    /**
     * The method on this interface that draws {@code kind}.
     *
     * <p>Kept next to the methods themselves so a test can prove the
     * bijection without copying the names.</p>
     *
     * @param kind a module shape
     * @return the method name, such as {@code "entriesDated"}
     */
    static String methodName(CvKind kind) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case PARAGRAPH -> "paragraph";
            case BULLETS -> "bullets";
            case BULLETS_STACKED -> "bulletsStacked";
            case INLINE_LIST -> "inlineList";
            case ENTRIES -> "entries";
            case ENTRIES_DATED -> "entriesDated";
        };
    }

    /**
     * Dispatches {@code module} to the kind method the author picked.
     *
     * @param host   host section receiving the body
     * @param module the module; its {@link ModuleSection#kind() kind} selects
     *               the method
     * @param theme  the active theme
     */
    default void render(SectionBuilder host, ModuleSection module, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(theme, "theme");
        switch (module.kind()) {
            case PARAGRAPH -> paragraph(host, module, theme);
            case BULLETS -> bullets(host, module, theme);
            case BULLETS_STACKED -> bulletsStacked(host, module, theme);
            case INLINE_LIST -> inlineList(host, module, theme);
            case ENTRIES -> entries(host, module, theme);
            case ENTRIES_DATED -> entriesDated(host, module, theme);
        }
    }

    /**
     * Prose under the section heading. Reads each item's body only.
     *
     * @param host   host section receiving the body
     * @param module a {@link CvKind#PARAGRAPH} module
     * @param theme  the active theme
     */
    void paragraph(SectionBuilder host, ModuleSection module, BrandTheme theme);

    /**
     * A bullet per item, description on the same line.
     *
     * @param host   host section receiving the body
     * @param module a {@link CvKind#BULLETS} module
     * @param theme  the active theme
     */
    void bullets(SectionBuilder host, ModuleSection module, BrandTheme theme);

    /**
     * A bullet per item, description stacked underneath.
     *
     * @param host   host section receiving the body
     * @param module a {@link CvKind#BULLETS_STACKED} module
     * @param theme  the active theme
     */
    void bulletsStacked(SectionBuilder host, ModuleSection module, BrandTheme theme);

    /**
     * One line per item, the description collapsed after a bold label.
     *
     * @param host   host section receiving the body
     * @param module a {@link CvKind#INLINE_LIST} module
     * @param theme  the active theme
     */
    void inlineList(SectionBuilder host, ModuleSection module, BrandTheme theme);

    /**
     * Timeline entries without the date column.
     *
     * @param host   host section receiving the body
     * @param module a {@link CvKind#ENTRIES} module
     * @param theme  the active theme
     */
    void entries(SectionBuilder host, ModuleSection module, BrandTheme theme);

    /**
     * Timeline entries with the date column.
     *
     * @param host   host section receiving the body
     * @param module a {@link CvKind#ENTRIES_DATED} module
     * @param theme  the active theme
     */
    void entriesDated(SectionBuilder host, ModuleSection module, BrandTheme theme);
}
