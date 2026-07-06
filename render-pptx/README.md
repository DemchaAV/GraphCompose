# GraphCompose Render — PPTX

`io.github.demchaav:graph-compose-render-pptx`

The semantic PPTX export backend for GraphCompose. Split out of the DOCX artifact so a DOCX
consumer never pulls the PPTX code (and vice versa).

## Status

`PptxSemanticBackend` is a **slide-safe semantic manifest skeleton** — it validates the node
graph against what a slide surface can represent; full `.pptx` file emission is not built out
yet. Depend on it for the semantic manifest / forward-compatibility, not for production slide
export. Track the [ROADMAP](../ROADMAP.md) for real PPTX output.

## When to depend on it

Opt-in, at compile scope, only if you target the PPTX semantic surface. Not included by any
of `graph-compose`, `graph-compose-core`, or `graph-compose-bundle`.

## Install

Same version as the rest of the GraphCompose train (lockstep). Copy-paste snippet and the
full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
