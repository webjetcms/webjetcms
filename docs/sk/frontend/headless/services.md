# Headless REST služby

Všetky služby sú dostupné pod základnou cestou:

```txt
/rest/headless/v1/
```

Odpovede sú vo formáte JSON. Chybové odpovede majú štruktúru:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Page not found: /neexistujuca-stranka"
}
```

---

## Stránky

### GET `/rest/headless/v1/pages/by-path`

Vráti obsah stránky podľa jej virtuálnej cesty.

**Parametre (query string):**

| Parameter | Povinný | Popis |
| --- | --- | --- |
| `path` | áno | Virtuálna cesta stránky, napr. `/o-nas` |
| `lng` | nie | Kód jazyka (napr. `en`). Ak nie je zadaný, použije sa predvolený jazyk domény. |
| `preview` | nie | `true` = náhľad neuverejnenej verzie (vyžaduje admin session) |

**Príklad volania:**

```bash
curl "https://cms.example.com/rest/headless/v1/pages/by-path?path=/o-nas"
```

**Odpoveď:**

```json
{
  "docId": 42,
  "title": "O nás",
  "virtualPath": "/o-nas",
  "language": "sk",
  "body": "<h1>O nás</h1><p>Text stránky...</p>",
  "seo": {
    "metaTitle": "O nás – Firma s.r.o.",
    "metaDescription": "Krátky popis stránky pre vyhľadávače.",
    "metaKeywords": "firma, o nás",
    "canonicalUrl": "https://cms.example.com/o-nas",
    "robots": "index, follow"
  }
}
```

Pole `body` obsahuje kompletné HTML telo stránky vrátane všetkých WebJET komponentov (news, formuláre, …) tak, ako by sa zobrazilo v klasickom režime. Frontend ho môže vložiť priamo do DOM alebo použiť len časti.

---

### GET `/rest/headless/v1/preview/pages/by-id`

Vráti obsah stránky podľa `docId` (ID dokumentu). Určené pre CMS editor – náhľad pred uverejnením.

**Parametre:**

| Parameter | Povinný | Popis |
| --- | --- | --- |
| `docId` | áno | ID dokumentu |
| `lng` | nie | Kód jazyka |

---

## Navigácia

### GET `/rest/headless/v1/navigation`

Vráti stromovú štruktúru navigácie.

**Parametre:**

| Parameter | Povinný | Popis |
| --- | --- | --- |
| `rootPath` | nie | Virtuálna cesta koreňovej skupiny, napr. `/` |
| `rootGroupId` | nie | ID skupiny – alternatíva k `rootPath` |
| `depth` | nie | Maximálna hĺbka (0 = neobmedzená, predvolene 0) |
| `lng` | nie | Kód jazyka |

**Príklad volania:**

```bash
curl "https://cms.example.com/rest/headless/v1/navigation?rootPath=/&depth=2"
```

**Odpoveď:**

```json
[
  {
    "docId": 10,
    "title": "Domov",
    "virtualPath": "/",
    "language": "sk",
    "level": 0,
    "hasChildren": true,
    "children": [
      {
        "docId": 11,
        "title": "O nás",
        "virtualPath": "/o-nas",
        "level": 1,
        "hasChildren": false
      }
    ]
  }
]
```

---

## Novinky (News)

### POST `/rest/headless/v1/news`

Vráti stránkovaný zoznam noviniek. Používa POST kvôli zložitejšej vstupnej štruktúre.

**Content-Type:** `application/json`

**Telo požiadavky (HeadlessNewsRequest):**

```json
{
  "groupIds": [24],
  "alsoSubGroups": false,
  "publishType": "new",
  "order": "date",
  "ascending": false,
  "paging": false,
  "pageSize": 10,
  "offset": 0,
  "perexNotRequired": false,
  "loadData": false,
  "checkDuplicity": false,
  "perexGroup": [],
  "perexGroupNot": []
}
```

| Pole | Typ | Popis |
| --- | --- | --- |
| `groupIds` | `number[]` | **Povinné.** Zoznam ID skupín noviniek |
| `alsoSubGroups` | `boolean` | Zahrnúť aj podskupiny (predvolene `false`) |
| `publishType` | `string` | Filter publikovania: `new` = aktuálne, `archive` = archív |
| `order` | `string` | Zoradenie: `date`, `title`, `priority` |
| `ascending` | `boolean` | `true` = vzostupne, `false` = zostupne |
| `paging` | `boolean` | Zapnúť stránkovanie (predvolene `false`) |
| `pageSize` | `number` | Počet položiek na stránku |
| `offset` | `number` | Posun (číslo záznamu od 0) |
| `perexNotRequired` | `boolean` | Zahrnúť aj novinky bez perexu |
| `loadData` | `boolean` | Načítať aj plné HTML telo novinky (`htmlData`) |
| `checkDuplicity` | `boolean` | Kontrola duplicity |
| `perexGroup` | `number[]` | Filter podľa skupín perexu (zahrnutie) |
| `perexGroupNot` | `number[]` | Filter podľa skupín perexu (vylúčenie) |

**Odpoveď:**

```json
{
  "items": [
    {
      "docId": 101,
      "title": "Nová novinka",
      "virtualPath": "/novinky/nova-novinka",
      "language": "sk",
      "perexImage": "/images/novinky/foto.jpg",
      "perexGroups": [1, 2],
      "htmlData": "Krátky perex text...",
      "publishStart": "2026-07-01T10:00:00",
      "groupId": 24,
      "available": true
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

---

## Vyhľadávanie

### GET `/rest/headless/v1/actions/search`

Fulltextové vyhľadávanie v dokumentoch.

**Parametre:**

| Parameter | Povinný | Popis |
| --- | --- | --- |
| `q` | áno | Hľadaný výraz |
| `page` | nie | Číslo stránky (0-based, predvolene 0) |
| `size` | nie | Počet výsledkov na stránku (predvolene 20, max 100) |
| `scope` | nie | Obmedzenie hľadania (voliteľné) |
| `lng` | nie | Kód jazyka |

**Príklad volania:**

```bash
curl "https://cms.example.com/rest/headless/v1/actions/search?q=produkt&page=0&size=20"
```

**Odpoveď:**

```json
{
  "items": [
    {
      "docId": 55,
      "title": "Produkt XY",
      "virtualPath": "/produkty/produkt-xy",
      "language": "sk",
      "perex": "Krátky popis produktu.",
      "snippet": "...text so zvýrazneným <b>produktom</b>..."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

---

## Formuláre

### POST `/rest/headless/v1/actions/forms/submit`

Odošle formulár definovaný v CMS.

**Content-Type:** `application/json`

**Telo požiadavky:**

```json
{
  "formId": "kontaktny-formular",
  "fields": {
    "meno": "Ján Novák",
    "email": "jan@example.com",
    "sprava": "Dobrý deň, mám otázku..."
  }
}
```

| Pole | Typ | Popis |
| --- | --- | --- |
| `formId` | `string` | ID formulára z CMS |
| `fields` | `object` | Mapa polí `{ nazovPola: hodnota }` |

**Odpoveď (úspech):**

```json
{
  "success": true,
  "message": "Formulár bol úspešne odoslaný."
}
```

**Odpoveď (chyba validácie):**

```json
{
  "success": false,
  "message": "Formulár obsahuje chyby.",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Neplatná e-mailová adresa."
    }
  ]
}
```

---

## Posielanie cookies

Väčšina služieb vracia v odpovedi `Set-Cookie` hlavičku (napr. session ID). Frontend musí túto hlavičku posúvať prehliadaču, aby sa zachoval stav session (prihlásenie, jazykové nastavenie, …).

Príklad v Astro (server-side):

```typescript
const result = await getPage('/o-nas', undefined, Astro.request);
const setCookie = result.headers.get('set-cookie');
if (setCookie) {
  Astro.response.headers.append('Set-Cookie', setCookie);
}
```

---

## TypeScript typy (lib/api.ts)

Ukážková Astro aplikácia obsahuje hotový TypeScript klient v `docs/examples/headless-astro/src/lib/api.ts` so všetkými typmi a funkciami pre každý endpoint. Môžete ho skopírovať do svojho projektu.
