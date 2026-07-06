function resolveUploadMaxFiles(conf) {
    // UPLOAD column type is single-file by default.
    let maxFiles = 1;

    // Backward-compatible toggle via @DataTableColumn(className = "...").
    // - wjupload-multiple => unlimited files (Dropzone maxFiles = null)
    // - wjupload-single   => exactly one file
    if (conf != null && typeof conf.className === "string") {
        if (conf.className.indexOf("wjupload-multiple") !== -1) {
            maxFiles = null;
        } else if (conf.className.indexOf("wjupload-single") !== -1) {
            maxFiles = 1;
        }
    }

    // Preferred fine-grained config via editor attrs.
    // data-dt-field-upload-mode has precedence over className.
    if (conf != null && conf.attr != null) {
        const uploadMode = conf.attr["data-dt-field-upload-mode"];
        if (uploadMode === "multiple") {
            maxFiles = null;
        } else if (uploadMode === "single") {
            maxFiles = 1;
        }

        // Optional explicit numeric cap (e.g. 3) has highest precedence.
        const configuredMaxFiles = conf.attr["data-dt-field-upload-max-files"];
        if (configuredMaxFiles != null && configuredMaxFiles !== "") {
            const parsedMaxFiles = parseInt(configuredMaxFiles, 10);
            if (!Number.isNaN(parsedMaxFiles) && parsedMaxFiles > 0) {
                maxFiles = parsedMaxFiles;
            }
        }
    }

    // Returned value is passed directly to AdminUpload/Dropzone as maxFiles.
    return maxFiles;
}

function bindUploadLimitGuard(uploadInstance, maxFiles) {
    if (uploadInstance == null || maxFiles == null || maxFiles <= 0) {
        return;
    }

    // Extra defensive guard: Dropzone maxFiles usually blocks this already,
    // but drag-and-drop can still queue multiple files in some edge cases.
    uploadInstance.on("addedfile", function(file) {
        if (uploadInstance.files.length > maxFiles) {
            uploadInstance.removeFile(file);
        }
    });

    uploadInstance.on("maxfilesexceeded", function(file) {
        uploadInstance.removeFile(file);
    });
}

function cleanUploader(conf) {
    $("#" + conf._id).show();

    //Remove all files + force stop uploading actual files
    if(conf._adminUpload != null) conf._adminUpload.removeAllFiles(true);

    //Remove event listeners from previous upload session
    if (conf._eventListeners) {
        for (const [event, handler] of conf._eventListeners) {
            window.removeEventListener(event, handler);
        }
        conf._eventListeners = null;
    }

    //Remove upload toaster
    conf._input.find(".toast-container-upload").html("");

    //Hide whole upload wrapper
    conf._input.filter(".upload-wrapper").hide();
}

function prepareUploader(conf) {
    let id = conf._id;

    let dropzone = $("#" + id);
    if(dropzone.length > 0) {
        clearInterval(conf._interval);
        conf._interval = null;

        const maxFiles = resolveUploadMaxFiles(conf);

        const uploadInstance = window.AdminUpload({
            element: "#" + id,
            destinationFolder: '/files/protected/upload/',
            writeDirectlyToDestination: false,
            maxFiles: maxFiles,
        });
        conf._adminUpload = uploadInstance;

        bindUploadLimitGuard(uploadInstance, maxFiles);

        let dteSubmitButton = "div.DTE .DTE_Footer button.btn-primary";

        //Store listeners so they can be removed on cleanup
        conf._eventListeners = [];

        const onSuccess = (e) => {
            if (!e.detail || !e.detail.uploader) return;
            if (e.detail.uploader !== uploadInstance) return;

            //Save uploaded file key
            conf.uploadedFileKey = e.detail.key;

            //We have uploaded new file, enable save button
            $(dteSubmitButton).prop('disabled', false);
        };

        const onError = (e) => {
            if (!e.detail || !e.detail.uploader) return;
            if (e.detail.uploader !== uploadInstance) return;

            $(dteSubmitButton).prop('disabled', false);
        };

        const onAddedfile = (e) => {
            if (!e.detail || !e.detail.uploader) return;
            if (e.detail.uploader !== uploadInstance) return;

            $("#" + conf._id).hide();

            //We are uploading new file, disable save button until upload is finished
            $(dteSubmitButton).prop('disabled', true);

            //Find cancel button and add event listener
            let cancelButton = conf._input.find(".upload-wrapper > div.input-group > button");
            cancelButton.off("click.wjupload").on("click.wjupload", function() {
                //We have canceled upload, enable save button
                $(dteSubmitButton).prop('disabled', false);
            });
        };

        window.addEventListener('WJ.AdminUpload.success', onSuccess);
        window.addEventListener('WJ.AdminUpload.error', onError);
        window.addEventListener('WJ.AdminUpload.addedfile', onAddedfile);

        conf._eventListeners.push(['WJ.AdminUpload.success', onSuccess]);
        conf._eventListeners.push(['WJ.AdminUpload.error', onError]);
        conf._eventListeners.push(['WJ.AdminUpload.addedfile', onAddedfile]);
    }
}

export function typeWjupload() {
    return {
        create: function ( conf ) {
            var id = $.fn.dataTable.Editor.safeId(conf.id);
            var htmlCode = $(
                '<div id="' + id + '" class="drop-zone-box dropzone form-control" style="align-content: center;"></div>' +
                '<div class="upload-wrapper" id="' + id + '-upload-wrapper" style="display: none">' +
                    '<div class="toast-container-progress">' +
                        '<span>' + WJ.translate("admin.welcome.feedback.dialog.uploaded_files.js") + '</span>' +
                        '<svg class="fa-progress-bar float-end" xmlns="http://www.w3.org/2000/svg" viewBox="-1 -1 34 34">' +
                            '<circle cx="16" cy="16" r="15" class="fa-progress-bar__background"/>' +
                            '<circle cx="16" cy="16" r="15" class="fa-progress-bar__progress" style="stroke-dashoffset: 100px"/>' +
                        '</svg>' +
                    '</div>' +
                    '<div class="input-group">' +
                        '<div id="' + id + '-toast-container-upload" class="toast-container-upload form-control"></div>' +
                        '<button class="btn btn-outline-secondary" type="button"><i class="ti ti-x"></i></button>' +
                    '</div>' +
                '</div>' +
                '<div id="' + id + '-upload-toastr-template" class="upload-toastr-template" style="display: none">' +
                    '<i class="ti ti-polaroid"></i>' +
                   ' <span>{FILE_NAME}</span>' +
                   ' <i class="ti ti-circle-check float-end"></i>' +
                   ' <i class="ti ti-alert-triangle float-end"></i>' +
                   '<i class="ti ti-loader-2 ti-spin float-end"></i>' +
                   '<i class="ti ti-alert-circle float-end"></i>' +
                    '<svg class="fa-progress-bar float-end" xmlns="http://www.w3.org/2000/svg" viewBox="-1 -1 34 34">' +
                        '<circle cx="16" cy="16" r="15" class="fa-progress-bar__background"/>' +
                        '<circle cx="16" cy="16" r="15" class="fa-progress-bar__progress" style="stroke-dashoffset: 100px"/>' +
                    '</svg>' +
                '</div>'
            );

            conf._id = id;
            conf._input = htmlCode;

            conf._input.find(".btn-outline-secondary").on("click", function() {
                cleanUploader(conf);
            });

            return htmlCode;
        },

        get: function ( conf ) {
            return conf.uploadedFileKey;
        },

        set: function ( conf, val ) {
            //Clean uploader
            cleanUploader(conf);

            //Clear any previous interval before starting a new one
            if (conf._interval != null) {
                clearInterval(conf._interval);
                conf._interval = null;
            }
            conf._interval = setInterval(prepareUploader, 500, conf);
        }
    }
}
