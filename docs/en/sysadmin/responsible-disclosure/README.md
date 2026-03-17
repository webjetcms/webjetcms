# Responsible Vulnerability Disclosure

We take the security of WebJET CMS seriously. If you have discovered a security vulnerability, please report it responsibly to allow us to fix it before it is publicly disclosed.

**Please do not report security vulnerabilities through public GitHub issues.**

## How to Report a Vulnerability

Report security vulnerabilities using our responsible disclosure form:

**[Responsible Vulnerability Disclosure](https://www.interway.sk/sluzby-riesenia/zodpovedne-oznamovanie-zranitelnosti/)**

## What to Include

When reporting a vulnerability, please include:

- Type of vulnerability (e.g. SQL injection, XSS, authentication bypass)
- Full paths of source file(s) related to the vulnerability
- Location of the affected source code (tag/branch/commit or direct URL)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the vulnerability and how an attacker could exploit it

## What to Expect

- We will acknowledge receipt of your report
- We will investigate and work to confirm the issue
- We will release a fix as soon as possible depending on complexity
- We will notify you when the vulnerability has been fixed

## GitHub Setup

GitHub allows you to configure a repository security policy. The `.github/SECURITY.md` file is displayed on the **Security** tab of the repository and GitHub automatically links to this file when issues are reported. We recommend:

1. Verifying that the `.github/SECURITY.md` file is present in the repository.
2. In GitHub, go to **Settings → Code security and analysis** and enable **Private vulnerability reporting** to allow private reporting of vulnerabilities directly through GitHub.

## Supported Versions

We provide security updates for the latest stable release of WebJET CMS. We recommend always running the latest version.

## Security Documentation

For more information about security configuration and hardening of WebJET CMS, see:

- [Safety tests](/sysadmin/pentests/README.md)
- [Vulnerability scanning of libraries](/sysadmin/dependency-check/README.md)
- [WebJET update](/sysadmin/update/README.md)
