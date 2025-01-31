# Nastavení šablon

## Jedna webová stránka

Při použití pro jednu webovou stránku/doménu není třeba nastavovat nic zvláštního/odlišného od WebJET 8.

## Správa více domén

Při správě více domén je třeba definovat následující konfigurační proměnné:
- `multiDomainEnabled=true` - umožnění správy více domén
- `enableStaticFilesExternalDir=true` - umožnění odděleného dělení aplikačních dat pro domény a použití externího adresáře pro statické soubory.
- `cloudStaticFilesDir=/cesta/na/disku/` - Nastavení cesty k souborům externí domény. Jedná se o cestu na disku (např. `/mnt/cluster/static-files/`), který může být i mimo složku webové aplikace (např. na síťovém disku sdíleném mezi uzly clusteru). Pokud však chcete mít soubory domény ve složce webové aplikace, můžete nastavit hodnotu `{FILE_ROOT}static-files` ve kterém makro `{FILE_ROOT}` nahradí složkou, ze které je webová aplikace spuštěna.
- `templatesUseDomainLocalSystemFolder=true` - zapnout používání místních `System` adresáře pro domény
- `multiDomainAdminHost` - pokud máte samostatnou/vyhrazenou doménu pro přístup ke správě, nastavte název domény. Pokud je hodnota prázdná, budete mít přístup k administraci na všech doménách.

Po nastavení těchto proměnných doporučujeme restartovat server nebo alespoň odstranit všechny objekty z mezipaměti.

Toto nastavení zajišťuje oddělení dat a souborů jednotlivých domén. Pokud potřebujete sdílet soubory mezi doménami, můžete použít příkaz `/shared` který je sdílený mezi doménami.

!>**Varování:** při použití externího adresáře musí mít WebJET také prázdné složky. `/images, /files` a případně `/shared` v kořenovém adresáři, aby se zobrazily v části Všechny soubory. Technicky nelze do úložiště GIT vložit prázdný adresář, takže v těchto adresářích vytvořte několik prázdných souborů (nejlépe něco jako `velmi-dlhy-nahodny-text.txt`).

WebJET 2021 zobrazuje složky v seznamu webových stránek pouze pro vybranou doménu. Při uložení kořenové složky domény se zobrazí místní `System` složka pro záhlaví stránek, zápatí, nabídky atd. Při ukládání kořenové složky domény je proto nutné obnovit celou stránku, aby se správně načetl odkaz na složku. `System`.

Složka `System` je na rozdíl od WebJETu 8 uložen v kořenovém adresáři (můžete si jej zobrazit ve struktuře složek kliknutím na list Složky při stisknutí tlačítka `shift` a ne v prvním adresáři vybrané domény (pokud však v adresáři domény existuje, bude použit).

Výběrová pole pro záhlaví, zápatí, menu, volná pole v seznamu webových stránek nebo v úpravě šablony zobrazit stránky ve složce `System` včetně první úrovně podsložek (na rozdíl od WebJET 8). Ve složce `System` takže můžete vytvářet vlastní podsložky pro záhlaví, zápatí, nabídky nebo podle šablon či jiného klíče. Název stránky je uvozen názvem adresáře, např. `Hlavičky/Homepage`.

![](header-footer.png)
