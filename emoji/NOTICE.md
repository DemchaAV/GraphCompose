# graph-compose-emoji — NOTICE

## What ships here

The colour-emoji glyphs under `src/main/resources/emoji/svg/<codepoint>.svg` are
the **[Noto Emoji](https://github.com/googlefonts/noto-emoji)** SVG set
(© Google), licensed under the **SIL Open Font License 1.1** — see
[`OFL.txt`](OFL.txt). They are vector SVG (not the CBDT colour *font*, which
PDFBox renders blank), so the engine draws them as crisp inline vectors.

The shortcode index `src/main/resources/emoji/emoji-index.properties`
(`shortcode=codepoint`) maps GitHub-style shortcodes to glyphs, generated from
the **[github/gemoji](https://github.com/github/gemoji)** database (MIT).

The engine resolves these via `com.demcha.compose.document.emoji.EmojiLibrary`
and `RichText.emoji(":rocket:", size)` — it carries no emoji art and has no
Maven dependency on this module.

## Regenerating the set

`emoji/tools/build-emoji-set.py` rebuilds `svg/` + `emoji-index.properties` from
fresh sources — re-run it to track a newer Noto Emoji / gemoji, no engine change:

```bash
# 1) noto-emoji SVGs (sparse, shallow)
git clone --depth 1 --filter=blob:none --sparse \
    https://github.com/googlefonts/noto-emoji.git target/noto-emoji
(cd target/noto-emoji && git sparse-checkout set svg)

# 2) gemoji shortcode database
curl -fsSL https://raw.githubusercontent.com/github/gemoji/master/db/emoji.json \
    -o target/gemoji.json

# 3) generate the module resources
python emoji/tools/build-emoji-set.py \
    --noto target/noto-emoji/svg --gemoji target/gemoji.json \
    --out emoji/src/main/resources/emoji
```

The tool copies each `noto svg/emoji_u<cps>.svg` to `emoji/svg/<cps>.svg`
(`_`→`-`), and maps each gemoji alias to its codepoint (dropping the `FE0F`
variation selector, which Noto omits from filenames). Glyphs a real-world SVG
feature the engine's parser cannot handle are skipped at render time and fall
back to the literal shortcode text.
