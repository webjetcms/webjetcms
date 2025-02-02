<template>
    <section class="overview__dashboard">
        <div class="overview__dashboard__title">
            <h2>
                {{$welcome}}, {{user.name}}
            </h2>
            <p v-html="$changelog">
            </p>
            <a :href="'http://docs.webjetcms.sk/latest/'+$window.userLng+'/CHANGELOG'" target="_blank" class="btn btn-primary">{{$seeCompleteChangelog}}</a>
        </div>

        <div class="row">
            <div class="col-md-3"
                v-for="(item, index) in items"
                :key="index"
            >
                <div :class="item.itemClass + ' overview__dashboard__item'">
                    <p class="overview__dashboard__item__title">
                        {{ item.title }}
                    </p>
                    <a :href="item.link" class="overview__dashboard__item__info">
                        <i :class="item.icon" :style="{color: item.iconColor}"></i>
                        <div class="overview__dashboard__item__info__number">
                            {{ item.number }}
                        </div>
                    </a>
                </div>
            </div>
        </div>
    </section>
</template>

<script lang="js">
export default {
    props: ['overviewbackdata'],
    data() {
        let items = [];

        if (WJ.hasPermission("cmp_stat") && window.statMode !== "none") items.push({
            title: window.vueOverviewApp.config.globalProperties.$overviewViews,
            itemClass: "navstevy",
            icon: "fas fa-chart-line",
            iconColor: "#0063fb",
            number: this.$props.overviewbackdata.statViewsNumber,
            link: "/apps/stat/admin/"
        });

        if (WJ.hasPermission("cmp_form")) items.push({
            title: window.vueOverviewApp.config.globalProperties.$overviewForms,
            itemClass: "formulare",
            icon: "fas fa-server",
            iconColor: "#d90575",
            number: "+"+this.$props.overviewbackdata.fillFormsNumber,
            link: "/apps/form/admin/"
        });

        if (WJ.hasPermission("cmp_diskusia")) items.push({
            title: window.vueOverviewApp.config.globalProperties.$overviewForum,
            itemClass: "foto",
            icon: "fas fa-images",
            iconColor: "#c000d5",
            number: "+"+this.$props.overviewbackdata.documentForumNumber,
            link: "/apps/forum/admin/"
        });

        if (WJ.hasPermission("cmp_stat") && window.statMode !== "none") items.push({
            title: window.vueOverviewApp.config.globalProperties.$overviewErrors,
            itemClass: "dokumenty",
            icon: "fas fa-file-alt",
            iconColor: "#007f5e",
            number: "+"+this.$props.overviewbackdata.statErrorNumber,
            link: "/apps/stat/admin/error/"
        });

        return {
            items: items,
            user: {
                name: window.currentUser.fullName
            }
        }
    },
    mounted() {
        //console.log("Mounted, overviewbackdata=", this.$props.overviewbackdata);
    }
}
</script>

<style lang="scss">
.overview__dashboard {
    background: #edeff6;
    margin: 15px 0;
    padding: 20px;
    border-radius: 10px;

    &__title {
        padding: 25px 40px 100px;
        background-size: 350px;
        background-repeat: no-repeat;
        background-position: right bottom 15px;

        h2,
        p {
            margin-bottom: 20px;
            max-width: 600px;
        }
        p {
            max-width: 70%;
        }

        @media screen and (max-width: 575px) {
            padding: 25px 10px 20px;
            background: transparent;
        }
    }
    &__item {
        &__title {
            color: #585d6c;
            font-size: 13px;
            font-weight: bold;

            @media screen and (max-width: 767px) {
                margin-top: 20px;
                margin-bottom: 5px;
            }
        }
        &__info {
            border: 2px solid #e6e8ef;
            border-radius: 10px;
            padding: 25px 20px;
            position: relative;
            display: block;
            color: #072764;
            transition: all .3s ease;

            i {
                font-size: 33px;
            }

            &__number {
                display: inline-block;
                position: absolute;
                right: 15px;
                top: 0;
                bottom: 0;
                margin: auto;
                height: 28px;
                font-size: 20px;
                font-weight: bold;
            }

            &:hover {
                box-shadow: 0 11px 13px 0px #c3c2c2;
                border: 2px solid transparent;
            }
        }
        &.navstevy {
            .overview__dashboard__item__info {
                color: #072764;

                &:hover {
                    background: #b3caf7;
                }
            }
        }
        &.formulare {
            .overview__dashboard__item__info {
                color: #54052b;

                &:hover {
                    background: #eaaac9;
                }
            }
        }
        &.foto {
            .overview__dashboard__item__info {
                color: #480250;

                &:hover {
                    background: #e3dbff;
                }
            }
        }
        &.dokumenty {
            .overview__dashboard__item__info {
                color: #0c3629;

                &:hover {
                    background: #c1e8dc;
                }
            }
        }
    }
}
</style>