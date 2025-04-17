<template>
    <div>
        <div ref="modal" id="feedback_modal" class="modal fade DTED" aria-modal="true" role="dialog">
            <form @submit.prevent="sendForm()" class="form-horizontal">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <div class="row">
                                <div class="col-sm-6">
                                    <h5 class="modal-title">{{ this.$WJ.translate('admin.welcome.feedback.dialog.title.js') }}</h5>
                                </div>
                            </div>
                        </div>
                        <div class="modal-body">
                            <div class="modal-body-bg">
                                <div class="from-content">
                                    <div class="DTE_Field form-group row required">
                                        <label class="col-sm-4 col-form-label">{{ this.$WJ.translate('admin.welcome.feedback.dialog.feedback_text.js') }} </label>
                                        <div class="col-sm-7">
                                            <div class="DTE_Field_InputControl">
                                                <textarea
                                                    v-model="feedback.text"
                                                    class="form-control"
                                                    id="feedback-group-text"
                                                    cols="30"
                                                    rows="7"
                                                ></textarea>
                                                <div ref="text-error" class="form-text text-danger small invisible">
                                                    {{ this.$WJ.translate('admin.welcome.feedback.dialog.error.js') }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="DTE_Field form-group row">
                                        <label class="col-sm-4 col-form-label">{{ this.$WJ.translate('admin.welcome.feedback.dialog.files.js') }} </label>
                                        <div class="col-sm-7">
                                            <div id="feedback-upload" class="drop-zone-box dropzone"></div>
                                            <div class="upload-wrapper" id="upload-wrapper" style="display: none">
                                                <div class="toast-container-progress">
                                                    <span>{{ this.$WJ.translate('admin.welcome.feedback.dialog.uploaded_files.js') }}</span>
                                                    <svg
                                                        class="fa-progress-bar float-end"
                                                        xmlns="http://www.w3.org/2000/svg"
                                                        viewBox="-1 -1 34 34"
                                                    >
                                                        <circle
                                                            cx="16"
                                                            cy="16"
                                                            r="15"
                                                            class="fa-progress-bar__background"
                                                        />
                                                        <circle
                                                            cx="16"
                                                            cy="16"
                                                            r="15"
                                                            class="fa-progress-bar__progress"
                                                            style="stroke-dashoffset: 100px"
                                                        />
                                                    </svg>
                                                </div>
                                                <div id="toast-container-upload"></div>
                                            </div>
                                            <div id="upload-toastr-template" style="display: none">
                                                <i class="ti ti-polaroid"></i>
                                                <span>{FILE_NAME}</span>
                                                <i class="ti ti-circle-check float-end"></i>
                                                <i class="ti ti-alert-triangle float-end"></i>
                                                <i class="ti ti-loader-2 ti-spin float-end"></i>
                                                <i class="ti ti-alert-circle float-end"></i>
                                                <svg
                                                    class="fa-progress-bar float-end"
                                                    xmlns="http://www.w3.org/2000/svg"
                                                    viewBox="-1 -1 34 34"
                                                >
                                                    <circle
                                                        cx="16"
                                                        cy="16"
                                                        r="15"
                                                        class="fa-progress-bar__background"
                                                    />
                                                    <circle
                                                        cx="16"
                                                        cy="16"
                                                        r="15"
                                                        class="fa-progress-bar__progress"
                                                        style="stroke-dashoffset: 100px"
                                                    />
                                                </svg>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="DTE_Field form-group row">
                                        <label class="col-sm-4 col-form-label" for="feedback-group-anonymous"
                                            > {{ this.$WJ.translate('admin.welcome.feedback.dialog.send_anonym.js') }}
                                        </label>
                                        <div class="col-sm-7 d-flex align-items-center">
                                            <input
                                                v-model="feedback.isAnonymous"
                                                id="feedback-group-anonymous"
                                                type="checkbox"
                                                class="form-check-input"
                                                value="true"
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <div class="footer-content"></div>
                            <div class="DTE_Form_Buttons">
                                <span class="buttons-footer-left">
                                    <button type="button" class="btn btn-link" @click="$WJ.showHelpWindow()">
                                        <i class="ti ti-help me-1"></i>{{ this.$WJ.translate('button.help') }}
                                    </button>
                                </span>
                                <button
                                    @click="cancelModal()"
                                    type="button"
                                    class="btn btn-outline-secondary btn-close-editor"
                                >
                                    <i class="ti ti-x"></i> {{ this.$WJ.translate('button.cancel') }}
                                </button>
                                <button
                                    :class="{ disabled: feedbackSubmitDisabled }"
                                    type="submit"
                                    class="btn btn-primary"
                                    tabindex="0"
                                >
                                    <i class="ti ti-check"></i> {{ this.$WJ.translate('button.send') }}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
</template>

<script>
import $ from 'jquery';
import validationMixin from '../../../mixins/validationMixin';
import modalMixin from '../../../mixins/modalMixin';
export default {
    name: 'webjet-overview-dashboard-mini-app-feedback-modal',
    mixins: [validationMixin, modalMixin],
    data() {
        return {
            adminUpload: null,
            feedbackSubmitDisabled: false,
            feedback: {
                text: '',
                fileKeys: [],
                isAnonymous: false,
            },
        };
    },

    // JEFF tu si potom asi uprav ten upload na co budes potrebovat
    mounted() {
        this.initModal('feedback_modal');
        this.modal.show();

        this.initUpload();
    },

    methods: {
        initUpload() {
            this.adminUpload = window.AdminUpload({
                element: '#feedback-upload',
                destinationFolder: '/files/protected/feedback-form/',
                writeDirectlyToDestination: false,
            });
            window.addEventListener('WJ.AdminUpload.success', (e) => {
                this.feedbackSubmitDisabled = false;
                this.feedback.fileKeys.push(e.detail.key);
            });
            window.addEventListener('WJ.AdminUpload.addedfile', (e) => {
                //nevadi ked sa nepodari upload, poslime aspon text
                //this.feedbackSubmitDisabled = true;
            });
        },
        validate() {
            if (this.feedback.text === '') {
                this.showError('text-error');
                return false;
            }
            return true;
        },
        cancelModal() {
            this.removeModal('feedback_modal');
            this.$emit('cancelModal');
        },
        sendForm() {
            if (this.validate()) {
                $.ajax({
                    type: 'POST',
                    url: '/admin/rest/feedback',
                    data: { data: this.feedback },
                    success: function (result) {
                        //console.log("Success, result=", result);
                        if ("OK"===result) {
                            WJ.notifySuccess(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.success_notify.js"), 20000);
                        } else {
                            WJ.notifyError(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.error_notify.js"), 60000);
                        }
                    },
                    error: function (result) {
                        console.log("Error, result=", result);
                        WJ.notifyError(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.error_notify.js"), 60000);
                    },
                });
                this.cancelModal();
            }
        },
    },
};
</script>
