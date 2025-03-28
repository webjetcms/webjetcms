# Nahrazování textů

Text stránky vytvořené přímo v editoru WebJET může obsahovat speciální značky, které se při zobrazení web stránky aktualizují:

## Datum a čas

```html
<table width="100%" class="ramikTable">
<thead>
	<tr>
		<td width="33%" class="head">Značka</td>
		<td width="33%" class="head">Význam</td>
		<td width="33%" class="head">Príklad</td>
	</tr>
</thead>
<tbody>
	<tr>
		<td class="t_body">!DATUM!</td>
		<td class="t_body">aktuálny dátum</td>
		<td class="t_body">16.6.2002</td>
	</tr>
	<tr>
		<td class="t_body">!DEN_DATUM!</td>
		<td class="t_body">deň a dátum</td>
		<td class="t_body">Nedeľa 16.6.2002</td>
	</tr>
	<tr>
		<td class="t_body">!DEN_DATUM_CZ!</td>
		<td class="t_body">deň a dátum v českom jazyku</td>
		<td class="t_body">Pondělí 16.6.2002</td>
	</tr>
	<tr>
		<td class="t_body">!DATE!</td>
		<td class="t_body">deň v anglickom jazyku</td>
		<td class="t_body">16.6.2002</td>
	</tr>
	<tr>
		<td class="t_body">!DAY_DATE!</td>
		<td class="t_body">deň a dátum v anglickom jazyku</td>
		<td class="t_body">Sunday 16.6.2002</td>
	</tr>
	<tr>
		<td class="t_body">!TIME!</td>
		<td class="t_body">aktuálny čas</td>
		<td class="t_body">22:17</td>
	</tr>
	<tr>
		<td class="t_body">!DOC_TITLE!</td>
		<td class="t_body">názov web stránky</td>
		<td class="t_body">Web JET</td>
	</tr>
</tbody>
</table>
```

## Texty

```html
<table width="100%" class="ramikTable">
	<thead>
	<tr>
		<td width="33%" class="head">Značka</td>
		<td width="33%" class="head">Význam</td>
		<td width="33%" class="head">Príklad</td>
	</tr>
	</thead>
	<tbody>
	<tr>
		<td class="t_body">!TEXT(textovy_kluc)!</td>
		<td class="t_body">Vypíše hodnotu textového kľúča</td>
		<td class="t_body">!TEXT(button.cancel)! - Zrušiť</td>
	</tr>
	</tbody>
</table>
```

## Parametry a atributy

```html
<table width="100%" class="ramikTable">
<thead>
	<tr>
		<td width="33%" class="head">Značka</td>
		<td width="33%" class="head">Význam</td>
		<td width="33%" class="head">Príklad</td>
	</tr>
</thead>
<tbody>
<tr>
		<td class="t_body">!PARAMETER(nazov_parametra)!</td>
		<td class="t_body">Vypíše hodnotu parametra HTTP požiadavky s menom nazov_parametra</td>
		<td class="t_body">!PARAMETER(docid)! - 145</td>
	</tr>
	<tr>
		<td class="t_body">!REQUEST(nazov_atributu)!</td>
		<td class="t_body">Vypíše hodnotu atribútu požiadavky s menom nazov_atributu. Ako nazov_atributu je možné použiť rovnaké názvy ako pri <a href='dynamic_tags.jsp'>HTML šablónach</a>.
			<br>
			pomocou !REQUEST(hodnota)! je este mozne pristupovat k nasledovnym informaciam (zadane ako hodnota):
			<br>
			header.* - hodnota HTTP hlavicky<br>
			remoteIP - IP adresa navstevnika<br>
			remoteHost - host name navstevnika<br>
			baseHref - domena aktualnej stranky vratane http prefixu<br>
			url - url adresa aktualnej stranky<br>
			urlQuery - url adresa + query string http dotazu (URL adresa + hodnota za znakom ? v URL)<br>
			path_filter_query_string - query string http dotazu (hodnota za znakom ? v URL)<br>
			<br>
			Upozornenie: hodnota je pred vložením do stránky filtrovaná (špeciálne znaky nahradené HTML entitami), nový riadok je nahradený za medzeru. Ak potrebujete ne-filtrovanú hodnotu použite prefix `unfilter.` v názve parametra. Filtrácia sa predvolene nepoužije pre štandardné objekty typu `doc_data, title`, vynútiť ju môžete prefixom `filter.`.
		</td>
		<td class="t_body">!REQUEST(doc_title)! - WebJET Content Management</td>
	</tr>
</tbody>
</table>
```

## Přihlášený uživatel

Údaje o přihlášeném uživateli (pokud existuje sekce web sídla, která je dostupná pouze pod heslem). Hodnoty lze použít k před vyplnění hodnot formulářových polí. Pokud není přihlášen žádný uživatel, hodnoty všech značek budou prázdný znak.

```html
<table width="100%" class="ramikTable">
<thead>
	<tr>
		<td width="33%" class="head">Značka</td>
		<td width="33%" class="head">Význam</td>
		<td width="33%" class="head">Príklad</td>
	</tr>
</thead>
<tbody>
	<tr>
		<td class="t_body">!LOGGED_USER_ID!</td>
		<td class="t_body">ID používateľa</td>
		<td class="t_body">21</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_NAME!</td>
		<td class="t_body">celé meno a titul aktuálne prihláseného používateľa</td>
		<td class="t_body">Ing. Ľuboš Balát</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_FIRSTNAME!</td>
		<td class="t_body">meno aktuálne prihláseného používateľa</td>
		<td class="t_body">Ľuboš</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_LASTNAME!</td>
		<td class="t_body">priezvisko aktuálne prihláseného používateľa</td>
		<td class="t_body">Balát</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_TITLE!</td>
		<td class="t_body">titul aktuálne prihláseného používateľa</td>
		<td class="t_body">Ing.</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_LOGIN!</td>
		<td class="t_body">prihlasovacie meno</td>
		<td class="t_body">lbalat</td>
	</tr>
	<tr>
		<td class="t_body" nowrap="nowrap">!LOGGED_USER_PASSWORD!</td>
		<td class="t_body">heslo</td>
		<td class="t_body">abcdefgh</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_EMAIL!</td>
		<td class="t_body">email</td>
		<td class="t_body">lubos.balat@interway.sk</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_PHONE!</td>
		<td class="t_body">telefón</td>
		<td class="t_body">02/4959 1589</td>
	</tr>
	<tr>
		<td class="t_body" nowrap="nowrap">!LOGGED_USER_BIRTH_DATE!</td>
		<td class="t_body">dátum narodenia</td>
		<td class="t_body">5.6.1977</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_COMPANY!</td>
		<td class="t_body">firma</td>
		<td class="t_body">InterWay, a. s.</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_ADDRESS!</td>
		<td class="t_body">ulica</td>
		<td class="t_body">Stará Vajnorská 21</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_CITY!</td>
		<td class="t_body">mesto</td>
		<td class="t_body">Bratislava</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_COUNTRY!</td>
		<td class="t_body">krajina</td>
		<td class="t_body">Slovensko</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_ZIP!</td>
		<td class="t_body">PSČ</td>
		<td class="t_body">83104</td>
	</tr>
	<tr>
		<td class="t_body">!LOGGED_USER_FIELDA! - !LOGGED_USER_FIELDE!</td>
		<td class="t_body">voľné pole A až E</td>
		<td class="t_body">hodnota</td>
	</tr>
</tbody>
</table>
```
