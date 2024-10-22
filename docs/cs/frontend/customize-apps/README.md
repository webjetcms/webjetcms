# Přizpůsobení aplikací

Pokud potřebujete upravit existující aplikaci (např. banner), nikdy to nedělejte přímo v původním souboru JSP, protože vaše změny mohou být při aktualizaci přepsány.

Úprava souboru `/components/search/search.jsp`, nejprve vytvořte kopii souboru s názvem `/components/INSTALL_NAME/search/search.jsp`.

Hodnota `INSTALL_NAME` je název instalace, v konfiguraci v položce s názvem `installName`. V souboru můžete provádět změny, aniž byste se museli obávat přepsání souboru při aktualizaci. Na stránce může zůstat `!INCLUDE(...)!` původní aplikace, WebJET provede automatickou výměnu cesty:
- `/components/INSTALL_NAME/search/search.jsp`
- `/components/search/search.jsp`

Vyhledání správného souboru se provede automaticky. Bude použit první nalezený soubor podle zadaného pořadí.
