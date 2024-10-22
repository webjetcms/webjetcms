# Vue.js

- [Vue.js](#vuey)
  - [Databázová vazba](#vazba-na-databázi)
  - [Vue.js jako samostatná komponenta](#vuejs-jako-samostatná-komponenta)
  - [Formátování data a času](#formátování-data-a-času)

## Databázová vazba

Pro Databinding můžete použít také Vue.js, který je importován v části `app.js` ze třídy `vuetools.js` a k dispozici jako globální objekt `window.VueTools.getVue()`. Příklad použití v [folder-tree-gallery.pug](src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug).

Použití v kódu HTML pomocí atributů v-modelu a `{{ objekt.property }}`:

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

Inicializace vazby na databázi:

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

## Vue.js jako samostatná komponenta

Pokud ji chceme použít jako samostatnou komponentu, je k tomu určena složka. `vue/components`, pak musí být komponenta zaregistrována v `vuetools.js` pro jeho asynchronní načítání. Důvodem je, že přímo v `pug` JavaScript nelze použít `import`. Můžete si však vytvořit vlastní soubor JavaScriptu, použít potřebné `import` a prostřednictvím `script src=...` označit v souboru pug.

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

V současné době neinicializujeme nový objekt Vue v položce `app.js` to znamená, že pro každý soubor .pug, ve kterém chceme použít naši komponentu, musíme inicializovat nový objekt Vue, pokud ještě není vytvořen.

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

Pokud chceme použít místo `css` Například `scss` by měly být doplněny do `webpack.common.js` na `rules.options.loaders` Také `scss loader`.

## Formátování data a času

V `VueTools.setDefaultObjects` objekt je nastaven `$WJ`, z něhož je možné volat [standardní funkce](webjetjs.md) pro formátování data a času.

Příklad použití:

```html
<span class="date">{{ $WJ.formatDateTimeSeconds(todo.createDate) }}</span>
```
