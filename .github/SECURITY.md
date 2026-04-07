# Security Policy

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security vulnerability in WebJET CMS, please report it responsibly.

**Please do not report security vulnerabilities through public GitHub issues.**

### How to Report

You can report security vulnerabilities via:

- form on our website [Responsible Vulnerability Reporting](https://www.interway.sk/sluzby-riesenia/zodpovedne-oznamovanie-zranitelnosti/)
- by email to the address `zranitelnosti@interway.sk`, you can use the PGP key `38F5F695642A7A1C73AF6C7E90DC0AF8975E50AD`
- via [GitHub Report a vulnerability](https://github.com/webjetcms/webjetcms/security)

### What to Include

When reporting a vulnerability, please include:

- Type of vulnerability (e.g. SQL injection, XSS, authentication bypass)
- Full paths of source file(s) related to the vulnerability
- Location of the affected source code (tag/branch/commit or direct URL)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the vulnerability and how an attacker could exploit it

### What to Expect

- We will acknowledge receipt of your report
- We will investigate and work to confirm the issue
- We will release a fix as soon as possible depending on complexity
- We will notify you when the vulnerability has been fixed

### Supported Versions

We provide security updates for the latest stable release of WebJET CMS. We recommend always running the latest version.

### Security Documentation

For more information about security configuration and hardening of WebJET CMS, see:

- [Security documentation](../docs/en/sysadmin/responsible-disclosure/README)
- [Penetration testing guide](../docs/en/sysadmin/pentests/README)
- [Vulnerability scanning of libraries](../docs/en/sysadmin/dependency-check/README)
