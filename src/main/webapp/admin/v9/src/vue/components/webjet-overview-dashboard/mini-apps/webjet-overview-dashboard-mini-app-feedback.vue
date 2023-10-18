<template>
    <div :class="'overview-logged feedback'">
        <div class="overview-logged__head">
            <div
                @click="showModal()"
                class="overview-logged__head__icon"
                v-tooltip:top="this.$WJ.translate('admin.welcome.feedback.sendButton.js')"
            >
                <i class="fas fa-comments"></i>
            </div>
            <span> {{ this.$WJ.translate('admin.welcome.feedback.title.js') }}</span>
            <div class="overview-logged__head__more">
                <i class="far fa-ellipsis-v" style="display: none;"></i>
            </div>
        </div>

        <div class="overview-logged__content">
            <p class="perex">{{ this.$WJ.translate('admin.welcome.feedback.intro.js') }}</p>
            <p style="text-align: right">
                <button
                    @click="showModal()"
                    class="btn btn-primary"
                >{{ this.$WJ.translate('admin.welcome.feedback.sendButton.js') }}</button>
            </p>
        </div>
        <webjet-overview-dashboard-mini-app-feedback-modal
            v-if="modalVisible"
            @cancelModal="closeModal()"
        ></webjet-overview-dashboard-mini-app-feedback-modal>
    </div>
</template>

<script lang="js">
import WebjetOverviewDashboardMiniAppFeedbackModal from "./webjet-overview-dashboard-mini-app-feedback-modal"
export default {
  name: 'webjet-overview-dashboard-mini-app-feedback',
  components: {WebjetOverviewDashboardMiniAppFeedbackModal},

  data() {
    return {
      modalVisible : false
    }
  },
  mounted() {

  },
  methods: {
    closeModal() {
      this.modalVisible = false;

    },

     showModal() {
       this.modalVisible = true;
    },

  }
}
</script>

<style lang="scss">
.overview-logged {
    &.feedback {
        .overview-logged__head__icon {
            background: lighten(#c000d5, 20%);
            &:hover {
                cursor: pointer;
                background: #c000d5;
                color: #fff;
                transition: all 0.3s ease;
            }
            &:hover > i:before {
                content: '\f067';
            }
        }
    }
    &.feedback {
        .overview-logged__head {
            &__more {
                cursor: pointer;
                .dropdown-item {
                    padding-left: 10px;
                }
            }
        }
    }

    &__content {
        margin-bottom: 15px;
        margin-top: 15px;
        .perex {
            font-size: 14px;
        }
    }
}

.overview-logged.bookmark {
    ul {
        li {
            padding-left: 0px;
        }
    }
}
</style>
