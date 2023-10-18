<template>
  <div class="overview">
    <div class="row">
      <div class="col-lg-9">
        <webjet-overview-dashboard-infobox
          v-bind:overviewbackdata="overviewbackdata"
        ></webjet-overview-dashboard-infobox>
        <webjet-overview-dashboard-websites
          v-bind:overviewrecentpages="overviewrecentpages"
          v-bind:overviewchangedpages="overviewchangedpages"
          v-bind:overviewadminlog="overviewadminlog"
        ></webjet-overview-dashboard-websites>
      </div>
      <div class="col-lg-3 pl-0-lg">
        <webjet-overview-dashboard-mini-apps
          v-bind:overviewtodo="overviewtodo"
          v-bind:overviewadmins="overviewadmins"
        ></webjet-overview-dashboard-mini-apps>
      </div>
    </div>
    <div class="row">
      <div class="col-lg-5">
        <webjet-overview-dashboard-apps></webjet-overview-dashboard-apps>
      </div>
      <div class="col-lg-7 pl-0-lg">
        <webjet-overview-dashboard-news></webjet-overview-dashboard-news>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <webjet-server-monitoring :iscomplex="false"></webjet-server-monitoring>
      </div>
    </div>
  </div>
</template>

<script lang="js">
import WebjetOverviewDashboardInfobox from './components/webjet-overview-dashboard-infobox.vue';
import WebjetOverviewDashboardMiniApps from './components/webjet-overview-dashboard-mini-apps.vue';
import WebjetOverviewDashboardWebsites from './components/webjet-overview-dashboard-websites.vue';
import WebjetOverviewDashboardApps from './components/webjet-overview-dashboard-apps.vue';
import WebjetOverviewDashboardNews from './components/webjet-overview-dashboard-news.vue';
import WebjetServerMonitoring from '../webjet-server-monitoring/vue-server-monitoring.vue';
import WebjetOverviewDashboardMiniAppBookmarkModal from "./mini-apps/webjet-overview-dashboard-mini-app-bookmark-modal";

export default {
    name: 'webjet-overview-dashboard',
    props: ['overviewUser', 'overviewbackdata', 'overviewadmins', 'overviewrecentpages', 'overviewchangedpages', 'overviewadminlog', 'overviewtodo'],
    components: {
      WebjetOverviewDashboardMiniAppBookmarkModal,
        'webjet-overview-dashboard-infobox': WebjetOverviewDashboardInfobox,
        'webjet-overview-dashboard-mini-apps': WebjetOverviewDashboardMiniApps,
        'webjet-overview-dashboard-websites': WebjetOverviewDashboardWebsites,
        'webjet-overview-dashboard-apps': WebjetOverviewDashboardApps,
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
