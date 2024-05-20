<template>
    <div>
        <div id="bookmark_modal" class="modal fade DTED" aria-modal="true" role="dialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <div class="row">
                            <div class="col-sm-4">
                                <h5 class="modal-title">{{ this.$WJ.translate('admin.welcome.bookmarks.dialog.title.js') }}</h5>
                            </div>
                        </div>
                    </div>
                    <div class="modal-body">
                        <div class="modal-body-bg">
                            <form class="form-horizontal">
                                <div class="from-content">
                                    <div class="DTE_Field form-group row required">
                                        <label class="col-sm-4 col-form-label" for="bookmark-group-name"
                                            >{{ this.$WJ.translate('admin.welcome.bookmarks.dialog.name.js') }}
                                        </label>
                                        <div class="col-sm-7">
                                            <div class="DTE_Field_InputControl">
                                                <input
                                                    v-model="newBookmark.name"
                                                    id="bookmark-group-name"
                                                    type="text"
                                                    class="form-control"
                                                    @focus="hideError('name-error')"
                                                />
                                                <div ref="name-error" class="form-text text-danger small invisible">
                                                    {{ this.$WJ.translate('admin.welcome.bookmarks.dialog.requiredField.js') }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="DTE_Field form-group row required">
                                        <label class="col-sm-4 col-form-label" for="bookmark-group-path">{{ this.$WJ.translate('admin.welcome.bookmarks.dialog.urlAddress.js') }}</label>
                                        <div class="col-sm-7">
                                            <div class="DTE_Field_InputControl">
                                                <input
                                                    v-model="newBookmark.path"
                                                    id="bookmark-group-path"
                                                    type="text"
                                                    class="form-control"
                                                    @focus="hideError('path-error')"
                                                    @input="handlePathInput()"
                                                />
                                            </div>
                                            <div ref="path-error" class="form-text text-danger small invisible">
                                                {{ this.$WJ.translate('admin.welcome.bookmarks.dialog.requiredField.js') }}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </form>
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
                                <i class="ti ti-x"></i> {{ this.$WJ.translate('button.cancel') }}</button
                            ><button @click="addBookmark()" class="btn btn-primary" tabindex="0">
                                <i class="ti ti-check"></i> {{ this.$WJ.translate('button.add') }}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import validationMixin from '../../../mixins/validationMixin';
import modalMixin from '../../../mixins/modalMixin';

export default {
    name: 'webjet-overview-dashboard-mini-app-bookmark-modal',
    mixins: [modalMixin, validationMixin],
    data() {
        return {
            newBookmark: {
                name: '',
                path: '/admin/v9/',
            },
        };
    },

    mounted() {
        this.initModal('bookmark_modal');
        this.modal.show();
    },
    methods: {
        addBookmark() {
            if (this.validate()) {
                this.removeModal('bookmark_modal');
                this.$emit('addBookmark', this.newBookmark);
            }
        },
        // pokial user zadava z aktualnej domeny tak ju odsekne
        // takto budu fungovat aj na deve atd...
        handlePathInput() {
            let originPath = window.location.origin;
            if (this.newBookmark.path.includes(originPath)) {
                this.newBookmark.path = this.newBookmark.path.substr(originPath.length, this.newBookmark.path.length);
            }
        },
        validate() {
            var isValid = true;
            if (this.newBookmark.name === '') {
                this.showError('name-error');
                isValid = false;
            }
            if (this.newBookmark.path === '') {
                this.showError('path-error');
                isValid = false;
            }
            return isValid;
        },

        cancelModal() {
            this.removeModal('bookmark_modal');
            this.$emit('cancelModal');
        },
    },
};
</script>

<style lang="scss">
.overview-logged {
    &.bookmark {
        .modal {
            color: black;
            .modal-content {
                overflow: hidden !important;
            }
            &-body {
                padding: 0 !important;
                .modal-body-bg {
                    &::before {
                        bottom: -15px !important;
                    }
                }
            }
        }
    }
}
</style>
