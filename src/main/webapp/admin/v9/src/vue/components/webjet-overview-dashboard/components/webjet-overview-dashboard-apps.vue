<template>
    <div class="overview__apps">
        <div class="overview__apps__head">
            <div class="overview__apps__head__icon">
                <i class="ti ti-rocket fs-4"></i>
            </div>
            <span>
                {{$appsForNewWebJET}}
            </span>
            <div class="overview__apps__head__more">
                <i class="aaafar f-a-ellipsis-v"></i>
            </div>
        </div>

        <div class="overview__apps__content">
            <ul>
                <li
                    v-for="(app, index) in apps"
                    :key="index"
                >
                    <a :href="app.link" class="overview__apps__content__link">
                        <span class="overview__apps__content__icon">
                            <i :class="app.icon"></i>
                        </span>
                        <span class="name" v-text="app.name"></span>
                        <span class="category" v-text="app.category"></span>
                    </a>
                </li>
            </ul>
        </div>
    </div>
</template>


<script lang="js">
    export default {
        name: 'webjet-overview-dashboard-apps',
        data() {
            return {
               apps: null
            }
        },
        mounted () {
            //console.log("this.refs=", this, "overviewJsonUrl=", window.vueOverviewApp.config.globalProperties.$overviewJsonUrl);
            let self = this;
            let lngkey = "sk";
            if ("en"===window.userLng) lngkey = "en";
            else if ("cs"===window.userLng) lngkey = "cs";
            let url = '/admin/v9/json/apps.'+lngkey+'.json?v=1';
            $.get({
                url: url,
                success: function(data) {
                    //console.log("data=", data);
                    self.apps = data.apps;
                }
            });
        }
    }
</script>

<style lang="scss">
.overview__apps {
    background: #2d2e32;
    margin: 15px 0;
    border-radius: 10px;
    padding: 20px;
    color: #FFF;
    position: relative;

    &__head {
        padding-right: 90px;
        padding-left: 60px;
        height: 45px;
        position: relative;

        &__icon {
            background: #4d5262;
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
            right: 5px;
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
            transform: translateY(-50%);
        }
    }
    &__content {
        ul {
            list-style: none;
            margin: 0;
            padding: 0;
            margin-top: 20px;
            margin-left: -20px;
            margin-right: -20px;

            li {
                padding-left: 66px;
                position: relative;
                transition: all .3s ease;

                &:first-child {
                    margin-top: 0px;
                }

                .name {
                    display: block;
                    padding-top: 4px;
                    color: #f1f1f1;
                    font-size: 14px;
                    font-weight: bold;
                }
                .category {
                    font-size: 13px;
                    color: #8a8a8c;
                }
                &:hover {
                    background-color: #4d5262;
                }
            }
        }
        &__link {
            display: block;
            padding: 3px;
            background: transparent;
            transition: all .3s ease;
            &:hover {
                text-decoration: none;
                text-transform: none;
            }
        }
        &__icon {
            width: 70px;
            position: absolute;
            left: 20px;
            padding-top: 10px;
            i {
                font-size: 32px;
                color: #C2C6D2;
            }
        }
    }
}

</style>