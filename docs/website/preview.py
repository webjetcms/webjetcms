#!/usr/bin/env python3
"""Preview article fragments in the existing public WebJET CMS template."""

import argparse
import mimetypes
import re
from html import escape, unescape
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urljoin, urlsplit
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parent
SITE = "https://www.webjetcms.sk"
PAGES = {
    "/": "index.html",
    "/domov/": "index.html",
    "/benefity/": "benefity.html",
    "/funkcie/": "funkcie.html",
    "/aplikacie/": "aplikacie.html",
    "/pre-vyvojarov/": "pre-vyvojarov.html",
    "/pre-agentury/": "pre-agentury.html",
}
CACHE = {}
VIDEO_INCLUDE = re.compile(
    r"!INCLUDE\(/components/video/video_player\.jsp,\s*"
    r"file=https://www\.youtube\.com/watch\?v=([A-Za-z0-9_-]{11}),\s*"
    r"width=425,\s*height=355,\s*percentageWidth=100,\s*align=left,\s*"
    r"autoplay=0,\s*controls=1,\s*fullscreen=1,\s*branding=0,\s*rel=1,\s*showinfo=0\)!"
)
VIDEO_TITLES = {
    "B_m_vPPel80": "PageBuilder: tvorba stránky z blokov",
    "LhXo7zx7bEc": "AI asistenti vo WebJET CMS",
    "z3QGUdDG2pk": "Plánované publikovanie textu, obrázkov a súborov",
    "XRnwipQ-mH4": "Editor formulárov WebJET CMS",
    "2r6-0zk5ZNQ": "Vlastné aplikácie v katalógu WebJET CMS",
}


def render_videos(fragment):
    """Preview only: substitute the supported video macro without executing JSP."""
    def render(match):
        video_id = match.group(1)
        title = escape(VIDEO_TITLES.get(video_id, "Videoukážka WebJET CMS"), quote=True)
        return (
            '<div class="videoBox"><div class="embed-responsive embed-responsive-16by9">'
            '<iframe class="embed-responsive-item" width="425" height="355" '
            f'src="https://www.youtube.com/embed/{video_id}'
            '?autoplay=0&amp;controls=1&amp;modestbranding=0&amp;rel=1&amp;showinfo=0" '
            f'title="{title}" loading="lazy" frameborder="0" allowfullscreen '
            'referrerpolicy="strict-origin-when-cross-origin"></iframe></div></div>'
        )

    return VIDEO_INCLUDE.sub(render, fragment)


def download(url):
    if url not in CACHE:
        with urlopen(url, timeout=20) as response:
            CACHE[url] = (response.read(), response.headers.get_content_type())
    return CACHE[url]


def template():
    source = download(SITE + "/")[0].decode("utf-8")
    source = source.replace('class="no-js', 'class="js', 1)
    source = re.sub(r"<script\b[^>]*>.*?</script\s*>", "", source, flags=re.S | re.I)
    source = re.sub(r"<noscript\b[^>]*>.*?</noscript\s*>", "", source, flags=re.S | re.I)
    source = re.sub(r"<form\b[^>]*>.*?</form\s*>", "", source, flags=re.S | re.I)
    source = re.sub(r"\s+on\w+\s*=\s*([\"']).*?\1", "", source, flags=re.S | re.I)
    source = re.sub(r"(<article\b[^>]*>).*?(</article>)", r"\1<!-- ARTICLE -->\2", source, count=1, flags=re.S)
    source = re.sub(r'<link\b[^>]*href="https://fonts.googleapis.com/[^>]+>', '', source)

    def rewrite(match):
        name, value = match.group(1), unescape(match.group(2))
        if value.startswith("javascript:"):
            value = SITE + "/kontakt/"
        elif value.startswith("/") and not value.startswith("//"):
            path = urlsplit(value).path
            if path not in PAGES and not path.startswith(("/templates/", "/images/", "/thumb/", "/components/")):
                value = urljoin(SITE, value)
        return name + '="' + escape(value, quote=True) + '"'

    source = re.sub(r'\b(href|src)="([^"]*)"', rewrite, source)
    source = source.replace("<title>", "<title>Náhľad obsahu – ", 1)
    return source


SHELL = None


class PreviewHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = urlsplit(self.path).path
        try:
            if path in PAGES:
                fragment = (ROOT / "sk" / PAGES[path]).read_text()
                fragment = render_videos(fragment)
                fragment = re.sub(
                    r'href="(/[^\"]*)"',
                    lambda match: match.group(0) if urlsplit(unescape(match.group(1))).path in PAGES
                    or match.group(1).startswith("/images/") else 'href="' + SITE + match.group(1) + '"',
                    fragment,
                )
                shell = SHELL if PAGES[path] == "index.html" else SHELL.replace("pg-homepage", "")
                content = shell.replace("<!-- ARTICLE -->", fragment).encode()
                content_type = "text/html; charset=utf-8"
            elif path.startswith("/images/webjetcms-2026/"):
                image = ROOT / "sk" / "images" / path.rsplit("/", 1)[-1]
                content = image.read_bytes()
                content_type = mimetypes.guess_type(image.name)[0] or "application/octet-stream"
            elif path.startswith(("/templates/", "/images/", "/thumb/", "/components/")):
                content, content_type = download(SITE + self.path)
            else:
                self.send_error(404)
                return
            self.send_response(200)
            self.send_header("Content-Type", content_type)
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(content)
        except Exception as error:
            self.send_error(502, str(error))

    def log_message(self, format, *args):
        if args and str(args[1]) not in ("200", "304"):
            super().log_message(format, *args)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    SHELL = template()
    print(f"Preview: http://127.0.0.1:{args.port}/", flush=True)
    print("The six local fragments reload on each request. Ctrl+C stops the server.", flush=True)
    ThreadingHTTPServer(("127.0.0.1", args.port), PreviewHandler).serve_forever()
