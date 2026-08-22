package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.api.ModularCvTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Every shipped CV preset, by the id it publishes.
 *
 * <p>A preset is a class, and picking one at compile time is a constructor
 * call. Picking one at <em>runtime</em> — from a dropdown, a config file, a
 * request field — is a lookup, and until now every caller wrote its own:
 * a switch, a map, a list that has to be remembered when a preset ships.
 * The consumer this model exists for keeps exactly such a map in another
 * repository, where nothing tells it a preset was added.</p>
 *
 * <pre>{@code
 * CvTemplates.byId("modern-professional")
 *            .orElseThrow()
 *            .compose(session, doc);
 * }</pre>
 *
 * <p>{@link #modular()} is the list to offer when the document is assembled
 * at runtime: the presets that promise to render whatever they are handed
 * (see {@link ModularCvTemplate}). The rest stay in {@link #all()} for
 * callers who build the canonical sections by hand — they are not lesser
 * templates, they are templates with a fixed idea of what a CV contains.</p>
 *
 * <p>Every lookup builds a fresh template with the preset's own default
 * theme and default layout; a caller wanting a variant calls that preset's
 * own factory directly — {@code create(BrandTheme)}, or {@code create(Options)}
 * where the preset offers one, as {@code NordicClean} and
 * {@code TimelineMinimal} do. {@code CvTemplatesCoverageTest} holds
 * this catalogue to the presets package, so a preset added and not
 * registered fails the build rather than staying invisible to every runtime
 * caller.</p>
 *
 * @since 2.3.0
 */
public final class CvTemplates {

    /**
     * One catalogue entry. Keeping the id, the margin, and the factory
     * together is what makes the catalogue impossible to half-update: there
     * is one list, and a preset is in it or it is not.
     */
    private record Preset(String id, double recommendedMargin,
                          Supplier<DocumentTemplate<CvDocument>> factory) {
    }

    /** The catalogue, in the order a gallery shows it. */
    private static final List<Preset> PRESETS = List.of(
            new Preset(ModernProfessional.ID, ModernProfessional.RECOMMENDED_MARGIN,
                    ModernProfessional::create),
            new Preset(BoxedSections.ID, BoxedSections.RECOMMENDED_MARGIN,
                    BoxedSections::create),
            new Preset(MinimalUnderlined.ID, MinimalUnderlined.RECOMMENDED_MARGIN,
                    MinimalUnderlined::create),
            new Preset(Executive.ID, Executive.RECOMMENDED_MARGIN, Executive::create),
            new Preset(CenteredHeadline.ID, CenteredHeadline.RECOMMENDED_MARGIN,
                    CenteredHeadline::create),
            new Preset(BlueBanner.ID, BlueBanner.RECOMMENDED_MARGIN, BlueBanner::create),
            new Preset(ClassicSerif.ID, ClassicSerif.RECOMMENDED_MARGIN, ClassicSerif::create),
            new Preset(EditorialBlue.ID, EditorialBlue.RECOMMENDED_MARGIN,
                    EditorialBlue::create),
            new Preset(CompactMono.ID, CompactMono.RECOMMENDED_MARGIN, CompactMono::create),
            new Preset(EngineeringResume.ID, EngineeringResume.RECOMMENDED_MARGIN,
                    EngineeringResume::create),
            new Preset(NordicClean.ID, NordicClean.RECOMMENDED_MARGIN, NordicClean::create),
            new Preset(Panel.ID, Panel.RECOMMENDED_MARGIN, Panel::create),
            new Preset(TimelineMinimal.ID, TimelineMinimal.RECOMMENDED_MARGIN,
                    TimelineMinimal::create),
            new Preset(MonogramSidebar.ID, MonogramSidebar.RECOMMENDED_MARGIN,
                    MonogramSidebar::create),
            new Preset(SidebarPortrait.ID, SidebarPortrait.RECOMMENDED_MARGIN,
                    SidebarPortrait::create),
            new Preset(MintEditorial.ID, MintEditorial.RECOMMENDED_MARGIN,
                    MintEditorial::create));

    private CvTemplates() {
    }

    /**
     * The template published under {@code id}, built with its own default
     * theme.
     *
     * @param id a preset id such as {@code "modern-professional"}; leading
     *           and trailing whitespace is ignored. An unknown or null id
     *           yields an empty result rather than an exception — an id
     *           arriving from a config file or a request is input to
     *           validate, not a programming error
     * @return the template, or empty when no preset publishes that id
     */
    public static Optional<DocumentTemplate<CvDocument>> byId(String id) {
        return find(id).map(preset -> preset.factory().get());
    }

    /**
     * Every shipped preset, freshly built, in gallery order.
     *
     * @return one template per preset
     */
    public static List<DocumentTemplate<CvDocument>> all() {
        List<DocumentTemplate<CvDocument>> templates = new ArrayList<>(PRESETS.size());
        for (Preset preset : PRESETS) {
            templates.add(preset.factory().get());
        }
        return List.copyOf(templates);
    }

    /**
     * The presets that render whatever the document hands them — the ones
     * to offer for a CV assembled at runtime.
     *
     * @return one template per preset implementing {@link ModularCvTemplate},
     * in gallery order
     */
    public static List<ModularCvTemplate> modular() {
        List<ModularCvTemplate> templates = new ArrayList<>();
        for (DocumentTemplate<CvDocument> template : all()) {
            if (template instanceof ModularCvTemplate modular) {
                templates.add(modular);
            }
        }
        return List.copyOf(templates);
    }

    /**
     * Every registered preset id, in gallery order.
     *
     * @return the ids {@link #byId(String)} answers to
     */
    public static List<String> ids() {
        List<String> ids = new ArrayList<>(PRESETS.size());
        for (Preset preset : PRESETS) {
            ids.add(preset.id());
        }
        return List.copyOf(ids);
    }

    /**
     * The page margin the preset was designed at, in points — which a caller
     * needs while building the session, before it has a template.
     *
     * @param id a preset id
     * @return the margin, or empty when no preset publishes that id
     */
    public static Optional<Double> recommendedMargin(String id) {
        return find(id).map(Preset::recommendedMargin);
    }

    private static Optional<Preset> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.trim();
        for (Preset preset : PRESETS) {
            if (preset.id().equals(wanted)) {
                return Optional.of(preset);
            }
        }
        return Optional.empty();
    }
}
