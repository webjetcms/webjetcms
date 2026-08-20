<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Constants" %>
<%@ page import="sk.iway.iwcm.PageParams" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

PageParams pp = new PageParams(request);
%>
<c:if test="${not requestScope.dropzone}">
    <script type="text/javascript" src="/components/_common/upload/dropzone.js"></script>
    <link rel="stylesheet" href="/components/_common/upload/dropzone.css">
    <style type="text/css">
        div.WebJETMailWrapper .dropzone {
            text-align: left;
            padding: 0px;
            min-height: auto;
            border: 0px;
        }

    </style>
    <script type="text/javascript">

        Dropzone.autoDiscover = false;

        $(document).ready(function() {

            $(".wjdropzone").each(function() {

                var element = $(this);

                var initialized = element.attr("data-dropzone-initialized");;
                if (initialized == "true") {
                    //console.log("Dropzone already initialized for this element, skipping.");
                    return;
                }
                element.attr("data-dropzone-initialized", "true");

                var dzmaxfiles = element.data("dzmaxfiles") || 25;
                //console.log("dzmaxfiles="+dzmaxfiles);

                var singleFile = element.hasClass("wjdropzone-single-file");

                var acceptedFiles = element.data("dzacceptedfiles") || null;
                //console.log("acceptedFiles="+acceptedFiles);

                //tu na backende plnime mena suborov, pole sa vo frontend verzii schova
                element.find('input.uploadedObjectsNames').hide();

                element.dropzone({
                    url: "/XhrFileUpload",
                    createImageThumbnails: !singleFile,
                    clickable: singleFile ? element.find(".dz-message")[0] : true,
                    parallelUploads: 1,
                    uploadMultiple: false,
                    maxFilesize: <%=Tools.replace(Constants.getString("stripes.FileUpload.MaximumPostSize"), "m", "")%>,

                    maxFiles: dzmaxfiles,
                    acceptedFiles: acceptedFiles,

                    addRemoveLinks: !singleFile,

                    chunking: true,
                    chunkSize: 2000000,
                    forceChunking: true,

                    previewTemplate: singleFile ? '<div class="dz-preview d-flex align-items-center flex-wrap gap-2"><span class="small text-muted" data-dz-name></span><span class="dz-error-message text-danger small"><span data-dz-errormessage></span></span><button type="button" class="btn btn-outline-danger btn-sm" data-dz-remove><iwcm:text key="admin.dragDropFiles.dictRemoveFile"/></button></div>' : Dropzone.prototype.defaultOptions.previewTemplate,
                    dictDefaultMessage: "<iwcm:text key="admin.dragDropFiles.dragFilesHereOrClick"/>",
                    dictFileTooBig: "<iwcm:text key="admin.dragDropFiles.dictFileTooBig"/>",
                    dictResponseError: "<iwcm:text key="admin.dragDropFiles.dictResponseError"/>",
                    dictCancelUpload: "<iwcm:text key="admin.dragDropFiles.dictCancelUpload"/>",
                    dictCancelUploadConfirmation: "<iwcm:text key="admin.dragDropFiles.dictCancelUploadConfirmation"/>",
                    dictRemoveFile: "<iwcm:text key="admin.dragDropFiles.dictRemoveFile"/>",
                    dictMaxFilesExceeded: "<iwcm:text key="admin.dragDropFiles.dictMaxFilesExceeded"/>",

                    init: function()
                    {
                        var myDropzone = this;
                        var uploadedFiles = {};
                        var singleFileLocked = false;
                        var setSingleFileLocked = function(locked) {
                            if (singleFile == false || singleFileLocked == locked) return;

                            singleFileLocked = locked;
                            element.find(".dz-message").prop("disabled", locked);
                            if (locked) myDropzone.disable();
                            else myDropzone.enable();
                        };
                        //console.log("uploadedObjectsInfo1=" + element.find('input.uploadedObjectsInfo')[0].value);
                        setTimeout(function() {
                            //console.log("uploadedObjectsInfo2=" + element.find('input.uploadedObjectsInfo')[0].value);
                            if(element.find('input.uploadedObjectsInfo').length > 0 && element.find('input.uploadedObjectsInfo')[0].value != undefined && element.find('input.uploadedObjectsInfo')[0].value != '') {
                                //console.log("nasetovane");
                                var prepopulateWithFiles = element.find('input.uploadedObjectsInfo')[0].value;

                            }

                            if(typeof prepopulateWithFiles != 'undefined' && prepopulateWithFiles != undefined && prepopulateWithFiles != 'undefined') {
                                var files = JSON.parse(prepopulateWithFiles);
                                uploadedFiles = JSON.parse(prepopulateWithFiles);

                                for(var i in files) {
                                    myDropzone.emit("addedfile", files[i]);
                                    myDropzone.files.push(files[i]);
                                }
                                if (myDropzone.files.length > 0) setSingleFileLocked(true);
                            }
                        }, 700);


                        this.on("success", function(file)
                        {
                            //console.log("Upload success, file: ", file);

                            var response = JSON.parse(file.xhr.response);
                            var key = response.key;

                            //console.log("response: ", response, "key: ", key, "element: ", element);

                            var input = element.find("input");
                            var uploadedObjectsInfo = element.find('input.uploadedObjectsInfo');

                            if (response.success === false) {
                                this.removeFile(file);
                                if (response.error) window.alert(response.error);
                                return;
                            }

                            if (input.length>0 && response.success === true)
                            {
                                if (input[0].value == "" || dzmaxfiles==1) input[0].value = key;
                                else input[0].value = input[0].value + ";" + key;

                                if(uploadedObjectsInfo.length > 0) {
                                    //console.log("added!");
                                    //console.log(uploadedFiles);
                                    uploadedFiles[key] = response;
                                    if(uploadedFiles[key].name) uploadedFiles[key].name = uploadedFiles[key].name[0];
                                    if(uploadedFiles[key].key) uploadedFiles[key].key = uploadedFiles[key].key[0];
                                    uploadedObjectsInfo[0].value = JSON.stringify(uploadedFiles);
                                }
                            }

                            if (response.success === true) setSingleFileLocked(true);
                        });
                        this.on("maxfilesexceeded", function(file) {
                            if (singleFile) this.removeFile(file);
                        });
                        this.on("removedfile", function(file)
                        {
                            /*
                            console.log("removedfile");
                            console.log(file);
                            */

                            if (typeof file.xhr == 'undefined') {
                                var key = file.key;

                                var input = element.find("input");
                                var uploadedObjectsInfo = element.find('input.uploadedObjectsInfo');
                                if (input.length>0)
                                {
                                    //console.log("before delete " + input[0].value);
                                    var values = input[0].value.split(";");
                                    var newValues = new Array();
                                    //console.log(values);
                                    for (var i=0; i<values.length; i++)
                                    {
                                        if (values[i]==key) continue;
                                        newValues.push(values[i]);
                                    }
                                    input[0].value = newValues.join(";");
                                    //console.log("after delete " + input[0].value);

                                    delete uploadedFiles[key];
                                    if (uploadedObjectsInfo != null) {
                                        //console.log("uploadedObjectsInfo=", uploadedObjectsInfo, "uploadedFiles=", uploadedFiles);
                                        uploadedObjectsInfo[0].value = JSON.stringify(uploadedFiles);
                                    }

                                    element.trigger("removedFileAfter", file);
                                    /*
                                    console.log("removed!");
                                    console.log(uploadedFiles);
                                    */
                                }
                                setSingleFileLocked(false);
                                return;
                            };

                            var response = JSON.parse(file.xhr.response);
                            var key = response.key;

                            var input = element.find("input");
                            var uploadedObjectsInfo = element.find('input.uploadedObjectsInfo');
                            if (input.length>0)
                            {
                                //console.log("before delete " + input[0].value);
                                var values = input[0].value.split(";");
                                var newValues = new Array();
                                //console.log(values);
                                for (var i=0; i<values.length; i++)
                                {
                                    if (values[i]==key) continue;
                                    newValues.push(values[i]);
                                }
                                input[0].value = newValues.join(";");
                                //console.log("after delete " + input[0].value);

                                delete uploadedFiles[key];

                                if (uploadedObjectsInfo != null) {
                                    //console.log("uploadedObjectsInfo=", uploadedObjectsInfo, "uploadedFiles=", uploadedFiles);
                                    uploadedObjectsInfo[0].value = JSON.stringify(uploadedFiles);
                                }

                                //console.log("removed!");
                                //console.log(uploadedFiles);
                                element.trigger("removedFileAfter", file);
                            }
                            setSingleFileLocked(false);
                        });

                        this.on('addedfile', function(file) {

                            var ext = file.name.split('.').pop();
                            var knownIcons = ["doc", "docx", "mp3", "mp4", "pdf", "ppt", "pptx", "tiff", "xls", "xlsx", "zip"];

                            for (var i=0; i<knownIcons.length; i++)
                            {
                                if (ext == knownIcons[i]) {
                                    $(file.previewElement).find(".dz-image img").attr("src", "/components/_common/upload/icons/"+knownIcons[i]+".png");
                                    break;
                                }
                            }
                        });

                        element.on('resetFiles', function() {
                            //console.log("ResetFiles event, uploadedFiles=", uploadedFiles);
                            myDropzone.removeAllFiles();
                        });
                    }
                });
            });
        });
    </script>
    <% request.setAttribute("dropzone", Boolean.TRUE);%>
</c:if>
