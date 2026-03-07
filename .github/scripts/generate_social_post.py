#!/usr/bin/env python3
"""Generate a social media post from a merged GitHub PR and collect screenshots.

Uses GitHub Models API (Copilot LLM) as primary LLM, falls back to Google Gemini.
Outputs:
  - post_content.txt   : Markdown-formatted comment body with the generated post
  - screenshots/       : directory with any screenshots found in the PR
  - screenshot_list.txt: newline-separated list of downloaded screenshot paths
"""

import json
import os
import re
import shutil
import sys
import urllib.error
import urllib.request
from pathlib import Path

# Maximum character length for the generated social media post
POST_MAX_CHARS = 1220

# ---------------------------------------------------------------------------
# Prompt building
# ---------------------------------------------------------------------------

def build_prompt(pr_title: str, pr_body: str, pr_files: list[str]) -> str:
    """Build the LLM prompt to generate the social media post."""
    file_summary = "\n".join(pr_files[:20]) if pr_files else "No changed files listed."
    body = (pr_body or "")[:3000]

    return f"""Based on this GitHub pull request, create a short, friendly social media post for regular users (not developers).

PR Title: {pr_title}
PR Description:
{body}

Changed files:
{file_summary}

Requirements:
- Maximum {POST_MAX_CHARS} characters
- Start with paragraph about what was improved and what will be main benefit for the users.
- Add 1 relevant emoji
- Continue with more details in a 2-4 paragraphs, but keep it concise and engaging. Here you can add more technical details if relevant, but still in an easy-to-understand way.
- IMPORTANT: Focus on user benefits, not features.
- Add relevant hashtags and a question at the end
- Write in Slovak language
- Tone: friendly, easy to understand for non-technical users

Respond with ONLY the post text, nothing else."""


# ---------------------------------------------------------------------------
# LLM back-ends
# ---------------------------------------------------------------------------

_MODEL_GITHUB = "gpt-4o"
_MODEL_GEMINI = "gemini-3.1-pro-preview"
# Maximum tokens for the LLM response (2048 gives safe headroom; Slovak text with diacritics
# can consume more tokens than English, and the model may add preamble before the post)
_POST_MAX_TOKENS = 10000


def call_github_models(pr_title: str, pr_body: str, pr_files: list[str]) -> tuple[str, str, dict] | None:
    """Try to generate post using GitHub Models API (Copilot LLM, gpt-4o).

    Returns (post_text, model_name, token_info) or None on failure.
    """
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        print("GITHUB_TOKEN not available, skipping GitHub Models API.", file=sys.stderr)
        return None

    payload = {
        "model": _MODEL_GITHUB,
        "messages": [
            {
                "role": "system",
                "content": (
                    "You are a helpful assistant that creates engaging social media posts "
                    "about software updates for regular, non-technical users."
                ),
            },
            {"role": "user", "content": build_prompt(pr_title, pr_body, pr_files)},
        ],
        "max_tokens": _POST_MAX_TOKENS,
        "temperature": 0.7,
    }

    url = "https://models.inference.ai.azure.com/chat/completions"
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            post = result["choices"][0]["message"]["content"].strip()
            usage = result.get("usage", {})
            token_info = {
                "prompt": usage.get("prompt_tokens", 0),
                "completion": usage.get("completion_tokens", 0),
                "total": usage.get("total_tokens", 0),
            }
            return post, _MODEL_GITHUB, token_info
    except urllib.error.HTTPError as exc:
        body_text = exc.read().decode("utf-8", errors="replace")[:500]
        print(f"GitHub Models API HTTP {exc.code}: {body_text}", file=sys.stderr)
        return None
    except Exception as exc:  # noqa: BLE001
        print(f"GitHub Models API failed: {exc}", file=sys.stderr)
        return None


def call_gemini(pr_title: str, pr_body: str, pr_files: list[str]) -> tuple[str, str, dict] | None:
    """Generate post using Google Gemini API.

    Returns (post_text, model_name, token_info) or None on failure.
    """
    api_key = os.environ.get("GEMINI_API_KEY", "")
    if not api_key:
        print("GEMINI_API_KEY not set, skipping Gemini API.", file=sys.stderr)
        return None

    payload = {
        "contents": [{"parts": [{"text": build_prompt(pr_title, pr_body, pr_files)}]}],
        "generationConfig": {"maxOutputTokens": _POST_MAX_TOKENS, "temperature": 0.7},
    }

    url = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        f"{_MODEL_GEMINI}:generateContent?key={api_key}"
    )
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Referer": "https://github.com/webjetcms/webjetcms",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            candidate = result["candidates"][0]
            finish_reason = candidate.get("finishReason", "UNKNOWN")
            if finish_reason not in ("STOP", "MAX_TOKENS"):
                print(f"Gemini unexpected finishReason: {finish_reason}", file=sys.stderr)
            if finish_reason == "MAX_TOKENS":
                print("Warning: Gemini response was cut off by MAX_TOKENS limit.", file=sys.stderr)
            post = candidate["content"]["parts"][0]["text"].strip()
            usage = result.get("usageMetadata", {})
            token_info = {
                "prompt": usage.get("promptTokenCount", 0),
                "completion": usage.get("candidatesTokenCount", 0),
                "total": usage.get("totalTokenCount", 0),
            }
            return post, _MODEL_GEMINI, token_info
    except urllib.error.HTTPError as exc:
        body_text = exc.read().decode("utf-8", errors="replace")[:500]
        print(f"Gemini API HTTP {exc.code}: {body_text}", file=sys.stderr)
        return None
    except Exception as exc:  # noqa: BLE001
        print(f"Gemini API failed: {exc}", file=sys.stderr)
        return None


# ---------------------------------------------------------------------------
# GitHub API helpers
# ---------------------------------------------------------------------------

def get_pr_files(repo: str, pr_number: str, token: str) -> list[str]:
    """Return list of filenames changed in the PR (max 100 files)."""
    if not all([repo, pr_number, token]):
        return []

    url = f"https://api.github.com/repos/{repo}/pulls/{pr_number}/files?per_page=100"
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            files = json.loads(resp.read().decode("utf-8"))
            return [f["filename"] for f in files]
    except Exception as exc:  # noqa: BLE001
        print(f"Failed to get PR files: {exc}", file=sys.stderr)
        return []


# ---------------------------------------------------------------------------
# Screenshot extraction
# ---------------------------------------------------------------------------

_IMAGE_EXT = {".png", ".jpg", ".jpeg", ".gif", ".webp"}

# Matches Markdown image syntax: ![alt](path)
_MD_IMAGE_RE = re.compile(
    r'!\[.*?\]\(([^)]+\.(?:png|jpg|jpeg|gif|webp))\)',
    re.IGNORECASE,
)

# Matches HTML <img src="..."> tags
_HTML_IMAGE_RE = re.compile(
    r'<img[^>]+src=["\']([^"\']+\.(?:png|jpg|jpeg|gif|webp))["\']',
    re.IGNORECASE,
)


def find_screenshots(pr_body: str, pr_files: list[str]) -> list[str]:
    """Find screenshot references in the PR body and changed documentation files."""
    found: list[str] = []

    if pr_body:
        for pattern in (_MD_IMAGE_RE, _HTML_IMAGE_RE):
            for m in pattern.finditer(pr_body):
                found.append(m.group(1))

    # Include image files that were added/modified in docs
    for fname in pr_files:
        ext = Path(fname).suffix.lower()
        if ext in _IMAGE_EXT and (
            fname.startswith("docs/")
            or "screenshot" in fname.lower()
        ):
            found.append(fname)

    # Deduplicate while preserving order
    seen: set[str] = set()
    unique: list[str] = []
    for item in found:
        if item not in seen:
            seen.add(item)
            unique.append(item)
    return unique


def download_screenshots(screenshots: list[str]) -> list[str]:
    """Download remote images and copy local files to the screenshots/ directory."""
    output_dir = Path("screenshots")
    output_dir.mkdir(exist_ok=True)
    downloaded: list[str] = []

    for src in screenshots:
        dest_name = Path(src).name
        dest = output_dir / dest_name

        if src.startswith(("http://", "https://")):
            # Only allow GitHub raw content and githubusercontent.com to prevent SSRF.
            allowed_hosts = (
                "raw.githubusercontent.com",
                "github.com",
                "user-images.githubusercontent.com",
            )
            from urllib.parse import urlparse  # noqa: PLC0415 (import inside loop for clarity)
            parsed = urlparse(src)
            if not any(parsed.hostname == h or (parsed.hostname or "").endswith(f".{h}") for h in allowed_hosts):
                print(f"  Skipping untrusted URL: {src}", file=sys.stderr)
                continue
            try:
                # Limit download to 10 MB to avoid excessive resource consumption
                max_bytes = 10 * 1024 * 1024
                req = urllib.request.Request(src, headers={"User-Agent": "WebJET-SocialPost/1.0"})
                with urllib.request.urlopen(req, timeout=30) as resp:  # noqa: S310
                    data = resp.read(max_bytes)
                dest.write_bytes(data)
                downloaded.append(str(dest))
                print(f"  Downloaded: {src} -> {dest}")
            except Exception as exc:  # noqa: BLE001
                print(f"  Failed to download {src}: {exc}", file=sys.stderr)
        else:
            local = Path(src)
            if local.exists():
                shutil.copy2(local, dest)
                downloaded.append(str(dest))
                print(f"  Copied: {src} -> {dest}")
            else:
                print(f"  Screenshot not found locally: {src}", file=sys.stderr)

    return downloaded


# ---------------------------------------------------------------------------
# Output helpers
# ---------------------------------------------------------------------------

def write_outputs(
    post: str,
    pr_title: str,
    pr_number: str,
    pr_url: str,
    downloaded: list[str],
    model_used: str = "",
    token_info: dict | None = None,
) -> None:
    """Write post_content.txt, screenshot_list.txt and GitHub Actions outputs."""
    lines: list[str] = []
    lines.append("## 📢 Návrh príspevku na sociálne siete")
    lines.append("")
    lines.append("> " + post.replace("\n", "\n> "))
    lines.append("")
    lines.append(f"*Počet znakov: {len(post)} / {POST_MAX_CHARS}*")
    if downloaded:
        lines.append("")
        lines.append(f"**Priložené screenshoty ({len(downloaded)}):**")
        for s in downloaded:
            lines.append(f"- `{s}`")
    lines.append("")
    model_info = ""
    if model_used:
        model_info = f" | model: `{model_used}`"
        if token_info and token_info.get("total"):
            model_info += f" | tokeny: {token_info['total']} (prompt: {token_info['prompt']}, odpoveď: {token_info['completion']})"
    lines.append(
        f"<sub>Príspevok vygenerovaný automaticky po mergnutí PR pomocou LLM{model_info}. "
        "Skontrolujte ho pred zverejnením.</sub>"
    )

    content = "\n".join(lines)

    with open("post_content.txt", "w", encoding="utf-8") as fh:
        fh.write(content)

    with open("screenshot_list.txt", "w", encoding="utf-8") as fh:
        for s in downloaded:
            fh.write(f"{s}\n")

    # Publish outputs to GitHub Actions environment
    github_output = os.environ.get("GITHUB_OUTPUT", "")
    if github_output:
        import hashlib  # noqa: PLC0415
        # Use heredoc syntax for multiline values (recommended by GitHub Actions docs)
        delimiter = "EOF_" + hashlib.md5(post.encode()).hexdigest()[:8]
        with open(github_output, "a", encoding="utf-8") as fh:
            fh.write(f"post<<{delimiter}\n")
            fh.write(post)
            fh.write(f"\n{delimiter}\n")
            fh.write(f"has_screenshots={'true' if downloaded else 'false'}\n")
            fh.write(f"screenshot_count={len(downloaded)}\n")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    pr_title = os.environ.get("PR_TITLE", "")
    pr_body = os.environ.get("PR_BODY", "")
    pr_number = os.environ.get("PR_NUMBER", "")
    pr_url = os.environ.get("PR_URL", "")
    repo = os.environ.get("REPO", "")
    token = os.environ.get("GITHUB_TOKEN", "")

    if not pr_title:
        print("Error: PR_TITLE environment variable is required.", file=sys.stderr)
        sys.exit(1)

    print(f"Generating social media post for PR #{pr_number}: {pr_title}")

    # Fetch changed files from the GitHub API
    pr_files = get_pr_files(repo, pr_number, token)
    print(f"Changed files: {len(pr_files)}")

    # Generate the post.
    # If GEMINI_API_KEY is set, prefer Gemini (higher quality), otherwise fall back to GitHub Models.
    result: tuple[str, str, dict] | None = None
    model_used = "fallback"
    token_info: dict = {}

    if os.environ.get("GEMINI_API_KEY"):
        print("Trying Google Gemini API (priority)...")
        result = call_gemini(pr_title, pr_body, pr_files)

    if not result:
        print("Trying GitHub Models API (Copilot LLM)...")
        result = call_github_models(pr_title, pr_body, pr_files)

    if result:
        post, model_used, token_info = result
        print(f"Model used: {model_used}, tokens: {token_info.get('total', 'N/A')}")
    else:
        # Basic hardcoded fallback so the workflow never fails silently
        print("Warning: all LLM back-ends failed, using built-in fallback.", file=sys.stderr)
        truncated_title = pr_title[:100]
        post = (
            f"🚀 WebJET CMS sa neustále zlepšuje! "
            f"Nové vylepšenie: {truncated_title}. "
            f"Čo si myslíte? #WebJET #CMS #update"
        )

    print(f"\nGenerated post ({len(post)} chars):\n{post}\n")

    # Collect screenshots referenced in the PR
    print("Searching for screenshots...")
    screenshots = find_screenshots(pr_body, pr_files)
    print(f"Found {len(screenshots)} screenshot reference(s)")

    downloaded = download_screenshots(screenshots)
    print(f"Downloaded/copied {len(downloaded)} screenshot(s)")

    # Write all output files
    write_outputs(post, pr_title, pr_number, pr_url, downloaded, model_used=model_used, token_info=token_info)
    print("\n::notice::Social media post generation complete.")


if __name__ == "__main__":
    main()
