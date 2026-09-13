# Templates

A template is a finished document design you feed with typed data. You supply the
data, pick a `BrandTheme` where the preset accepts one, and it renders a consistent,
branded document — you never position anything by hand.

Reach for a template when your document is a **known kind**. Everything else — a
report, a statement, a one-off, anything generated from application data — is
ordinary [DSL authoring](../first-document.md), which is the default, not a fallback.

> **Dependency.** Templates ship in `graph-compose-templates`, which is **not**
> part of `graph-compose`. Add it separately, or use `graph-compose-bundle`
> (PDF + templates + fonts + emoji).

### Three words used throughout

| Term | Means | You use it when |
|---|---|---|
| **DSL** | Authoring the document yourself with `pageFlow(...)` and content blocks | The document is not a known kind — the default |
| **Preset** | A shipped, named design such as `ModernInvoice` or `CharcoalGold` | Your document *is* a known kind and you just supply data |
| **Template** | The `DocumentTemplate<S>` contract every preset implements | You want your own reusable design for a new document kind |

A preset is one template. Templates and hand-written DSL compose into the same
`DocumentSession`, so you can mix them in one document.

## Business documents

| Family | Presets | Data records | Start here |
|---|---|---|---|
| **Invoice** | 11 | `templates.data.invoice` | [Business templates](business-templates.md) |
| **Proposal** | 4 | `templates.data.proposal` | [Business templates](business-templates.md) |
| **Receipt** | 1 — `ModernReceipt` | `templates.data.receipt` | [The shipped families](v2-layered/README.md#the-shipped-families) |
| **Rota** (shift schedule) | 1 — `CobaltRota` | `templates.data.rota` | [The shipped families](v2-layered/README.md#the-shipped-families) |

## Profile documents

| Family | Presets | Data records | Start here |
|---|---|---|---|
| **CV** | 26 | `templates.cv.data` (`CvDocument`) | [CV and cover-letter quickstart](v2-layered/quickstart.md) |
| **Cover letter** | 15 | `templates.coverletter.data` (`CoverLetterDocument`) | [CV and cover-letter quickstart](v2-layered/quickstart.md) |

Every preset is a static factory: `SomePreset.create()`, and `create(BrandTheme)`
where the design accepts your palette. The exact spec type a preset takes is the
type parameter on its `create()` — within the invoice family, for example, older
presets take `InvoiceDocumentSpec` while newer ones take `StructuredInvoiceData`.

All six families are built the same way — family data, then components, widgets and
presets over the shared `templates.core.theme`. Receipt and rota are the newest and
smallest, which makes them the easiest two to read end to end.

See the presets rendered in the [examples gallery](../../examples/README.md) and on
the [live showcase](https://demchaav.github.io/GraphCompose/).

## Going further

- **Restyle a shipped preset** — pass your own `BrandTheme`; see the family guide above.
- **Design a new preset in an existing family** — [Authoring presets](v2-layered/authoring-presets.md).
- **Add a whole new family** — [Template contributor guide](v2-layered/contributor-guide.md).
- **Upgrading a pre-2.0 caller** — [Which template system should I use?](which-template-system.md).
