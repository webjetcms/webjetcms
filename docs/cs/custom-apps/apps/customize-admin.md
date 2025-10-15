# Přizpůsobení administrace

WebJET umožňuje přizpůsobit administraci doplňkovým CSS stylům a JavaScript souborům. Umíte tak změnit. logo za vaše firemní, doplnit text na úvodní obrazovku a podobně.

## Doplňkový CSS styl

Pokud potřebujete doplnit/upravit design administrace stačí vytvořit soubor `/components/INSTALL-NAME/admin/style.css` přičemž `INSTALL-NAME` je jméno instalace. Pokud soubor existuje přidá se do CSS stylů administrační části.

Můžete tak doplnit CSS styly například pro [pole typu Quill](../../developer/datatables-editor/standard-fields.md#quill):

```css
div.ql-editor .klass {
    font-weight: bold;
    background-color: yellow;
}
```

## Doplňkový JavaScript

Podobně můžete vytvořit JavaScript soubor `/components/INSTALL-NAME/admin/script.js` s požadovaným obsahem. Skripty doporučujeme spouštět pomocí funkce `window.domReady.add(function () {...}, 900)`, která při hodnotě `orderId>=900` zajistí provedení až po provedení obsahu standardní stránky administrace. Pokud hodnotu `orderId` nezadáte, provede se funkce před obsahem aktuální stránky.

```javascript
if ("/admin/v9/"===window.location.pathname) {
    //domReady orderId >= 900 puts the script last in the queue
    window.domReady.add(function () {
        console.log("Admin script loaded for path");
        //because of the custom container we need to call WJ.notify("warning") instead of WJ.notifyWarning
        WJ.notify("warning", "Production environment", "You are in a production environment, please be careful.", 0, [], false, "toast-container-overview");
    }, 900);
}
```
