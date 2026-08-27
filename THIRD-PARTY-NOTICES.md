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

| Language | Stated license (per its own README / header) |
|----------|----------------------------------------------|
| `cs_CZ`  | GNU GPL (csTeX patterns); conversion dual LGPL/SISSL |
| `de_DE`  | LPPL (TeX `dehyphn.tex`) + LGPL 2 or later (OOo adaptation) |
| `en_GB`  | BSD-style |
| `en_US`  | BSD-style |
| `es`     | Triple: GNU GPL v3+ / GNU LGPL v3+ / MPL |
| `it_IT`  | LGPL |
| `pl_PL`  | LPPL (original); `plhyph.tex` public domain; LGPL (OOo adaptation) |
| `pt_BR`  | LGPL v3 + MPL |
| `pt_PT`  | GNU GPL |
| `ru_RU`  | BSD-3-clause-style, Copyright (c) 1997-2008 Alexander I. Lebedev |
| `sk_SK`  | **No license stated** — attribution only (Jana Chlebikova, via lingucomponent-tools) |
| `uk_UA`  | GNU GPL v2 or later |

Note that several entries state GPL **without** an LGPL or MPL alternative. Those carry
different obligations from the permissively licensed ones, so the set you ship matters.

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
