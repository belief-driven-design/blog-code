# This Blog Now Speaks Gemini

Everything that turns the Hugo-based blog into a Gemini capsule, taken as-is from the real build.
Nothing here needs an extra tool: the same `hugo` run that renders the HTML writes an `index.gmi` next to every `index.html`.

```text
content/posts/*.md ──> hugo ──┬──> index.html   ──> nginx  (HTTPS)
                              └──> index.gmi    ──> Agate  (Gemini)
```

## Configuration

* [`hugo.toml`](hugo.toml)\
  The new media types, the `gemini` and `gemfeed` output formats, and which page kinds get them.
  The `protocol` makes `.Permalink` return `gemini://` URLs, which the Atom feed needs.

## The converter

The actual work happens in four partials.
[`content.html`](layouts/partials/gemini/content.html) gets the page's Markdown from `.RenderShortcodes`, meaning with all shortcodes already replaced by their `.gemini.gmi` variants, and walks it line by line:

* [`content.html`](layouts/partials/gemini/content.html)\
  Block level: joins soft-wrapped paragraphs, list items, and quotes into single lines, caps headings at three levels, passes code fences through, and turns tables into preformatted text.

* [`inline.html`](layouts/partials/gemini/inline.html)\
  Inline level: reduces links to their text and collects them as `=>` lines that go right below the block.

* [`plain.html`](layouts/partials/gemini/plain.html)\
  Strips emphasis and Markdown escapes, but only outside of code spans.

* [`url.html`](layouts/partials/gemini/url.html)\
  Maps link destinations to the capsule: `ref` links become relative paths, images in `assets/` get published, bare `#fragments` are dropped.

## The templates

* [`single.gemini.gmi`](layouts/_default/single.gemini.gmi): a single article, with series and tag links.
* [`home.gemini.gmi`](layouts/_default/home.gemini.gmi): the capsule's home page, all articles grouped by year.
  The `YYYY-MM-DD Title` link format makes it subscribable as a gemlog, too.
* [`term.gemini.gmi`](layouts/_default/term.gemini.gmi): tag and series pages.
* [`home.gemfeed.xml`](layouts/_default/home.gemfeed.xml): the Atom feed with `gemini://` links.

## The shortcodes

Every shortcode the blog uses has a gemtext variant in [`layouts/shortcodes`](layouts/shortcodes).
They're specific to this blog, so they're more of a reference for what such a variant looks like than something to copy as-is.

`botsafe` and `email` deliberately fail the build.
The HTML versions hide contact details from scrapers with JavaScript, and gemtext has no way to do that, so pages using them are kept HTML-only.

## Serving it

* [`docker-compose.yaml`](docker-compose.yaml)\
  [Agate](https://github.com/mbrubeck/agate) running next to the existing nginx container, reading the same directory.
  It generates a self-signed certificate on first start, which is normal for Gemini: clients pin it on first use instead of trusting a certificate authority.

* [`justfile`](justfile)\
  `just serve-gemini` builds the site and serves it on `gemini://localhost/`.

Without a Gemini client at hand, `openssl` is enough to check a running capsule:

```shell
printf 'gemini://localhost/\r\n' \
  | openssl s_client -quiet -connect localhost:1965 -servername localhost
```

The response starts with `20 text/gemini; lang=en`, followed by the home page.

## Caveats

The converter is a line-based state machine in Go templates, not a Markdown parser.
It handles everything the blog's ~130 articles actually use, but nested block structures, reference-style links, or footnotes aren't supported.

The templates were verified with Hugo 0.169.0.
