# Definition of Done

This document is being written, it is not finished, it is more like notes for now.

## Code review

- for comment all debug-ing `console.log` from JavaScript files
- think about whether the added variable/`property` it has a meaningful name (it is possible to clearly identify what it represents)
- all texts must be retrieved from the translation `properties` files, no text in the code (displayed in the UI) must be hardcoded

## Tests

The code needs to be covered by automated tests. Tests need to be repeatable, they need to be verified by multiple testing. Care must be taken to ensure asynchronous execution and correct waitFor calls.

Manually you need to perform tests of export and import of datatable data:
- Export data to Excel format.
- Import data as new records - duplication of records will occur.
- Import data with update by name column (not by ID). Before importing, edit some data and after importing with matching based on name (not ID), verify that it has been set to the original values.
- Switch your domain and verify the data is displayed in a different domain. Most applications have separate data by domain. Import Excel as new records and verify that the data is in both the original and changed domain. Edit some data in Excel and perform an import by name and verify that the data change occurred only in the new domain and not also in the original domain that Excel was from (this verifies correct matching based on name with the selection of the current domain).

## Documentation

**Completing the documentation is well done** during code review in **merge requeste**. Therefore, always prepare `merge request` and subsequently in the letter `Changes` browse all changed files. **With every change, think about**whether it is understandable from the code and whether it modifies actual behaviour or adds features to existing code. Notice:
With each change, think about whether the reason for the change is clear to you and whether it will be clear to you in a year's time, and especially whether it will be clear to another programmer. Unintelligible changes should be commented out directly in the source code - documentation and source code comments go hand in hand and can be duplicative at first glance. But it doesn't matter, write the documentation more verbally, the comments more technically.

**NOTICE:** not all developers need to have access to the source code, the documentation is also available for clients who are interested in programming features for WebJET. The documentation must be comprehensive enough to allow the component to be used without the source code.
I recommend you have **open search window** v `docs` folder, and when you see in the `Changes` a change in a variable/configuration/object, you can find it in the documentation (folder ./docs). If you find it, you add the change to the text, if you don't find it, you add the full description of the change.

When writing documentation, pay attention to the used [terms/expressions](terms.md).

### New functionality

Create an MD file in an appropriate section (or extend an existing one) and describe how the functionality works, what the thought processes are. The document should be structured as follows:
- what is the purpose of the new component - describe in one/two paragraphs what the component is for
- configuration options/settings - if the component has configuration options/settings, list the configuration variables with a description
- examples of use - practical examples of use, ideally with a link to a file where the component is actually used
- implementation details - implementation details, describe the main methods/functions, what is their meaning, provide links to source files

### Change existing functionality

- configuration is extended (e.g. new option added in JS framework) - add to existing documentation

### Changelog

After the documentation has been written `pushnite` and go through the list of changes in `gitlabe`. Write the main points in a file [CHANGELOG.md](../../CHANGELOG.md) with links to more detailed documentation. Keep the following structure in the changelog file:
- heading YEAR.WEEK
- Module title + basic description, after the title please enter the ticket number
- general - in the section indicate general/minor changes not directly related to the change in the module, always indicate the ticket number at the beginning
- documentation - please list changes/new sections in the documentation with references
After adding the changelog, check the file [ROADMAP.md](../../ROADMAP.md)if you have implemented any part of the roadmap, mark it as solved. Also add the ticket number to the end of the line in the change.
