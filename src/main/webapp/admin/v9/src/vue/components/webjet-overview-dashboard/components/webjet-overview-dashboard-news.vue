<template>
    <div class="overview__news">
        <div class="overview__news__head">
            <div class="overview__news__head__icon">
                <i class="ti ti-rss"></i>
            </div>
            <span>
                {{$newsInWebJET}}
            </span>
            <div class="overview__news__head__more">
                <i class="aaafar f-a-ellipsis-v"></i>
            </div>
        </div>

        <div class="overview__news__content">
            <ul>
                <li
                    v-for="(item, index) in news"
                    :key="index"
                >
                    <a :href="item.link" :title="item.title" target="_blank" class="overview__news__content__link">
                        <span class="title" v-text="item.title"></span>
                        <span class="perex" v-html="processMarkdown(item.perex)"></span>
                    </a>
                </li>
            </ul>
        </div>
    </div>
</template>


<script lang="js">
    export default {
        name: 'webjet-overview-dashboard-news',
        data() {
            return {
                news: null
            }
        },
        mounted () {
            //console.log("this.refs=", this, "overviewJsonUrl=", window.vueOverviewApp.config.globalProperties.$overviewJsonUrl);
            let self = this;
            let lngkey = "sk";
            if ("en"===window.userLng) lngkey = "en";
            else if ("cs"===window.userLng) lngkey = "cs";
            let url = window.vueOverviewApp.config.globalProperties.$overviewJsonUrl+'wjnews.'+lngkey+'.json';
            setTimeout(() => {
                $.get({
                    url: url,
                    success: function(data) {
                        //console.log("data=", data);
                        self.news = [];
                        try {
                            if (data.news.length>3) {
                                self.news.push(data.news.shift());
                                self.news.push(data.news.shift());
                                self.news.push(data.news.shift());
                            }
                        } catch (e) {
                            console.error("Error parsing news data", e);
                        }
                    }
                });
            }, 500);
        },
        methods: {
            processMarkdown: function(perex) {
                return WJ.parseMarkdown(perex);
            }
        }
    }
</script>

<style lang="scss">
.overview__news {
    background: #dddfe6;
    margin: 15px 0;
    border-radius: 10px;
    padding: 20px;
    color: #232426;
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
            color: #FFF;
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
        }
        &__link {
            color: #262729;
            font-size: 14px;
            display: block;
            padding: 10px 20px;
            background: transparent;
            transition: all .3s ease;

            &:hover {
                background: #f3f3f6;
                text-decoration: none;
                text-transform: none;
                color: #262729;
            }

            .title {
                display: block;
                font-weight: bold;
                margin-bottom: 5px;
            }
            .perex {
                color: #48494d;
            }
        }
    }
}


</style>