let dropZoneId = null;
let adminUpload = null;
let interval = null;

function cleanUploader() {
    $("#" + dropZoneId).show();

    //Remove all files + force stopn uploading actual files
    if(adminUpload != null) adminUpload.removeAllFiles(true);

    //Remove upload toaster
    $("#toast-container-upload").html("");

    //Hide whole upload wrapper
    $("#upload-wrapper").hide();
}

function prepareUploader(conf) {
    let id = conf._id;

    let dropzone = $("#" + id);
    if(dropzone.length > 0) {
        clearInterval(interval);

        adminUpload = window.AdminUpload({
            element: "#" + id,
            destinationFolder: '/files/protected/',
            writeDirectlyToDestination: false,
        });

        let dteSubmitButton = "div.DTE .DTE_Footer button.btn-primary";

        window.addEventListener('WJ.AdminUpload.success', (e) => {
            //console.log("WJ.AdminUpload.success", e.detail);

            //Save uploaded file key
            conf.uploadedFileKey = e.detail.key;

            //We have uploaded new file, enable save button
            $(dteSubmitButton).prop('disabled', false);
        });

        window.addEventListener('WJ.AdminUpload.error', (e) => {
            //console.log("WJ.AdminUpload.error", e.detail);
            $(dteSubmitButton).prop('disabled', false);
        });

        window.addEventListener('WJ.AdminUpload.addedfile', () => {
            //console.log("WJ.AdminUpload.addedfile");
            $("#" + dropZoneId).hide();

            //We are uploading new file, disable save button until upload is finished
            $(dteSubmitButton).prop('disabled', true);

            //Find cancel button and add event listener
            let cancelButton = $("#upload-wrapper > div.input-group > button");
            cancelButton.on("click", function() {
                //We have canceled upload, enable save button
                $(dteSubmitButton).prop('disabled', false);
            });
        });
    }
}

export function typeWjupload() {
    return {
        create: function ( conf ) {
            var id = $.fn.dataTable.Editor.safeId(conf.id);
            var htmlCode = $(
                '<div id="' + id + '" class="drop-zone-box dropzone form-control" style="align-content: center;"></div>' +
                '<div class="upload-wrapper" id="upload-wrapper" style="display: none">' +
                    '<div class="toast-container-progress">' +
                        '<span>' + WJ.translate("admin.welcome.feedback.dialog.uploaded_files.js") + '</span>' +
                        '<svg class="fa-progress-bar float-end" xmlns="http://www.w3.org/2000/svg" viewBox="-1 -1 34 34">' +
                            '<circle cx="16" cy="16" r="15" class="fa-progress-bar__background"/>' +
                            '<circle cx="16" cy="16" r="15" class="fa-progress-bar__progress" style="stroke-dashoffset: 100px"/>' +
                        '</svg>' +
                    '</div>' +
                    '<div class="input-group">' +
                        '<div id="toast-container-upload" class="form-control"></div>' +
                        '<button class="btn btn-outline-secondary" type="button"><i class="ti ti-x"></i></button>' +
                    '</div>' +
                '</div>' +
                '<div id="upload-toastr-template" style="display: none">' +
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
                cleanUploader();
            });

            return htmlCode;
        },

        get: function ( conf ) {
            return conf.uploadedFileKey;
        },

        set: function ( conf, val ) {
            //Clean uploader
            cleanUploader();

            if(dropZoneId == null) dropZoneId = conf._id;

            if(interval == undefined || interval == null) {
                interval = setInterval(prepareUploader, 500, conf);
            }
        }
    }
}