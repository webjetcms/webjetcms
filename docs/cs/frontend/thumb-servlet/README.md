# Generování náhledových obrázků ve WebJETu

WebJET umožňuje na požadavek generovat obrázky zadané velikosti z libovolného obrázku ze složek `/images,/files,/shared,/video,/templates`.

## Základní generování

Představme si, že máme obrázek (nemusí být z galerie, je to jen ukázka):

`/images/gallery/test-vela-foto/dsc04131.jpg`

![Original Image](original-image.png)

a potřebujeme, abychom ho měli v max. rozměru `200x200` bodů. Před URL obrázku stačí přidat prefix `/thumb` a přidat URL parametry `w` a `h` s požadovaným rozměr, čili:

`/thumb/images/gallery/test-vela-foto/dsc04131.jpg?w=200&h=200`

![Thumb Image 200x200](thumb-image.png)

Obrázek reálné může být menší než požadovaných `200x200` bodů, záleží na jeho poměru stran. V tomto případě se vygeneroval jako `200x134` bodů, vždy se ale vejde do požadovaného rozměru.

## Výchozí obrázek

Ve výchozím nastavení se pro neexistující obrázek při použití `/thumb` adresy vrátí standardní chyba 404. Pokud ale potřebujete pro takový případ zobrazit výchozí obrázek, lze použít konfigurační proměnnou `thumbServletMissingImg`. Do ní lze do řádků doplnit jméno složky a jméno souboru, který se pro tento případ má použít. Příklad nastavení:

```
/images/gallery/test/|/images/photo3.jpg
/images/|/images/photo1.jpg
```

Nastavení podle uvedeného formátu pro neexistující obrázky ze složky `/images/gallery/test/` a jeho pod-adresářů zobrazí obrázek `/images/photo3.jpg`. Pro volání obrázku ze složky `/images/test/podadresar/` se zobrazí obrázek `/images/photo1.jpg`, protože nejlepší shoda bude právě s `/images` adresářům. Při volání `/templates/meno/assets/image.jpg` se zobrazí standardní chyba 404, jelikož v konfigurační proměnné není definován žádný prefix pro tuto složku.

Nalezený obrázek projde procesem přes `/thumb`, takže je vygenerován v zadaném rozměru z URL parametrů.

## Omezení

Generování obrázků zatěžuje server, je tedy chráněno SPAM ochranou. Používají se následující konf. proměnné:
- `spamProtectionTimeout-ThumbServlet` - čas mezi HTTP požadavky, nastaveno na hodnotu `-2` pro vypnutí, protože na stránce může být více obrázků, které se generují najednou.
- `spamProtectionHourlyLimit-ThumbServlet` - maximální počet vygenerovaných obrázků z jedné IP adresy za hodinu, ve výchozím nastavení nastaveno na hodnotu `300`.
- `cloudCloneAllowedIps` - seznam začátků IP adres oddělených čárkou pro které se omezení nebude aplikovat, ve výchozím stavu prázdné (nepoužije se).
