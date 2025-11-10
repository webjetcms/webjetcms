<template>
    <div :class="'overview-logged active-sessions'">
        <div class="overview-logged__head">
            <div class="overview-logged__head__icon">
                <i class="ti ti-browser fs-4"></i>
            </div>
            <span> {{ this.$WJ.translate('admin.welcome.active_sessions.title.js') }}</span>
        </div>

        <div class="overview-logged__content">
            <ul>
                <li v-for="(session, index) in allSessions" :key="index">
                    <span v-tooltip:top="{title : formatSessionTooltip(session), class: 'session-tooltip'}" class="active-session-entry">
                        {{ this.$WJ.formatTimeSeconds(session.logonTime) }} ({{ session.browserName }}, {{ session.remoteAddr }})
                    </span>

                    <span v-if="this.currentSession == session.sessionId" class="float-end">
                        <i class="ti ti-current-location fs-6" v-tooltip:top="{title: this.$WJ.translate('admin.welcome.active_sessions.current_session.js'), class: 'session-tooltip'}"></i>
                    </span>
                    <button v-else class="float-end btn btn-sm" @click="removeSession(session.sessionId)">
                        <i class="ti ti-logout fs-6" v-tooltip:top="{title: this.$WJ.translate('menu.logout', {domain: session.domainName}), class: 'session-tooltip'}"></i>
                    </button>
                </li>
            </ul>
        </div>
    </div>
</template>

<script lang="js">
export default {
  name: 'webjet-overview-dashboard-mini-app-active-sessions',

  props: ['overviewcurrentsessions'],

  data() {
    return {
        sessions: [],
        currentSession: null,
        // reactive flags
        isLoading: false,
        reRender: false,
        allSessions: []
    }
  },
  created() {
    // Fetch as early as possible (before DOM mount)
    //console.log("sessions=", this.$props.overviewcurrentsessions);
    if (typeof this.$props.overviewcurrentsessions !== 'object' || this.$props.overviewcurrentsessions === null) {
        console.log("Invalid overviewcurrentsessions prop:", this.$props.overviewcurrentsessions);
        return;
    }
    this.currentSession = this.$props.overviewcurrentsessions.currentSessionId;
    this.sessions = this.$props.overviewcurrentsessions.userSessions;

    //tricky part - we need to sort sessions by session.logonTime desc within all sessionCluster, value of sessionCluster doesn't really matter for users
    //so first we need to convert it to flat list with added sessionCluster.cluster property and then sort it
    this.allSessions = [];
    for (let sessionCluster of this.sessions) {
        for (let session of sessionCluster.userSessions) {
            session.cluster = sessionCluster.cluster;
            this.allSessions.push(session);
        }
    }
    console.log("allSessions before sort=", this.allSessions);
    this.allSessions.sort((a, b) => b.logonTime - a.logonTime);
  },
  methods: {
    async removeSession(sessionId) {
        try {
            const params = new URLSearchParams();
            params.append('sessionId', sessionId);

            const response = await fetch("/admin/rest/removeSession", {
                method: 'POST',
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded; charset=utf-8",
                    "X-CSRF-Token": window.csrfToken
                },
                body: params.toString()
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            // Refresh session list after removal
            this.getSessions();
        } catch (error) {
            console.log("Error removing session", error);
        }
    },

    formatSessionTooltip(session) {
        if (!session || typeof session !== 'object') return '';
        const parts = [];

        let logonTime = session.logonTime ? new Date(session.logonTime).toUTCString() : null;
        if (session.browserName) parts.push(this.$WJ.translate("admin.welcome.active_sessions.userAgent.js") + ":\n\t" + session.browserName);
        if (session.remoteAddr) parts.push(this.$WJ.translate("admin.welcome.active_sessions.remoteAddr.js") + ":\n\t" + session.remoteAddr);
        if (session.logonTime) parts.push(this.$WJ.translate("admin.welcome.active_sessions.logonTime.js") + ":\n\t" + this.$WJ.formatDateTimeSeconds(logonTime));
        if (session.domainName) parts.push(this.$WJ.translate("admin.welcome.active_sessions.domainName.js") + ":\n\t" + session.domainName);
        if (session.cluster) parts.push(this.$WJ.translate("admin.welcome.active_sessions.server.js") + ":\n\t" + session.cluster);
        return parts.join('\n'); // newline works in native title for multi-line in most browsers
    }
  }
}
</script>

<style lang="scss">
.overview-logged {
    &__content {
        margin-bottom: 15px;
        margin-top: 15px;
        .perex {
            font-size: 14px;
        }
    }
}

.active-sessions .overview-logged__content ul li {
    padding-left: 0px !important;
    padding-top: 2px;
    min-height: auto;
}

.overview-logged__content {
    .active-session-entry {
        display: inline-block;
        max-width: 208px;
        text-overflow: ellipsis;
        overflow: hidden;
        white-space: nowrap;
    }
    .btn {
        padding: 0px;
    }
    .cluster-name {
        font-weight: bold;
        margin-top: 10px;
    }
    .cluster-name:first-child {
        margin-top: 0px;
    }
}

.session-tooltip .tooltip-inner {
    max-width: 600px !important;
    white-space: pre-wrap;
    text-align: left;

}

</style>
