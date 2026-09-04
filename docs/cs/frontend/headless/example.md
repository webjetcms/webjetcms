# Ukázková headless aplikace (Astro)

V adresáři [docs/examples/headless-astro/](https://github.com/webjetcms/webjetcms/tree/main/docs/examples/headless-astro) se nachází kompletní ukázková aplikace postavená na frameworku **[Astro 7](https://astro.build) **, která demonstruje.

![](home.png)

## Technologie

- ** Astro 7** – SSR (server-side rendering) framework
- **Bootstrap 5** – CSS framework
- **TypeScript** – typovaný JavaScript
- **Node.js** – runtime prostředí pro server

## Spuštění

```bash
cd docs/examples/headless-astro
npm install
npm run dev
```

Aplikace standardně běží na `http://localhost:3000`. CMS backend a další nastavení se konfigurují přes soubor `.env` v adresáři projektu. Zkopírujte `.env.example` jako základ:

```bash
cd docs/examples/headless-astro
cp .env.example .env
```

Upravte `.env` podle vaší instalace:

```bash
# Headless CMS API endpoint (full path including /rest/headless/v1)
# Update this to match your Headless CMS server
PUBLIC_API_BASE=https://cms.example.com/rest/headless/v1

#Optional variables
#HEADLESS_PROXY_PREFIXES=/images/,/files/,/thumb/,/shared/,/components,/FormMailAjax.action,/rest/,/apps/form/mvc/
#HEADLESS_HOST=127.0.0.1
#HEADLESS_PORT=3000
#HEADLESS_HTTPS=true
#HEADLESS_HTTPS_CERT=./.cert/localhost.pem
#HEADLESS_HTTPS_KEY=./.cert/localhost-key.pem

# Disable SSL certificate verification for local development with self-signed certificates
#NODE_TLS_REJECT_UNAUTHORIZED=0
```

## HTTPS režim v lokálním vývoji

Pokud backend posílá session cookie s příznakem `Secure`, frontend musí běžet na HTTPS, jinak prohlížeč cookie zahodí.

### 1. Vygenerování self-signed certifikátu

```bash
cd docs/examples/headless-astro
npm run cert:generate
```

Tento příkaz vytvoří soubory:

- `.cert/localhost.pem`
- `.cert/localhost-key.pem`

### 2. Zapnutí HTTPS v `.env`

Do souboru `.env` přidejte (nebo odkomentujte):

```bash
HEADLESS_HTTPS=true
HEADLESS_HTTPS_CERT=./.cert/localhost.pem
HEADLESS_HTTPS_KEY=./.cert/localhost-key.pem
```

### 3. Spuštění Astro serveru přes HTTPS

```bash
cd docs/examples/headless-astro
npm run dev:https
```

Frontend pak běží na adrese `https://127.0.0.1:3000` (resp. podle `HEADLESS_HOST` a `HEADLESS_PORT`).

### Poznámky

- U self-signed certifikátu je při prvním otevření stránky běžné bezpečnostní upozornění v prohlížeči.
- Pro Astro 7 použijte Node.js verzi minimálně `22.12.0`.

## Struktura projektu

```txt
src/
  lib/
    api.ts         – TypeScript klient pre všetky headless REST endpointy
  layouts/
    Layout.astro   – Spoločný layout s navigáciou a search boxom
  middleware.ts      – Smerovanie CMS stránok: presmerovania 404 na /cms/[...slug]
  pages/
    index.astro        – Domovská stránka
    cms/
      [...slug].astro  – CMS renderer (obsah z CMS podľa URL, volaný cez middleware)
    news.astro         – Novinky (server-side rendering)
    news-client.astro  – Novinky (client-side rendering, volanie z browsera)
    search.astro       – Fulltext vyhľadávanie
  styles/
    global.css     – Globálne CSS štýly
```

## Stránky a jejich funkce

### `/` – Domovská stránka (`index.astro`)

![](home.png)

Zobrazí úvodní stránku s:

- Uvítací sekcí s popisem headless demo
- Kartičkami odkazů na ukázky (novinky, vyhledávání)
- Dynamickým seznamem top navigačních položek z CMS

Používá API: `getNavigation()`

---

### Dynamické CMS stránky (`cms/[...slug].astro` + `middleware.ts`)

![](gallery.png)

Obsah z CMS je obsluhován kombinací middleware a CMS render:

- **`middleware.ts`** zachytí každou 404 odpověď od dedikovaných stránek a přepisuje URL na `/cms/render?path=<originálna-cesta>&query=<pôvodné-parametre>` (interní Astro `rewrite`). Cesta se posílá do CMS API bez URL parametrů, aby se správně našla stránka; původní parametry zůstávají dostupné rendereru.
- **`cms/[...slug].astro`** je CMS renderer – čte parametr `path`, zavolá API `GET /rest/headless/v1/pages/by-path?path=<cesta>` a zobrazí HTML obsah.

CMS catch-all je úmyslně umístěn pod `/cms/`, ne v root `pages/`. Díky tomu nepřepisuje dedikované stránky (např. `/news`, `/search`) a Astro správně zobrazí skutečnou chybu při chybě kompilování dedikované stránky (bez silent fallback na CMS).

Vlastnosti:

- Automatický fallback na 404 pokud stránka neexistuje v CMS
- SEO meta tagy (`<title>`, `<meta description>`) ze SEO metadat stránky
- Navigace načtená z CMS
- Posílání `Set-Cookie` hlaviček (session cookies) prohlížeči

Používá API: `getPage()`, `getNavigation()`

---

### `/news` – Novinky SSR (`news.astro`)

![](news.png)

Server-side rendered seznam novinek ze skupiny s ID `24`. Renderování probíhá na serveru při každém požadavku (Astro SSR).

Vlastnosti:

- Zobrazí thumbnail obrázek novinky (je-li nastaven)
- Zobrazí perex text
- Zobrazí štítky podle `perexGroups` (např. `Investície`, `Podnikanie`)
- Odkaz na detail novinky

Používá API: `listNews()` (POST `/rest/headless/v1/news`), `getNavigation()`

**Příklad konfigurace novinek:**

```typescript
const NEWS_GROUP_ID = 24;
const NEWS_PAGE_SIZE = 10;

await listNews({
  groupIds: [NEWS_GROUP_ID],
  publishType: 'new',
  order: 'date',
  ascending: false,
  pageSize: NEWS_PAGE_SIZE,
  offset: 0,
}, Astro.request);
```

---

### `/news-client` – Novinky CSR (`news-client.astro`)

![](news-client.png)

Client-side rendered seznam novinek. HTML stránka se načte prázdná a JavaScript v prohlížeči volá API přímo z browseru.

Vlastnosti:

- Volání API ze strany klienta (fetch z prohlížeče)
- DOM manipulace bez `innerHTML` (bezpečné generování HTML)
- Zobrazení loading stavu během načítání
- Zobrazení chybové zprávy při neúspěchu

Používá API: `POST /rest/headless/v1/news` (volané přímo z browseru)

> Poznámka: Pro klient-side volání musí mít backend nakonfigurovanou CORS politiku v konfigurační proměnné `accessControlAllowOriginValue`. Například:

```txt
{HTTP_PROTOCOL}://{SERVER_NAME}:{HTTP_PORT}
http://headless.example.com:3000,https://headless.example.com:8443
```

---

### `/search` – Vyhledávání (`search.astro`)

![](search.png)

Server-side fulltext vyhledávání přes CMS. Výsledky se zobrazují ihned po odeslání search formuláře.

Vlastnosti:

- Vyhledávání podle zadaného výrazu z URL parametru `?q=`
- Stránkování výsledků
- Zobrazení počtu nalezených výsledků
- Snippet s kontextem nalezeného textu
- Stav „žádné výsledky" a prázdný stav (bez zadaného výrazu)

Používá API: `search()` (GET `/rest/headless/v1/actions/search`), `getNavigation()`

---

## Layout (`Layout.astro`)

Společný layout pro všechny stránky obsahuje:

- Levý panel s navigací načtenou z CMS
- Search box v hlavičce (odešle na `/search?q=...`)
- Bootstrap 5 CSS
- Globální CSS styly
- Zprávu `<title>` a meta tagů

---

## API klient (`src/lib/api.ts`)

Centrální TypeScript soubor, který obaluje všechny headless REST volání. Klíčová vlastnost je funkce `createFetchOptions(request)`, která:

1. Posouvá `Cookie` hlavičku z příchozího requestu na backend (zachování session)
2. Posouvá `Referer` hlavičku (ochrana před XSRF zamítnutím ze strany backendu)

```typescript
import { getPage, getNavigation, search, listNews } from '../lib/api';
```

### Dostupné funkce

| Funkce | Endpoint | Popis |
| --- | --- | --- |
| `getPage(path, lng?, request?)` | GET `/pages/by-path` | Načte obsah stránky |
| `getNavigation(request?, rootPath?, rootGroupId?, depth?)` | GET `/navigation` | Načte navigační strom |
| `search(query, request?, page?, size?)` | GET `/actions/search` | Fulltext hledání |
| `listNews(request, request_?)` | POST `/news` | Načte seznam novinek |

---

## Proxy konfigurace (`astro.config.mjs`)

Konfigurace automaticky proxy-uje požadavky na obrázky, `/thumb`, soubory a REST API na CMS backend. To umožňuje, aby frontend aplikace servírovala celý obsah z jedné domény a předešlo se problémům s CORS pro statické soubory.

```txt
// Prefixy, ktoré sa presmerujú na CMS backend
/images/, /files/, /thumb/, /shared/, /components, /FormMailAjax.action, /rest/, /apps/form/mvc/
```

![](multistep-form.png)

## Nasazení na server (Node.js)

Ukázka je nastavena na SSR režim s Node adaptérem (`@astrojs/node`) av produkci se spouští jako Node server.

### 1. Build aplikace

```bash
cd docs/examples/headless-astro
nvm use 22.12
npm ci
npm run build
```

### 2. Spuštění produkčního serveru

```bash
cd docs/examples/headless-astro
npm run start
```

Skript `start` spouští `node ./dist/server/entry.mjs`.

### 3. Proměnné prostředí pro produkci

Minimálně nastavte:

```bash
PUBLIC_API_BASE=https://cms.vasadomena.sk/rest/headless/v1
HEADLESS_HOST=0.0.0.0
HEADLESS_PORT=3000
```

Doporučení pro produkci:

- Nenechávejte zapnuto `NODE_TLS_REJECT_UNAUTHORIZED=0`.
- Před Node server dejte reverzní proxy server (např. Nginx/Apache) a TLS ukončujte na 443.

## Nasazení na Vercel nebo Cloudflare

Pro tyto platformy změňte Astro adapter podle cíle nasazení.

### Vercel

1. Nainstalujte adaptér:

```bash
npm install @astrojs/vercel
```

1. V `astro.config.mjs` změňte import a adaptér konfiguraci:

```javascript
import vercel from '@astrojs/vercel';

export default defineConfig({
  output: 'server',
  adapter: vercel(),
  // ...ostatná konfigurácia
});
```

1. Deploy řeší Vercel build pipeline, lokální `npm run start` již není třeba.

### Cloudflare (Workers)

1. Nainstalujte adaptér:

```bash
npm install @astrojs/cloudflare
```

1. V `astro.config.mjs` změňte import a adaptér konfiguraci:

```javascript
import cloudflare from '@astrojs/cloudflare';

export default defineConfig({
  output: 'server',
  adapter: cloudflare(),
  // ...ostatná konfigurácia
});
```

1. Následně nasazujete jako Cloudflare Worker (typicky přes `wrangler`).

Poznámka: Při změně adaptéru ponechte `output: 'server'`, ale vždy používejte pouze jeden adaptér podle cílové platformy.
