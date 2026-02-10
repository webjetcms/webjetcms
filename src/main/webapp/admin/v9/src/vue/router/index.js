import * as Vue from 'vue';
import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router';

//Vue.use(Router);

const routes = []

let history = createWebHistory();
if (window.location.href.indexOf("/apps/form/admin/")!=-1) history = createWebHashHistory();

export default createRouter({
    history: history,
    routes
});