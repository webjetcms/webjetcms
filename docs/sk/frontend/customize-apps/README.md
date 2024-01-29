# Prispôsobenie aplikácií

Ak potrebujete modifikovať existujúcu aplikáciu (napr. banner), nikdy to nerobte priamo v originálnom JSP súbore, pretože pri aktualizácii sa môžu vaše zmeny prepísať.

Ak chcete modifikovať súbor `/components/search/search.jsp`, vytvorte si najskôr kópiu súboru s názvom `/components/INSTALL_NAME/search/search.jsp`.

Hodnota `INSTALL_NAME` je meno inštalácie, v konfigurácii v položke s názvom `installName`. V takto vytvorenom súbore môžete vytvárať modifikácie bez obáv z prepísania súboru pri aktualizácii. V stránke môže zostať `!INCLUDE(...)!` pôvodnej aplikácie, WebJET vykoná automatické nahradenie cesty:

- `/components/INSTALL_NAME/search/search.jsp`
- `/components/search/search.jsp`

Vyhľadanie správneho súboru prebehne automaticky. Použije sa prvý nájdený podľa uvedeného poradia.