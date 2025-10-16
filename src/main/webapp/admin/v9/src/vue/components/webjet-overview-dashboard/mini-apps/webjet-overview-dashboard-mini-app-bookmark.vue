<template>
    <div :class="'overview-logged bookmark'">
        <div class="overview-logged__head">
            <div class="overview-logged__head__icon">
                <i class="ti ti-bookmarks fs-4"></i>
            </div>
            <span> {{ this.$WJ.translate('admin.welcome.bookmarks.title.js') }}</span>
            <div class="overview-logged__head__more">
                <i @click="showModal()" v-tooltip:top="this.$WJ.translate('button.add')" class="ti ti-plus"></i>
            </div>
        </div>

        <div class="overview-logged__content">
            <ul>
                <li v-for="(bookmark, index) in bookmarks" :key="index">
                    <a :href="bookmark.path">{{ bookmark.name }}</a>
                    <button
                        v-if="!bookmark.baseline"
                        @click="deleteBookmark(index)"
                        class="float-end btn btn-sm buttons-selected buttons-remove buttons-divider"
                        v-tooltip:left="$WJ.translate('button.delete')"
                    >
                      <span><i class="ti ti-trash fs-6"></i></span>
                    </button>
                </li>
            </ul>
        </div>
        <webjet-overview-dashboard-mini-app-bookmark-modal
            v-if="modalVisible"
            @addBookmark="(newBookmark) => addBookmark(newBookmark)"
            @cancelModal="closeModal()"
        ></webjet-overview-dashboard-mini-app-bookmark-modal>
    </div>
</template>

<script lang="js">

import WebjetOverviewDashboardMiniAppBookmarkModal from "./webjet-overview-dashboard-mini-app-bookmark-modal"
import modalMixin from "../../../mixins/modalMixin";
export default {
  name: 'webjet-overview-dashboard-mini-app-bookmark',
  components: {WebjetOverviewDashboardMiniAppBookmarkModal},
  mixins: [modalMixin],
  data() {
    return {
      bookmarks: [],
      modalVisible : false,
    }
  },
  mounted() {
    this.getBookmarks();

  },
  methods: {
    getBookmarks() {
      const baseLineBookmarks = [
        {
          name: this.$WJ.translate("admin.welcome.bookmarks.default.webPages.js"),
          path: "/admin/v9/webpages/web-pages-list/",
        },
        {
          name: this.$WJ.translate("admin.welcome.bookmarks.default.forms.js"),
          path: "/apps/form/admin/",
        }
      ]
      let storeItem = JSON.parse(localStorage.getItem("bookmarks"));
      if ( Array.isArray(storeItem) && storeItem.length !== 0 ) {
        this.bookmarks = storeItem;
      } else {
        this.bookmarks = baseLineBookmarks;
        //uloz defaultne
        this.updateStoreItem();
      }
    },
    addBookmark(bookmark) {
      bookmark.baseline = false;
      this.bookmarks.push(bookmark);
      this.updateStoreItem();
      this.closeModal();
    },
    deleteBookmark(index) {
      this.bookmarks.splice(index,1);
      this.updateStoreItem();
    },
    updateStoreItem() {
      let filteredBookmarks = this.bookmarks.filter((element ) => {
        return !element.baseline;
      })
      this.bookmarks = filteredBookmarks;
      localStorage.removeItem("bookmarks");
      localStorage.setItem("bookmarks",JSON.stringify(filteredBookmarks));
      this.getBookmarks();
    },

    closeModal() {
      this.modalVisible= false;
    },

    showModal() {
      this.modalVisible = true;
    },

  }
}
</script>

<style lang="scss">
.overview-logged {
    &.bookmark {
        .overview-logged__head__icon {
            background: lighten(#ffa500, 20%);
        }
    }
    &.bookmark {
        .overview-logged__head {
            &__more {
                cursor: pointer;
                .dropdown-item {
                    padding-left: 10px;
                }
            }
        }
    }

    &.bookmark {
      .overview-logged__content {
        button {
          padding: 0px;
        }
      }
    }

    &__content {
        ul li {
            margin-bottom: 0px;
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
