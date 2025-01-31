# MultiWeb

## Úvod

MultiWeb je speciální typ instalace pro více uživatelů/více nájemců v jednom WebJETu. Domény jsou navenek oddělené a každá se tváří jako samostatná instalace systému WebJET CMS.

Instalace obsahuje řídicí doménu (první vytvořenou), prostřednictvím které jsou k dispozici zvýšená oprávnění (např. pro konfiguraci, překladové klíče atd.). Aplikace, které mají dopad na všechny domény, nejsou v jednotlivých doménách nájemců dostupné.

## Omezení

Instalace MultiWebu obsahují následující omezení:
- Webové stránky
  - Skupiny médií - zobrazují se a lze je upravovat pouze u skupin médií, které mají nastavena práva na složky v aktuální doméně; nově přidané skupině médií jsou automaticky přidělena práva na kořenové složky v doméně. Globální skupiny médií v řídicí doméně můžete spravovat bez zadání omezení Zobrazit pro.
- Aplikace
  - Přesměrování domény - lze spravovat pouze přesměrování domény směřující na aktuální doménu.
- Šablony
  - V seznamu jsou zobrazeny šablony, které mají nastavený přístup do složek aktuálně zobrazené domény, při vytvoření nové šablony, která by neměla nastavený přístup, se přístup automaticky nastaví do kořenových složek aktuální domény.
  - Skupiny šablon - zobrazí skupiny šablon použité v šablonách.
- Uživatelé
  - Každá doména má svůj vlastní seznam uživatelů.
  - Pro uživatele, který nemá nastavena žádná omezení stromové struktury složek webu, se při kontrole oprávnění interně nastaví seznam kořenových složek domény.

## Aplikace dostupné pouze v doméně správy

Následující aplikace jsou k dispozici pouze v doméně správy:
- Úvod
  - Audit
  - Monitorování serveru
- Aplikace
  - GDPR - Regulární výrazy
  - GDPR - Výmaz údajů
  - Hromadný e-mail - Omezení domény
- Uživatelé
  - Skupiny uživatelů
  - Skupiny práv
- Ovládací panel
  - Konfigurace
  - Překladové klíče
  - Hlavičky HTTP
  - Vymazání dat
  - Automatizované úkoly
  - Aktualizace systému WebJET CMS
  - Restartování

Pokud potřebujete přidat uživateli v doméně nějaká práva, můžete přidat proměnnou conf. `multiwebSpecialPerms-USERID` Seznam dalších práv, která má uživatel získat, oddělený čárkou.
