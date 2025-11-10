<template>
  <div class="overview">
    <div class="row">
      <div class="col-lg-9">
        <div id="toast-container-overview" class="toast-container"></div>
        <webjet-overview-dashboard-infobox
          v-bind:overviewbackdata="overviewbackdata"
        ></webjet-overview-dashboard-infobox>
        <webjet-overview-dashboard-websites
          v-bind:overviewrecentpages="overviewrecentpages"
          v-bind:overviewchangedpages="overviewchangedpages"
          v-bind:overviewadminlog="overviewadminlog"
        ></webjet-overview-dashboard-websites>
        <webjet-server-monitoring :iscomplex="false"></webjet-server-monitoring>
      </div>
      <div class="col-lg-3 pl-0-lg">
        <webjet-overview-dashboard-mini-apps
          v-bind:overviewtodo="overviewtodo"
          v-bind:overviewadmins="overviewadmins"
          v-bind:overviewcurrentsessions="overviewcurrentsessions"
        ></webjet-overview-dashboard-mini-apps>
        <webjet-overview-dashboard-news></webjet-overview-dashboard-news>
      </div>
    </div>
  </div>
</template>

<script lang="js">
import WebjetOverviewDashboardInfobox from './components/webjet-overview-dashboard-infobox.vue';
import WebjetOverviewDashboardMiniApps from './components/webjet-overview-dashboard-mini-apps.vue';
import WebjetOverviewDashboardWebsites from './components/webjet-overview-dashboard-websites.vue';
import WebjetOverviewDashboardNews from './components/webjet-overview-dashboard-news.vue';
import WebjetServerMonitoring from '../webjet-server-monitoring/vue-server-monitoring.vue';
import WebjetOverviewDashboardMiniAppBookmarkModal from "./mini-apps/webjet-overview-dashboard-mini-app-bookmark-modal";

export default {
    name: 'webjet-overview-dashboard',
    props: ['overviewUser', 'overviewbackdata', 'overviewadmins', 'overviewrecentpages', 'overviewchangedpages', 'overviewadminlog', 'overviewtodo', 'overviewcurrentsessions'],
    components: {
      WebjetOverviewDashboardMiniAppBookmarkModal,
        'webjet-overview-dashboard-infobox': WebjetOverviewDashboardInfobox,
        'webjet-overview-dashboard-mini-apps': WebjetOverviewDashboardMiniApps,
        'webjet-overview-dashboard-websites': WebjetOverviewDashboardWebsites,
        'webjet-overview-dashboard-news': WebjetOverviewDashboardNews,
        'webjet-server-monitoring': WebjetServerMonitoring,
    },
    mounted() {
        //console.log("Mounted 1, props=", this.$props, " errors=", this.$props.overviewbackdata.statErrorNumber);
    }
}
</script>

<style lang="scss">
#webjet-overview-dashboard {
  .server-monitoring-section {
    height: auto;
  }
  .pl-0-lg {
    @media screen and (min-width: 992px) {
      padding-left: 0px !important;
    }
  }
}
</style>
