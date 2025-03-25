# Nastavení licenčního čísla

Licenční číslo pro WebJET se zadává v sekci Nastavení/Konfigurace do konf. proměnné `lisense`. K hlavní licenci je možné mít nastaveny doplňkové licence pro další domény (např. při použití Enterprise licence) v konf. proměnné `licenseDomains` kde se zadává každá doplňková licence domény na nový řádek.

## Nesprávné licenční číslo

Pokud WebJET obsahuje licenční číslo s exspirovaným datem platnosti, nesprávným doménovým jménem, nebo licenční číslo je nesprávně zadané, zobrazí se po přihlášení do administrace možnost aktualizovat licenční číslo, v logech serveru se při startu zobrazí:

```log
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 - -----------------------------------------------
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 - WebJET initializing, root: /www/tomcat/webapps/webjet
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 -
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 - VERSION: 9
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 - Checking database connection:
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 -    Database connection: [OK]
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 - INIT (db): license=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 - License is valid until: 01.06.2022 23:59:59
[webjet][s.i.i.InitServlet][INFO][0] 2023-10-02 09:27:30 - ERROR: License is out of date, please contact
  InterWay (www.interway.sk)
  for new license.
```

Pokud se možnost zadat licenční číslo nezobrazí automaticky po otevření administrační části otevřete adresu `/wjerrorpages/setup/license`.

![](license.png)

Zadejte přihlašovací údaje pro ověření oprávnění do administrace a nový licenční klíč. Klepněte na OK pro uložení. Zobrazí se potvrzení uložení:

![](license-saved.png)

Pokud se aplikační server nerestartuje automaticky, proveďte restart aplikačního serveru. Při novém startu se použije zadané licenční číslo.

## Zadání licenčního čísla přímo do databáze

V případě nemožnosti zadání licenčního čísla přes web rozhraní můžete použít následující SQL příkaz přímo na databázovém serveru:

```sql
UPDATE _conf_ SET value='xxx' WHERE name='license';
```

pro Oracle použijte zápis:

```sql
UPDATE webjet_conf SET value='xxx' WHERE name='license';
```

## Exspirace licence

WebJET umí automaticky zjistit blížící se konec platnosti Vaše licence. Pokud zjistí, že platnost licence vyprší do 2 (nebo méně) měsíců, zobrazí upozornění v úvodní části.

![](license-expiration-notification.png)
