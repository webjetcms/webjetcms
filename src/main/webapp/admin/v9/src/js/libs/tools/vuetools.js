import * as Vue from 'vue';

//toto mame staticky importnute, aby sa pre dashboard nemusel robit async request
import * as vueWebjetOverviewDashboard from '../../../vue/components/webjet-overview-dashboard/webjet-overview-dashboard.vue';

import * as router from '../../../vue/router/index';

/**
 * Trieda pre zakladnu podporu Vue v PUG suboroch
 */
export class VueTools {

    /**
     * setup Vue pre pouzitie vo WebJET CMS
     */
    static setup() {
        //console.log("VueTools.setup, Vue=", Vue);
    }

    /**
     * Vrati Vue kniznicu
     * @returns
     */
    static getVue() {
        return Vue;
    }

    /**
     * Na VUE instancii zavola createApp, skratka voci VueTools.getVue().createApp(config)
     * @param {*} config
     * @returns
     */
    static createApp(config) {
        return this.getVue().createApp(config);
    }

    /**
     * Nastavi standardne WJ objekty na vue instancii
     * @param {*} vm
     */
    static setDefaultObjects(vm) {
        // aby som vo vue app mohol pristupit bez nutnosti importu
        vm.config.globalProperties.$WJ = window.WJ;
        vm.config.globalProperties.$window = window;
        // direktiva na bootstrap tooltip do vue
        vm.directive('tooltip',
            {
                created (el, binding) {
                    $(el)
                        .tooltip({
                            title: binding.value,
                            placement: binding.arg,
                            trigger: 'hover',
                        })
                        .on('click', function () {
                            $(el).tooltip('dispose');
                        });
                }
            }
        );
    }

    /**
     * Vrati Vue komponentu pre pouzitie priamo v pug subore
     * @param {*} name
     * @returns
     */
    static getComponent(name) {
        // dynamicky importujem vue komponentu
        if ('vue-server-monitoring'===name) {
            return Vue.defineAsyncComponent(() =>
                import(/* webpackChunkName: "vueServerMonitoring" */ '../../../vue/components/webjet-server-monitoring/vue-server-monitoring.vue')
            )
        } else if ('webjet-cropper-component'===name) {
            return Vue.defineAsyncComponent(() =>
                import(/* webpackChunkName: "vueCropper" */ '../../../vue/components/vue-cropper.vue')
            );
        } else if ('webjet-dte-jstree'===name) {
            return Vue.defineAsyncComponent(() =>
                import(/* webpackChunkName: "webjetDteJstree" */ '../../../vue/components/webjet-dte-jstree/webjet-dte-jstree.vue')
            );
        } else if ('webjet-overview-dashboard'===name) {

            return vueWebjetOverviewDashboard.default;
        }
    }

    /**
     * Vrati instanciu vue routera
     * @returns
     */
    static getRouter() {
        return router.default;
    }

}