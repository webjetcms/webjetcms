# Zapomenuté heslo

Pokud jste zapomněli své heslo, můžete jej obnovit následujícím způsobem.

## Sekce správce

Pokud jste zapomněli heslo do sekce správce, můžete si na přihlašovací stránce vyžádat obnovení hesla.

Klikněte na možnost ![](admin-recover-password-btn.png ":no-zoom") zobrazí se formulář pro obnovení hesla.

![](admin-recovery-page.png)

Musíte zadat svůj **e-mailová adresa** nebo **přihlašovací jméno**. Chcete-li žádost odeslat, stiskněte tlačítko ![](admin-send-btn.png ":no-zoom"). Upozornění vás upozorní, že pokud účet existuje, bude vám na příslušnou e-mailovou adresu zaslán e-mail.

![](admin-recovery-page-notif.png)

## Zákaznická zóna

Pokud jste zapomněli heslo do zákaznické zóny, můžete si na přihlašovací stránce vyžádat obnovení hesla.

Klikněte na možnost **Zapomněli jste heslo?** zobrazí se skryté pole pro obnovení.

| Před | Po |
| :---------------------------: | :---------------------------: |
| ![](user-recovery-page-1.png) | ![](user-recovery-page-2.png) |

Musíte zadat svůj **e-mailová adresa** nebo **přihlašovací jméno**. Chcete-li žádost odeslat, stiskněte tlačítko ![](user-send-btn.png ":no-zoom"). Upozornění vás upozorní, že pokud účet existuje, bude vám na příslušnou e-mailovou adresu zaslán e-mail.

![](user-recovery-page-notif.png)

## E-mail pro změnu hesla

Odeslaný e-mail obsahuje 2 odkazy:
- odkaz na změnu hesla, **Chcete-li změnit heslo, klikněte do 30 minut zde.**
- odkaz na zrušení akce změny hesla, **Pokud jste o změnu hesla nepožádali, můžete tuto akci zrušit kliknutím sem.**

![](email.png)

### Akce změny hesla

Klikněte na první odkaz, **Chcete-li změnit heslo, klikněte do 30 minut zde.**, budete přesměrováni na stránku pro změnu hesla.

| Sekce správce | Uživatelská sekce
| :----------------------------: | :---------------------------: |
| ![](admin-recovery-form-1.png) | ![](user-recovery-form-1.png) |

!> **Varování:** přihlašovací jméno je typem výběrového pole kvůli možnosti registrace více přihlašovacích jmen se stejným e-mailem (např. přihlášení do administrace a zákaznického účtu). Výběrové pole tedy obsahuje všechna přihlašovací jména, která sdílejí zadanou e-mailovou adresu. **Heslo se změní pouze pro uživatele, jehož přihlašovací jméno jste zvolili.**

Poté musíte zadat nové heslo a znovu jej zadat pro ověření. Pokud se hesla neshodují nebo nesplňují minimální požadavky na kvalitu hesla, budete na to upozorněni.

| Heslo neodpovídá | Slabé heslo |
| :----------------------------: | :----------------------------: |
| ![](admin-recovery-form-2.png) | ![](admin-recovery-form-3.png) |
| ![](user-recovery-form-2.png)  | ![](user-recovery-form-3.png)  |

Pokud se heslo vybraného uživatele úspěšně změní, zobrazí se následující zpráva.

| Sekce správce | Uživatelská sekce
| :----------------------------: | :---------------------------: |
| ![](admin-recovery-form-4.png) | ![](user-recovery-form-4.png) |

!> **Varování:** po úspěšné změně hesla odkaz, který vás přivedl na formulář pro změnu hesla. **se stává nefunkční** to znamená, že jej nelze použít k opětovné změně hesla pro stejného nebo jiného uživatele. Odkaz se rovněž stane nefunkčním, pokud jste akci neprovedli do 30 minut od obdržení e-mailu.

| Sekce správce | Uživatelská sekce
| :-------------------------------------: | :------------------------------------: |
| ![](admin-recovery-form-notWorking.png) | ![](user-recovery-form-notWorking.png) |

### Změna akce zrušení

Klikněte na druhý odkaz **Pokud jste o změnu hesla nepožádali, můžete tuto akci zrušit kliknutím sem.** budete přesměrováni zpět na stránku, která vás informuje, že akce změny hesla byla zrušena, a na které se zobrazí první odkaz pro změnu hesla. **se stala nefunkční**.

| Sekce správce | Uživatelská sekce
| :---------------------------------: | :--------------------------------: |
| ![](admin-recovery-form-cancel.png) | ![](user-recovery-form-cancel.png) |

## Poznámky k implementaci

- změna hesla funguje prostřednictvím auditní stopy, kdy se při požadavku na změnu hesla vytvoří záznam typu. `USER_CHANGE_PASSWORD` který má v popisu text `Vyžiadanie zmeny hesla`
- při požadavku na změnu hesla se kontroluje, zda tato auditní stopa existuje a zda není starší než 30 minut; pokud neexistuje nebo je starší, odkaz na změnu hesla již nebude fungovat a auditní stopa zůstane zachována.
- při použití odkazu pro zrušení změny hesla je tento záznam z auditu vymazán.
- pokud je e-mail použitý ke změně hesla spojen s více účty, obsahuje auditní záznam vždy přihlašovací jméno posledního uživatele, který může změnit heslo prostřednictvím tohoto e-mailu.
- po úspěšné změně hesla je auditní záznam vymazán.
