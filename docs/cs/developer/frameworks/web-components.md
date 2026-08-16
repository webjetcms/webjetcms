# Webové komponenty

Administrace WebJET CMS používá pro menší interaktivní prvky standardní JavaScript [webové komponenty](https://developer.mozilla.org/en-US/docs/Web/API/Web_components). Komponenta je třída odvozená od `HTMLElement`, registrovaná pomocí `customElements.define`.

Zdrojové soubory jsou v adresáři `src/main/webapp/admin/v9/src/js/web-components`. Komponenta se importuje v JavaScript vstupu a v HTML se používá jako vlastní element.

Konfiguraci předávejte veřejnou metodou komponenty a změny oznamujte pomocí bublajícího `CustomEvent`. V `disconnectedCallback` zrušte časovače, síťové požadavky a listenery. Styly patří do SCSS administrace; komponenty používají light DOM, aby zdědily Bootstrap a WebJET styly.
