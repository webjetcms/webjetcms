# NPM moduly

> Pokiaľ chceme vytvoriť z nejakého modulu lokálny NPM modul môžme to spraviť v tomto priečinku.

Každý modul obsahuje package.json s príslušnými parametrami. Najjednoduchšie je zadať `npm init` príkaz a ```node``` nás sám prevedie základnými nastaveniami. [https://docs.npmjs.com/creating-node-js-modules]

``` javascript
{
  "name": "webjetdatatables-node-module",
  "version": "1.0.0",
  "description": "webjet module for init datatables in any project",
  "main": "index.js",
  "scripts": {
    "test": "echo \"Error: no test specified\" && exit 1"
  },
  "keywords": [
    "datatables"
  ],
  "author": "interway <sales@interway.sk> (https://www.interway.sk)",
  "license": "ISC"
}
```

Príkazom ```npm pack``` vytvoríme ```.tgz``` archív ktorý vieme inštalovať pomocou ```npm install /absolute/path/to/project/npm_packages/<local-npm-package>/<nazov-modulu>.tgz```

```shell
rm webjetdatatables-node-module-1.0.0.tgz
npm pack
```

POZOR: v tomto adresári nesmie byť ```node_modules```, po aktualizácii cez ```ncu -u``` nevykonajte ```npm install```. Ak adresár existuje, zmažte ho. Aktualizované balíky je potrebné inštalovať zmazaním ```node_modules``` a ```package-lock.js``` v adresári ```/src/main/webapp/admin/v9``` a vykonaním inštalácie:

```sh
rm -rf node_modules
rm package-lock.json
npm install
```

Keď sa spraví zmena v zdrojovom kóde treba vymazať ```.tgz``` a znovu ho nainštalovať. Výnimkou je ```webjetdatatables```, ktorý je importovaný priamo do hlavného JS objektu a pri zmene sa dynamicky všetko skompiluje.