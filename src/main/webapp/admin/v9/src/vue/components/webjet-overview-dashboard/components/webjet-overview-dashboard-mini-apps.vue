<template>
    <div>
        <webjet-overview-dashboard-mini-app-users
            v-bind:overviewadmins="overviewadmins"
        ></webjet-overview-dashboard-mini-app-users>
        <webjet-overview-dashboard-mini-app-active-sessions
            v-bind:overviewcurrentsessions="overviewcurrentsessions"
        ></webjet-overview-dashboard-mini-app-active-sessions>
        <webjet-overview-dashboard-mini-app-todo
            v-bind:overviewtodo="overviewtodo"
            style="display: none"
        ></webjet-overview-dashboard-mini-app-todo>
        <webjet-overview-dashboard-mini-app-bookmark></webjet-overview-dashboard-mini-app-bookmark>
        <webjet-overview-dashboard-mini-app-feedback></webjet-overview-dashboard-mini-app-feedback>
    </div>
</template>

<script lang="js">



import WebjetOverviewDashboardMiniAppUsers from "../mini-apps/webjet-overview-dashboard-mini-app-users";
import WebjetOverviewDashboardMiniAppTodo from "../mini-apps/webjet-overview-dashboard-mini-app-todo";
import WebjetOverviewDashboardMiniAppBookmark from "../mini-apps/webjet-overview-dashboard-mini-app-bookmark";
import WebjetOverviewDashboardMiniAppFeedback from "../mini-apps/webjet-overview-dashboard-mini-app-feedback";
import WebjetOverviewDashboardMiniAppActiveSessions from "../mini-apps/webjet-overview-dashboard-mini-app-active-sessions";
export default {
  name: 'webjet-overview-dashboard-mini-apps',
  components: {
    WebjetOverviewDashboardMiniAppFeedback,
    WebjetOverviewDashboardMiniAppBookmark,
    WebjetOverviewDashboardMiniAppTodo,
    WebjetOverviewDashboardMiniAppUsers,
    WebjetOverviewDashboardMiniAppActiveSessions
  },
  props: ['overviewadmins', 'overviewtodo', 'overviewcurrentsessions'],

}
</script>

<!--spolocne css pre vsetky miniapp-->
<style lang="scss">
.modal {
    &-content {
        overflow: hidden;
    }
    &-body {
        padding: 0;
    }
}

.overview-logged {
    background: #edeff6;
    margin: 15px 0;
    border-radius: 10px;
    padding: 20px;
    color: #48494d;
    position: relative;

    &__head {
        padding-right: 90px;
        padding-left: 60px;
        height: 45px;
        position: relative;

        &__icon {
            background: #00a186;
            border-radius: 10px;
            width: 45px;
            height: 45px;
            position: relative;
            justify-content: center;
            align-items: center;
            display: inline-flex;
            margin-right: 10px;
            position: absolute;
            left: 0px;
            font-size: 20px;
        }
        &__more {
            position: absolute;
            right: -1px;
            top: 10px;

            i {
                font-size: 22px;
            }
        }
        span {
            line-height: 18px;
            font-size: 13px;
            display: inline-block;
            font-weight: bold;
            position: absolute;
            top: 50%;
            max-width: 190px;
            transform: translateY(-50%);
        }
    }
    &__content {
        ul {
            list-style: none;
            padding: 0;
            margin: 0;
            margin-top: 20px;

            li {
                font-size: 14px;
                padding-left: 50px;
                position: relative;
                min-height: 40px;
                padding-top: 10px;

                a {
                    transition: all 0.3s ease;
                    color: #48494d;
                    &:hover {
                        color: #202123;
                    }
                }

                span {
                    display: inline-block;
                    margin-left: 0px;
                }

                img, span.no-photo {
                    border-radius: 50%;
                    width: 40px;
                    position: absolute;
                    left: 0;
                    top: 0;
                    bottom: 0;
                    margin: auto;
                }
                span.no-photo {
                    left: 5px;
                    top: 6px;
                }
                &.show-more {
                    a {
                        border-radius: 50%;
                        width: 40px;
                        height: 40px;
                        position: absolute;
                        left: 0;
                        top: 0;
                        bottom: 0;
                        margin: auto;
                        background: #00a186;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        cursor: pointer;
                    }
                }
            }
        }
    }
}
</style>
