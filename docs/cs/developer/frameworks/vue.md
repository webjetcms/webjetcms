# Vue.js

- [Vue.js](#vuejs)
  - [Databinding](#databinding)
  - [Vue.js jako samostatný komponent](#vuejs-jako-samostatný-komponent)
  - [Formátování data a času](#formátování-data-a-času)

## Databinding

Na Databinding lze využít i Vue.js, to je importováno v `app.js` ze třídy `vuetools.js` a dostupné jako globální objekt `window.VueTools.getVue()`. Příklad použití v [folder-tree-galeria.pug](src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug).

Použití v HTML kódu přes v-model atributy a `{{ objekt.property }}`:

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

Inicializace Databinding:

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

## Vue.js jako samostatný komponent

Pokud chceme použít, jako samostatný komponent je k tomu provedena složka `vue/components`, následně je třeba komponentu zaregistrovat v `vuetools.js` pro jeho asynchronní načítání. Důvod je ten, že přímo v `pug` nelze použít JavaScript `import`. Můžete ale vytvořit vlastní JavaScript soubor, v něm použít potřebný/é `import` a přes `script src=...` tag jej vložit do pug souboru.

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

Momentálně neinicializujeme nový Vue objekt v `app.js`, tedy třeba pro každý .pug soubor, kde chceme použít naši komponentu inicializovat nový Vue objekt pokud již není vytvořen.

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

Pokud chceme využívat místo `css` například `scss` je třeba doplnit ve `webpack.common.js` do `rules.options.loaders` i `scss loader`.

## Formátování data a času

V `VueTools.setDefaultObjects` je nastaven objekt `$WJ`, z něj lze volat [standardní funkce](webjetjs.md) pro formátování data a času.

Příklad použití:

```html
<span class="date">{{ $WJ.formatDateTimeSeconds(todo.createDate) }}</span>
```
