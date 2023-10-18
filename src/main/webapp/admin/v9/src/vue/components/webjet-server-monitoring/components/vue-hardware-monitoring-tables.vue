<template>
    <section class="monitoring-disk-space col">
        <p class="empty-space-header">{{ $emptySpaceOnDisk }}</p>
        <div class="empty-space">
            <div class="monitoring-container empty-space-ticks-container">
                <div class="empty-space-ticks"></div>
            </div>
            <div class="monitoring-container full-space-container">
                <div class="full-space" data-toogle="tooltip" :data-original-title="storageFree + ' GB'" :data-bs-original-title="storageFree + ' GB'"></div>
            </div>
        </div>
        <div class="monitoring-container space-numbers-container">
            <div class="space-numbers">
                <span v-for="(number, index) in interval" :key="index">
                    {{ number }}
                </span>
            </div>
        </div>
    </section>
</template>

<script lang="js">
    export default {
        props: ['obj'],
        name: 'hardware-monitoring-tables',
        data() {
            return {
                fullSpaceWidth: 150,
                numbers: [10,20,30,40,50,60,70,80,90,100,110],
            }
        },

        computed: {
            storageFree: function() {
                return Math.round(this.obj.storageFree / 1000000000)
            },
            storageTotal: function() {
                return Math.round(this.obj.storageTotal / 10000000000)  * 10;
            },
            interval: function() {
                var t = this.storageTotal / 10;
                var arr  = [];
                arr.push("");
                for (let index = 1; index <=9 ; index++) {
                    var r = t*index;
                    arr.push(r+" GB");
                }
                arr.push("");
                return arr;
            },
            actual: function() {
                return this.storageFree / this.storageTotal * 100
            }
        },

        methods: {
            updateSpace(availableSpace) {
                availableSpace > 10 ? this.fullSpaceWidth += availableSpace : this.fullSpaceWidth -= availableSpace;
                $('.full-space').css('width', `${this.actual}%`)
            }
        }
    }
</script>

