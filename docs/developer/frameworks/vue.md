# Vue.js

- [Vue.js](#vuejs)
  - [Databinding](#databinding)
  - [Vue.js ako samostatný komponent](#vuejs-ako-samostatný-komponent)
  - [Formátovanie dátumu a času](#formátovanie-dátumu-a-času)

## Databinding

Na Databinding je možné využiť aj Vue.js, to je importované v ```app.js``` z triedy ```vuetools.js``` a dostupné ako globálny objekt ```window.VueTools.getVue()```. Príklad použitia v [folder-tree-galeria.pug](src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug).

Použitie v HTML kóde cez v-model atribúty a ```{{ objekt.property }}```:

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

Inicializácia Databinding:

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

## Vue.js ako samostatný komponent

Pokiaľ chceme použiť, ako samostatný komponent je na to spravený priečinok ```vue/components```, následne treba komponent zaregistrovať v ```vuetools.js``` pre jeho asynchrónne načítanie. Dôvod je ten, že priamo v ```pug``` nie je možné použiť JavaScript ```import```. Môžete ale vytvoriť vlastný JavaScript súbor, v ňom použiť potrebný/é ```import``` a cez ```script src=...``` tag ho vložiť do pug súboru.

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

Momentálne neinicializujeme nový Vue objekt v ```app.js```, čiže treba pre každý .pug súbor, kde chceme použiť náš komponent inicializovať nový Vue objekt pokiaľ už nie je vytvorený.

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

Pokiaľ chceme využívať namiesto ```css``` napríklad ```scss``` treba doplniť vo ```webpack.common.js``` do ```rules.options.loaders``` aj ```scss loader```.

## Formátovanie dátumu a času

V ```VueTools.setDefaultObjects``` je nastavený objekt ```$WJ```, z neho je možné volať [štandardné funkcie](webjetjs.md) na formátovanie dátumu a času.

Príklad použitia:

```html
<span class="date">{{ $WJ.formatDateTimeSeconds(todo.createDate) }}</span>
```