# JAR size analysis — 18 September 2026

Read-only analysis of the six existing JAR files in `build/updatezip/artifacts/`, generated at approximately 11:05 local time. ZIP archives, POM files, signatures and checksums are excluded. No application source, images or build configuration were changed; the build was not rerun.

## Measurement

MB means 1,000,000 bytes; MiB means 1,048,576 bytes. JAR totals below are actual file sizes. Directory and file contributions use ZIP central-directory `compress_size`, i.e. their compressed contribution inside each JAR, not source-directory disk usage. Archive headers and directory entries account for the remaining overhead. Parent and child directory totals overlap and must not be added together. Deletion savings are compressed payload estimates; actual rebuilt JARs also change their ZIP overhead. Optimization savings have not been measured.

Total JAR size: **97.723 MB (93.195 MiB)**. Image payload: **55.297 MB (56.6% of all JAR bytes)**. Reaching 80 decimal MB would require at least **17.723 MB** less data, before allowing for other publishing files.

| JAR | File size MB | Compressed payload MB | Uncompressed content MB | Files |
| --- | --- | --- | --- | --- |
| webjetcms-2026.0-jakarta-SNAPSHOT-components.jar | 56.509 | 55.799 | 73.387 | 3235 |
| webjetcms-2026.0-jakarta-SNAPSHOT-admin.jar | 19.320 | 18.282 | 45.925 | 4113 |
| webjetcms-2026.0-jakarta-SNAPSHOT.jar | 8.606 | 8.070 | 21.780 | 2683 |
| webjetcms-2026.0-jakarta-SNAPSHOT-javadoc.jar | 8.119 | 7.665 | 59.975 | 2275 |
| webjetcms-2026.0-jakarta-SNAPSHOT-sources.jar | 3.592 | 3.238 | 13.023 | 1703 |
| webjetcms-2026.0-jakarta-SNAPSHOT-libs.jar | 1.576 | 1.388 | 3.388 | 956 |

## Largest opportunities

1. **AppStore screenshots: 243 files, 27.234 MB.** Of these, 225 `.jpg` files are actually PNG images (verified by signature), contributing 26.300 MB. These files follow the preview-gallery convention consumed by active AppStore code; individual screenshots may still be obsolete. Converting/re-encoding them, or deciding that fewer previews are sufficient, is the largest focused opportunity. Keep text legibility in mind; conversion alone does not guarantee a specific saving. Merely renaming the extension saves nothing.
2. **Slider transition previews: 19 GIF files, 7.225 MB.** These are 250 × 150 pixel animated demonstrations displayed in the application editor. The largest, `kenburns.gif`, contains 180 frames and contributes 1.171 MB. They are separate from the JavaScript that performs the actual transitions on published pages. Removing previews requires changing the editor that requests them; alternatively reduce frame count/palette/duration, or replace their format together with the editor references.
3. **PageBuilder/GridEditor block previews: 104 images, 4.534 MB, within a 4.574 MB data directory.** The older-looking `obsahove_bloky/` subtree contributes 3.551 MB, of which 3.519 MB is images. It is still discoverable through directory enumeration, so zero literal references to its name does not establish that it is unused. Review the supplied block catalog and thumbnail resolution. Deleting a block also removes it from the default library.
4. **HTMLBox placeholders: seven JPEG photographs, 4.512 MB.** Several are near 1920 pixels in one or both dimensions. They are referenced by AI PageBuilder instructions in the SQL updates and documentation, so treat them as usable content assets. Recompressing while preserving URLs is a better initial candidate than deletion. Full `htmlbox/objects/` is 5.207 MB and full `components/htmlbox/` is 5.650 MB; these amounts overlap.
5. **Old administration backgrounds: six `webjet_bg_*` JPEGs, 1.902 MB.** Both original and `-optimized` variants are packaged. No references to this filename prefix were found in the scanned source or runtime JAR text/classes. This is a removal candidate, subject to custom installations and database-stored URLs. The enclosing `.../global/img/wj/` directory is 2.810 MB; other assets there are not automatically unused.

## Evidence and regeneration

- AppStore gallery enumeration and locale fallback: [AppBean.java](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/java/sk/iway/iwcm/editor/appstore/AppBean.java:71). It recognizes JPG/GIF/PNG; switching to WebP would also require changing detection and any explicit paths.
- Screenshot generator names outputs `.jpg`: [apps-editor-component.js](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/test/webapp/screenshots/generator/apps-editor-component.js:72). The wrapper calls CodeceptJS screenshot helpers: [document.js](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/test/webapp/pages/document.js:50). The installed [Playwright helper](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/test/webapp/node_modules/codeceptjs/lib/helper/Playwright.js:2304) explicitly uses PNG, including full-page captures at line 2319. Fix the generation path as part of any later conversion so regeneration does not restore PNG content under JPG names.
- Slider GIF consumer: [editor-component.html](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/apps/slider/admin/editor-component.html:74). The Slider and SlitSlider remain registered applications: [SliderApp.java](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/java/sk/iway/iwcm/components/appslider/SliderApp.java:25) and [SlitSliderApp.java](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/java/sk/iway/iwcm/components/appslitslider/SlitSliderApp.java:25).
- GridEditor enumerates directories dynamically: [GridEditorController.java](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/java/sk/iway/iwcm/grideditor/controller/GridEditorController.java:87), default library at line 154, preview image selection at line 655.
- Placeholder photographs appear in [AI instructions](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/docs/sk/redactor/ai/instructions/README.md:222) and [SQL AI prompt definitions](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/WEB-INF/sql/autoupdate-webjet9.xml:2366).

## Libraries and other data worth reviewing

| Candidate | Compressed MB | Assessment |
| --- | --- | --- |
| [components/_common/amcharts/](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/components/_common/amcharts) | 2.265 | Bundled amCharts 3.21.2; current source imports amCharts 5. No runtime reference to the old path found except updater cleanup. Strong legacy candidate, not proof against customer-defined use. |
| [components/grideditor/support/Open_Sans/](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/components/grideditor/support/Open_Sans) | 1.141 | Ten TTF font files plus license. No Open_Sans reference found in scanned source or runtime JAR contents. Check template/database usage before removing. |
| [admin/v9/dist/fonts/](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/admin/v9/dist/fonts) | 1.757 | Only four Tabler TTF/WOFF fallbacks, not the whole directory. CSS also references WOFF2. Dropping fallback formats requires an explicit browser-support decision and source/build CSS changes. |
| [admin/skins/webjet8/assets/global/plugins/datatables/](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/admin/skins/webjet8/assets/global/plugins/datatables) | 1.532 | datatables.js alone contributes 1.501 MB. Still explicitly loaded by CombineTag for old administration; not safe to delete independently. |
| [components/grideditor/support/font-awesome-4.7.0/](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/components/grideditor/support/font-awesome-4.7.0) | 0.527 | Still conditionally loaded for GridEditor and news inline editing. |
| [admin/v9/npm_packages/webjetdatatables/](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/admin/v9/npm_packages/webjetdatatables) | 0.231 | Includes an 0.081 MB .tgz package. Small packaging-review candidate; not a major source of savings. |
| [admin/skins/webjet6/](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/admin/skins/webjet6) | 0.248 | Only 0.248 MB. Old naming alone does not establish removability. |

Current charts: [amcharts.js](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/admin/v9/src/js/libs/chart/amcharts.js:1). Old DataTables consumer: [CombineTag.java](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/java/sk/iway/iwcm/tags/CombineTag.java:48). Font Awesome consumer: [inline_script.jsp](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/src/main/webapp/admin/inline/inline_script.jsp:70). The [roadmap](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/docs/sk/ROADMAP.md:76) lists separation of old version-8 JSP/classes/libraries as unfinished work. Moving these into another artifact under the same organization would not itself reduce the aggregate bytes if that artifact is published with every release.

## Decision scenarios — not measured optimization results

- All AppStore screenshot files represent 27.234 MB of current payload. Removing that entire preview catalog would reduce the existing JAR baseline to approximately 70.489 MB before archive-overhead changes, but would remove application previews.
- The 109 Czech screenshot variants represent 13.315 MB. AppStore directory-based discovery has fallback to the unsuffixed image. Confirm every explicit gallery definition and product expectations before considering removal of localized previews.
- Removing those Czech screenshots plus all 19 transition GIF previews would remove 20.540 MB of current payload and leave approximately 77.182 MB before archive-overhead changes. This is a product tradeoff and would require adapting the slider editor; it is not a recommendation to remove used assets blindly.
- Optimizing screenshots, animated previews and the seven placeholder photographs targets 38.971 MB of existing image payload without requiring removal of entire CMS modules. Actual achievable savings and quality require a separate encoding comparison.

## Duplicate content

SHA-256 comparison found 383 groups of byte-identical files, with a theoretical 2.122 MB payload saving if only one copy of each remained. This includes cross-module/cross-JAR copies and cannot be achieved by deleting arbitrary paths: references and class-loading behavior must be preserved. It is lower priority than screenshot encoding. The largest example is `bg-body.jpg`, duplicated under `global/img/` and `global/img/wj/` (0.239 MB extra). The GridEditor block catalogs also contain duplicated preview images.

## Scope limits

This is static analysis of the current build and checked-out sources. Customer templates, pages and configuration held in databases are not visible here. A missing literal path reference is a candidate for review, not proof of non-use. No whole feature module was confirmed safe to remove. Runtime tests and visual encoding comparisons were not run because this task only requests analysis.

## Detailed inventories

- [Directory inventory](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/build/reports/jar-size-audit/directories.md): all compressed directories at or above 0.1 MB, including parents/children; do not sum overlapping rows.
- [Image inventory](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/build/reports/jar-size-audit/images.md): the 100 largest images, actual formats, dimensions and GIF frame counts; all misnamed PNG screenshots.
- [AppStore screenshots](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/build/reports/jar-size-audit/screenshots.md): per-directory totals and all 243 files, including language variants.
- [Other data and duplicates](/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms/build/reports/jar-size-audit/other-data.md): largest non-image files and exact duplicate examples.
