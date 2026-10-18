---
name: wj-cke-stylescombo
description: "Develop, debug, style, build, and integrate WebJET CMS's CKEditor 4 wjstylescombo picker. Use for the Styles dropdown, active style summaries, CSS editor metadata, selection and undo behavior, or transferring CKEditor changes from libs-ckeditor4 into WebJET. Covers classic and Page Builder editors; not a general Page Builder block authoring skill."
---

# WebJET CKEditor style picker

Maintain the production `wjstylescombo` plugin and its WebJET integration. The accepted default is a wider dropdown that stays open after applying a style. A sidebar was a prototype alternative, not the selected production design. Follow later user requests when changing the design.

## Find the two working copies

- **CMS root (`<cms>`, `cms_root`):** the active WebJET checkout containing `src/main/webapp/admin/skins/webjet8/ckeditor/build.sh`. Resolve it with `git rev-parse --show-toplevel` from within that checkout; its directory name and location are not fixed.
- **CKEditor source (`<cke>`, `cke_root`):** normally `../libs-ckeditor4` relative to the CMS root. The user may call it `libs-ckeditor`; the build script currently expects **`libs-ckeditor4`**. Confirm the path used by the CMS build script before editing.
- Paths without a root prefix are relative to the CMS root unless their context names another directory. Skill reference links are relative to this skill directory.
- Read applicable repository instructions and inspect changes in **both** repositories. They have separate Git histories. Never create commits; leave changes for the user to review and commit.

## Source authority and scope

Edit `<cke>/plugins/wjstylescombo/plugin.js`, `styles.css`, and `lang/{en,sk,cs}.js`. Do not implement fixes directly in the CMS `ckeditor/dist` directory: it is rebuilt and replaced. Keep the original `stylescombo` plugin available for compatibility.

Preserve ES5 **syntax** in CKEditor plugin sources for the legacy CKBuilder compiler. Modern runtime APIs already used by the plugin are a separate browser compatibility question. CMS administration JS has its own Rspack build.

Use current source as the authority when it differs from older plans or prose. In particular, as verified on 2026-10-16, `config.jsp` replaces `Styles` with `WJStyles` only; it no longer automatically inserts `Format`. Do not reintroduce the earlier behavior incidentally. Semantic HTML formatting remains separate from CSS appearance; inspect the active toolbar configuration when changing it.

## Route the work

- For behavior, UI, selection, metadata, or integration changes, read [architecture.md](references/architecture.md). It maps files and describes the selection model, class rules, external ancestor undo, focus handling, and design constraints.
- Before building, transferring, testing, or refreshing the running application, read [build-and-validation.md](references/build-and-validation.md). The build working directory is significant, and the transfer can overwrite unrelated distribution files.
- For client catalog changes, update the template's **source SCSS** and compile its CSS; the plugin must not contain Orange-specific class names or category mappings.
- If adding or editing CodeceptJS scenarios, follow the current CMS `.agents/skills/wj-codeceptjs/SKILL.md` when available. Existing focused tests and commands are listed in the build reference.

## Working contract

Keep these behaviors unless the user explicitly changes them: readable active labels and removable chips; persistent dropdown after apply; independent category and exclusive-group semantics; preserved unknown/structural classes; explicit selection of ancestors; one undo step per style operation; equivalent behavior in classic and Page Builder editing.

For a plugin change, finish the source edit, build from the CMS directory, inspect generated changes, and verify the loaded result. A successful source edit alone is not an integrated change. Select checks appropriate to the change; repeat them after new fixes, not merely to accumulate runs.

When handing off, report which repositories and client template files changed, which builds/tests actually ran, and any remaining limitation. Client template files may be Git-ignored by the CMS repository. Do not claim they will be included in a CMS commit. Preserve existing page content during verification; a UI test does not require saving a real article.
