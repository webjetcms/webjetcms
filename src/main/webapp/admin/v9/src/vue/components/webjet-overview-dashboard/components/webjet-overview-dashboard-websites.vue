<template>
    <div class="overview__websites">
        <nav>
            <div class="nav nav-tabs" id="nav-tab" role="tablist">
                <a class="nav-item nav-link active noperms-menuWebpages" id="nav-mysites-tab" data-bs-toggle="tab" href="#nav-mysites" role="tab" aria-controls="nav-mysites" aria-selected="true">{{$myLastPages}}</a>
                <a class="nav-item nav-link noperms-menuWebpages" id="nav-websites-tab" data-bs-toggle="tab" href="#nav-websites" role="tab" aria-controls="nav-websites" aria-selected="false">{{$changedWebPages}}</a>
                <a class="nav-item nav-link noperms-cmp_adminlog" id="nav-audit-tab" data-bs-toggle="tab" href="#nav-audit" role="tab" aria-controls="nav-audit" aria-selected="false">{{$audit}}</a>
            </div>
        </nav>
        <div class="tab-content" id="nav-tabContent">
            <div class="tab-pane fade show active overview__websites-list" id="nav-mysites" role="tabpanel" aria-labelledby="nav-mysites-tab">
                <ul>
                    <li
                    v-for="(item, index) in recentpages"
                    :key="index"
                    >
                        <a :href="'/admin/v9/webpages/web-pages-list/?docid='+item.docId" class="text-truncate">
                            <i class="ti ti-pencil fs-5"></i>
                            <span class="title" v-text="item.title"></span>
                            <br/>
                            <span class="path" v-text="item.fullPath"></span>
                            <span class="date" v-text="item.saveDate"></span>
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
                        <a :href="'/admin/v9/webpages/web-pages-list/?docid='+item.docId" class="overview__websites-list__link text-truncate">
                            <i class="ti ti-pencil fs-5"></i>
                            <span v-if="item.createdByUserId > 0">
                                <span class="user" v-text="item.createdByUserName"></span>:
                            </span>
                            <span class="title" v-text="item.title"></span>
                            <br/>
                            <span class="path" v-text="item.fullPath"></span>
                            <span class="date" v-text="item.saveDate"></span>
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
                        <a :href="'/admin/v9/apps/audit-search/?id='+item.logId" class="overview__websites-list__link">
                            <i class="ti ti-shield-search"></i>
                            <span v-if="item.createdByUserId > 0">
                                <span class="user" v-text="item.createdByUserName"></span>:
                            </span>
                            <span class="type" v-text="item.type"></span>
                            <span class="description" v-text="item.description"></span>
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
                    padding: 6px 120px 6px 50px;
                    border-radius: 10px;
                    background: #f3f3f6;
                    position: relative;
                    color: #2d2e30;
                    font-size: 15px;
                    transition: all .3s ease;

                    i {
                        position: absolute;
                        left: 18px;
                        top: 0;
                        bottom: 0;
                        margin: auto;
                        height: 16px;
                        display: flex;
                        align-items: center;
                    }
                    .title {
                        position: relative;
                        font-weight: bold;
                    }
                    .date {
                        position: absolute;
                        font-size: 12px;
                        font-weight: bold;
                        right: 20px;
                        top: 0;
                        bottom: 0;
                        margin: auto;
                        height: 17px;
                    }
                    .user {
                        //font-weight: bold;
                    }
                    .type {
                        font-weight: bold;
                        margin-right: 6px;
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