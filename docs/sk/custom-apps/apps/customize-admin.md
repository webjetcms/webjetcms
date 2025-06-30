# Prispôsobenie administrácie

WebJET umožňuje prispôsobiť administráciu doplnkovým CSS štýlom a JavaScript súborom. Viete tak zmeniť, napr. logo za vaše firemné, doplniť text na úvodnú obrazovku a podobne.

## Doplnkový CSS štýl

Ak potrebujete doplniť/upraviť dizajn administrácie stačí vytvoriť súbor `/components/INSTALL-NAME/admin/style.css` pričom `INSTALL-NAME` je meno inštalácie. Ak súbor existuje pridá sa do CSS štýlov administračnej časti.

Môžete tak doplniť CSS štýly napríklad pre [pole typu Quill](../../developer/datatables-editor/standard-fields.md#quill):

```css
div.ql-editor .klass {
    font-weight: bold;
    background-color: yellow;
}
```

## Doplnkový JavaScript

Podobne môžete vytvoriť JavaScript súbor `/components/INSTALL-NAME/admin/script.js` s požadovaným obsahom. Skripty odporúčame spúšťať pomocou funkcie `window.domReady.add(function () {...}, 900)`, ktorá pri hodnote `orderId>=900` zabezpečí vykonanie až po vykonaní obsahu štandardnej stránky administrácie. Ak hodnotu `orderId` nezadáte, vykoná sa funkcia pred obsahom aktuálnej stránky.

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