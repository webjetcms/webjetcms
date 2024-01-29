# Prechod zo Struts do Spring

Struts je stará MVC technológia (framework), ktorá s pomocou konfiguračného súboru `struts-config.xml` mapuje prichádzajúce požiadavky na `Struts Action` objekty. Vo WebJETe postupne Struts nahrádzame za Spring MVC/REST.

## Pôvodné mapovanie pomocou Struts

Mapovanie URL adries `*.do` sa konfiguruje v súbore `struts-config.xml`, v ukážke mapovanie `/inquiry.answer` (skutočné URL má príponu `.do`, čiže `/inquiry.answer.do`) na triedu `InquiryAnswerAction`.

```xml
<action path="/inquiry.answer" type="sk.iway.iwcm.inquiry.InquiryAnswerAction">
		<forward name="ok" path="/components/inquiry/ok.jsp"/>
		<forward name="fail" path="/components/inquiry/fail.jsp"/>
</action>
```

Action trieda, v tomto prípade `InquiryAnswerAction`, spracováva požiadavku pomocou metódy `execute` a môže využívať údaje uložené vo `ActionForm` bean. Následne používa metódu `mapping.findForward()` na presmerovanie požiadavky na výstupné zobrazenie v JSP súbore. V príklade je použité presmerovanie na zobrazenie `fail`, ktoré je definované v súbore `struts-config.xml` ako `/components/inquiry/fail.jsp`.

```java
    @Override
	public ActionForward execute(ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {

            .
            .
            .

        return(mapping.findForward("fail"));
    }
```

## Prechod z mapovania Struts na Spring

Pre prechod na Spring vymažte mapovanie zo `struts-config.xml` a vytvorte nové v Spring kontrolóri. Problém je, ak potrebujete zachovať pôvodné URL adresy (keďže sa môžu používať v rôznych JSP súboroch). Nie je možné vytvoriť Spring mapovanie na `/cokolvek.do`, keďže `*.do` spracuje Struts `servlet`. Riešením je trieda `UnknownAction`, ktorá mapuje neznáme Struts `*.do` volania na `*.struts`, ktoré už môžete v Spring mapovať:

```xml
    <action path="/unknown" type="sk.iway.iwcm.sync.UnknownAction" unknown="true" />
```

Trieda `UnknownAction` sa neznáme požiadavky upravia tak, že prípona `.do` sa nahradí za príponu `.struts`:

```java
    public class UnknownAction extends Action {

        @Override
        public ActionForward execute(ActionMapping mapping,
                ActionForm form,
                HttpServletRequest request,
                HttpServletResponse response) throws IOException, ServletException {

            HttpSession session = request.getSession();
            if (session == null)
                return (mapping.findForward("logon_admin"));

            String path = PathFilter.getOrigPath(request);

            request.getRequestDispatcher(Tools.replace(path, ".do", ".struts")).forward(request, response);

            return null;
        }
    }
```

Čiže ak potrebujete zachovať URL adresu `/cokolvek.do` v Spring triede nastavíte mapovanie na `/cokolvek.struts`:

```java
    @Controller
    public class InquiryController {

        @GetMapping("/inquiry.answer.struts")
        public String saveAnswer(HttpServletRequest request, HttpServletResponse response) {
            try {
                return InquiryDB.saveAnswer(request, response);
            } catch(Exception e) {
                sk.iway.iwcm.Logger.error(e);
            }

            return null;
        }
    }
```
## Príklady implementácie

### Presun požiadavky na JSP súbor

Vo väčšine prípadov bude zrejme stačiť, aby metóda kontroléra mapujúca požiadavku mala návratový typ `String` pre presun požiadavky na JSP súbor. , napríklad môžeme vrátiť odkaz k súboru `ok.jsp`, ktorý slúžil ako výstupné zobrazenie úspešného hlasovania v ankete. Treba si dávať pozor, aby linka neobsahovala príponu `.jsp`, to zabezpečí nastavené Spring mapovanie, ktoré k vrátenému odkazu pridá príponu `.html` pre hľadanie `Thymeleaf` šablóny alebo ak ju nenájde príponu `.jsp` pre použitie `jsp` súboru.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        return "/components/inquiry/ok";
    }
```

### Presmerovanie

Ak potrebujete vykonať presmerovanie na inú stránku stačí vrátenej hodnote pridať predponu `redirect:`, odporúčame ale použiť metódu `String SpringUrlMapping.redirect(String url)` pre pridanie prefixu, aby ste nespravili preklep. Príklad využitia je situácia kedy užívateľ ešte nie je prihlásený, tak ho presmeruje na prihlasovaciu stránku.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        //return "redirect:/admin/logon/";
        return SpringUrlMapping.redirect("/admin/logon/");
    }
```

### Priamy výstup HTML kódu

Ak potrebujete v triede generovať priamo HTML kód môžete použiť `PrintWriter`. Metóda musí mať návratový typ `void` a anotáciu `@ResponseBody`.

V takomto prípade presmerovanie už nerobíte pomocou predpony `redirect:` ale priamo cez `HttpServletResponse.sendRedirect` metódy.

V prípade presunu požiadavky na JSP súbor môžete využiť metódu `request.getRequestDispatcher("/admin/findex.jsp").forward(request, response);`.

```java
    @ResponseBody
    @GetMapping("/findex.struts")
    public void indexFileOrFolder(@RequestParam(required = false) String file, @RequestParam(required = false) String dir, HttpServletRequest request, HttpServletResponse response) {
        ...
        if (user == null || user.isAdmin() == false) {
			response.sendRedirect("/admin/logon.jsp");
			return;
		}
        ...
        PrintWriter out = response.getWriter();
        out.println("<html><head><LINK rel='stylesheet' href='/admin/css/style.css'></head><body>");
		out.println("<strong>" + prop.getText("findexer.indexing") + "</strong><br/>");
		out.flush();
        ...
        request.setAttribute("indexedFiles", indexedFiles);
		request.getRequestDispatcher("/admin/findex.jsp").forward(request, response);
    }
```