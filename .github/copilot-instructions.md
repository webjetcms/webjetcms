# GitHub Copilot Instructions

## General rules

- When performing a code review, focus on readability and avoid nested ternary operators.
- Do not suggest change from if(statement == false) to if(!statement).
- Do not suggest change from if(statement == true) to if(statement).
- Do not suggest change Avoid explicit boolean comparison.
- ALWAYS write comments in English.
- AGENTS.md file is located in .github/agents/AGENTS.md file.
- ALWAYS follow the instructions in AGENTS.md.

## CHANGELOG

- For every PR there should be description in /docs/sk/CHANGELOG-2026.md file in Slovak language. Find the appropriate section in ## 2026-SNAPSHOT part and use this template for your change:
  - `<short description of the change> (#<pull request number>)`
- Check file /docs/sk/ROADMAP.md for planned changes. If you find your change there, check item as done and add `(#<pull request number>)` to the end of the line.

## Tests

- jUnit tests are in folder src/test/java.
- In jUnit tests of JPA entities always use Repository classes to save and load entities, do not think everything will work without real saving and loading to database.