# make_theme

Everything is in **`MOTD theme maker.html`** — open it in a browser, no server or
install needed. The old Python script is gone.

## The one thing to understand

There are **two different renderings** of the same MOTD, and they are not the
same text:

| who | what they get |
|---|---|
| the 1.14 browser client | the themed card, **hex colours preserved** |
| everyone else | a legacy `§`-code string, **16 colours only** |

Only the browser client asks for the themed card (it sends `MOTD.eaglertheme`).
Vanilla, older versions and the server-list API never do, and they read the MOTD
as a plain string. The **What other clients see** section decides that second
rendering; the `eaglermotd` Velocity plugin is what applies it.

A hex gradient therefore only exists on the browser client. There is no way
around that: 16 colour codes cannot carry 24 bits per channel, so shades that
are close together land on the same code and the gradient flattens out. That is
why **bands** exists below.

## Four outputs

### 1. Legacy codes

The plain-text rendering, following the chosen fallback mode. Paste into
whatever `motd` config a plain server uses.

### 2. Themed MOTD

The JSON chat component with the hidden marker. This is what goes to the browser
client, and it keeps hex.

### 3. EaglerXServer — `listeners.toml`

Output 2 wrapped as a ready-to-paste line:

```toml
server_motd = ['''{ ... }''']
```

Paste it over the `server_motd` line in
`plugins/eaglerxserver/listeners.toml`.

**The `'''` wrapper is not optional.** `server_motd` is a single TOML string, and
a TOML basic string (`"..."`) may not contain a raw newline — so pasting
multi-line JSON straight into `"..."` makes the whole file fail to parse. The
plugin then aborts startup:

```
Server startup aborted: Could not read one or more config files!
Caused by: java.io.IOException: Failed to load config file: .../listeners.toml
```

and every other Eaglercraft plugin fails afterwards with
`EaglerXServer has not been initialized yet!`.

A multi-line **literal** string (`''' ... '''`) is the fix: it allows newlines,
and it keeps backslashes verbatim, so the `\n` inside the JSON stays a
two-character escape and JSON still reads it as a newline.

### 4. eaglermotd plugin settings

The `FALLBACK_MODE` / `FALLBACK_STEPS` / `FALLBACK_MOTD` block matching your
choice under **What other clients see**. Paste into
`velocity/plugin-src/net/lcc/eaglermotd/EaglerThemeMOTDPlugin.java`, then:

```bash
lcc-server/velocity/plugin-src/build.sh
```

## What other clients see

| mode | behaviour |
|---|---|
| **per character** | every character gets the nearest of the 16 classic colours. Smoothest, and the default. |
| **bands** | each gradient is collapsed into N solid bands first. Fewer, wider bands stay visible; N is the slider. |
| **your own text** | sent verbatim, gradient or not. Use this when the plain rendering should be something other than a conversion. |
| **raw JSON** | the themed JSON untouched. Only correct for 1.16+, which parse chat components in the MOTD; anything older shows a wall of text. |

Bands are equal-width slices of each gradient, computed per line, and each band
takes its band's average colour. Averaging is what stops a band flickering
between two codes at its boundary.

## Checks

```bash
node test-page.js          # the page's own logic
./check-output.sh          # page vs plugin, must agree
lcc-server/velocity/plugin-src/build.sh   # plugin tests, then repackage
```

`check-output.sh` exists because the page previews what other clients will see
but only the plugin decides it. If the two drift apart the preview is lying,
and that is invisible from inside a browser. It builds the plugin, has both
sides band the same gradients, and diffs the colour sequences.

The colour metric is mirrored in both: a luminance-weighted distance, so pure
`#00ff00` does not land on dark green and `#ffff00` does not land on gold. CIE
Lab scores one answer better on a 40-colour sample and was not worth the
matrix multiply and cube root.

## How the marker is hidden

The themed MOTD is a JSON chat component:

```json
{
  "text": "My Server",
  "bold": true,
  "color": "#ffd24a",
  "eagler_theme": { "v": 1, "accent": 4880312, "...": "..." },
  "extra": [ { "text": "\nA themed server entry", "color": "#a0a0ad", "bold": false } ]
}
```

`eagler_theme` is not obfuscated or truncated. It's a field the vanilla chat
component parser doesn't know, so vanilla clients ignore it and render only
`text`, `color` and `extra`. Any fork that hasn't implemented this ignores it
too, so it's safe to leave in place permanently.

The preview shows both renderings so you can confirm before shipping.

## Theme fields

| Field | Meaning |
|---|---|
| `v` | Marker version. Must match `ServerMotdTheme.VERSION` in the client. |
| `accent` | Primary accent, `0xRRGGBB`. Left bar and ping colour. |
| `accentDark` | Gradient start, `0xRRGGBB`. |
| `bg` | Card background, `0xRRGGBB`. |
| `gradient` | Draw the stepped gradient. |
| `radius` | Corner radius. |
| `showLogo` | Draw the logo if one is set. |
| `showMotd` | Draw the subtitle line. |
| `logo` | Base64 PNG, same encoding as a vanilla favicon. Omitted if no image. |

Pick the colours with the colour inputs; they're serialised into the marker
automatically.

## Logo

Use a **64×64 PNG** — the same constraint vanilla puts on server icons, and
whatever `logo.png` is picked here gets encoded inline into the MOTD. Keep it
small; large logos make for long MOTDs.

## Notes

- The theme only affects the server list entry, nothing inside the world.
- To disable a theme, drop back to the plain legacy string. The marker does
  nothing on its own.
- If you change the schema, bump `VERSION` in `MOTD theme maker.html` **and**
  `ServerMotdTheme.VERSION` in the client together, or the marker is ignored.
- `colors.txt` is a leftover from the deleted `theme.py` and is **not read** by
  the page. The colours live in the page's own inputs.