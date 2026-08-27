package com.demcha.compose.document.templates.cv.api;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.Slot;

/**
 * A CV template that implements every constructor shape and renders every
 * section placed in {@link Slot#MAIN} — every {@link CvKind}, under
 * whatever heading the author wrote.
 *
 * <p>The constructor half is {@link CvConstructor}: one method per module
 * shape, no defaults. A JSON mapper (or any runtime assembler) picks a
 * kind; this template draws that kind. It does not know whether the
 * section is Experience or a heading nobody anticipated — that knowledge
 * is not in the contract.</p>
 *
 * <p><strong>The placement promise is exactly {@link Slot#MAIN}, and the
 * slot is part of it.</strong>
 * Every shipped preset reads {@code sectionsIn(Slot.MAIN)} and no other
 * slot — including the ones that compose a sidebar of their own, which fill
 * it from the identity and from the main-slot sections their routing sends
 * there; a section placed in {@link Slot#SIDEBAR} or
 * {@link Slot#FOOTER} is dropped, by these templates as by every other, which
 * is the behaviour {@link com.demcha.compose.document.templates.cv.data.CvDocument}
 * has always documented. Saying so here rather than leaving "whatever the
 * document hands it" to be read generously is the difference between a
 * contract and a slogan — a caller assembling a CV at runtime needs to know
 * that placing a module in a sidebar loses it today.</p>
 *
 * <p>This is the promise a CV assembled at runtime needs, and it is not one
 * every preset can make. Several place their sections into fixed slots and
 * guard each slot on the section's Java type, so a module routed to one is
 * skipped rather than drawn; the CV still renders, minus a section, and
 * looks finished. That failure is invisible from the outside, which is why
 * the capability is declared in the type system rather than assumed: a
 * constructor asks {@link com.demcha.compose.document.templates.cv.presets.CvTemplates#modular()}
 * for the templates it may offer, and the rest stay available to callers
 * who build the canonical sections by hand.</p>
 *
 * <p>Declaring it is not enough to have it. {@code ModularCvTemplateFidelityTest}
 * enumerates the implementations and renders a document carrying every kind,
 * a section this catalogue has no name for, a heading in a script no keyword
 * list contains, and a heading that <em>does</em> match a keyword list — the
 * last because a preset with an editorial vocabulary of its own is the one
 * likely to rename what the author wrote. Each item must reach the page, so
 * the interface cannot be worn by a template that would drop or retitle
 * one. {@code CvConstructorKindGateTest} holds the other half: every kind
 * has a non-default method, and every modular template declares it.</p>
 *
 * @since 2.3.0
 */
public interface ModularCvTemplate extends DocumentTemplate<CvDocument>, CvConstructor {
}
