# Vue.js

- [Vue.js](#vuey)
  - [Databinding](#databinding)
  - [Vue.js as a standalone component](#vuejs-as-a-standalone-component)
  - [Date and time formatting](#date-and-time-formatting)

## Databinding

You can also use Vue.js for Databinding, it is imported in `app.js` from the class `vuetools.js` and available as a global object `window.VueTools.getVue()`. Example of use in [folder-tree-gallery.pug](src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug).

Use in HTML code via v-model attributes and `{{ objekt.property }}`:

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

Initializing Databinding:

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

If we want to use it as a separate component, there is a folder for that `vue/components`, then the component must be registered in `vuetools.js` for its asynchronous retrieval. The reason is that directly in `pug` JavaScript cannot be used `import`. But you can create your own JavaScript file, use the necessary `import` and through `script src=...` tag it in the pug file.

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

We are not currently initializing a new Vue object in `app.js` that is, for each .pug file where we want to use our component, we need to initialize a new Vue object if it is not already created.

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

If we want to use instead of `css` For example `scss` should be added in `webpack.common.js` to `rules.options.loaders` Also `scss loader`.

## Date and time formatting

V `VueTools.setDefaultObjects` the object is set `$WJ`, from which it is possible to call [standard functions](webjetjs.md) to format the date and time.

Example of use:

```html
<span class="date">{{ $WJ.formatDateTimeSeconds(todo.createDate) }}</span>
```
