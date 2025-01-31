# Generování náhledových obrázků v aplikaci WebJET

WebJET umožňuje na vyžádání vygenerovat z libovolného obrázku ve složkách obrázky zadané velikosti. `/images,/files,/shared,/video,/templates`.

## Základní generace

Představme si, že máme obrázek (nemusí být z galerie, je to jen ukázka):

`/images/gallery/test-vela-foto/dsc04131.jpg`

![Original Image](original-image.png)

a potřebujeme ji mít v maximální velikosti `200x200` bodů. Stačí přidat před adresu URL obrázku předponu `/thumb` a přidejte parametry URL `w` a `h` s požadovaným rozměrem, tj:

`/thumb/images/gallery/test-vela-foto/dsc04131.jpg?w=200&h=200`

![Thumb Image 200x200](thumb-image.png)

Realistický obraz může být menší než požadovaný `200x200` bodů v závislosti na poměru stran. V tomto případě byl vygenerován jako `200x134` bodů, ale vždy se vejde do požadovaného rozměru.

## Výchozí obrázek

Ve výchozím nastavení je pro neexistující obrázek při použití `/thumb` adresy vrátí standardní chybu 404. Pokud však potřebujete pro takový případ zobrazit výchozí obrázek, můžete použít konfigurační proměnnou `thumbServletMissingImg`. Do řádků můžete přidat název složky a název souboru, který má být pro tento případ použit. Příklad nastavení:

```
/images/gallery/test/|/images/photo3.jpg
/images/|/images/photo1.jpg
```

Nastavení podle zadaného formátu pro neexistující obrázky ze složky `/images/gallery/test/` a jeho podadresáře zobrazí obrázek `/images/photo3.jpg`. Vyvolání obrázku ze složky `/images/test/podadresar/` zobrazí se obrázek `/images/photo1.jpg`, protože nejlepší shoda bude právě s `/images` adresář. Při volání `/templates/meno/assets/image.jpg` zobrazí se standardní chyba 404, protože v konfigurační proměnné není definována žádná předpona pro tuto složku.

Nalezený obrázek prochází procesem `/thumb`, takže se generuje v zadaném rozměru z parametrů URL.

## Omezení

Generování obrázků zatěžuje server, proto je chráněno ochranou proti SPAMu. Používají se následující konfigurační proměnné:
- `spamProtectionTimeout-ThumbServlet` - doba mezi požadavky HTTP, nastavená na `-2` vypnout, protože na stránce může být současně generováno více obrázků.
- `spamProtectionHourlyLimit-ThumbServlet` - maximální počet obrázků vygenerovaných z jedné IP adresy za hodinu, ve výchozím nastavení nastaveno na hodnotu `300`.
- `cloudCloneAllowedIps` - čárkou oddělený seznam počátečních IP adres, pro které nebude omezení použito, ve výchozím nastavení prázdný (neplatí).
