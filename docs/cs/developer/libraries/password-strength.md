# Kvalita hesel

Naše implementace zapouzdruje knihovnu [zxcvbn-ts](https://zxcvbn-ts.github.io/zxcvbn/), která hodnotí kvalitu hesla na základě více parametrů. Kromě standardních pravidel jako **délka** hesla, **velké** písmena, **speciální** znaky kontroluje v hesle také:
- posloupnost znaků na klávesnici, například. `asdf`
- data a roky
- opakující sekvence typu `abcabc`
- běžná jména a příjmení
- známá hesla jako `password`

Knihovna má **vestavěný slovník nejpoužívanějších hesel** a jmen, vůči kterým kontroluje heslo.

Kontrola je implementována v administraci při vytváření/úpravě uživatele ale i do přihlašovací stránky v administraci, na které informuje uživatele o kvalitě zadaného hesla.

![](password-strength.png)

## Příklad použití

Automatické použití v datatabulce je jednoduché, konstruktor třídy `WjPasswordStrength` používá následující možnosti:
- `element` - HTML DOM element, nebo ID elementu (který se následně získá jako `document.querySelector`)

```javascript
(new WjPasswordStrength({element: "#DTE_Field_password"})).load();
```

Při takovém použití se automaticky na zadaný `element` inicializuje kontrola kvality hesla. K elementu se vyhledá příslušný `div[data-dte-e=msg-info]` do kterého se vypíše informace o kvalitě hesla.

## Funkce

- `load()` - asynchronně načte slovník často používaných hesel a po načtení volá funkci `bindToElement()`.
- `bindToElement(element)` - inicializuje `keyup` událost na zadaný element pro kontrolu kvality hesla. Informaci vypíše do příslušného `div[data-dte-e=msg-info]`.
- `checkPassword(password)` - ověří kvalitu zadaného hesla, výsledek vrátí v JSON objektu.

## Implementační detaily

Aktuální knihovna `zxcvbn-ts` obsahuje jen `en,de,fr` slovníky často používaných hesel. V naší implementaci se ve funkci `load()` používá jen `en` slovník.

Informace o kvalitě hesla využívá překladové klíče z WebJET-u `wj-password-strength.warnings.` a `wj-password-strength.rating.`. Objekt `zxcvbn` je inicializován s prázdným `translations` objektem a vrací přímo klíče. Ty jsou dosazeny do WebJET překladových klíčů pro vypsání informace. Originální knihovna `zxcvbn` neobsahuje překlady do jazyků potřebných pro WebJET, proto jsme zvolili takové řešení. Navíc lze texty přes WebJET přímo upravovat.

Knihovna je importována v `app.js` a dostupná globálně:

```javascript
import { WjPasswordStrength } from './libs/wj-password-strength';
global.WjPasswordStrength = WjPasswordStrength;
```

## Přihlašovací obrazovka

Knihovna je využita i na přihlašovací obrazovce do administrace.

Jelikož ale přihlašování je implementováno ve starém JSP formátu a zároveň nechceme již na přihlašovací obrazovce zpřístupnit JavaScript soubory plné administrace používá přihlašovací obrazovka přímo `zxcvbn-ts` knihovnu. Protože adresář `node_modules` není přímo dostupný jsou během ant buildu kopírované soubory z adresářů v `node_modules/@zxcvbn-ts` do adresářů v `admin/skins/webjet8/assets/js/zxcvbn`, aby byla zabezpečena aktualizace knihoven po aktualizaci přes `npm update`.

Použití knihovny je implementováno přímo v `logon-spring.js` podobně jako v této knihovně, překladové texty jsou také zadané přímo v JSP souboru (ale přes překladové klíče synchronizované s administrací).

![](../../_media/changelog/2021q2/2021-26-password-strength.png)
