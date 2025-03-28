# Přizpůsobení aplikací

Pokud potřebujete modifikovat existující aplikaci (např. banner), nikdy to nedělejte přímo v originálním JSP souboru, protože při aktualizaci se mohou vaše změny přepsat.

Chcete-li modifikovat soubor `/components/search/search.jsp`, vytvořte si nejprve kopii souboru s názvem `/components/INSTALL_NAME/search/search.jsp`.

Hodnota `INSTALL_NAME` je jméno instalace, v konfiguraci v položce s názvem `installName`. V takto vytvořeném souboru můžete vytvářet modifikace bez obav z přepsání souboru při aktualizaci. Ve stránce může zůstat `!INCLUDE(...)!` původní aplikace, WebJET provede automatické nahrazení cesty:
- `/components/INSTALL_NAME/search/search.jsp`
- `/components/search/search.jsp`

Vyhledání správného souboru proběhne automaticky. Použije se první nalezený podle uvedeného pořadí.
