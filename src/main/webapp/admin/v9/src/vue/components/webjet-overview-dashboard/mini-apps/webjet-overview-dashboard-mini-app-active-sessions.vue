<template>
    <div :class="'overview-logged active-sessions'">
        <div class="overview-logged__head">
            <div class="overview-logged__head__icon">
                <i class="ti ti-list-details fs-4"></i>
            </div>
            <span> {{ this.$WJ.translate('admin.welcome.active_sessions.title.js') }}</span>
        </div>

        <div class="overview-logged__content">
            <ul v-for="(sessionCluster, index) in sessions" :key="index">
                <li class="cluster-name">{{ sessionCluster.cluster }}</li>
                <li v-for="(session, index) in sessionCluster.userSessions" :key="index">
                    <!-- Added native tooltip with session parameters -->
                    <span v-tooltip:top="{title : formatSessionTooltip(session), class: 'session-tooltip'}" class="active-session-entry">
                        {{ session.domainName }} ( {{ session.remoteAddr }} )
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
        reRender: false
    }
  },
  created() {
    // Fetch as early as possible (before DOM mount)
    //console.log("sessions=", JSON.stringify(this.$props));
    if (typeof this.$props.overviewcurrentsessions !== 'object' || this.$props.overviewcurrentsessions === null) {
        console.log("Invalid overviewcurrentsessions prop:", this.$props.overviewcurrentsessions);
        return;
    }
    this.currentSession = this.$props.overviewcurrentsessions.currentSessionId;
    this.sessions = this.$props.overviewcurrentsessions.userSessions;
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
        if (session.remoteAddr) parts.push(this.$WJ.translate("admin.welcome.active_sessions.remoteAddr.js") + ":\n\t" + session.remoteAddr);
        if (session.logonTime) parts.push(this.$WJ.translate("admin.welcome.active_sessions.logonTime.js") + ":\n\t" + window.WJ.formatDateTimeSeconds(logonTime));
        if (session.domainName) parts.push(this.$WJ.translate("admin.welcome.active_sessions.domainName.js") + ":\n\t" + session.domainName);
        //Rest of params do not make sense, because they are not updated regularly - like last URL etc.
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
    padding-left: 10px !important;
    padding-top: 2px;
    min-height: auto;
}

.overview-logged__content {
    .active-session-entry {
        display: inline-block;
        max-width: 180px;
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
