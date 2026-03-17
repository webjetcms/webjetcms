# Responsible Vulnerability Reporting

We take the security of WebJET CMS very seriously. If you have discovered a security vulnerability, we ask that you report it responsibly and allow us to fix it before it is made public.

**Please do not report security vulnerabilities via public GitHub issues.**

## How to report a vulnerability

You can report security vulnerabilities via:

- form on our website [Responsible Vulnerability Reporting](https://www.interway.sk/sluzby-riesenia/zodpovedne-oznamovanie-zranitelnosti/)
- by email to the address `zranitelnosti@interway.sk`, you can use the PGP key `38F5F695642A7A1C73AF6C7E90DC0AF8975E50AD`
- via [GitHub Report a vulnerability](https://github.com/webjetcms/webjetcms/security)

## What to include in the report

When reporting a vulnerability, please include:

- Vulnerability type (e.g. SQL injection, XSS, authentication bypass)
- Paths to files related to the vulnerability
- Location of the affected source code (tag/branch/commit or direct URL)
- Steps to reproduce the problem
- `Proof-of-concept` or `exploit` code (if possible)
- The impact of the vulnerability and how an attacker could exploit it

## What to expect

- We will confirm receipt of your report.
- We will check and confirm the problem.
- We will fix the vulnerability as soon as possible depending on its complexity
- We will notify you when the vulnerability is fixed

## Supported versions

We provide security updates for the latest stable release of WebJET CMS. We recommend always using the latest version.

## Safety documentation

For more information about security configuration and `hardening` WebJET CMS, see:

- [Security Tests](../pentests/README.md)
- [Library Vulnerability Check](../dependency-check/README.md)
- [WebJET Update](../update/README.md)
