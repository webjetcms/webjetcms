/*
Drag & Drop upload pre Datatables, priklad pouzitia:

    var adminUpload = window.AdminUpload({
        element: "#dt-upload",
        uploadType: "image",
        acceptedFiles: ".jpg,.jpeg,.mp4,.gif,.png",
        destinationFolder: "/images/gallery/test/subfolder1",
        writeDirectlyToDestination: true
    });
    window.addEventListener("WJ.AdminUpload.success", function(e) {
        console.log("Upload success", e);
        //refreshni DT
        galleryTable.ajax.reload();
    });

    div(id="dt-upload" class="drop-zone-box dropzone " style="visibility:hidden; opacity:0")

vyuziva dropzonejs a cez fintu pri evente dragenter zobrazi overlay ponad okno pre moznost dropnutia suboru
lokalny drag&drop v okne (napr. presun v stromovej strukture) sa detekuje pomocou dragstart, vtedy je zobrazenie overlayu pre drop suboru bloknute
nabinduje sa aj na + tlacitko v datatabulke

pouziva toastr pre zobrazenie moznosti preskocit/prepisat/ponechat obe ak subor uz existuje
*/

import $ from 'jquery';
import Dropzone from 'dropzone/dist/dropzone.js';

Dropzone.autoDiscover = false;
function adminUploadInit(options) {
    var element = document.querySelector(options.element) || options.element;
    var maxFiles = options.maxFiles || null;
    var acceptedFiles = options.acceptedFiles || null;
    var uploadType = options.uploadType || '';
    var destinationFolder = options.destinationFolder || '';
    var writeDirectlyToDestination = options.writeDirectlyToDestination || false;
    var overwriteMode = options.overwriteMode || '';

    var toastrMessageTemplate = $('#upload-toastr-template').html();
    let fromImageEditor = false;

    //console.log("maxFiles=", maxFiles);

    var adminUpload = new Dropzone(element, {
        url: '/admin/upload/chunk',
        params: function (files, xhr, chunk) {
            var retParams = {};

            if (chunk) {
                retParams = {
                    dzuuid: chunk.file.upload.uuid,
                    dzchunkindex: chunk.index,
                    dztotalfilesize: chunk.file.size,
                    dzchunksize: this.options.chunkSize,
                    dztotalchunkcount: chunk.file.upload.totalChunkCount,
                    dzchunkbyteoffset: chunk.index * this.options.chunkSize,
                };
            }

            //console.log("retparams1=", retParams);

            retParams.uploadType = uploadType;
            retParams.destinationFolder = destinationFolder;
            retParams.writeDirectlyToDestination = writeDirectlyToDestination;
            retParams.overwriteMode = overwriteMode;

            //console.log("returning params=", retParams);

            return retParams;
        },
        createImageThumbnails: false,
        parallelUploads: 1,
        uploadMultiple: false,
        maxFilesize: 5000, //<%=Tools.replace(Constants.getString("stripes.FileUpload.MaximumPostSize"), "m", "")%>,

        maxFiles: maxFiles,
        acceptedFiles: acceptedFiles,

        addRemoveLinks: true,

        chunking: true,
        chunkSize: 2000000,
        forceChunking: true,

        dictDefaultMessage: '<i class="ti ti-upload"></i>',
        headers: {
            'X-CSRF-TOKEN': window.csrfToken,
        },

        /*dictDefaultMessage: "<iwcm:text key="admin.dragDropFiles.dragFilesHereOrClick"/>",
        dictFileTooBig: "<iwcm:text key="admin.dragDropFiles.dictFileTooBig"/>",
        dictResponseError: "<iwcm:text key="admin.dragDropFiles.dictResponseError"/>",
        dictCancelUpload: "<iwcm:text key="admin.dragDropFiles.dictCancelUpload"/>",
        dictCancelUploadConfirmation: "<iwcm:text key="admin.dragDropFiles.dictCancelUploadConfirmation"/>",
        dictRemoveFile: "<iwcm:text key="admin.dragDropFiles.dictRemoveFile"/>",
        dictMaxFilesExceeded: "<iwcm:text key="admin.dragDropFiles.dictMaxFilesExceeded"/>",
        */

        init: function () {
            $(document).on('initAddedFileFromImageOutside', (e, file) => {
                fromImageEditor = true;
                this.emit('addedfile', file);
                setStatus(file.toaster, "processing");
            });

            $(document).on('initUploadProgressFromOutside', (e, file) => {
                this.emit('uploadprogress', file);
            });

            $(document).on('initErrorMessageFromOutside', (e, file, errorMessage) => {
                this.emit('error', file, errorMessage);
            });

            this.on('success', function (file) {
                // console.log('success', file);
                if (fromImageEditor) {
                    updateOverallProgress();
                    setStatus(file.toaster, 'success');
                    fromImageEditor = false;
                    return;
                }
                var response = JSON.parse(file.xhr.response);
                //console.log("Response: ", response);
                //console.log("queuedFiles=", this.getQueuedFiles().length, " uploadingFiles=", this.getUploadingFiles().length);

                updateOverallProgress();

                if (response.error) {
                    setError(file.toaster, response.error);
                    return;
                }

                //console.log("Upload success, file=", file);
                var key = response.key;
                //console.log("key=", key);

                if (response.destinationFolder && response.destinationFolder!="" && response.exists === true)
                {
                    var overwriteToast = file.toaster;

                    setStatus(file.toaster, 'exist');
                    $('div.upload-wrapper-footer div.process-all').show();

                    function callback(ajaxResponse) {
                        if (ajaxResponse.error) {
                            setError(file.toaster, ajaxResponse.error);
                        } else {
                            setStatus(file.toaster, 'success');
                            //console.log("Callback, ajaxResponse=",ajaxResponse," response=", response, "key=",key," file=",file);
                            if ('skip' != overwriteMode) fireUploadSuccessEvent(response, key, file);
                        }
                        //sprocesuj dalsi toast ak caka na applyToAll
                        processApplyToAllNext();
                    }

                    overwriteToast.delegate('.btn-toast-skip', 'click', function () {
                        adminUpload.skipkey(response.key, callback);
                        setStatus(overwriteToast, 'processing');
                    });

                    overwriteToast.delegate('.btn-toast-overwrite', 'click', function () {
                        adminUpload.overwrite(
                            response.key,
                            response.destinationFolder,
                            response.name,
                            response.uploadType,
                            callback
                        );
                        setStatus(overwriteToast, 'processing');
                    });

                    overwriteToast.delegate('.btn-toast-keepboth', 'click', function () {
                        adminUpload.keepboth(
                            response.key,
                            response.destinationFolder,
                            response.name,
                            response.uploadType,
                            callback
                        );
                        setStatus(overwriteToast, 'processing');
                    });

                    return;
                }

                setStatus(file.toaster, "success");
                fireUploadSuccessEvent(response, key, file);
            });
            this.on('error', function (file, errorMessage) {
                // console.log("ERROR: file=",file," message=",errorMessage);
                setError(file.toaster, errorMessage);
            });
            this.on('removedfile', function (file) {
                //nebudeme riesit
                //console.log("Removed file, file=", file);
            });
            this.on('addedfile', function (file) {
                //firni event ze zacina addedfile
                var event = new CustomEvent('WJ.AdminUpload.addedfile', {});

                window.dispatchEvent(event);
                // console.log('addedFile', file);

                file.toaster = createFileToaster(file);


                setStatus(file.toaster, 'progress');
            });

            this.on('uploadprogress', function (file, progress) {
                // console.log('uploadprogress', file);
                //console.log("Upload progress, file=", file.upload);
                //console.log("Progress: ", file.name, " ", file.status, " ", file.upload.progress, " dz=", file.previewElement.querySelectorAll("[data-dz-uploadprogress]")[0].style.width, " prog=", progress);
                const dhis = this;
                //cez timeout pretoze progress hlasi 100 po kazdom chunku, takto je sanca, ze sa to medzi tym zmeni
                setTimeout(function () {
                    var progress = file.upload.progress;
                    if (fromImageEditor && progress == 100) {
                        dhis.emit('success', file);
                    }

                    //console.log("Setting progress: ", progress);
                    try {
                        //console.log("Progress 1: ",file.toaster.find(".fa-progress-bar__progress"));
                        file.toaster
                            .find('.fa-progress-bar__progress')
                            .css('stroke-dashoffset', Math.round(100 - progress) + 'px');
                        updateOverallProgress();

                        if (file.toaster.attr("data-upload-status")!="exist") {
                            //console.log("progress=", progress);
                            //nastavujeme processing, lebo na serveri sa este nieco deje
                            if (progress == 100) {
                                //lebo sa nam to vyvola 2x, pri komplet uploade a potom este po skonceni requestu (resize chvilu trva)
                                if ("processing"==file.toaster.attr("data-upload-status")) setStatus(file.toaster, "success");
                                else if ("progress"==file.toaster.attr("data-upload-status")) setStatus(file.toaster, "processing");
                            }
                            else setStatus(file.toaster, "progress");
                        }

                    } catch(e) {console.log(e);}
                }, 300);
            });

            this.on('totaluploadprogress', function (uploadProgress, totalBytes, totalBytesSent) {
                //toto vracalo bludy, neda sa pouzit
                //console.log("totaluploadprogress=",uploadProgress," totalBytes=",totalBytes," totalBytesSent=",totalBytesSent);
                //console.log("queuedFiles=", this.getQueuedFiles().length, " uploadingFiles=", this.getUploadingFiles().length);
            });
        },
    });

    function createFileToaster(file) {
        $('#upload-wrapper').show();
        $('#dt-upload').css('visibility', 'hidden');
        $('#dt-upload').css('opacity', 0);

        var message = toastrMessageTemplate.replace('{FILE_NAME}', file.name);
        let toaster = toastr.info(message, '', {
            closeButton: true,
            timeOut: 0,
            toastClass: 'toast toast-dzpreview',
            tapToDismiss: false,
            extendedTimeOut: 0, //prevent hide after mouse over
            progressBar: false,
            containerId: 'toast-container-upload',
        });

        //scrollni kontajner
        $('#toast-container-upload').scrollTop(0);

        return toaster;
    }

    function updateOverallProgress() {
        //vypocitaj to podla ciastkovych progressbarov
        var progressSum = 0;
        var progressCount = 0;
        $('#toast-container-upload .fa-progress-bar__progress').each(function (index, value) {
            var bar = $(value);
            var percent = 100 - parseInt(bar.css('stroke-dashoffset'));

            progressCount++;
            progressSum += percent;
        });
        var total = 100 * progressCount;
        var progress = 0;
        if (total > 0) progress = (progressSum / total) * 100;

        $('div.toast-container-progress .fa-progress-bar__progress').css(
            'stroke-dashoffset',
            Math.round(100 - progress) + 'px'
        );
    }

    function fireUploadSuccessEvent(response, key, file) {
        setStatus(file.toaster, 'success');

        //firni event
        var event = new CustomEvent('WJ.AdminUpload.success', {
            detail: {
                response: response,
                key: key,
                file: file,
            },
        });

        window.dispatchEvent(event);
    }

    function callRestService(url, params, callback) {
        $.post({
            url: url,
            data: params,
            success: callback,
        });
    }

    function setStatus(toast, status) {
        //console.log("Setting status ", status, "to", toast);
        toast.attr("data-upload-status", status);
    }

    function setError(toast, message) {
        setStatus(toast, 'error');
        //console.log(toast.find('div.toast-error-message'));
        let errorDiv = toast.find('div.toast-error-message');
        if (errorDiv.length==0) {
            toast.append('<div class="toast-error-message"></div>');
            errorDiv = toast.find('div.toast-error-message');
        }
        errorDiv.html(message);
    }

    function processAplyToAll(mode) {
        overwriteMode = mode;

        //oznac vsetky exists na waitforprocess
        $('div.toast[data-upload-status=exist]').each(function (index, value) {
            console.log(value);
            var toast = $(value);
            setStatus(toast, 'waitforprocess');
        });

        //oznac button v paticke
        $('div.upload-wrapper-footer div.toast-links a').each(function (index, value) {
            $(value).removeClass('active');
        });
        $('#btn-toast-' + mode + '-all').addClass('active');

        //spusti nasledujuci v rade
        processApplyToAllNext();
    }

    function processApplyToAllNext() {
        if (overwriteMode.length > 1) {
            var waitForProcess = $('div.toast[data-upload-status=waitforprocess]');
            if (waitForProcess.length > 0) {
                //procesujeme max 1 toast
                var toast = $(waitForProcess[0]);
                setStatus(toast, 'processing');
                //ochrana pred 429 too many requests
                setTimeout(function () {
                    toast.find('.btn-toast-' + overwriteMode).trigger('click');
                }, 500);
            }
        }
    }

    adminUpload.skipkey = function (key, callback) {
        var url = '/admin/upload/skipkey';
        var params = {
            fileKey: key,
        };

        callRestService(url, params, callback);
    };

    adminUpload.overwrite = function (key, destinationFolder, fileName, uploadType, callback) {
        var url = '/admin/upload/overwrite';
        var params = {
            fileKey: key,
            destinationFolder: destinationFolder,
            fileName: fileName,
            uploadType: uploadType,
        };

        callRestService(url, params, callback);
    };

    adminUpload.keepboth = function (key, destinationFolder, fileName, uploadType, callback) {
        var url = '/admin/upload/keepboth';
        var params = {
            fileKey: key,
            destinationFolder: destinationFolder,
            fileName: fileName,
            uploadType: uploadType,
        };

        callRestService(url, params, callback);
    };

    adminUpload.setDestinationFolder = function (newDetinationFolder) {
        destinationFolder = newDetinationFolder;
    };

    adminUpload.getDestinationFolder = function () {
        return destinationFolder;
    };

    $('#btn-toast-skip-all').click(function () {
        processAplyToAll('skip');
    });
    $('#btn-toast-overwrite-all').click(function () {
        processAplyToAll('overwrite');
    });
    $('#btn-toast-keepboth-all').click(function () {
        processAplyToAll('keepboth');
    });

    function hideUploadWrapper() {
        $('#upload-wrapper').hide();
    }

    $('#upload-wrapper-close').click(function () {
        hideUploadWrapper();
    });

    //console.log("adminUpload", adminUpload);
    return adminUpload;
}

//Drop blok sa zobrazi az ked nieco skusam dragovat ponad obrazovku, vtedy sa zobrazi fullscreen overlay pre moznost dropnutia
/* lastTarget is set first on dragenter, then
compared with during dragleave. */
var lastTarget = null;
var blockDragEnter = false;

window.addEventListener('dragstart', function (e) {
    //console.log("dragstart, e=", e);
    blockDragEnter = true;
});

window.addEventListener('dragend', function (e) {
    //console.log("dragend, e=", e);
    blockDragEnter = false;
});

window.addEventListener('dragenter', function (e) {
    //console.log("dragenter, e=", e, " target=", e.target);

    if (blockDragEnter) return;

    lastTarget = e.target; // cache the last target here
    // unhide our dropzone overlay
    $('#dt-upload').css('visibility', '');
    $('#dt-upload').css('opacity', 1);
});

window.addEventListener('dragleave', function (e) {
    // this is the magic part. when leaving the window,
    // e.target happens to be exactly what we want: what we cached
    // at the start, the dropzone we dragged into.
    // so..if dragleave target matches our cache, we hide the dropzone.
    // `e.target === document` is a workaround for Firefox 57

    //console.log("dragleave, lastTarget=", lastTarget, " target=", e.target);

    if (lastTarget == null) return;

    if (e.target === lastTarget || e.target === document) {
        setTimeout(function () {
            $('#dt-upload').css('visibility', 'hidden');
            $('#dt-upload').css('opacity', 0);
        }, 5000);
    }
});

window.AdminUpload = adminUploadInit;
