import {Tools} from "./libs/tools/tools";
import { Base64 } from 'js-base64';

const WJ = (() => {

    /**
     * @param {string} key
     * @returns {string}
     */
    function translate(key, params) {
        //console.log("translate, key=", key, "params=", params);
        var translated = window.webjetTranslationService.translate(key);
        if (typeof params != "undefined" && params != null && params.length>0) {
            //console.log("translate, key=", key, "params=", params);
            for (var i=1; i<= params.length; i++) {
                //console.log("Adding param=", params[i-1], "i=", i);
                translated = replaceTranslateParameter(translated, params[i-1], i);
            }
        }

        //remove unused params
        if (translated.indexOf("{")!=-1 || translated.indexOf("}")!=-1) {
            for (var i=1; i<= 30; i++) {
                //components.ai_assistants.unknownError.js=Nastala chyba pri volaní AI asistenta {1}.
                translated = translated.replaceAll(" {"+i+"}.", ".");
                //components.ai_assistants.editor.loading.js=AI už na tom pracuje... {1}
                translated = translated.replaceAll("{"+i+"}", "");
            }
        }

        return translated;
    }

    function replaceTranslateParameter(translated, parameter, index) {
        if (typeof parameter != "undefined" && parameter != null) {
            translated = translated.replaceAll("{"+index+"}", parameter);
        }
        return translated;
    }

    function showHelpWindow(link=null) {
        var url = "http://docs.webjetcms.sk/latest/"+window.userLng;
        if (typeof link != "undefined" && link != null) url += link;
        else if (typeof window.helpLink != "undefined") url += window.helpLink;
        else url += "/";
        window.open(url, '_blank');
    }

    function changeDomainImpl(domainName) {
        $.ajax({
            method: 'POST',
            url: '/admin/skins/webjet8/change_domain_ajax.jsp',
            data: {
                domain: domainName
            }
        })
        .done(() => {
            //odstranime URL parametre, aby sa nezapamatalo docid z inej domeny
            if (window.location.pathname=="/admin/v9/" && hasPermission("menuWebpages")) {
                window.location="/admin/v9/webpages/web-pages-list/"
            } else {
                window.location=window.location.pathname;
            }
        });
    }

    /**
     * @param {HTMLSelectElement} select
     */
    function changeDomain(select) {
        /** @type {string} */
        const domainName = select.value;
        /** @type {string} */
        const title = WJ.translate('groupedit.domain.js');
        /** @type {string} */
        const message = WJ.translate('admin.top.domena.confirm.js');

        //console.log('title=', title);

        if (window.location.pathname=="/admin/v9/") {
            //z uvodnej stranky je mozne domenu zmenit bez potvrdenia
            changeDomainImpl(domainName);
        }
        else {
            WJ.confirm({
                title: title,
                message: message,
                success: () => {
                    changeDomainImpl(domainName);
                },
                cancel: () => {
                    select.value = $(select).data('previous');
                    $(select).selectpicker('val', select.value);
                }
            });
        }
    }

    /**
     * @param {string} url
     * @param {string} [width]
     * @param {string} [height]
     */
    function openPopupDialog(url, width = '650', height = '550') {
        const options = `status=no,menubar=no,toolbar=no,scrollbars=yes,resizable=yes,width=${width},height=${height}`;
        const popUpWindow = window.open(url, '', options);
        if (window.focus && !!popUpWindow) {
            popUpWindow.focus();
        }
        //Return instance of window, so we can manipulate with it
        return popUpWindow;
    }

    let modalIframe = null;
    let modalIframeOptions = null;

    /**
     * Zobrazi modalne okno s vlozenym iframe
     * @param options:
     * - url = URL adresa vlozeneho iframe
     * - width = sirka okna
     * - height = vyska vlozeneho iframe (modal bude o hlavicku a paticku vyssi)
     * - title = titulok okna
     * - buttonTitleKey = textovy kluc na primary tlacidle (predvolene kluc button.submit)
     * - closeButtonPosition = pozicia tlacidla na zatvorenie, prazdne - male X v hlavicke, close-button-over - male X v hlavicke ponad content (nevytvara samostatny riadok), pridanim nopadding sa zrusi horny padding hlavicky
     * - okclick = callback po kliknuti na tlacidlo potvrdit
     * - onload = callback po nacitani okna, ako parameter dostane event.detail obsahujuci property window s odkazom na okno v iframe
     */
    function openIframeModal(options) {

        const iframe = $('#modalIframeIframeElement');
        iframe.attr('src', options.url);
        iframe.attr('width', options.width);
        iframe.attr('height', options.height);

        let closeAfterSave = options.closeAfterSave;
        if(closeAfterSave == undefined || closeAfterSave == null) closeAfterSave = true;

        //nastav height aj na modal-body, inak ho renderovalo o par px vyssie ako iframe
        $('#modalIframe div.modal-body').css('height', options.height + 'px');

        //nastav velkost
        const borderWidth = 2;
        $('#modalIframe div.modal-dialog').css('max-width', (options.width + borderWidth) + 'px');
        //nastav title
        const title = options.title || '';
        $('#modalIframe .modal-header h5').html(title);
        if (title === "") $('#modalIframe .modal-header h5').hide();
        else $('#modalIframe .modal-header h5').show();

        //nastav button title
        const buttonTitle = options.buttonTitleKey ? WJ.translate(options.buttonTitleKey) : WJ.translate("button.submit");
        $('#modalIframe div.modal-footer button.btn-primary span').text(buttonTitle);

        //console.log('modalIframe=', modalIframe);
        modalIframeOptions = options;
        if (modalIframe === null) {
            modalIframe = new bootstrap.Modal(document.getElementById('modalIframe'), {keyboard: false, backdrop: "static"});

            _showIframeModal();

            //console.log("modalIframeShow, modalIframe=", modalIframe);
            $('#modalIframe button.btn-primary').on('click', (e) => {
                //console.log("Klik na btn-prinary, e=", e);
                let success = _modalOkClick();
                if (false === success) return;

                if(closeAfterSave === true) closeIframeModal();
            });
            $('#modalIframe button.btn-close').on('click', () => {
                closeIframeModal();
            });

            window.addEventListener("WJ.iframeLoaded", function(event) {
                //console.log("HAHA yes, WJ.iframeLoaded, event=", event);
                if (typeof modalIframeOptions.onload === 'function') {
                    modalIframeOptions.onload(event.detail);
                }
            });
        } else {
            _showIframeModal();
        }

        let $modalIframe = $("#modalIframe");
        $modalIframe.removeClass("close-button-over");
        $modalIframe.removeClass("nopadding");
        if (typeof options.closeButtonPosition != "undefined") {
            if (options.closeButtonPosition.indexOf("close-button-over")!=-1) $modalIframe.addClass("close-button-over");
            if (options.closeButtonPosition.indexOf("nopadding")!=-1) $modalIframe.addClass("nopadding");
        }

        //nastav CSS triedu backdropu (potrebujeme ho posunut vyssie)
        $('.modal-backdrop').addClass('modalIframeShown');
    }

    function closeIframeModal() {
        //console.log("closeIframeModal, moda=", modalIframe);
        //$('#modalIframe').modal('hide');
        const iframe = $('#modalIframeIframeElement');
        iframe.attr('src', "about:blank");
        modalIframe.hide();
        $('.modal-backdrop').removeClass('modalIframeShown');

        if (window.parent!=window.self) $(window.parent.document).find("#modalIframe").removeClass("child-iframe-open");
    }

    /**
     * Special version of openIframeModal with datatable support
     * - it will trigger click on primary button in datatable modal
     * - it will handle close of modal after datatables editor save
     * @param {*} options - same as for openIframeModal
     */
    function openIframeModalDatatable(options) {
        if (typeof options.okclick === 'undefined') {
            options.okclick = function() {
                $('#modalIframeIframeElement').contents().find('div.modal.DTED.show div.DTE_Footer button.btn-primary').trigger("click");
                return false;
            }
        }
        if (typeof options.onload === 'undefined') {
            options.onload = function(detail) {
                let iframeWindow = detail.window;
                //console.log("iframeWindow=", iframeWindow.location.href);
                iframeWindow.addEventListener("WJ.DTE.close", function(event) {
                    //console.log("CLOSE EVENT RECEIVED, event=", event);

                    //ulozenie nested modalu nema sposobit zatvorenie okna
                    if (true===event.detail.dte.TABLE.DATA.nestedModal) return;
                    setTimeout(() => {
                        WJ.closeIframeModal();
                    }, 100);
                });
            }
        }
        if (typeof options.closeButtonPosition === 'undefined') {
            options.closeButtonPosition = "close-button-over";
        }
        //verify window height and adjust it
        var height = options.height;
        var windowHeight = $(window).height() - 80;
        if (height > windowHeight) height = windowHeight;
        if (height > 300) options.height = height;
        openIframeModal(options);
    }

    function _showIframeModal() {
        modalIframe.show();
        if (window.parent!=window.self) $(window.parent.document).find("#modalIframe").addClass("child-iframe-open");
    }

    function _modalOkClick() {
        //musi to byt takto, aby sa pouzila posledna hodnota options, inak to klikalo v druhom dialogu aj prvy
        if (typeof modalIframeOptions.okclick === 'function') {
            return modalIframeOptions.okclick();
        }
    }

    /**
     * Show a toast notification
     * @param {*} type - type of notification (success, info, warning, error)
     * @param {*} title - title of the notification
     * @param {*} text - text of the notification
     * @param {*} timeOut - timeout in ms
     * @param {*} buttons - custom buttons for the notification
     * @param {*} appendToExisting - whether to append to existing notifications
     * @param {*} containerId - ID of the container to append the notification to
     * @returns
     */
    function toastNotify(type, title, text, timeOut = 0, buttons = null, appendToExisting = false, containerId = null) {

        if (typeof containerId === "undefined" || containerId === null) containerId = 'toast-container-webjet';

        if (title === '' && text === '') {
            return false
        }

        if (title === '') title = "&nbsp;";

        if (appendToExisting) {
            var found = false;
            try {
                $('#'+containerId+' .toast-'+type).each(function() {
                    var $this = $(this);
                    var currentTitle = $this.find(".toast-title").html();
                    if (currentTitle === title) {
                        var $message = $this.find(".toast-message");
                        var newMessage = $message.html();
                        if (newMessage.endsWith("<br>")===false) newMessage += "<br>";
                        newMessage += text;
                        $message.html(newMessage);
                        found=true;
                    }
                });
            } catch (e) {}
            if (found === true) return;
        }

        const options = {
            closeButton: true,
            timeOut: timeOut,
            tapToDismiss: false,
            extendedTimeOut: timeOut,
            progressBar: true,
            positionClass: 'toast-container toast-top-right',
            containerId: containerId,
            closeHtml: '<button class="btn btn-close toast-close-button"><i class="ti ti-x"></i></button>',
        };

        if ("toast-container-webjet"!=containerId) {
            options.showDuration = 0;
            options.showMethod = "show";
        }

        let buttonsHtml = null;
        if (typeof buttons != "undefined" && buttons != null && buttons.length > 0) {
            //vygeneruj HTML kod pre tlacidla
            buttonsHtml = `<div class="toast-links toast-links-standard">`;
            buttons.forEach(function(button, index) {
                if (typeof button.closeOnClick == "undefined") button.closeOnClick = true;
                buttonsHtml += `<a class="${button.cssClass}" title="${button.title}" onclick="${button.click}" data-close="${button.closeOnClick}"><i class="${button.icon}"></i> ${button.title}</a>`;
            });
            buttonsHtml += `</div>`;

            text += buttonsHtml;
        }

        //console.log("toastNotify, type=", type, "options=", options);
        let toastrInstance = null;

        switch (type) {
            case 'success':
                toastrInstance = toastr.success(text, title, options);
                break;
            case 'info':
                toastrInstance = toastr.info(text, title, options);
                break;
            case 'warning':
                toastrInstance = toastr.warning(text, title, options);
                break;
            case 'error':
                toastrInstance = toastr.error(text, title, options);
                break;
            default:
                console.error('wrong type, allowed types: success, info, warning, error');
        }

        if (toastrInstance != null && buttonsHtml != null) {
            //console.log("TOASTR INSTANCE=", toastrInstance);
            toastrInstance.on("click", "a.btn", (e) => {
                //console.log("btn click, this=", this, e);
                if ("false"===$(e.target).attr("data-close")) return;
                toastr.clear(toastrInstance);
            })
        }

        return true;
    }

    /**
     * @param {{title: string: string, message: string, btnCancelText: string, btnOkText: string, success: function, cancel: function}} options
     */
    function confirm(options) {
        const title = options.title ? options.title : '';
        const message = options.message ? `<div class="toastr-message">${options.message}</div>` : '';
        const btnId = (new Date()).getTime();
        //TODO: preklady
        const btnCancelText = options.btnCancelText ? options.btnCancelText : WJ.translate('button.cancel');
        const btnOkText = options.btnOkText ? options.btnOkText : WJ.translate('button.submit');
        let promptHtml = '';
        const typePrompt = "prompt"===options.type;
        if (typePrompt === true) {
            promptHtml =
            `<div class="toastr-input">
                <input type="text" id="toastrPromptInput${btnId}" class="form-control"/>
             </div>
            `;
        }
        //console.log("WJ.confirm, options=", options, "promptHtml=", promptHtml);
        let toastrInstance = null;
        toastrInstance = toastr.info(
            `${message}
             ${promptHtml}
            <div class="toastr-buttons">
                <button type="button" id="confirmationNo${btnId}" class="btn btn-outline-secondary">
                    ${btnCancelText}
                </button>
                <button type="button" id="confirmationYes${btnId}" class="btn btn-primary">
                    ${btnOkText}
                </button>
            </div>`,
            title,
            {
                timeOut: 0,
                extendedTimeOut: 0,
                closeButton: true,
                tapToDismiss: false,
                allowHtml: true,
                onShown: () => {
                    //console.log("onshow, toastrInstance=", toastrInstance);
                    $('#confirmationYes' + btnId).on('click', () => {
                        //console.log('Yes'+btnId+' clicked');
                        if (typeof options.success === 'function') {
                            if (typePrompt === true) {
                                options.success($("#toastrPromptInput"+btnId).val());
                            } else {
                                options.success();
                            }
                        }
                        //volanie toastr.clear(toastrInstance) z nejakeho dovodu nefunguje
                        toastrInstance.find('button.toast-close-button').click();
                    });
                    $('#confirmationNo' + btnId).on('click', () => {
                        //console.log('No'+btnId+' clicked');
                        if (typeof options.cancel === 'function') {
                            options.cancel();
                        }
                        toastrInstance.find('button.toast-close-button').click();
                    });
                },
                positionClass: 'toast-container toast-top-right',
                containerId: 'toast-container-webjet'
            }
        );
    }

    /**
     * Prida do (rest) URL cestu, kontroluje, ci v URL nie je ?param
     * url=/admin/rest/tree?click=groups, pathAppend=/list -> /admin/rest/tree/list?click=groups
     * @param {*} url
     * @param {*} pathAppend
     */
    function urlAddPath(url, pathAppend) {
        if (pathAppend === null || '' === pathAppend) return url;
        const split = url.split('?');
        let newUrl = split[0] + pathAppend;
        newUrl = newUrl.replaceAll('//', '/');
        if (split.length > 1) {
            newUrl += '?' + split[1];
        }
        return newUrl;
    }

    /**
     * Prida do URL parameter, kontroluje, ci v URL uz nejaky parameter je, a podla toho prida ? alebo &
     * @param {*} url
     * @param {*} paramName
     * @param {*} paramValue
     */
    function urlAddParam(url, paramName, paramValue) {
        url += url.indexOf('?') === -1 ? '?' : '&';
        url += paramName + '=' + encodeURIComponent(paramValue);
        return (url);
    }

    /**
     * Zmení URL parameter, kontroluje, ci v URL daný parameter je, ak áno, zmení jeho hodnotu na novú zadanú
     * ak nie, prida novy param
     * @param {*} url
     * @param {*} paramName
     * @param {*} paramValue
     */
    function urlUpdateParam(url, paramName, paramValue) {
        //Check if url includes this param
        if(url.includes((paramName + '='))) {
            //Url ends with param but without value
            if(url.endsWith((paramName + '='))) return url + encodeURIComponent(paramValue);

            //Update parm value inside of url
            let urlParams = (url.split("?")[1]).split("&")
            for (var i = 0; i < urlParams.length; i++)
                if(urlParams[i].startsWith((paramName + '=')))
                    urlParams[i] = paramName + "=" + encodeURIComponent(paramValue)

            let newUrl = url.split("?")[0] + "?";
            for (var i = 0; i < urlParams.length; i++) {
                newUrl += urlParams[i];
                if(i + 1 < urlParams.length) newUrl += "&"
            }

            return newUrl;
        } else {
            //Add params
            return urlAddParam(url, paramName, paramValue);
        }
    }

    /**
     * returns URL parameter value
     * @param {*} name - parameter name
     * @param {*} queryString - URL query string OR null to ger from current window.location.search
     * @returns
     */
    function urlGetParam(name, queryString=null) {
        if (queryString == null) queryString = window.location.search;
        const urlParams = new URLSearchParams(queryString);
        return urlParams.get(name);
    }

    /**
     * Nastavi JSON property v objekte podla zadaneho mena, akcetupje aj nested property typu editorFields.groupCopyDetails
     * @param {*} obj
     * @param {*} path
     * @param {*} value
     */
    function setJsonProperty(obj, path, value) {
        let schema = obj;  // a moving reference to internal objects within obj
        const pList = path.split('.');
        const len = pList.length;
        for (let i = 0; i < len - 1; i++) {
            const elem = pList[i];
            if (!schema[elem]) {
                schema[elem] = {}
            }
            schema = schema[elem];
        }
        schema[pList[len - 1]] = value;
    }

    /**
     * Ziska JSON property v objekte podla zadaneho mena, akcetupje aj nested property typu editorFields.groupCopyDetails
     * @param {*} obj
     * @param {*} path
     * @param {*} value
     */
     function getJsonProperty(obj, path) {
        let schema = obj;  // a moving reference to internal objects within obj
        const pList = path.split('.');
        const len = pList.length;
        for (let i = 0; i < len - 1; i++) {
            const elem = pList[i];
            if (!schema[elem]) {
                schema[elem] = {}
            }
            schema = schema[elem];
        }
        return schema[pList[len - 1]];
    }

    /**
     * @description Otvori dialogove okno elfindera
     * @param {{link:string, width: number, height: number, url: string, okclick: function}} options
     * @info options.link - URL adresa zobrazena v elfinderi (=aktualna hodnota)
     * @info options.title - nazov dialogoveho okna
     * @returns {void}
     */
    function openElFinder(options) {
        /** @type {string} */
        let url = '/admin/v9/files/dialog/';
        //if (options.link !== undefined && options.link !== null && options.link !== '')
        if (!!options.link) {
            url = urlAddParam(url, 'link', options.link);
        }
        if (typeof options.volumes !== "undefined" && options.volumes !== null && options.volumes !== '') {
            url = urlAddParam(url, 'volumes', options.volumes);
        }

        options.width = 800;
        options.height = 540;
        options.url = url;

        //verify window height and adjust it
        var height = options.height;
        var windowHeight = $(window).height() - 80;
        if (height > windowHeight) height = windowHeight;
        if (height > 300) options.height = height;

        //musis osefovat callback
        /** @type {function} */
        const originalCallback = options.okclick;
        //console.log('originalCallback=', originalCallback);
        options.okclick = () => {
            /** @type {string} */
            let link = $('#modalIframeIframeElement').contents().find('div.inputs').find('.row:not(.template)').find('input').val();
            if (typeof link == "undefined") {
                link = $('#modalIframeIframeElement').contents().find('div.inputs').find('input.elfinder-url-input').val();
            }
            //console.log('elfinder ok click link=', link, 'callback=', originalCallback);
            if (typeof originalCallback === 'function') {
                originalCallback(link);
            }
        }

        options.closeButtonPosition = "close-button-over-nopadding";

        openIframeModal(options);
    }

    //WebJET - odstranenie diakritiky
    /**
     * @param {string} text
     * @param {boolean} [whitespaceToDash]
     * @returns {string}
     */
    function internationalToEnglish(text, whitespaceToDash = true) {
        /** @type {string} */
        const dash = whitespaceToDash ? '-' : ' ';
        return Tools.removeDiacritics(text, dash);
    }

    //WebJET - ponecha len safe znaky
    /**
     * @param {string} text
     * @returns {string}
     */
    function removeChars(text) {
        /** @type {string} */
        let ptext = text.replace(/[^a-z0-9-_.]+/gi, '-');
        ptext = ptext.replaceAll(/(_-_)+|(-_-)+|(-_)+|(_-)+/gi, '-');
        ptext = ptext.replaceAll(/[-]+/gi, '-');
        ptext = ptext.replaceAll(/[_]+/gi, '_');
        return ptext;
    }

    function encodeValue(value) {
        value = escape(value);
        //value = value.replace(/aaa/g, '%20');
        return (value);
    }

    function unencodeValue(value) {
        value = unescape(value);
        //value = value.replace(/aaa/g, '%20');
        return (value);
    }

    /**
     * Nahradi nebezpecne znaky v HTML kode za entity pre ich bezpecne vypisanie
     * @param {*} string
     * @returns
     */
    function escapeHtml(string) {

        string = string.toString();

        string = string.replaceAll("&#47;", "/");
        //fix to &lt; -> &amp;lt;
        string = string.replaceAll("&lt;", "<");
        string = string.replaceAll("&gt;", ">");
        string = string.replaceAll("&quot;", "\"");

        var entityMap = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;',
            '/': '&#x2F;',
            '`': '&#x60;',
            '=': '&#x3D;'
        };

        return String(string).replace(/[&<>"'`=\/]/g, function (s) {
            return entityMap[s];
        });
    }

    function fixFileName(fileName)
    {
        fileName = fileName.replace(/&/gi, "");
        fileName = internationalToEnglish(fileName);
        fileName = removeChars(fileName);
        return fileName.toLowerCase();
    }

    function removeSpojky(text) {
        let urlRemoveSpojky = window.Constants["urlRemoveSpojky"];

        if(urlRemoveSpojky) {
            let spojky = window.Constants["urlRemoveSpojkyList"];
            spojky.forEach((element) => {
                let spojka = element.replace(".", "\\.");

                if(text != undefined && text != null && text.length > 1) {
                    var regexA = new RegExp("-" + spojka + "-", "gi");
                    var regexB = new RegExp("-" + spojka + "\/", "gi");
                    text = text.replace(regexA, "-");
                    text = text.replace(regexB, "/");
                }
            });

            return text;
        }
    }

    function _formatTime(timestamp, format) {
        if (typeof timestamp === 'undefined' || timestamp === null || timestamp === '') return "";
        return moment(timestamp).format(format);
    }

    function formatDate(timestamp) {
        return _formatTime(timestamp, 'L');
    }

    function formatDateTime(timestamp) {
        return _formatTime(timestamp, 'L HH:mm');
    }

    function formatDateTimeSeconds(timestamp) {
        return _formatTime(timestamp, 'L HH:mm:ss');
    }

    function formatTime(timestamp) {
        return _formatTime(timestamp, 'HH:mm');
    }

    function formatTimeSeconds(timestamp) {
        return _formatTime(timestamp, 'HH:mm:ss');
    }

    function formatPrice(price) {
        return numeral(price).format('0,0.00');
    }

    /**
     * Udrzuje session zo serverom, ked server neodpovie, zobrazi hlasenie
     */
    window.toastrLogoffMessageShown = false;
    window.toastrTokenMessageShown = false;

    function _callRefresher() {
        let now = new Date();
        $.ajax({
            url: '/admin/rest/refresher',
            data: {t: now.getTime()},
            method: 'post',
            timeout: 15000,
            success: data => {
                //console.log('data=', data);
                if (typeof data === 'string' && data.indexOf('logonForm') !== -1) {
                    keepSessionShowLogoffMessage();
                } else {
                    //spracuj messages a zobraz ich
                    try {
                        if (data.messages !== null) {
                            let options = 'toolbar=no,scrollbars=no,resizable=yes,width=500,height=400;'
                            for (const id of data.messages) {
                                window.open('/components/messages/message_popup.jsp?messageId=' + id, 'msgpop' + id, options);
                            }
                            // for (let i = 0; i < data.messages.length; i++) {
                            //     const id = data.messages[i];
                            //     window.open('/components/messages/message_popup.jsp?messageId=' + id, 'msgpop' + id, options);
                            // }
                        }
                    } catch (e) {
                        console.log(e);
                    }
                }
            },
            error: () => {
                //console.log('error, data=', data);
                keepSessionShowLogoffMessage();
            }
        });
    }

    function keepSessionShowTokenMessage(errorMessage) {
        try {
            if (window.toastrTokenMessageShown === false) {
                toastr.error("", errorMessage, {
                    positionClass: 'toast-container toast-top-full-width',
                    closeButton: true,
                    timeOut: 0,
                    tapToDismiss: false,
                    extendedTimeOut: 0, //prevent hide after mouse over
                    progressBar: false,
                    containerId: 'toast-container-logoff'
                });

                window.toastrTokenMessageShown = true;
            }
        } catch (e) {
        }
    }

    function keepSessionShowLogoffMessage() {
        const logonTimeout = 5 * 60 * 1000;
        try {
            if (window.toastrLogoffMessageShown === false) {
                toastr.error(WJ.translate('session.logoff.warn.js'), WJ.translate('session.logoff.info.js'), {
                        positionClass: 'toast-container toast-top-full-width',
                        timeOut: logonTimeout,
                        progressBar: true,
                        closeButton: true,
                        tapToDismiss: false,
                        closeOnHover: false,
                        containerId: 'toast-container-logoff'
                    });

                window.toastrLogoffMessageShown = true;
            }
        } catch (e) {
        }

        setTimeout(() => {
            window.location.href = '/admin/';
        }, logonTimeout);
    }

    function keepSession() {
        window.setInterval(() => {
            _callRefresher();
        }, 60000);
        window.setTimeout(() => {
            //prvy zavolaj po 10 sekundach, nech sa nezatazuje spojenie so serverom
            _callRefresher();
        }, 10000);
    }

    //firne globalny event nad window objektom
    function dispatchEvent(name, detail) {
        //firni event
        const event = new CustomEvent(name, {
            detail: detail
        });
        //console.log('Dispatching event, name=', name, 'detail=', detail);
        window.dispatchEvent(event);
    }

    function setTitle(title) {
        let $headerTitle = $(".header-title");
        $headerTitle.text(title);

        let pipeIndex = document.title.indexOf("|");
        if (pipeIndex==-1) document.title = title + " | " + document.title;
        else document.title = title + " " + document.title.substring(pipeIndex);
    }

    /**
     * Vygeneruje hlavnu navigacnu listu vo web stranke, pouziva sa primarne v /apps/ adresari
     * @param {*} config - JSON objekt konfiguracie typu:
     *      {
     *          id: "regexp",
     *           tabs: [
     *               {
     *                   url: '/apps/gdpr/admin/',
     *                   title: '[[#{components.gdpr.menu}]]',
     *                   active: false
     *               },
     *               {
     *                   url: '/apps/gdpr/admin/regexps/',
     *                   title: '[[#{components.gdpr.regexp.title}]]'
     *               }
     *           ],
     *           backlink: {
     *              url: '/apps/forms/',
     *              title: 'Formulare'
     *           }
     *       }
     */
    function breadcrumb(config) {

        //console.log("navbar, config=", config);
        let breadcrumb = $("div.ly-content div.ly-container.container div.md-breadcrumb");
        if (breadcrumb.length === 0) {
            breadcrumb = $("<div class='md-breadcrumb'></div>");
            $("div.ly-content div.ly-container.container").prepend(breadcrumb);
        }

        //default is hidden, show it
        breadcrumb.show();

        let ul = $(`<ul class="nav" id="pills-${config.id}" role="tablist"></ul>`);

        if (typeof config.backlink != "undefined" && config.backlink != null) {
            let backlink = $(`<li class="nav-item"><a class="nav-link back-link" href="${config.backlink.url}"><i class="ti ti-chevron-left"></i>${config.backlink.title}</a></li>`);
            ul.append(backlink);
        }

        let counter = 0;
        let currentNavbarTab = $(".ly-submenu .nav-link.active").text();
        config.tabs.forEach(function(data, index) {

            //skip with same name as navbar tab
            if (currentNavbarTab!="" && currentNavbarTab === data.title) return;

            let li = $(`<li class="nav-item"></li>`);

            if ("{filter}"===data.title && typeof config.showInIframe == "undefined") {
                config.showInIframe = true;
            }

            if ("{LANGUAGE-SELECT}"===data.title) {
                let select = $("div.breadcrumb-language-select").first();
                if (select.length==0) window.alert("Breadcrumb language select not found");
                select.appendTo(li);
                select.show();
            } else if (data.title.indexOf("{")==0) {
                let div = $(`<div class="navbar-container" id="pills-${data.url.substring(1)}-tab">${data.title}</div>`);
                li.append(div);
            } else {
                let anchor = $(`<a class="nav-link" href="${data.url}">${data.title}</a>`);

                if (typeof data.active == "undefined" || data.active===true) {
                    anchor.addClass("active");
                } else {
                    if (counter == 0) {

                        if (data.backlink === "true") {
                            anchor.addClass("back-link");
                            anchor.prepend('<i class="ti ti-chevron-left"></i>');
                        } else {
                            anchor.addClass("front-link");
                            anchor.append('<i class="ti ti-chevron-right"></i>');
                        }
                    }
                }

                li.append(anchor);
            }

            ul.append(li);
            counter++;
        });

        breadcrumb.html("");
        //console.log("Appending breadcrumb, ul=", ul.find("li").length);
        breadcrumb.append(ul);

        if (ul.find("li").length === 0) breadcrumb.hide();

        if (config.showInIframe===true) breadcrumb.addClass("show-in-iframe");
        if (config.noBorderBottom===true) breadcrumb.addClass("no-border-bottom");
    }

    function headerTabs(config) {
        //console.log("headerTabs, config=", config);

        let tabs = $("div.ly-header div.ly-submenu");

        let ul = $(`<ul class="nav" id="pills-${config.id}" role="tablist"></ul>`);

        config.tabs.forEach(function(data, index) {
            let li = $(`<li class="nav-item" role="presentation"></li>`);

            let idNoHash = data.url;
            if (typeof idNoHash != "undefined" && idNoHash.indexOf("#")==0) idNoHash = idNoHash.substring(1);
            let href = "#pills-"+idNoHash;
            //ak je to URL nepridavaj #pills-
            if (idNoHash.indexOf("/")==0) {
                href = idNoHash;
            }
            if (typeof data.id != "undefined") {
                idNoHash = data.id;
            }

            let anchor = $(`<a class="nav-link" id="pills-${idNoHash}-tab">${data.title}</a>`);
            if (data.url.indexOf("javascript:")==0) {
                anchor.attr("href", data.url);
            } else {
                anchor.attr("href", href);
            }
            if (href.indexOf("#")==0) {
                //cant use data-bs-toggle because in elfinder is initialized after app-init and events will colide for mobile menu
                anchor.attr("data-wj-toggle", "tab");
                anchor.attr("role", "presentation");
            }

            if (typeof data.active != "undefined" && data.active===true) {
                anchor.addClass("active");
            }
            li.append(anchor);
            ul.append(li);
        });

        //wrap UL with md-tabs div
        ul = $("<div class='md-tabs md-tabs-dropdown'></div>").append(ul);

        tabs.html("");
        tabs.append(ul);
        $("body").addClass("ly-submenu-active");
        window.initSubmenuTabsClick();
    }

    const htmlToTextRegex = /(<([^>]+)>)/ig;
    /**
     * Skonvertuje zadany HTML kod na jednoduchy text
     * @param {*} html
     * @returns
     */
    function htmlToText(html) {
        if (typeof html != "string") return html;

        if (html.indexOf("<")==-1 && html.indexOf(">")==-1) return html;

        //POZOR: nejde pouzit moznost cez document.createElement("div") a innerHTML/innerText lebo to vykona mozny XSS v danom DIV elemente
        html = html.replace(/<br>/gi, " ");
        html = html.replace(/<br\/>/gi, " ");
        html = html.replace(/<\/p>/gi, " ");
        html = html.replace(/<div>/gi, " ");
        html = html.replace(/<\/div>/gi, " ");
        return html.replace(htmlToTextRegex, "").trim();
    }


    /**
     * Overi, ci aktualne prihlaseny pouzivatel ma uvedene pravo
     * @param {String} permission
     * @returns
     */
    function hasPermission(permission) {
        if (window.nopermsJavascript[permission] === true) return false;
        return true;
    }

    /**
     * Skonvertuje zakladny markdown format do HTML kodu
     * @param {*} markdownText - nepovoluje HTML kod, striktne iba zakladne markdown znacky
     * @param {*} options
     *  - link {boolean} - ak je true, povolene su aj HTML linky
     *  - badge {boolean} - ak je true, povolene su aj badge
     *  - imgSrcPrefix {string} - URL prefix (domenove meno) pre obrazky
     *  - removeLastBr {boolean} - ak je true, odstrani sa posledny <br />
     * @returns
     */
    function parseMarkdown(markdownText, options) {
        let htmlText = markdownText;

        //nepovolujeme ine ako markdown znacky
        htmlText = htmlText.replace(/</gi, "&amp;lt;")
            .replace(/>/gi, "&amp;gt;");

        //return back ->
        htmlText = htmlText.replace(/-&amp;gt;/gi, "-&gt;")

        //console.log(htmlText);

        //For us is very inportant to end file with \n\n -> in case of embedded tree, using "-"
        if(! htmlText.endsWith("\n\n")) {
            if(! htmlText.endsWith("\n")) htmlText += "\n";
            else htmlText += "\n\n";
        }

        htmlText = htmlText.replace(/^### (.*$)/gim, '<h3>$1</h3>')

            //headings
            .replace(/^## (.*$)/gim, '<h2>$1</h2>')
            .replace(/^# (.*$)/gim, '<h1>$1</h1>')

            //blockquote
            .replace(/\n&amp;gt;\s*(.+?\.)(\n)/gm, '<blockquote>$1</blockquote>$2')

            //UL LI
            .replace(/\n+\s{0,1}-\s(.*)/gm, '<li>$1</li>')
            .replace(/(<\/li>)\n\s{2}-([\S\s]*?)(<li>)/gm, ' </li> <ul>\n  -  $2 </ul> <li>')
            .replace(/<\/li>\n\s{2}-([\S\s]*?)\n\n/gm, '</li> <ul>\n  - $1 </ul> \n\n')
            .replace(/\n\s+-(.*)/gm, '<li>$1</li>')
            .replace(/(<li>[\S\s]*?)(<\/li>|<\/ul>)\n\n/gm, '<ul> $1 $2 </ul>')

            //code block
            .replace(/\n\s*\`\`\`\n*([\S\s]*?)\`\`\`/gim, '<div class="code">$1</div>')
            .replace(/\`\`\`\n*([^\`]*?)\`\`\`/gi, '<span class="code">$1</span>')
            .replace(/\`([^\`]*?)\`/gi, '<span class="code-inline">$1</span>')

            //replace escaped \* with entity value
            .replace(/\\\*/gim, '&#42;')

            //formatting
            .replace(/\*\*(.*?)\*\*/gim, '<b>$1</b>')
            .replace(/\*(.*?)\*/gim, '<i>$1</i>')
            .replace(/\n/gim, '<br />')

            //fix h1 or h2 tag followed by br tag including whitespace (possible 2 br tags)
            .replace(/(<h[1-6]>)(.*?)(<\/h[1-6]>)\s*<br \/>/gim, '$1$2$3')
            .replace(/(<h[1-6]>)(.*?)(<\/h[1-6]>)\s*<br \/>/gim, '$1$2$3')

            //fix 2 br tags after blocquote
            .replace(/<\/blockquote><br \/><br \/>/gim, '</blockquote>')

            //fix BR tag before H1-H6 tag
            .replace(/<br \/><h([1-6])>/gim, '<h$1>')
            .replace(/<br \/><h([1-6])>/gim, '<h$1>')
            ;

        if (typeof options != "undefined") {
            if (true === options.link) {
                htmlText = htmlText.replace(/\[(\s*http[^\[\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
                                   .replace(/\[(\s*[^\[\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');

                //external links fix
                htmlText = htmlText.replace(/<a href="(http[^\"]+)"/g, '<a href="$1" target="_blank"');
            }

            //badge span
            if (true === options.badge) {
                htmlText = htmlText.replace(/<li>\s*([^`\n<]{1,20})-/gm, '<li> <span class="badge bg-secondary">$1</span>');
            }

            //img and href prefix
            if(null !== options.imgSrcPrefix) {
                htmlText = htmlText.replace(/!\[(.*?)\]\((.*?)\)/gim, "<img alt='$1' src='" + options.imgSrcPrefix + "$2' class='img-fluid' loading='lazy'/>")
                                   .replace(/(\[([^\]]+)])\(([^:)]+)\)/g, "<a href='" + options.imgSrcPrefix + "#/$3' target='_blank'>$2</a>")
            }

            if (true === options.removeLastBr) {
                htmlText = htmlText.replace(/<br \/>$/, '');
            }
        }

        //This is for cases where [](link) link isn't for our documentation but for some web page (so do it without our prefix)
        //OR in case that options are not set at all
        htmlText = htmlText.replace(/!\[(.*?)\]\((.*?)\)/gim, "<img alt='$1' src='$2' class='img-fluid' loading='lazy'/>")
                            .replace(/\[(\s*http[^\[\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
                            .replace(/\[(\s*[^\[\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')

        //console.log(htmlText);

        //return back &amp;lt; to &lt; and gt;
        htmlText = htmlText.replace(/&amp;lt;/gi, "&lt;")
            .replace(/&amp;gt;/gi, "&gt;")
            .replace(/&amp;quot;/gi, "&quot;");

        return htmlText.trim()
    }

    /**
     * Inicializuje tooltip na zadanom element
     * @param {jQuery element} $element
     */
    function initTooltip($element, customClass = null) {
        $element.each(function (key, el) {
            var $el = $(el);
            var tooltipText = $el.attr("title");

            var conf = {
                placement: 'top',
                trigger: 'hover'
            };
            if (customClass != null) conf.customClass = customClass;

            if (typeof tooltipText != "undefined" && tooltipText.length > 0) {
                //console.log("Tooltiptext=", tooltipText);
                tooltipText = WJ.parseMarkdown(tooltipText, {"removeLastBr": true});
                //console.log("Tooltiptext parsed=", tooltipText);
                $el.attr("title", tooltipText);
                conf.html = true;
                $el.tooltip(conf);
            } else {
                $el.tooltip(conf);
            }
        });
    }

    /**
     * Vrati objekt admin nastaveni podla zadaneho kluca.
     * Je to perzistentna obdoba localStorage
     * @param {*} key
     * @returns
     */
    function getAdminSetting(key) {
        return window.currentUser.adminSettings[key];
    }

    /**
     * Ulozi admin nastavenia so zadanym klucom a hodnotou.
     * Hodnota je ulozena do databazy a nasledne dostupna aj v inom prehliadaci pouzivatela.
     * Je to perzistentna obdoba localStorage
     * @param {*} key
     * @param {*} value
     */
    function setAdminSetting(key, value) {
        //ihned nastav, aby boli aktualne nastavene pre JS kod a nemusel cakat na odpoved z REST sluzby
        window.currentUser.adminSettings[key] = value;

        //uloz to na server
        let data = {
            label: key,
            value: value
        }
        $.post({
            url: "/admin/rest/admin-settings/",
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function (response) {
                //console.log("response=", response);
            }
        });
    }

    /**
     * Show #loader div
     * @param {*} loaderText - optional text to show inside #loaderText element
     */
    function showLoader(loaderText=null, insertAfter=null) {
        $(".hide-while-loading").hide();
        let insertAfterSelector = ".ly-content >.scroll-content > .ly-container > .md-breadcrumb";
        if(insertAfter != null) insertAfterSelector = insertAfter;

        let loaderEl = $(insertAfterSelector + " .webjetAnimatedLoader");
        if (loaderEl.length<1) {
            let loaderText = WJ.translate("webjetjs.webjetAnimatedLoader.text.js");
            loaderEl = $(`
            <div class="webjetAnimatedLoader">
                <div class="lds-dual-ring"></div>
                <p class="loaderText">${loaderText}</p>
            </div>`);
            //console.log("showLoader, insertAfterSelector=", insertAfterSelector, "insertAfter=", insertAfter);
            loaderEl.insertAfter(insertAfterSelector);
        }
        if (loaderText != null) loaderEl.find(".loaderText").text(loaderText);
        loaderEl.show();
    }

    /**
     * Hides #loader div
     */
    function hideLoader() {
        $(".hide-while-loading").show();
        $(".webjetAnimatedLoader").hide();
    }

    /**
     * Select/activate menu item with provided href attribute
     * Used in slave/details page to open menu for master/parent page URL
     * @param {*} href
     */
    function selectMenuItem (href) {
        //oznaci menu polozku podla zadaneho href atributu
        $(".md-large-menu__wrapper .md-large-menu__item").removeClass("md-large-menu__item--open md-large-menu__item--active");
        $("div.menu-wrapper div.md-main-menu--open").removeClass("md-main-menu--open");
        $("div.menu-wrapper div.md-main-menu__item--open").removeClass("md-main-menu__item--open md-main-menu__item--active");
        $("div.menu-wrapper div.md-main-menu__item__sub-menu__item--active").removeClass("md-main-menu__item__sub-menu__item--active");

        var $this = $("a[href='"+href+"']");

        $this.parents(".md-main-menu__item__sub-menu__item").addClass("md-main-menu__item__sub-menu__item--active");

        $this.parents(".md-main-menu__item").addClass("md-main-menu__item--active");
        //$this.parents(".md-main-menu__item").addClass("md-main-menu__item--open");

        $this.parents(".md-main-menu").addClass("md-main-menu--open");
        var menuId = $this.parents(".md-main-menu").data("menu-id");
        $('.md-large-menu__item__link[data-menu-id="' + menuId + '"]').parents(".md-large-menu__item").addClass("md-large-menu__item--open md-large-menu__item--active");
    }

    /**
     * Encode text to base64 including utf-8 characters
     * @param {String} text
     * @returns
     */
    function base64encode(text) {
        return Base64.encode(text);
    }

    /**
     * Decode base64 text to utf-8 characters
     * @param {String} encodedText
     * @returns
     */
    function base64decode(encodedText) {
        return Base64.decode(encodedText);
    }

    /**
     * Log debug message with timestamp and diff in ms from last log.
     * To enable/disable debug timer use debugTimer(true) as first call.
     * @param {*} message
     */
    function debugTimer(message) {
        if (typeof message === "boolean") {
            window.debugTimerEnabled = message;
            return;
        }
        if (window.debugTimerEnabled !== true) return;

        let now = new Date();
        if (typeof window.lastDebugTimer == "undefined") window.lastDebugTimer = now;
        if (typeof window.firstDebugTimer == "undefined") window.firstDebugTimer = now;
        let diff = now - window.lastDebugTimer;
        let diffFirst = now - window.firstDebugTimer;
        window.lastDebugTimer = now;
        console.log("DebugTimer: " + message + " - " + diffFirst + "ms (+ " + diff + "ms)");
    }

    return {
        showHelpWindow: (link) => {
            return showHelpWindow(link);
        },
        changeDomain: select => {
            return changeDomain(select);
        },
        /**
         * @description Vyhľadá a vráti text v aktuálne používanom jazyku na základe translate kľúča.
         * @param {string} key
         * @returns {string}
         */
        translate: (key, ...params) => {
            //console.log("translate, params=", params);
            if (typeof params != "undefined" && params != null && params.length>0) {
                return translate(key, params);
            }
            return translate(key);
        },
        openPopupDialog: (url, width, height) => {
            return openPopupDialog(url, width, height);
        },
        notify: (type, title, text, timeOut, buttons, appendToExisting=false, conatinerId=null) => {
            return toastNotify(type, title, text, timeOut, buttons, appendToExisting, conatinerId)
        },
        notifySuccess: (title, text, timeOut, buttons) => {
            return toastNotify('success', title, text, timeOut, buttons)
        },
        notifyInfo: (title, text, timeOut, buttons) => {
            return toastNotify('info', title, text, timeOut, buttons)
        },
        notifyWarning: (title, text, timeOut, buttons) => {
            return toastNotify('warning', title, text, timeOut, buttons)
        },
        notifyError: (title, text, timeOut, buttons) => {
            return toastNotify('error', title, text, timeOut, buttons)
        },
        confirm: options => {
            return confirm(options);
        },
        prompt: options => {
            options.type = "prompt";
            return confirm(options);
        },
        urlAddPath: (url, pathAppend) => {
            return urlAddPath(url, pathAppend);
        },
        urlAddParam: (url, paramName, paramValue) => {
            return urlAddParam(url, paramName, paramValue);
        },
        urlUpdateParam: (url, paramName, paramValue) => {
            return urlUpdateParam(url, paramName, paramValue);
        },
        urlGetParam: (url, queryString) => {
            return urlGetParam(url, queryString);
        },
        setJsonProperty: (obj, path, value) => {
            return setJsonProperty(obj, path, value);
        },
        getJsonProperty: (obj, path) => {
            return getJsonProperty(obj, path);
        },
        openIframeModal: options => {
            return openIframeModal(options);
        },
        closeIframeModal: () => {
            return closeIframeModal();
        },
        openIframeModalDatatable: options => {
            return openIframeModalDatatable(options);
        },
        openElFinder: (options) => {
            return openElFinder(options);
        },
        openElFinderButton: (button) => {
            //console.log("openElfinderButton, button=", button);
            //kliklo sa na button, dohladaj pridruzeny input
            let $button = $(button);
            let input = $button.parents("div.input-group").find("input.form-control");
            let title = $button.parents("div.row").find("label.col-form-label").text();
            let prepend = $button.parents("div.row").find(".input-group-text");

            let options = {
                link: input.val(),
                title: title,
                okclick: function(link) {
                    //console.log("OK click, link=", link, "input=", input);
                    input.val(link);
                    if (prepend.length>0) {
                        if (link.indexOf(".jpg")!=-1 || link.indexOf(".png")!=-1) {
                            prepend.addClass("has-image");
                            prepend.css("background-image", "url("+link+")");
                        } else {
                            prepend.removeClass("has-image");
                            prepend.css("background-image", "none");
                        }
                    }
                }
            }
            //console.log("Opening elfinder, options: ", options);
            return openElFinder(options);
        },
        internationalToEnglish: (text, whitespaceToDash) => {
            return internationalToEnglish(text, whitespaceToDash);
        },
        removeChars: text => {
            return removeChars(text);
        },
        encodeValue: value => {
            return encodeValue(value);
        },
        unencodeValue: value => {
            return unencodeValue(value);
        },
        escapeHtml: value => {
            return escapeHtml(value);
        },
        formatDate: timestamp => {
            return formatDate(timestamp);
        },
        formatDateTime: timestamp => {
            return formatDateTime(timestamp);
        },
        formatDateTimeSeconds: timestamp => {
            return formatDateTimeSeconds(timestamp);
        },
        formatTime: timestamp => {
            return formatTime(timestamp);
        },
        formatTimeSeconds: timestamp => {
            return formatTimeSeconds(timestamp);
        },
        formatPrice: price => {
            return formatPrice(price);
        },
        keepSession: () => {
            return keepSession();
        },
        keepSessionShowLogoffMessage: () => {
            return keepSessionShowLogoffMessage();
        },
        keepSessionShowTokenMessage: errorMessage => {
            return keepSessionShowTokenMessage(errorMessage);
        },
        dispatchEvent: (name, detail) => {
            return dispatchEvent(name, detail);
        },
        log: (...data) => {
            return Tools.log('info', data);
        },
        setTitle: (title) => {
            return setTitle(title);
        },
        breadcrumb: (config) => {
            return breadcrumb(config);
        },
        headerTabs: (config) => {
            return headerTabs(config);
        },
        htmlToText: (html) => {
            return htmlToText(html);
        },
        hasPermission: (permission) => {
            return hasPermission(permission);
        },
        parseMarkdown: (markdownText, options) => {
            return parseMarkdown(markdownText, options);
        },
        initTooltip: ($el, customClass = null) => {
            return initTooltip($el, customClass);
        },
        getAdminSetting: (key) => {
            return getAdminSetting(key);
        },
        setAdminSetting: (key, value) => {
            return setAdminSetting(key, value);
        },
        showLoader: (loaderText=null, insertAfter=null) => {
            return showLoader(loaderText, insertAfter);
        },
        hideLoader: () => {
            return hideLoader();
        },
        selectMenuItem: (href) => {
            return selectMenuItem(href);
        },
        debugTimer: (message) => {
            return debugTimer(message);
        },
        //DEPRECATED
        toastWarning: (type, title, text, timeOut) => {
            return toastNotify(type, title, text, timeOut)
        },
        popup: (url, width, height) => {
            return openPopupDialog(url, width, height);
        },
        popupFromDialog: (url, width, height) => {
            //toto je len kvoli WJ8 kompatibilite
            return openPopupDialog(url, width, height);
        },
        confirmRestart: () => {
            if (window.confirm(WJ.translate("admin.conf_editor.do_you_really_want_to_restart")))
            {
                $.ajax({
                    type: "POST",
                    url: "/admin/rest/settings/configuration/restart",
                    data: "act=restart&name=",
                    success: function(msg)
                    {
                        alert(WJ.translate("admin.conf_editor.restarted"));
                    }
                });
            }
        },
        fixFileName: (fileName) => {
            return fixFileName(fileName);
        },
        removeSpojky: (text) => {
            return removeSpojky(text);
        },
        base64encode: (text) => {
            return base64encode(text);
        },
        base64decode: (encodedText) => {
            return base64decode(encodedText);
        }
    };

})();

export default WJ;

//spatna kompatibilita s WJ8
window.popupFromDialog = WJ.openPopupDialog;
window.openPopupDialogFromLeftMenu = url => {
    WJ.openPopupDialog(url, 650, 550);
}
window.openLinkDialogWindow = function(formName, fieldName, requestedImageDir, requestedFileDir) {
   openElFinderDialogWindow(formName, fieldName);
}
window.openImageDialogWindow = function(formName, fieldName, requestedImageDir) {
    openElFinderDialogWindow(formName, fieldName, requestedImageDir);
}
window.openElFinderDialogWindow = function(form, elementName, requestedImageDir) {
	var url = '/admin/v9/files/dialog/';
    //console.log("openElFinderDialogWindow, form=", form, "elementName=", elementName);

	if (form != null && elementName != null) {
		url = url + "?form=" + form;
		url = url + "&elementName=" + encodeURIComponent(elementName);

        try {
            var link = null;
            if ("ckEditorDialog"==form)
			{
                var dialog = CKEDITOR.dialog.getCurrent();
                var tabNamePair = elementName.split(":");
                var element = dialog.getContentElement(tabNamePair[0], tabNamePair[1]);
                link = element.getValue();
			}
			else {
                link = document.forms[form].elements[elementName].value;
            }
            if (link != null && link!=""){
                url = url + "&link=" + encodeURIComponent(link);
            }else  if (requestedImageDir!=undefined && requestedImageDir!=null && requestedImageDir!="") url += '&link=' + requestedImageDir;
        } catch (e) { console.log(e); }
	}
	//window.alert(navigator.userAgent);
    WJ.openIframeModal({
        url: url,
        width: 800,
        height: 540,
        closeButtonPosition: "close-button-over",
        okclick: function() {
            //console.log("OK clicked");
            return document.getElementById("modalIframeIframeElement").contentWindow.Ok();
        }
    });
}