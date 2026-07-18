# Third-party notices

This library's **code** is licensed under Apache-2.0 (see `LICENSE`). The bundled
**hyphenation pattern data** is third-party and carries its own licensing — read this
before you redistribute an app that ships these dictionaries.

## Hyphenation dictionaries (`src/main/assets/hyphenation/hyph_*.dic`)

The `.dic` files are the TeX/Liang hyphenation patterns distributed by the
[LibreOffice dictionaries project](https://git.libreoffice.org/dictionaries), the same
patterns used by the hunspell `libhyphen` engine, OpenOffice and LibreOffice.

- They are **tri-licensed under GPL 2.0 / LGPL 2.1 / MPL 1.1.** For use in a
  proprietary/closed-source app, rely on the **LGPL-2.1 or MPL-1.1** option (both permit
  redistribution of the unmodified data files in a larger work). The patterns are *data*,
  loaded at runtime from separate asset files — they are not linked into your code.
- **Per-language terms differ.** Each dictionary ships its original
  `README_hyph_<lang>.txt` next to the `.dic` (also bundled in the AAR). Some
  TeX-derived patterns carry their own or LPPL-style terms and attribution requirements.
  **Review the README for every language you actually ship.**
- Attribution: keep the `README_hyph_*.txt` files (this library bundles them) and this
  notice in your distribution.

### Bundled languages

`cs_CZ, de_DE, en_GB, en_US, es, fr, hu_HU, it_IT, nl_NL, pl_PL, pt_BR, pt_PT, ru_RU,
sk_SK, uk_UA`

If you don't need a language, delete its `hyph_<lang>.dic` (and `README_hyph_<lang>.txt`)
from `src/main/assets/hyphenation/` — no code change required, and it removes that
language's licensing obligations from your build.

## Algorithm

The Liang hyphenation algorithm implemented in `HyphenationEngine`/`HyphenationDictionary`
is an independent Kotlin reimplementation modelled on the behaviour of
[`pyphen`](https://github.com/Kozea/Pyphen) (GPL 2.0 / LGPL 2.1 / MPL 1.1). No pyphen
source code is included; only its documented algorithm and `.dic` parsing conventions were
followed.
