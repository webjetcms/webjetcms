import * as Vue from 'vue';
import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router';

//Vue.use(Router);

const routes = [
    {
        path: '/detail/:name',
        component: () => import(/* webpackChunkName: "formDetail" */ '../components/webjet-forms/vue-form-detail.vue')
    },
    {
        path: '/',
        component: () => import(/* webpackChunkName: "formsList" */ '../components/webjet-forms/vue-forms-list.vue')
    }
]

let history = createWebHistory();
if (window.location.href.indexOf("/apps/form/admin/")!=-1) history = createWebHashHistory();

export default createRouter({
    history: history,
    routes
});