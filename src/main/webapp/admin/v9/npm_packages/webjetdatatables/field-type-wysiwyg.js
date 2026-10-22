export function typeWysiwyg() {

    var FOCUS_READY_TIMEOUT_MS = 10000;
    var FOCUS_RETRY_DELAY_MS = 50;

    function getThisField(conf) { //NOSONAR
        return conf.EDITOR.field(conf.data);
    }

    /**
     * Rejects failed reads, including the literal "undefined" previously written as page content.
     * An empty string is valid because users may intentionally clear the page.
     * @param {unknown} html Value returned by the editor, a callback or a multi-value field.
     * @returns {boolean} True when the value cannot be used as page HTML.
     */
    function isInvalidHtml(html) {
        return typeof html !== 'string' || html.trim() === 'undefined';
    }

    /**
     * Removes editor attributes without a DOM round trip that would rewrite source HTML.
     * Comments, other attribute values and raw-text elements must remain unchanged.
     * @param {string} html Source HTML.
     * @returns {string} HTML without contenteditable attributes.
     */
    function removeContentEditable(html) {
        if (typeof html !== 'string' || html.toLowerCase().indexOf('contenteditable') === -1) return html;

        const tags = /<!--[\s\S]*?(?:-->|$)|<!\[CDATA\[[\s\S]*?(?:\]\]>|$)|<![^>]*>|<\/?([a-z][a-z0-9:-]*)(?=[\s/>])(?:"[^"]*"|'[^']*'|[^'">])*>/gi;
        const attributes = /\s+([^\s=/>]+)(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+))?/g;
        let result = '';
        let offset = 0;
        let match;
        while ((match = tags.exec(html)) !== null) {
            const tag = match[0];
            if (!match[1] || tag.startsWith('</')) continue;
            const cleaned = tag.replace(attributes, (attribute, name) => name.toLowerCase() === 'contenteditable' ? '' : attribute);
            if (cleaned !== tag) {
                result += html.slice(offset, match.index) + cleaned;
                offset = tags.lastIndex;
            }
            // HTML-looking strings inside scripts, styles and text-only elements are not markup.
            if (/^(script|style|textarea|title|xmp|iframe|noembed|noframes|noscript)$/i.test(match[1])) {
                const closingTag = new RegExp('</' + match[1] + '\\s*>', 'gi');
                closingTag.lastIndex = tags.lastIndex;
                const closing = closingTag.exec(html);
                tags.lastIndex = closing ? closingTag.lastIndex : html.length;
            }
        }
        return result + html.slice(offset);
    }

    /**
     * Shows a read error and stops later handlers from sending a save or preview request.
     * Returning false cancels DataTables submission; stopping propagation also blocks preview handlers.
     * @param {Object} conf DataTables Editor field configuration.
     * @param {jQuery.Event} event The initSubmit or preSubmit event being cancelled.
     * @returns {boolean} Always false to cancel submission.
     */
    function cancelInvalidSubmit(conf, event) {
        const message = WJ.translate('editor.content.read_error.js');
        getThisField(conf).error(message);
        conf.EDITOR.error(message);
        event.stopImmediatePropagation();
        return false;
    }

    /**
     * Captures the initial HTML for dirty tracking once the current editor is ready.
     * Ignores obsolete reset requests and the previous iframe document, which can remain accessible
     * during navigation. A failed read leaves the baseline undefined so it can be retried.
     * @param {Object} conf DataTables Editor field configuration.
     * @param {Object} request Identity of the resetDirty request that scheduled this capture.
     */
    function captureDirtySnapshot(conf, request) {
        if (!conf.editorOpen || conf.dirtyResetRequest !== request || conf.dirtyDataOriginal !== undefined) return;
        if (conf.wjeditor == null) return;

        if (conf.wjeditor.editingMode === 'pageBuilder') {
            const iframe = $("#"+conf._id+"-pageBuilderIframe");
            if (iframe[0]?.contentWindow?.pageBuilderReady !== true ||
                iframe[0].contentDocument === iframe.data('pageBuilderPreviousDocument')) return;
        }

        conf.dirtyDataOriginal = getThisField(conf).get(conf);
    }

    /**
     * Serializes CKEditor operations and skips queued work from a closed editor session.
     * wjeditorReadyPromise tracks all queued writes; wjeditorModeReadyPromise tracks template setup.
     * Content writes require successful template setup, but a new setup may recover after a failure.
     *
     * @param {Object} conf DataTables Editor field configuration.
     * @param {Function} operation Starts the next asynchronous CKEditor operation.
     * @param {boolean} [isModeChange=false] Allows a new template to recover from a failed mode change.
     * @returns {void}
     */
    function queueEditorOperation(conf, operation, isModeChange = false) {
        const session = conf.editorSession;
        const modeReady = conf.wjeditorModeReadyPromise;
        const run = async () => {
            if (!conf.editorOpen || conf.editorSession !== session) return;
            // Content requires its template to succeed, even when recovering from a previous error.
            if (!isModeChange) await modeReady;
            if (conf.editorOpen && conf.editorSession === session) return operation();
        };
        // A failed write must not poison subsequent requests or a newly opened editor.
        conf.wjeditorReadyPromise = conf.wjeditorReadyPromise.then(run, run);
        if (isModeChange) conf.wjeditorModeReadyPromise = conf.wjeditorReadyPromise;
        // Report the failure without changing the rejected readiness promise used by focus and dirty tracking.
        conf.wjeditorReadyPromise.catch(error => console.error("Error updating WYSIWYG editor:", error));
    }

    function wait(delay) {
        return new Promise(resolve => {
            setTimeout(resolve, delay);
        });
    }

    return {
        create: function ( conf ) {

            //vlozenie ckeditor.js je opozdene kvoli performance CPU v prehliadaci
            setTimeout(()=> {
                //nechceme zatazit CPU hned na zaciatku, takze ckeditor.js nacitame cez timenout mierne neskor
                var head = document.getElementsByTagName('head')[0];
                var script = document.createElement('script');
                script.type = 'text/javascript';
                script.src = "/admin/skins/webjet8/ckeditor/dist/ckeditor.js";

                head.appendChild(script);
            }, 2000);

            //console.log("Creating WYSIWYG field, conf=", conf, "datatable=", conf.datatable, "editor=", this);
            let EDITOR = this;
            conf.EDITOR = EDITOR;
            var id = $.fn.dataTable.Editor.safeId( conf.id );
            conf._id = id;

            // Field HTML structure
            let htmlCode =
                `<div class="wysiwyg" id="trEditor">
                    <div class="wysiwyg_textarea"><textarea id="${id}" style="display: none;"></textarea>
                    <div id="editorFormDiv">
                        <form name="editorForm">
                            <textarea name="data" style="border: 0px"></textarea>
                            <input type="hidden" name="docId"/>
                            <input type="hidden" name="groupId"/>
                            <input type="hidden" name="virtualPath"/>
                            <input type="hidden" name="title"/>
                        </form>
                    </div>
                </div>
                <div class="wysiwyg wysiwyg-pageBuilder" id="${id}-trPageBuilder">
                    <iframe id="${id}-pageBuilderIframe" class="md-pageBuilder" src="about:blank"></iframe>
                </div>
                <div class="exit-inline-editor" id="${id}-editorTypeSelector">
                    <label for="${id}-editorTypeSelect-select">${WJ.translate("editor.type_select.label.js")}</label><br/>
                    <select id="${id}-editorTypeSelect-select" onchange="window.switchEditorType(this)" data-hide-disabled="true">
                        <option value="">${WJ.translate("editor.type_select.standard.js")}</option>
                        <option value="html">${WJ.translate("editor.type_select.html.js")}</option>
                        <option value="pageBuilder">${WJ.translate("editor.type_select.page_builder.js")}</option>
                    </select>
                    <br/>
                    ${WJ.translate("pagebuilder.modal.tab.size")}:
                    <a href="javascript:pbSetWindowSize('phone')" title="${WJ.translate('pagebuilder.modal.visibility.sm')}" data-toggle="tooltip"><span class="ti ti-device-mobile"></span></a>
                    <a href="javascript:pbSetWindowSize('tablet')" title="${WJ.translate('pagebuilder.modal.visibility.md')}" data-toggle="tooltip"><span class="ti ti-device-tablet"></span></a>
                    <a href="javascript:pbSetWindowSize('desktop')" title="${WJ.translate('pagebuilder.modal.visibility.xl')}" data-toggle="tooltip"><span class="ti ti-device-desktop"></span></a>
                </div>
                `;
            let htmlCodeElement = $(htmlCode);

            conf._input = $(htmlCodeElement).find("textarea");

            //console.log("input=", conf._input);
            //nastav atributy
            if (typeof conf.attr != "undefined" && conf.attr != null) {
                $.each(conf.attr, function( key, value ) {
                    //console.log("Setting attr: key=", key, " value=", value);
                    $(conf._input).attr(key, value);
                });
            }

            conf.wjeditor = null;
            let resolveInitialEditorReady;
            conf.wjeditorReadyPromise = new Promise(resolve => {
                resolveInitialEditorReady = resolve;
            });
            EDITOR.on('initSubmit', function(event, action) {
                // Start fresh for every save or preview; never reuse HTML from an earlier attempt.
                conf.submitSnapshot = undefined;
                const field = getThisField(conf);
                if (action === 'remove' || !field.submittable()) return;
                // Multi-value reads bypass the WYSIWYG getter, so clear any previous read failure here.
                conf.invalidHtml = false;
                let values;
                if (field.isMultiValue()) {
                    // Distinct row values stay in Editor's multi-value storage; there is no shared HTML to snapshot.
                    values = Object.values(field.multiGet());
                } else {
                    // An empty snapshot asks the next get() to capture one successful, normalized HTML read.
                    // The type getter stores it before Editor applies getFormatter, avoiding double formatting.
                    conf.submitSnapshot = {};
                    values = [field.get()];
                }
                // Validate before Editor compares values or falls back to the original row data.
                if (values.some(isInvalidHtml)) return cancelInvalidSubmit(conf, event);
            });
            EDITOR.on('preSubmit', function(event, data, action) {
                // Editor has finished building the payload; discard the snapshot so later reads see live edits.
                conf.submitSnapshot = undefined;
                if (action !== 'remove' && getThisField(conf).submittable() && conf.invalidHtml) {
                    return cancelInvalidSubmit(conf, event);
                }
            });
            EDITOR.on('processing', function(event, processing) {
                // A cancelled or unchanged submission can skip preSubmit; release its snapshot here too.
                if (processing === false) conf.submitSnapshot = undefined;
            });
            EDITOR.on( 'open', function ( e, type ) {
                //console.log("DT WYSIWYG Editor OPEN, e=", e, "type=", type);

                conf.datatableEditingType = type;

                if ("main"!==type) {
                    //console.log("not editor, returning")
                    return;
                }

                conf.editorOpen = true;
                conf.editorSession = {};
                const json = EDITOR.currentJson;
                const data = conf._input.val();
                // The form is attached now. Finish the mode change before writing to its iframe.
                queueEditorOperation(conf, () => conf.wjeditor.setJson(json), true);
                queueEditorOperation(conf, () => conf.wjeditor.setData(data));

                //console.log("data field: ", EDITOR.field( 'data' ).val());
                if (conf.wjeditor==null) {
                    window.createDatatablesCkEditor().then(module => {
                        //allow to use module for Page Builder
                        window.datatablesCkEditorModule = module;
                        //console.log("Creating WYSIWYG editor, module=", module);

                        const options = {
                            datatable: EDITOR.TABLE,
                            fieldid: id,
                            constants: {
                                pixabayEnabled: window.pixabayEnabled,
                                editorImageAutoTitle: window.editorImageAutoTitle
                            },
                            lang: {
                                projectName: WJ.translate("default.project.name")
                            },
                            onReady: function() {
                                //console.log("Setting json onReady, json=", EDITOR.currentJson);
                                //instancia ckeditora, potrebuju to rozne pluginy a podobne, takze zatial takto kvoli spatnej kompatibilite
                                window.ckEditorInstance = conf.wjeditor.ckEditorInstance;

                                resolveInitialEditorReady();

                                //nastav otvorene docid do inputu
                                if (typeof window.jsTreeDocumentOpener != "undefined" && typeof EDITOR.currentJson != "undefined" && EDITOR.currentJson != null) window.jsTreeDocumentOpener.setInputValue(EDITOR.currentJson.docId);
                            }
                        }
                        if (typeof window.CKEDITOR == "undefined") {
                            //este nedobehol load ckeditora
                            setTimeout(() => {
                                if (typeof window.CKEDITOR != "undefined") conf.wjeditor = new module.DatatablesCkEditor(options);
                            }, 2100);
                        } else {
                            conf.wjeditor = new module.DatatablesCkEditor(options);
                        }

                        //zrus spodny padding na tab-pane
                        $("#"+id).parents(".tab-pane").css("padding-bottom", "0px");

                        WJ.initTooltip($('div.exit-inline-editor a[data-toggle*="tooltip"]'));

                        //console.log("editor constructed");
                    });
                    $("div.modal.DTED > div.modal-dialog").addClass("modal-xl");
                } else {
                    //console.log("Setting json, json=", EDITOR.currentJson);
                    //nastav otvorene docid do inputu
                    if (typeof window.jsTreeDocumentOpener != "undefined" && typeof EDITOR.currentJson != "undefined") window.jsTreeDocumentOpener.setInputValue(EDITOR.currentJson.docId);
                }
                getThisField(conf).resetDirty(conf);
            });

            window.addEventListener('WJ.PageBuilder.ready', function(event) {
                if (conf.wjeditor?.editingMode !== 'pageBuilder') return;
                const iframe = document.getElementById(id+"-pageBuilderIframe");
                if (event.detail.document !== iframe?.contentDocument) return;
                captureDirtySnapshot(conf, conf.dirtyResetRequest);
            });

            EDITOR.on( 'close', function ( e, type ) {
                //console.log("EDITOR.onClose");
                // Closing during an asynchronous submit must not leave this page's HTML cached for the next page.
                conf.submitSnapshot = undefined;
                conf.editorOpen = false;
                conf.editorSession = null;
                conf.dirtyResetRequest = null;
                conf.dirtyDataOriginal = undefined;
                const iframe = $("#"+id+"-pageBuilderIframe");
                iframe.data('pageBuilderPreviousDocument', iframe[0]?.contentDocument);
                iframe.attr("src", "about:blank");
            });

            if (typeof window.switchEditorType == "undefined") {
                /**
                 * Switches modes after pending editor writes finish, preserving the live content.
                 * If Page Builder cannot provide its HTML, a Page Builder-to-HTML switch may restore
                 * the preserved field value after confirmation. Save-time validation remains unchanged.
                 * Other failures show an error without attempting recovery.
                 *
                 * @param {HTMLSelectElement} select Mode selector in the parent page or Page Builder iframe.
                 * @returns {Promise<void>} Completes after the initial attempt or displaying the recovery
                 * dialog. Confirmed recovery runs separately through the dialog's success callback.
                 */
                window.switchEditorType = async function(select) {
                    const editorType = select.value;
                    const session = conf.editorSession;
                    const json = EDITOR.currentJson;
                    const request = {};
                    let ready = conf.wjeditorReadyPromise;
                    conf.editorModeSwitchRequest = request;
                    /**
                     * Rejects work that became obsolete while waiting for initialization or confirmation.
                     * Closing/reopening, changing rows, another mode request or a queued content write
                     * (for example, loading history) must invalidate the old recovery request.
                     * @returns {boolean} True while this request still belongs to the current editor state.
                     */
                    const isCurrent = () => conf.editorOpen && conf.editorSession === session &&
                        EDITOR.currentJson === json && conf.editorModeSwitchRequest === request &&
                        conf.wjeditorReadyPromise === ready;
                    /**
                     * Restores the actual mode in the initiating selector and the parent Bootstrap picker.
                     * The browser has already selected the requested mode, even if switching failed.
                     */
                    const resetSelector = () => {
                        const mode = conf.wjeditor?.editingMode || '';
                        $(select).val(mode);
                        $("#"+id+"-editorTypeSelect-select").selectpicker('val', mode);
                    };
                    /**
                     * Logs the failure and updates the UI only if the user is still in the same context.
                     * @param {unknown} error Failure from a mode switch or preserved-content validation.
                     */
                    const reportError = error => {
                        console.error('Error switching WYSIWYG mode:', error);
                        if (!isCurrent()) return;
                        resetSelector();
                        WJ.notifyError(WJ.translate('editor.content.mode_error.js'));
                    };
                    /**
                     * Applies a normal switch or an explicitly confirmed recovery and tracks its writes.
                     * Recovery bypasses the failed live read and revalidates conf._input at confirmation.
                     * That value comes from set() or a successful live read in get(); it is not a backup
                     * of every Page Builder edit and may omit recent changes. The hidden CKEditor is not
                     * used as a fallback source because its content may be stale.
                     * The shared readiness promise makes later page loads wait for this write to finish.
                     * Mode preferences are updated only if this request is still current after completion.
                     *
                     * @param {boolean} [restore=false] Use preserved HTML instead of reading the live editor.
                     * @returns {Promise<void>} Completes the switch or skips an obsolete request; rejects
                     * on invalid preserved HTML or a mode-switch failure.
                     */
                    const switchMode = async (restore = false) => {
                        if (!isCurrent()) return;
                        let savedHtml = null;
                        if (restore) {
                            savedHtml = conf._input.val();
                            if (isInvalidHtml(savedHtml)) throw new Error('Invalid preserved WYSIWYG content.');
                        }
                        const operation = conf.wjeditor.switchEditingMode(editorType, !restore, savedHtml);
                        // Subsequent page loads must wait for this mode's asynchronous content write.
                        ready = Promise.resolve(operation);
                        conf.wjeditorReadyPromise = ready;
                        await ready;
                        if (!isCurrent()) return;
                        window.editorTypeForced = editorType;
                        if ("html" !== editorType) WJ.setAdminSetting("editorTypeForced", editorType);
                    };
                    let oldMode;
                    try {
                        await ready;
                        if (!isCurrent()) return;
                        oldMode = conf.wjeditor.editingMode;
                        await switchMode();
                    } catch (error) {
                        if (!isCurrent()) return;
                        resetSelector();
                        if (oldMode !== 'pageBuilder' || editorType !== 'html' ||
                            conf.wjeditor.editingMode !== 'pageBuilder' || isInvalidHtml(conf._input.val())) {
                            reportError(error);
                            return;
                        }
                        WJ.confirm({
                            title: WJ.translate('editor.content.recover_html.title.js'),
                            message: WJ.translate('editor.content.recover_html.message.js'),
                            btnOkText: WJ.translate('editor.content.recover_html.button.js'),
                            success: () => {
                                if (!isCurrent() || conf.wjeditor.editingMode !== 'pageBuilder') return;
                                return switchMode(true).catch(reportError);
                            }
                        });
                    }
                }
            }

            window.pbSetWindowSize = function(size) {
                var iframeElement = null;

                if ("pageBuilder"==window.editorTypeForced) iframeElement = $("iframe.md-pageBuilder");
                else iframeElement = $("iframe.cke_wysiwyg_frame.cke_reset");

                //console.log("iframeElement=", iframeElement, "size=", size);
                var maxWidth = "";
                if ('tablet'==size) {
                    maxWidth = "991px";
                } else if ('phone'==size) {
                    maxWidth = "576px";
                }

                //console.log("iframeElement=", iframeElement[0].contentWindow);
                try {
                    if (iframeElement.length > 0 && iframeElement[0].contentWindow && typeof iframeElement[0].contentWindow.pbGetWindowSize === "function") {
                        maxWidth = iframeElement[0].contentWindow.pbGetWindowSize(size);
                    }
                } catch (e) {
                    console.error("pbSetWindowSize error:", e);
                }

                //console.log("Setting width: ", maxWidth);
                iframeElement.css("max-width", maxWidth);
            }

            return htmlCodeElement;
        },

        /**
         * Reads live HTML while open and retains it in conf._input for later reads or confirmed recovery.
         * Payload collection reuses the HTML already validated by initSubmit.
         * A failed read returns undefined and marks invalidHtml so submission is cancelled; it must not
         * silently save the preserved value, which may be older than the visible Page Builder content.
         * @param {Object} conf DataTables Editor field configuration.
         * @returns {string|undefined} Validated HTML without editor attributes, or undefined on failure.
         */
        get: function ( conf ) {
            // Editor may call get() once per row. Reuse the validated HTML so later read failures cannot
            // make it fall back to original row data or treat a changed page as unchanged.
            // An empty string is valid HTML, so check for undefined rather than truthiness.
            if (conf.submitSnapshot?.html !== undefined) return conf.submitSnapshot.html;
            conf.invalidHtml = false;
            try {
                //console.log("WYSIWYG get, conf=", conf, "wjeditor=", conf.wjeditor);
                if (conf.editorOpen) {
                    var htmlCode = conf.wjeditor.getData();
                    if (isInvalidHtml(htmlCode)) throw new Error('Invalid WYSIWYG content.');
                    //console.log("WYSIWYG get, htmlCode=", htmlCode);
                    //set htmlCode to input element, because it can be PageBuilder instance
                    conf._input.val(htmlCode);
                }
                try {
                    //fix for acunetix long texts which loads too long/ha too many JS errors, and acunetix will timeout scanning
                    if (window.currentUser.login.indexOf("tester")==0) {
                        let val = conf._input.val();
                        if (val != null && val.length > 32000) {
                            if (val.indexOf("<ScRiPt")!==-1 || val.indexOf("<zzz")!==-1|| val.indexOf("DBMS_PIPE")!==-1) {
                                val = val.substring(0, 16000);
                                console.log("shrinking val to length=", val.length);
                                conf._input.val(val);
                            }
                        }
                    }
                } catch (e) {
                    console.error(e);
                }
                var html = conf._input.val();
                if (isInvalidHtml(html)) throw new Error('Invalid WYSIWYG content.');
                //console.log("WYSIWYG get, conf=", conf, "returning=", html);

                if (typeof window.wysiwygGetCallback === "function") {
                    html = window.wysiwygGetCallback(html, conf);
                } else {
                    let pageBuilderIframe = $("#"+conf._id+"-pageBuilderIframe");
                    if (pageBuilderIframe.length > 0 && pageBuilderIframe[0].contentWindow && typeof pageBuilderIframe[0].contentWindow.wysiwygGetCallback === "function") {
                        html = pageBuilderIframe[0].contentWindow.wysiwygGetCallback(html, conf);
                    }
                }
                if (isInvalidHtml(html)) throw new Error('Invalid WYSIWYG callback content.');
                html = removeContentEditable(html);
                // Capture only after the read, callback and cleanup succeed. The snapshot exists only when
                // initSubmit requested it; ordinary get() calls must not start caching HTML.
                if (conf.submitSnapshot) conf.submitSnapshot.html = html;
                return html;
            } catch (error) {
                conf.invalidHtml = true;
                console.error('Error reading WYSIWYG content:', error);
                // Keep the visible editor and stored input intact; initSubmit/preSubmit will show the error.
                return undefined;
            }
        },

        /**
         * Stores the value immediately and writes to CKEditor only while its form is open.
         * @param {*} conf
         * @param {*} val
         */
        set: function ( conf, val ) {
            val = removeContentEditable(val);
            //console.log("WYSIWYG set, val=", val, "conf=", conf, "wjeditor=", conf.wjeditor);
            if (conf.editorOpen) {
                queueEditorOperation(conf, () => conf.wjeditor.setData(val));
            }
            // set directly as value to not propagate change events
            conf._input.val(val);

            // reset dirty status
            getThisField(conf).resetDirty(conf);
        },

        enable: function ( conf ) {
            conf._input.prop( 'disabled', false );
        },

        disable: function ( conf ) {
            conf._input.prop( 'disabled', true );
        },

        canReturnSubmit: function ( conf, node ) {
            return false;
        },

        isDirty: function ( conf ) {
            try
            {
                if (conf.dirtyDataOriginal === undefined)
                {
                    return false;
                }
                var currentData = getThisField(conf).get(conf);
                if (currentData != conf.dirtyDataOriginal) {
                    try {
                        // Keep carriage returns so changes in line endings remain visible.
                        const originalLines = String(conf.dirtyDataOriginal).split("\n");
                        const currentLines = String(currentData).split("\n");
                        let firstChangedLine = 0;
                        while (firstChangedLine < Math.min(originalLines.length, currentLines.length) &&
                            originalLines[firstChangedLine] === currentLines[firstChangedLine]) {
                            firstChangedLine++;
                        }
                        const contextStart = Math.max(0, firstChangedLine - 1);
                        const originalContext = originalLines.slice(contextStart, firstChangedLine + 2).join("\n");
                        const currentContext = currentLines.slice(contextStart, firstChangedLine + 2).join("\n");
                        const originalLine = originalLines[firstChangedLine] ?? "";
                        const currentLine = currentLines[firstChangedLine] ?? "";
                        let firstChangedCharacter = 0;
                        while (firstChangedCharacter < Math.min(originalLine.length, currentLine.length) &&
                            originalLine[firstChangedCharacter] === currentLine[firstChangedCharacter]) {
                            firstChangedCharacter++;
                        }
                        const previewStart = Math.max(0, firstChangedCharacter - 15);
                        const previewEnd = firstChangedCharacter + 20;
                        // Quote snippets so spaces, tabs and carriage returns can be inspected in the console heading.
                        const originalPreview = originalLines[firstChangedLine] === undefined ? "(end of content)" : JSON.stringify(originalLine.slice(previewStart, previewEnd));
                        const currentPreview = currentLines[firstChangedLine] === undefined ? "(end of content)" : JSON.stringify(currentLine.slice(previewStart, previewEnd));
                        const differencePreview = `${originalPreview} VS ${currentPreview}`;

                        window.top.lastDirty = {
                            field: conf.data,
                            original: conf.dirtyDataOriginal,
                            current: currentData,
                            summary: `Found difference on line ${firstChangedLine + 1}: ${differencePreview}\n\nOriginal (from line ${contextStart + 1}):\n${originalContext}\n\nChanged (from line ${contextStart + 1}):\n${currentContext}`,
                            firstDifference: {
                                line: firstChangedLine + 1,
                                contextStartLine: contextStart + 1,
                                original: originalContext,
                                current: currentContext
                            }
                        };

                        // TEMPORARY: customer diagnostics. Log each new difference without flooding the console.
                        const rows = [];
                        const contextEnd = Math.min(firstChangedLine + 2, Math.max(originalLines.length, currentLines.length));
                        for (let index = contextStart; index < contextEnd; index++) {
                            rows.push({
                                Line: index + 1,
                                Change: index === firstChangedLine ? ">>> FIRST DIFFERENCE" : "",
                                Original: originalLines[index] ?? "(end of content)",
                                Changed: currentLines[index] ?? "(end of content)"
                            });
                        }

                        console.groupCollapsed("%c[WebJET isDirty]%c %s: first difference on line %d: %s",
                            "background: #fff3cd; color: #664d03; padding: 2px 6px; border-radius: 3px; font-weight: bold;",
                            "font-weight: bold;", conf.data, firstChangedLine + 1, differencePreview);
                        console.table(rows);
                        console.groupCollapsed("Full HTML");
                        console.log("%cOriginal HTML", "color: #b42318; font-weight: bold;", conf.dirtyDataOriginal);
                        console.log("%cChanged HTML", "color: #067647; font-weight: bold;", currentData);
                        console.groupEnd();
                        console.log("Debug snapshot (window.top.lastDirty):", window.top.lastDirty);
                        console.groupEnd();
                    } catch (e) {
                        // Debug recording must not affect dirty detection when the top window is inaccessible.
                    }
                    return true;
                }
            }
            catch (e) {  }
            return false;
        },

        resetDirty: function ( conf ) {
            const request = {};
            conf.dirtyResetRequest = request;
            conf.dirtyDataOriginal = undefined;
            // Read the readiness chain after the synchronous DTE open handlers have queued their writes.
            Promise.resolve().then(() => conf.wjeditorReadyPromise).then(() => {
                captureDirtySnapshot(conf, request);
            }).catch(error => console.error("Error capturing editor dirty snapshot:", error));
        },

        /**
         * Focuses the editable CKEditor surface after initialization and pending data writes complete.
         *
         * @param {Object} conf DataTables Editor field configuration.
         * @param {Function} [canFocus] Returns false when the dialog focus request is obsolete.
         * @returns {Promise<boolean>} True when CKEditor received focus.
         */
        focusWhenReady: async function(conf, canFocus) {
            const isCurrentFocusRequest = typeof canFocus === 'function' ? canFocus : () => true;
            const deadline = Date.now() + FOCUS_READY_TIMEOUT_MS;

            try {
                const operationsReady = await Promise.race([
                    conf.wjeditorReadyPromise.then(() => true),
                    wait(FOCUS_READY_TIMEOUT_MS).then(() => false)
                ]);
                if (!operationsReady || !isCurrentFocusRequest()) return false;
            } catch (error) {
                console.error("Error preparing CKEditor for focus:", error);
                return false;
            }

            while (isCurrentFocusRequest() && Date.now() < deadline) {
                const wjeditor = conf.wjeditor;
                const editor = wjeditor?.ckEditorInstance;

                if (editor != null && typeof editor.isDestroyed === 'function' && editor.isDestroyed()) return false;
                if (wjeditor?.editingMode === 'pageBuilder' || wjeditor?.editingMode === 'html') return false;

                if (
                    editor?.status === 'ready' &&
                    editor.mode === 'wysiwyg' &&
                    editor.container?.$ != null &&
                    $(editor.container.$).is(':visible')
                ) {
                    editor.focus();
                    await new Promise(resolve => requestAnimationFrame(resolve));
                    if (isCurrentFocusRequest() && editor.focusManager?.hasFocus === true) return true;
                }

                await wait(Math.min(FOCUS_RETRY_DELAY_MS, Math.max(0, deadline - Date.now())));
            }

            return false;
        },

        setJson: function(conf, json) {
            //console.log("field-type-wysiwyg setJson, json=", json, "conf=", conf);
            if (conf.editorOpen) {
                queueEditorOperation(conf, () => conf.wjeditor.setJson(json), true);
            }
        }

    }
}
