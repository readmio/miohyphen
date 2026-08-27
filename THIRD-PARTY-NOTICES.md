# Third-party notices

This library's **code** is licensed under Apache-2.0 (see `LICENSE`). The bundled
**hyphenation pattern data** is third-party and carries its own licensing — read this
before you redistribute an app that ships these dictionaries.

## Hyphenation dictionaries (`src/main/assets/hyphenation/hyph_*.dic`)

The `.dic` files are the TeX/Liang hyphenation patterns distributed by the
[LibreOffice dictionaries project](https://git.libreoffice.org/dictionaries), the same
patterns used by the hunspell `libhyphen` engine, OpenOffice and LibreOffice.

The patterns are *data*, loaded at runtime from separate asset files — they are not linked
into your code. Every `.dic` bundled here is byte-identical to its LibreOffice upstream
(unmodified), and each ships its original attribution file as
`README_hyph_<lang>.txt` alongside it (also bundled in the AAR).

> **Terms differ per language — there is no single license covering all of them.**
> The table below records the license each dictionary states in its own README or header.
> It is a summary for orientation, not legal advice: **read the README for every language
> you actually ship, and get your own review before relying on this in a closed-source
> product.**

### Bundled languages and their stated terms

| Language | Stated license (per its own README / header) | License text |
|----------|----------------------------------------------|--------------|
| `cs_CZ`  | GNU GPL, version unstated (csTeX patterns); conversion dual LGPL/SISSL | `GPL-2.0-only`, `LGPL-2.1-only` |
| `de_DE`  | LPPL (TeX `dehyphn.tex`) + LGPL 2 or later (OOo adaptation) | `LPPL-1.3c`, `LGPL-2.1-only` |
| `en_GB`  | BSD-style | in its README |
| `en_US`  | BSD-style | in its README |
| `es`     | Triple: GNU GPL v3+ / GNU LGPL v3+ / MPL | `GPL-3.0-only`, `LGPL-3.0-only`, `MPL-1.1` |
| `it_IT`  | LGPL | `LGPL-2.1-only` |
| `pl_PL`  | LPPL (original); `plhyph.tex` public domain; LGPL (OOo adaptation) | `LPPL-1.3c`, `LGPL-2.1-only` |
| `pt_BR`  | LGPL v3 + MPL | `LGPL-3.0-only`, `MPL-1.1` |
| `pt_PT`  | GNU GPL, version unstated | `GPL-2.0-only` |
| `ru_RU`  | BSD-3-clause-style, Copyright (c) 1997-2008 Alexander I. Lebedev | in its README |
| `sk_SK`  | Package `LICENSE.txt` carries GPL 2.0 **and** LGPL **and** MPL 1.1 in full; see caveat below | `GPL-2.0-only`, `LGPL-2.1-only`, `MPL-1.1` |
| `uk_UA`  | GNU GPL v2 or later | `GPL-2.0-only` |

Note that several entries state GPL **without** an LGPL or MPL alternative. Those carry
different obligations from the permissively licensed ones, so the set you ship matters.

Where a README says "GNU GPL" or "LPPL" without naming a version, the earliest plausible
text is bundled (`GPL-2.0-only`, `LPPL-1.3c`). Confirm the intended version upstream if it
matters to you.

**`sk_SK` caveat.** The Slovak package ships a `LICENSE.txt` containing GPL 2.0, LGPL and
MPL 1.1 in full, and its `README_en.txt` says the data is released under those three
"you can select one". That grant is written under the README's **Spellchecker** heading;
the **Hyphenation dictionary** heading states attribution only (Jana Chlebikova, converted
with lingucomponent-tools) and names no license. Whether the package-level tri-license
reaches the hyphenation component is not stated explicitly either way. All three texts are
bundled so the choice is available if it applies.

### Bundled license texts

The full texts named above ship in `assets/hyphenation/licenses/` (so they travel into any
AAR/APK that includes this library) and are reproduced verbatim from the SPDX license list:

```
GPL-2.0-only.txt  GPL-3.0-only.txt  LGPL-2.1-only.txt
LGPL-3.0-only.txt  MPL-1.1.txt      LPPL-1.3c.txt
```

`sk_SK` additionally ships `README_hyph_sk_SK_LICENSE.txt`, the Slovak package's own
`LICENSE.txt` from upstream. **If you redistribute this library in an application, these
files must reach your users** — either bundled in the APK (the default, since they are
assets) or reproduced on your app's open-source-licenses screen.

If you don't need a language, delete its `hyph_<lang>.dic` (and `README_hyph_<lang>.txt`)
from `src/main/assets/hyphenation/` — no code change required, and it removes that
language's licensing obligations from your build.

Attribution: keep the `README_hyph_*.txt` files (this library bundles them) and this
notice in your distribution. The `ru_RU` file is upstream's `README_ru_RU.txt` and the
`de_DE` file upstream's `README_hyph_de.txt`, both copied verbatim.

## Algorithm

The Liang hyphenation algorithm implemented in `HyphenationEngine`/`HyphenationDictionary`
is an independent Kotlin reimplementation modelled on the behaviour of
[`pyphen`](https://github.com/Kozea/Pyphen) (GPL 2.0 / LGPL 2.1 / MPL 1.1). No pyphen
source code is included; only its documented algorithm and `.dic` parsing conventions were
followed.
