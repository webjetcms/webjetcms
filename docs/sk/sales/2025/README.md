# Obchodné informácie - rok 2025

Tento súbor obsahuje opisy vlastností WebJET CMS dodaných v roku 2025 z pohľadu predaja. Nové záznamy sa pridávajú na vrch (pod tento úvod), takže najnovšie vlastnosti sú vždy hore.

## AI asistenti

WebJET CMS prináša **kompletnú integráciu umelej inteligencie** priamo do redakčného systému. Redaktori a administrátori tak získavajú inteligentných pomocníkov, ktorí im pomáhajú s tvorbou a úpravou obsahu — od textu cez obrázky až po kompletné webové stránky. Na rozdiel od väčšiny CMS systémov, kde je AI len doplnková funkcia tretích strán, vo WebJET CMS je **AI natívne integrovaná do každého editačného okna** — textových polí, obrázkov, editora stránok aj nástroja PageBuilder.

Systém podporuje **viacerých poskytovateľov AI služieb** (OpenAI, Google Gemini, OpenRouter a dokonca AI priamo v prehliadači), čo zákazníkovi dáva slobodu výberu podľa ceny, kvality a dostupnosti. Administrátor môže pre rôzne úlohy nastaviť rôznych poskytovateľov — napríklad lacnejší model pre opravu gramatiky a výkonnejší pre generovanie obsahu. Vďaka podpore **OpenRouter** má zákazník prístup k stovkám AI modelov cez jedno rozhranie, vrátane mnohých bezplatných možností na testovanie.

Unikátnou vlastnosťou je **AI v prehliadači** — využitie modelov priamo na zariadení používateľa bez potreby externého API, čo znamená **nulové náklady za volania** a **maximálnu ochranu údajov**, pretože dáta nikdy neopustia počítač. Táto technológia je ideálna pre organizácie s prísnymi požiadavkami na ochranu osobných údajov.

**AI asistenti sú plne konfigurovateľní** — administrátor môže vytvárať vlastných asistentov s presnými inštrukciami pre konkrétne polia a entity. Každý asistent sa dá priradiť ku konkrétnemu poľu v systéme, takže redaktor vždy vidí len relevantných asistentov. Systém umožňuje definovať inštrukcie, vybrať model, nastaviť streamovanie odpovede a požadovať vstup od používateľa — všetko bez potreby programovania.

V nástroji **PageBuilder** funguje aj **režim chat**, kde AI dokáže generovať kompletné bloky webovej stránky, upravovať existujúce sekcie alebo navrhovať celú štruktúru stránky na základe požiadavky redaktora. Redaktor môže postupne zadávať požiadavky a doladiť výsledok bez manuálneho kódovania.

Súčasťou riešenia je aj **podrobná štatistika využívania** AI asistentov — grafy najpoužívanejších asistentov, spotreba tokenov v čase, identifikácia používateľov s najvyššou spotrebou. To umožňuje organizácii **kontrolovať náklady**, optimalizovať inštrukcie a vyhodnotiť návratnosť investície do AI nástrojov.

**Hlavné benefity:**

- **Natívna integrácia v celom systéme**: AI asistenti sú dostupní v každom textovom poli, obrázkovom poli, webovom editore aj PageBuilderi — redaktor nemusí prepínať medzi nástrojmi.
- **Flexibilita poskytovateľov**: Podpora OpenAI, Gemini, OpenRouter a AI v prehliadači — zákazník si vyberá podľa ceny, kvality a požiadaviek na ochranu údajov.
- **Nulové náklady s AI v prehliadači**: Lokálne spracovanie bez API volaní znamená žiadne poplatky za bežné úlohy ako sumarizácia, preklad alebo úprava textu.
- **Plná konfigurovateľnosť bez programovania**: Administrátor vytvára vlastných asistentov, definuje inštrukcie a priraďuje ich k poliam — žiadne zásahy do kódu.
- **Generovanie a úprava obrázkov**: AI vie vytvoriť ilustračné obrázky z textového popisu, odstrániť pozadie alebo upraviť existujúce fotografie priamo v CMS.
- **Chat režim pre PageBuilder**: Kompletné generovanie a úprava štruktúry webových stránok vrátane blokov, textov a rozloženia cez konverzáciu s AI.
- **Kontrola nákladov**: Podrobné štatistiky spotreby tokenov podľa asistentov, používateľov a dní umožňujú optimalizáciu a predvídateľné rozpočtovanie.
- **Bezpečnosť a súkromie**: Možnosť šifrovania API kľúčov, lokálne AI v prehliadači a podrobné oprávnenia zabezpečujú súlad s bezpečnostnými politikami organizácie.
- **Funkcia vrátenia zmien**: Každý výsledok AI je možné jedným kliknutím vrátiť späť, čo odstraňuje obavy z nesprávnych úprav.

![AI asistenti v editore](../../redactor/ai/datatables/ckeditor.png)

Podrobná dokumentácia: [AI asistenti](../../redactor/ai/README.md)

## Manažér dokumentov

WebJET CMS ponúka **Manažér dokumentov** — komplexnú aplikáciu na **správu dokumentov a ich verzií** na jednom mieste. Organizácia môže centrálne spravovať všetky dôležité dokumenty (zmluvy, formuláre, smernice, technické listy), **automaticky sledovať ich verzie** a zabezpečiť, že návštevníci webu alebo interní používatelia majú vždy prístup k aktuálnej verzii. Systém zároveň uchováva celú históriu zmien, takže je možné kedykoľvek sa vrátiť k predchádzajúcej verzii dokumentu.

Kľúčovou vlastnosťou je **plánovanie publikovania dokumentov do budúcnosti**. Ak organizácia potrebuje zverejniť nový cenník, smernicu alebo formulár k presnému dátumu (napríklad k 1. januáru nového roka), stačí dokument nahrať vopred a nastaviť dátum automatického zverejnenia. Systém v stanovený čas **sám vymení starú verziu za novú** a voliteľne odošle notifikáciu zodpovedným osobám. To eliminuje riziko ľudskej chyby a zabezpečuje **súlad s legislatívnymi termínmi**.

Dokumenty je možné organizovať pomocou **produktov, kategórií a kódov produktov**, čo umožňuje prehľadné filtrovanie aj pri stovkách dokumentov. Systém automaticky **kontroluje duplicitu obsahu** — ak sa niekto pokúsi nahrať dokument, ktorý už v manažéri existuje, systém na to upozorní. Na webovej stránke sa dokumenty zobrazujú pomocou **konfigurovateľnej aplikácie**, kde si redaktor nastaví, ktoré dokumenty a v akom poradí sa majú zobraziť, vrátane možnosti **zobrazenia historických verzií a vzorových dokumentov**.

**Hlavné benefity:**

- **Centrálna správa dokumentov**: Všetky dokumenty organizácie sú na jednom mieste s prehľadnou históriou verzií, kategorizáciou a fulltextovým vyhľadávaním.
- **Automatické publikovanie k dátumu**: Nové verzie dokumentov sa zverejnia automaticky v nastavený čas — ideálne pre cenníky, smernice alebo regulované dokumenty s pevným termínom účinnosti.
- **Správa verzií a rollback**: Kompletná história zmien s možnosťou okamžitého návratu k predchádzajúcej verzii jedným kliknutím, bez potreby IT oddelenia.
- **Ochrana pred duplicitou**: Systém kontroluje obsah nahrávaných súborov a upozorní na existujúce duplicity, čím predchádza chaosu a nekonzistencii.
- **Vzorové dokumenty**: Ku každému hlavnému dokumentu (napr. formuláru) je možné priradiť vzorovo vyplnený dokument, čo zlepšuje používateľský zážitok návštevníkov.
- **Export a import**: Hromadný export dokumentov do ZIP súboru a spätný import umožňujú jednoduché zálohovanie, migráciu medzi prostrediami alebo zdieľanie medzi tímami.
- **Podrobné oprávnenia**: Prístup k jednotlivým funkciám (správa, editácia, export, import, rollback) je riadený samostatnými oprávneniami, čo umožňuje bezpečnú delegáciu úloh.

![Manažér dokumentov](../../redactor/files/file-archive/datatable.png)

Podrobná dokumentácia: [Manažér dokumentov](../../redactor/files/file-archive/README.md)
