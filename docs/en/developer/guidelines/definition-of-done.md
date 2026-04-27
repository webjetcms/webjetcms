# Definition of Done

This document is in the writing stage, it is not finished, for now it is more of a note.

## Code review

- comment out all debugging ```console.log``` from JavaScript files
- consider whether the added variable/```property``` has a meaningful name (it is clearly possible to determine what it represents)
- all texts must be loaded from translation ```properties``` files, no text (displayed in the UI) must be hardcoded in the code

## Tests

The code needs to be covered by automated tests. The tests must be repeatable and verified by multiple testing. Attention must be paid to asynchronous execution and correct waitFor calls.

It is necessary to manually perform tests of exporting and importing data from the data table:

- Data export to Excel format.
- Import data as new records - records will be duplicated.
- Import data with update by name column (not by ID). Before importing, edit some data and after importing with matching by name (not by ID), verify that it is set to the original values.
- Switch your domain and verify that the data is displayed in a different domain. Most applications have separate data by domain. Import Excel as new records and verify that the data is in both the original and changed domains. Edit some data in Excel and import by name and verify that the data change occurred only in the new domain and not in the original one from which Excel was (this verifies correct matching based on name with the current domain selection).

## Documentation

**Adding documentation is best done** during code review in a **merge request**. Therefore, always prepare ```merge request``` and then go through all changed files in the ```Changes``` sheet. **For each change, consider** whether it is understandable from the code and whether it does not modify the current behavior or add features to the existing code. Note:

For each change, think about whether the reason for the change is clear to you and whether it will be clear to you in a year, and especially whether it will be clear to another programmer. Incomprehensible changes must be commented directly in the source code - documentation and comments in the source code go hand in hand and may seem duplicated at first glance. But that doesn't matter, write the documentation more verbally, the comments more technically.

!>**Note:** Not all developers may have access to the source code, the documentation is also available to clients who are interested in programming features for WebJET. The documentation must be comprehensive enough to allow the component to be used without the source code.

I recommend that you have a **search window** open in the ```docs``` folder, and when you see a change in a variable/configuration/object in the ```Changes``` list, look for it in the documentation (./docs folder). If you find it, add the text with the change, if you don't find it, add the entire description of the change.

When writing documentation, pay attention to the [terms/expressions](terms.md) used.

### New functionality

Create an MD file in the appropriate section (or extend an existing one) and describe how the functionality works, what the thought processes are. The document should have the following structure:

- what is the purpose of the new component - describe in one/two paragraphs what the component is for
- configuration options/settings - if the component has configuration options/settings, provide a list of configuration variables with a description
- usage examples - practical examples of use, ideally with a link to the file where the component is actually used
- implementation details - implementation details, describe the main methods/functions, what their meaning is, provide links to source files

### Changing existing functionality

- configuration is being expanded (e.g. new options added in JS framework) - supplement existing documentation

### Changelog

After writing the documentation, ```pushnite``` and go through the changelog again in ```gitlabe```. Write the main points in the file [CHANGELOG.md](../../CHANGELOG.md) with links to more detailed documentation. Maintain the following structure in the changelog file:

- heading YEAR.WEEK
- module title + basic description, enter the ticket number after the title
- general - enter general/minor changes not directly related to changes in the module in the section, always include the ticket number at the beginning
- documentation - list changes/new parts in the documentation with links

After completing the changelog, check the [ROADMAP.md](../../ROADMAP.md) file, if you have implemented any part of the roadmap, mark it as resolved. Also add the ticket number at the end of the line in the change.
