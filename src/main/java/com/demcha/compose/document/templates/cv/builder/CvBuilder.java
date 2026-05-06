package com.demcha.compose.document.templates.cv.builder;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.api.SlotMap;
import com.demcha.compose.document.templates.components.Header;
import com.demcha.compose.document.templates.components.Module;
import com.demcha.compose.document.templates.cv.layouts.CvLayout;
import com.demcha.compose.document.templates.cv.spec.CvModule;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for assembling a Templates v2 CV
 * {@link DocumentTemplate}.
 *
 * <p>A preset class typically wraps one call to
 * {@link #build()} inside its {@code create(BusinessTheme)} factory.
 * The result is a fully-configured {@code DocumentTemplate<CvSpec>}
 * that consumers can use as-is, or whose {@code create(...)} body they
 * can copy and tweak in their own preset class.</p>
 *
 * <p>All seven knobs ({@code id}, {@code displayName}, {@code layout},
 * {@code header}, {@code theme}, {@code spacing}, {@code moduleStyle})
 * must be configured before calling {@link #build()}; missing values
 * are rejected at build time with an explicit {@code NullPointerException}
 * naming the missing field.</p>
 */
public final class CvBuilder {

    private String id;
    private String displayName;
    private CvLayout layout;
    private Header header;
    private BusinessTheme theme;
    private Spacing spacing;
    private Module.Style moduleStyle;
    private final Map<String, List<String>> slotPlacements = new LinkedHashMap<>();

    private CvBuilder() {
    }

    /**
     * Returns a fresh builder.
     *
     * @return new builder instance
     */
    public static CvBuilder builder() {
        return new CvBuilder();
    }

    /**
     * Sets the stable identifier exposed via
     * {@link DocumentTemplate#id()}.
     *
     * @param value non-null identifier
     * @return this builder
     */
    public CvBuilder id(String value) {
        this.id = Objects.requireNonNull(value, "id");
        return this;
    }

    /**
     * Sets the human-readable display name exposed via
     * {@link DocumentTemplate#displayName()}.
     *
     * @param value non-null display name
     * @return this builder
     */
    public CvBuilder displayName(String value) {
        this.displayName = Objects.requireNonNull(value, "displayName");
        return this;
    }

    /**
     * Sets the layout responsible for the document caркас.
     *
     * @param value non-null layout
     * @return this builder
     */
    public CvBuilder layout(CvLayout value) {
        this.layout = Objects.requireNonNull(value, "layout");
        return this;
    }

    /**
     * Sets the header component used to render the document header.
     *
     * @param value non-null header component
     * @return this builder
     */
    public CvBuilder header(Header value) {
        this.header = Objects.requireNonNull(value, "header");
        return this;
    }

    /**
     * Sets the active business theme — provides typography and palette
     * tokens for module rendering.
     *
     * @param value non-null business theme
     * @return this builder
     */
    public CvBuilder theme(BusinessTheme value) {
        this.theme = Objects.requireNonNull(value, "theme");
        return this;
    }

    /**
     * Sets the active spacing tokens — used by module composition for
     * line, list, paragraph, and section margins.
     *
     * @param value non-null spacing tokens
     * @return this builder
     */
    public CvBuilder spacing(Spacing value) {
        this.spacing = Objects.requireNonNull(value, "spacing");
        return this;
    }

    /**
     * Sets the heading-rendering style applied to every module.
     *
     * @param value non-null module style
     * @return this builder
     */
    public CvBuilder moduleStyle(Module.Style value) {
        this.moduleStyle = Objects.requireNonNull(value, "moduleStyle");
        return this;
    }

    /**
     * Appends one or more module names to the given slot. Modules are
     * rendered in the order supplied; modules already placed in the
     * slot remain in front of newly-appended names.
     *
     * <p>Module names must match {@code CvModule.name()} entries in
     * the {@link CvSpec} passed to
     * {@link DocumentTemplate#compose(DocumentSession, Object)
     * compose(session, spec)} at runtime; missing names raise an
     * {@link IllegalStateException} at compose time, naming the
     * offending slot and module.</p>
     *
     * @param slot        non-null slot name (must match one of
     *                    {@code layout.slotNames()})
     * @param moduleNames one or more module names; none may be null
     * @return this builder
     * @throws NullPointerException if {@code slot} or any module name
     *                              is null
     */
    public CvBuilder place(String slot, String... moduleNames) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(moduleNames, "moduleNames");
        for (String name : moduleNames) {
            Objects.requireNonNull(name, "moduleName");
        }
        slotPlacements
                .computeIfAbsent(slot, key -> new ArrayList<>())
                .addAll(Arrays.asList(moduleNames));
        return this;
    }

    /**
     * Validates configuration and returns the assembled
     * {@link DocumentTemplate}.
     *
     * @return ready-to-use template instance
     * @throws NullPointerException if any required setter has not been
     *                              called
     */
    public DocumentTemplate<CvSpec> build() {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(spacing, "spacing");
        Objects.requireNonNull(moduleStyle, "moduleStyle");

        final String capturedId = id;
        final String capturedDisplay = displayName;
        final CvLayout capturedLayout = layout;
        final Header capturedHeader = header;
        final BusinessTheme capturedTheme = theme;
        final Spacing capturedSpacing = spacing;
        final Module.Style capturedStyle = moduleStyle;
        // Preserve insertion order across slots; defensive copy of the
        // per-slot lists.
        final Map<String, List<String>> capturedPlacements = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : slotPlacements.entrySet()) {
            capturedPlacements.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        final Map<String, List<String>> placements =
                Collections.unmodifiableMap(capturedPlacements);

        return new DocumentTemplate<CvSpec>() {
            @Override
            public String id() {
                return capturedId;
            }

            @Override
            public String displayName() {
                return capturedDisplay;
            }

            @Override
            public void compose(DocumentSession session, CvSpec spec) {
                Objects.requireNonNull(session, "session");
                Objects.requireNonNull(spec, "spec");

                DocumentNode headerNode = composeHeader(spec);
                SlotMap slots = composeSlots(spec);
                DocumentNode root = capturedLayout.compose(headerNode, slots);
                session.add(root);
            }

            private DocumentNode composeHeader(CvSpec spec) {
                Header.Input input = new Header.Input(
                        spec.header().name(),
                        spec.header().contactItems(),
                        spec.header().linkLabels());
                return capturedHeader.compose(input);
            }

            private SlotMap composeSlots(CvSpec spec) {
                SlotMap slots = new SlotMap();
                for (Map.Entry<String, List<String>> entry : placements.entrySet()) {
                    String slot = entry.getKey();
                    for (String moduleName : entry.getValue()) {
                        CvModule cvModule = spec.findModule(moduleName).orElseThrow(() ->
                                new IllegalStateException(
                                        "Module '" + moduleName + "' declared in slot '"
                                                + slot + "' but not found in CvSpec"));
                        Module rendered = Module.of(
                                cvModule.name(),
                                cvModule.title(),
                                cvModule.body(),
                                capturedStyle);
                        slots.add(slot, rendered.compose(capturedTheme, capturedSpacing));
                    }
                }
                return slots;
            }
        };
    }
}
