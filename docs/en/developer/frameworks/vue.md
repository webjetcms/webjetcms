# Vue.js

- [Vue.js](#vuejs)
  - [Databinding](#databinding)
  - [Vue.js as a standalone component](#vuejs-as-a-standalone-component)
  - [Date and time formatting](#date-and-time-formatting)

## Data binding

Vue.js can also be used for Databinding, it is imported in ```app.js``` from class ```vuetools.js``` and available as a global object ```window.VueTools.getVue()```. Example of use in [folder-tree-galleria.pug](src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug).

Usage in HTML code via v-model attributes and ```{{ objekt.property }}```:

```html
<input type="text" class="form-control" v-model="galleryFolder.galleryName">

<textarea class="form-control">{{ galleryFolder.galleryPerex }}</textarea>

<select v-model="galleryFolder.resizeMode">
    <option value="S">Zobrazenie na mieru</option>
    <option value="C">Orezať na mieru</option>
    <option value="A">Presný rozmer</option>
    <option value="W">Presná šírka</option>
    <option value="H">Presná výška</option>
    <option value="N">Negenerovať zmenšeniny</option>
</select>

```

Databinding initialization:

```javascript
    //zakladny objekt vlastnosti adresara galerie
    let galleryFolder = {
        galleryName: "Test gallery name",
        galleryPerex: "Perex text galerie",
        resizeMode: "A"
    };

    //nabindovanie objektu na element #galleryFolderEdit
    const vm = window.VueTools.createApp({
        data: { galleryFolder: galleryFolder }
    });
    window.VueTools.setDefaultObjects(vm);
    vm.mount('#galleryFolderEdit');
```

## Vue.js as a standalone component

If we want to use it as a separate component, a folder ```vue/components``` is created for it, then the component needs to be registered in ```vuetools.js``` for its asynchronous loading. The reason is that it is not possible to use JavaScript ```import``` directly in ```pug```. However, you can create your own JavaScript file, use the necessary ```import``` in it and insert it into the pug file via the ```script src=...``` tag.

```javascript
    //príklad s použitím dynamického importu
    static getComponent(name) {
        ...
        if ('nazov-nasho-komponentu'===name) {
            return Vue.defineAsyncComponent(() =>
                import(/* webpackChunkName */ '../components/nas-komponent')
            )
        }
        ...
    }
```

Currently, we do not initialize a new Vue object in ```app.js```, so we need to initialize a new Vue object for each .pug file where we want to use our component, unless it has already been created.

```html
    <div id="app"><nazov-nasho-komponentu></nazov-nasho-komponentu></div>
```

```javascript
    //je potrebné, aby mal deklarovanu property components, aj keby mala ostať prázdna ináč sa može stať že nebude poznat registrované komponenty.
    const vm = Vue.createApp({
        components: {}
    });
    vm.mount("#app")
```

If we want to use ```css``` instead of ```scss```, for example, we need to add ```webpack.common.js``` to ```rules.options.loaders``` and ```scss loader```.

## Date and time formatting

In ```VueTools.setDefaultObjects```, the object ```$WJ``` is set up, from which it is possible to call [standard functions](webjetjs.md) for formatting date and time.

Example of use:

```html
<span class="date">{{ $WJ.formatDateTimeSeconds(todo.createDate) }}</span>
```