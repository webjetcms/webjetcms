<template>
    <section>
        <div v-if="!imageIsReady" class="loading">
            <div class="spinner-border text-primary" role="status"></div>
        </div>
        <div class="ready">
            <header v-if="imageIsReady" class="coordinates">
                <label for="x" class="coordinate-label">
                    {{ galleryTable.EDITOR.field('selectedX').label() }}
                </label>
                <input
                    @input="onChangeCoordinate(+$event.target.value, 'left')"
                    v-model="coordinates.left"
                    class="form-control coordinate-input"
                    id="x"
                    type="text"
                >
                <label for="y" class="coordinate-label">
                    {{ galleryTable.EDITOR.field('selectedY').label() }}
                </label>
                <input
                    @input="onChangeCoordinate(+$event.target.value, 'top')"
                    v-model="coordinates.top"
                    class="form-control coordinate-input"
                    id="y"
                    type="text"
                >
                <label for="w" class="coordinate-label">
                    {{ galleryTable.EDITOR.field('selectedWidth').label() }}
                </label>
                <input
                    @input="onChangeCoordinate(+$event.target.value, 'width')"
                    v-model="coordinates.width"
                    class="form-control coordinate-input"
                    id="w"
                    type="text"
                >
                <label for="h" class="coordinate-label">
                    {{ galleryTable.EDITOR.field('selectedHeight').label() }}
                </label>
                <input
                    @input="onChangeCoordinate(+$event.target.value, 'height')"
                    v-model="coordinates.height"
                    class="form-control coordinate-input"
                    id="h"
                    type="text"
                >
                <label for="zoom" class="coordinate-label">
                    {{ zoomLabel }}
                </label>
                <input
                    @input="onZoom(+$event.target.value)"
                    v-model="zoom"
                    class="form-control coordinate-input"
                    id="zoom"
                    type="number"
                    step="25"
                    min="25"
                    max="500"
                >
            </header>
            <div>
                <vue-advanced-cropper
                    ref="cropper"
                    class="cropper"
                    classname="cropper"
                    :src="imgSrc"
                    :resizeImage="{ wheel: false, touch: false }"
                    @change="change"
                    @ready="ready"
                ></vue-advanced-cropper>
            </div>
        </div>
    </section>
</template>

<script lang="js">
    import { Cropper } from 'vue-advanced-cropper';
    import 'vue-advanced-cropper/dist/style.css';
    export default {
        name: 'cropper-component',
        data() {
            return {
                galleryTable: galleryTable,
                imagePath: $("#DTE_Field_imagePath").val(),
                imageName: $("#DTE_Field_imageName").val(),
                imageIsReady: false,
                coordinates: {
                    width: +galleryTable.EDITOR.field('selectedWidth').val(),
                    height: +galleryTable.EDITOR.field('selectedHeight').val(),
                    left: +galleryTable.EDITOR.field('selectedX').val(),
                    top: +galleryTable.EDITOR.field('selectedY').val()
                },
                zoom: 100,
                zoomLabel: window.WJ.translate("image_editor.zoom.js")+" (%):"
            }
        },
        computed: {
            imgSrc: {
                get() {
                    let now = (new Date()).getTime();
                    let prefix = "o_";
                    if (this.imagePath.indexOf("gallery") === -1) {
                        prefix = "";
                    }
                    let url = `${this.imagePath}/${prefix}${this.imageName}?v=${now}`;
                    //console.log("url=", url);
                    return url;
                },
                set() {
                    this.imagePath = $("#DTE_Field_imagePath").val();
                    this.imageName = $("#DTE_Field_imageName").val();
                }
            }
        },
        components: {
           'vue-advanced-cropper': Cropper
        },
        mounted() {
            if (!this.mapCoordinatesIfZeros(this.coordinates)) {
                this.onSetCoordinates(this.coordinates);
            }
        },
        methods: {
            refreshCoordinates() {
                const newCoordinates = {
                    width: +galleryTable.EDITOR.currentJson.selectedWidth,
                    height: +galleryTable.EDITOR.currentJson.selectedHeight,
                    left: +galleryTable.EDITOR.currentJson.selectedX,
                    top: +galleryTable.EDITOR.currentJson.selectedY,
                }
                //console.log("newCoordinates=", newCoordinates, "json=", galleryTable.EDITOR.currentJson);
                if (this.coordinates.width != newCoordinates.width) this.coordinates.width = newCoordinates.width;
                if (this.coordinates.height != newCoordinates.height) this.coordinates.height = newCoordinates.height;
                if (this.coordinates.left != newCoordinates.left) this.coordinates.left = newCoordinates.left;
                if (this.coordinates.top != newCoordinates.top) this.coordinates.top = newCoordinates.top;
            },
            refresh() {
                var instance = this;
                this.$refs.cropper.resetVisibleArea().then(function () {
                    instance.refreshImgSrc();
                    instance.refreshCoordinates();
                    instance.onSetCoordinates(instance.coordinates);
                });
            },
            resetVisibleArea() {
                var instance = this;
                this.$refs.cropper.resetVisibleArea().then(function () {
                    //console.log("THEN, coordinates=", instance.coordinates);
                    instance.$refs.cropper.setCoordinates(instance.coordinates);
                });
            },
            refreshImgSrc() {
                const newImageSrc = {
                    imageName: $("#DTE_Field_imageName").val(),
                    imagePath: $("#DTE_Field_imagePath").val()
                }
                if (this.imageName != newImageSrc.imageName) this.imageName = newImageSrc.imageName;
                if (this.imagePath != newImageSrc.imagePath) this.imagePath = newImageSrc.imagePath;
                this.imageIsReady = true;
            },
            onChangeCoordinate(value, coordinate) {
                clearTimeout(this.timeout);
                this.timeout = setTimeout(() => {
                let coordinates = {};
                coordinates[coordinate] = value;
                this.$refs.cropper.setCoordinates(coordinates);
                }, 1000);
            },
            onZoom(value) {
                //window.$("div.vue-advanced-cropper.cropper").css("zoom", value+"%");
                let zoomDiv = window.$("div.vue-advanced-cropper.cropper");
                let scaleValue = (value/100);
                //console.log("scaleValue=", scaleValue)
                zoomDiv.css("transform", "scale("+scaleValue+")");
                zoomDiv.css("transform-origin", "0 0");
            },
            mapCoordinatesIfZeros(coordinates) {
                let values = 0;
                for (let [key, value] of Object.entries(coordinates)) {
                    values += value;
                }
                return values === 0 ? true : false;
            },
            ready() {
                this.imageIsReady = true;
            },
            onSetCoordinates(coordinates) {
                this.$refs.cropper.setCoordinates(coordinates);
            },
            change({ coordinates, canvas }) {
                const { height, width, top, left } = coordinates;
                this.coordinates.width = width;
                this.coordinates.height = height;
                this.coordinates.top = top;
                this.coordinates.left = left;
                galleryTable.EDITOR
                .set("selectedHeight", height)
                .set("selectedWidth", width)
                .set("selectedX", left)
                .set("selectedY", top);
            }
        }
    }
</script>

<style lang="css">
    .coordinates {
        width: 100%;
        justify-content: center;
        display: flex;
        flex-direction: row;
        padding: 10px;
        background-color: #F3F3F6;
        position: absolute;
        top: 0px;
        left: 0px;
        z-index: 10;
    }
    .loading {
        height: 450px;
        display: flex;
        justify-content: center;
        flex-direction: column;
        align-items: center;
    }
    .coordinate-label {
        line-height: 35px;
        margin: 0;
    }
    .coordinate-input {
        width: 100px;
        margin: 0px 5px;
    }
    .ready > div {
        margin-top: 58px;
    }
</style>