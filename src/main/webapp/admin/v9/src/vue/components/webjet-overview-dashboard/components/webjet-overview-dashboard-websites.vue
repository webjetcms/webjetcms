<template>
    <div class="overview__websites">
        <nav>
            <div class="nav nav-tabs" id="nav-tab" role="tablist">
                <a class="nav-item nav-link active" id="nav-mysites-tab" data-bs-toggle="tab" href="#nav-mysites" role="tab" aria-controls="nav-mysites" aria-selected="true">{{$myLastPages}}</a>
                <a class="nav-item nav-link" id="nav-websites-tab" data-bs-toggle="tab" href="#nav-websites" role="tab" aria-controls="nav-websites" aria-selected="false">{{$changedWebPages}}</a>
                <a class="nav-item nav-link" id="nav-audit-tab" data-bs-toggle="tab" href="#nav-audit" role="tab" aria-controls="nav-audit" aria-selected="false">{{$audit}}</a>
            </div>
        </nav>
        <div class="tab-content" id="nav-tabContent">
            <div class="tab-pane fade show active overview__websites-list" id="nav-mysites" role="tabpanel" aria-labelledby="nav-mysites-tab">
                <ul>
                    <li
                    v-for="(item, index) in recentpages"
                    :key="index"
                    >
                        <a :href="'/admin/v9/webpages/web-pages-list/?docid='+item.docId">
                            <i class="far fa-eye"></i>
                            <span class="title" v-text="item.title"></span>
                            <span class="date" v-text="item.saveDate"></span>
                            <span class="dots"><i class="far fa-ellipsis-v"></i></span>
                        </a>
                    </li>
                </ul>
            </div>
            <div class="tab-pane fade overview__websites-list" id="nav-websites" role="tabpanel" aria-labelledby="nav-websites-tab">
                <ul>
                    <li
                    v-for="(item, index) in changedpages"
                    :key="index"
                    >
                        <a :href="'/admin/v9/webpages/web-pages-list/?docid='+item.docId" class="overview__websites-list__link">
                            <i class="far fa-eye"></i>
                            <span v-if="item.createdByUserId > 0">
                                <span class="user" v-text="item.createdByUserName"></span>:
                            </span>
                            <span class="title" v-text="item.title"></span>
                            <span class="date" v-text="item.saveDate"></span>
                            <span class="dots"><i class="far fa-ellipsis-v"></i></span>
                        </a>
                    </li>
                </ul>
            </div>
            <div class="tab-pane fade overview__websites-list" id="nav-audit" role="tabpanel" aria-labelledby="nav-audit-tab">
                <ul>
                    <li
                    v-for="(item, index) in adminlog"
                    :key="index"
                    >
                        <a href="#" class="overview__websites-list__link">
                            <span v-if="item.createdByUserId > 0">
                                <span class="user" v-text="item.createdByUserName"></span>:
                            </span>
                            <span class="type" v-text="item.type"></span>
                            <span class="title" v-text="item.description"></span>
                            <span class="date" v-text="item.date"></span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>


<script lang="js">
    export default {
        name: 'webjet-overview-dashboard-websites',
        props: ['overviewrecentpages', 'overviewchangedpages', 'overviewadminlog'],
        data() {
            return {
                recentpages: this.$props.overviewrecentpages,
                changedpages: this.$props.overviewchangedpages,
                adminlog: this.$props.overviewadminlog
            }
        }
    }
</script>

<style lang="scss">
.overview__websites {

    .tab-content {
        margin-top: 10px;
    }
    .nav-tabs {
        border-bottom: 1px solid #dee2e6;
        padding-bottom: 10px;
        margin-bottom: 15px;

        .nav-link {
            border: none;
            color: #636a7d;
            transition: all .3s ease;
            font-weight: bold;
            font-size: 14px;

            &.active {
                color: #202123;
            }
            &:hover {
                color: #0063fb;
            }
        }
    }
    &-list {
        ul {
            list-style: none;
            margin: 0;
            padding: 0;

            li {
                margin-bottom: 10px;

                a {
                    display: block;
                    padding: 10px 140px 10px 50px;
                    border-radius: 10px;
                    background: #f3f3f6;
                    position: relative;
                    color: #2d2e30;
                    font-size: 15px;
                    transition: all .3s ease;

                    @media screen and (max-width: 575px) {
                        padding: 10px 20px 30px 40px;
                    }

                    i {
                        position: absolute;
                        left: 20px;
                        top: 0;
                        bottom: 0;
                        margin: auto;
                        height: 16px;

                        @media screen and (max-width: 575px) {
                            left: 10px;
                        }
                    }
                    .title {
                        position: relative;
                    }
                    .date {
                        position: absolute;
                        font-size: 12px;
                        font-weight: bold;
                        right: 35px;
                        top: 0;
                        bottom: 0;
                        margin: auto;
                        height: 17px;

                        @media screen and (max-width: 575px) {
                            bottom: 10px;
                            height: auto;
                            top: auto;
                        }
                    }
                    .dots {
                        right: 20px;
                        position: absolute;
                        top: 50%;
                        transform: translateY(-50%);
                        font-size: 18px;

                        @media screen and (max-width: 575px) {
                            right: 15px;
                        }

                        i {
                            position: inherit;
                            left: auto;
                        }
                    }
                    .user {
                        font-weight: bold;
                    }
                    .type {
                        font-weight: bold;
                    }
                    &:hover {
                        text-decoration: none;
                        text-transform: none;
                        background: #0063fb;
                        color: #FFF;
                        box-shadow: 0 4px 11px -5px #000;
                        transform: translateX(10px);
                    }
                }
            }
        }
    }
}

</style>