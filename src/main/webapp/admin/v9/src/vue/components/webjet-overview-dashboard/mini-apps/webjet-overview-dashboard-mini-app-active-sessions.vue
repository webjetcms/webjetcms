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

                <span>{{ sessionCluster.cluster }}</span>

                <li v-for="(session, index) in sessionCluster.userSessions" :key="index">
                    <span class="home-span">
                        <i v-if="this.currentSession == session.sessionId" class="ti ti-home fs-6"></i>
                    </span>

                    <!-- Added native tooltip with session parameters -->
                    <span v-tooltip:top="{title : formatSessionTooltip(session), class: 'session-tooltip'}">{{ session.domainName }} ( {{ session.remoteAddr }} )</span>

                    <button class="float-end btn btn-sm buttons-selected buttons-remove buttons-divider" @click="removeSession(session.sessionId)">
                        <span><i class="ti ti-trash fs-6"></i></span>
                    </button>
                </li>
            </ul>
        </div>

        <webjet-overview-dashboard-mini-app-feedback-modal
            v-if="modalVisible"
            @cancelModal="closeModal()"
        ></webjet-overview-dashboard-mini-app-feedback-modal>
    </div>
</template>

<script lang="js">
export default {
  name: 'webjet-overview-dashboard-mini-app-active-sessions',

  data() {
    return {
        sessions: [],
        currentSession: null,
        // reactive flags
        isLoading: false,
        reRender: false
    }
  },
  mounted() {
    // Intentionally left blank: moved initialization to created() for earlier fetch
  },
  created() {
    // Fetch as early as possible (before DOM mount)
    this.setCurrentSession();
    this.getSessions();
  },
  methods: {
    async setCurrentSession() {
        // Load current session using Fetch API with async/await
        try {
            const response = await fetch('/admin/rest/monitoring/currentSession', {
                method: 'GET',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                    "X-CSRF-Token": window.csrfToken
                }
            });

            if (!response.ok) {
                console.log(`ERR HTTP ${response.status} ${response.statusText}`);
            }

            const result = await response.text();
            this.currentSession = result;
        } catch (error) {
            console.log('Error loading current session', error);
        }
    },

    async getSessions() {
        // Prevent parallel loads
        if (this.isLoading) return;
        this.isLoading = true;

        try {
            const response = await fetch("/admin/rest/monitoring/sessions", {
                method: 'GET',
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                    "X-CSRF-Token": window.csrfToken
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            this.sessions = data;
        } catch (error) {
            console.log('Error loading session list', error);
        } finally {
            this.isLoading = false;
        }
    },

    async removeSession(sessionId) {
        try {
            const params = new URLSearchParams();
            params.append('sessionId', sessionId);

            const response = await fetch("/admin/rest/monitoring/removeSession", {
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
        if (session.loggedUserName) parts.push(this.$WJ.translate("admin.welcome.active_sessions.loggedUserName.js") + "\n\t" + session.loggedUserName);
        if (session.remoteAddr) parts.push(this.$WJ.translate("admin.welcome.active_sessions.remoteAddr.js") + "\n\t" + session.remoteAddr);
        if (session.logonTime) parts.push(this.$WJ.translate("admin.welcome.active_sessions.logonTime.js") + "\n\t" + logonTime);
        if (session.domainName) parts.push(this.$WJ.translate("admin.welcome.active_sessions.domainName.js") + "\n\t" + session.domainName);
        //Rest of params do not make sense, because they are not updated regularly - like last URL etc.
        return parts.join('\n'); // newline works in native title for multi-line in most browsers
    }
  }
}
</script>

<style lang="scss">
.overview-logged {
    &.feedback {
        .overview-logged__head__icon {
            background: lighten(#c000d5, 20%);
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

.overview-logged__content ul li {
    padding-left: 10px !important;
}

.overview-logged__content .home-span {
    width: 20px;
}

.session-tooltip .tooltip-inner {
    max-width: 600px !important;
    white-space: pre-wrap;
    text-align: left;

}

</style>
