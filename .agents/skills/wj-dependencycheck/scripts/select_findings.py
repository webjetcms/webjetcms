#!/usr/bin/env python3
"""Select active Dependency Check summary rows by Highest Severity."""

import argparse
from collections import Counter
from html import unescape
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import sys


LEVELS = ("INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL")


class SummaryParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.table_depth = 0
        self.seen = False
        self.complete = False
        self.rows = []
        self.row = None
        self.cell = None

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "table":
            if self.table_depth:
                self.table_depth += 1
            elif attrs.get("id") == "summaryTable":
                self.seen = True
                self.table_depth = 1
        if self.table_depth != 1:
            return
        if tag == "tr":
            self.row = {"classes": attrs.get("class", "").split(), "cells": []}
        elif tag in ("th", "td") and self.row is not None:
            self.cell = {"kind": tag, "parts": [], "links": []}
        elif tag == "a" and self.cell is not None and attrs.get("href"):
            self.cell["links"].append(attrs["href"])

    def handle_data(self, data):
        if self.table_depth == 1 and self.cell is not None:
            self.cell["parts"].append(data)

    def handle_endtag(self, tag):
        if tag == "table" and self.table_depth:
            self.table_depth -= 1
            if self.table_depth == 0:
                self.complete = True
            return
        if self.table_depth != 1:
            return
        if tag in ("th", "td") and self.cell is not None:
            self.cell["text"] = " ".join("".join(self.cell.pop("parts")).split())
            self.row["cells"].append(self.cell)
            self.cell = None
        elif tag == "tr" and self.row is not None:
            self.rows.append(self.row)
            self.row = None


def select_report(path, requested, at_least=False):
    if at_least and (len(requested) != 1 or requested[0] not in LEVELS):
        raise ValueError("--at-least requires one ranked severity (INFO through CRITICAL)")
    selected = set(LEVELS[LEVELS.index(requested[0]):]) if at_least else set(requested)
    document = path.read_text(encoding="utf-8-sig")
    parser = SummaryParser()
    parser.feed(document)
    parser.close()
    if not parser.seen or not parser.complete:
        raise ValueError("Missing or incomplete summaryTable; inspect the report format")
    header_rows = [row for row in parser.rows if any(c["kind"] == "th" for c in row["cells"])]
    if len(header_rows) != 1:
        raise ValueError("Expected one summary header row; inspect the report format")
    headings = [c["text"].casefold() for c in header_rows[0]["cells"]]
    required = ("dependency", "package", "highest severity", "cve count")
    if any(name not in headings for name in required):
        raise ValueError("Unrecognized summary columns: " + ", ".join(headings))
    indexes = {name: headings.index(name) for name in required}
    counts = Counter()
    findings = []
    for row in parser.rows:
        cells = row["cells"]
        if any(c["kind"] == "th" for c in cells) or "suppressed" in row["classes"]:
            continue
        if len(cells) != len(headings):
            raise ValueError("Unexpected summary row width; inspect the report format")
        severity = cells[indexes["highest severity"]]["text"].upper()
        if not severity:
            continue
        if severity not in (*LEVELS, "UNKNOWN"):
            raise ValueError("Unrecognized Highest Severity: " + severity)
        counts[severity] += 1
        if severity not in selected:
            continue
        dependency = cells[indexes["dependency"]]
        anchors = [link for link in dependency["links"] if link.startswith("#") and len(link) > 1]
        if not anchors:
            raise ValueError("Selected dependency has no detail anchor: " + dependency["text"])
        package = cells[indexes["package"]]
        findings.append({
            "dependency": dependency["text"],
            "package": package["text"],
            "package_links": package["links"],
            "highest_severity": severity,
            "reported_cve_count": cells[indexes["cve count"]]["text"],
            "detail_anchor": anchors[0],
        })
    generated = re.search(r"Report Generated On\s*</i>\s*:\s*([^<]+)", document, re.I)
    return {
        "report": str(path),
        "generated_on": unescape(generated.group(1)).strip() if generated else None,
        "matching": "at-least" if at_least else "exact",
        "selected_severities": sorted(selected),
        "active_dependency_counts_by_highest_severity": dict(sorted(counts.items())),
        "matched_dependency_rows": len(findings),
        "dependencies": findings,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, default=Path("build/reports/dependency-check-report.html"))
    parser.add_argument("--severity", nargs="+", required=True, type=str.upper, choices=(*LEVELS, "UNKNOWN"))
    parser.add_argument("--at-least", action="store_true", help="Select this severity and higher")
    args = parser.parse_args()
    try:
        result = select_report(args.report, args.severity, args.at_least)
    except (OSError, ValueError, UnicodeError) as error:
        parser.exit(2, f"Cannot select findings: {error}\n")
    json.dump(result, sys.stdout, ensure_ascii=False, indent=2)
    print()


if __name__ == "__main__":
    main()
