# Přechod ze Struts do Spring

Struts je stará MVC technologie (framework), která s pomocí konfiguračního souboru `struts-config.xml` mapuje příchozí požadavky na `Struts Action` objekty. Ve WebJETu postupně Struts nahrazujeme za Spring MVC/REST.

## Původní mapování pomocí Struts

Mapování URL adres `*.do` se konfiguruje v souboru `struts-config.xml`, v ukázce mapování `/inquiry.answer` (skutečné URL má příponu `.do`, čili `/inquiry.answer.do`) na třídu `InquiryAnswerAction`.

```xml
<action path="/inquiry.answer" type="sk.iway.iwcm.inquiry.InquiryAnswerAction">
		<forward name="ok" path="/components/inquiry/ok.jsp"/>
		<forward name="fail" path="/components/inquiry/fail.jsp"/>
</action>
```

Action třída, v tomto případě `InquiryAnswerAction`, zpracovává požadavek pomocí metody `execute` a může využívat data uložená ve `ActionForm` bean. Následně používá metodu `mapping.findForward()` pro přesměrování požadavku na výstupní zobrazení v JSP souboru. V příkladu je použito přesměrování k zobrazení `fail`, které je definováno v souboru `struts-config.xml` jak `/components/inquiry/fail.jsp`.

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

## Přechod z mapování Struts na Spring

Pro přechod na Spring vymažte mapování ze `struts-config.xml` a vytvořte nové ve Spring kontroloři. Problém je, pokud potřebujete zachovat původní URL adresy (jelikož se mohou používat v různých JSP souborech). Nelze vytvořit Spring mapování na `/cokolvek.do`, vzhledem k tomu `*.do` zpracuje Struts `servlet`. Řešením je třída `UnknownAction`, která mapuje neznámé Struts `*.do` volání na `*.struts`, které již můžete ve Spring mapovat:

```xml
    <action path="/unknown" type="sk.iway.iwcm.sync.UnknownAction" unknown="true" />
```

Třída `UnknownAction` se neznámé požadavky upraví tak, že přípona `.do` se nahradí za příponu `.struts`:

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

Čili pokud potřebujete zachovat URL adresu `/cokolvek.do` ve Spring třídě nastavíte mapování na `/cokolvek.struts`:

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

## Příklady implementace

### Přesun požadavku na JSP soubor

Ve většině případů bude zřejmě stačit, aby metoda kontroléru mapující požadavek měla návratový typ `String` pro přesun požadavku na JSP soubor. , například můžeme vrátit odkaz k souboru `ok.jsp`, který sloužil jako výstupní zobrazení úspěšného hlasování v anketě. Třeba si dávat pozor, aby linka neobsahovala příponu `.jsp`, to zajistí nastavené Spring mapování, které k vrácenému odkazu přidá příponu `.html` pro hledání `Thymeleaf` šablony nebo pokud ji nenajde příponu `.jsp` pro použití `jsp` souboru.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        return "/components/inquiry/ok";
    }
```

### Přesměrování

Pokud potřebujete provést přesměrování na jinou stránku stačí vrácené hodnotě přidat předponu `redirect:`, doporučujeme ale použít metodu `String SpringUrlMapping.redirect(String url)` pro přidání prefixu, abyste neudělali překlep. Příklad využití je situace kdy uživatel ještě není přihlášen, tak jej přesměruje na přihlašovací stránku.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        //return "redirect:/admin/logon/";
        return SpringUrlMapping.redirect("/admin/logon/");
    }
```

### Přímý výstup HTML kódu

Pokud potřebujete ve třídě generovat přímo HTML kód můžete použít `PrintWriter`. Metoda musí mít návratový typ `void` a anotaci `@ResponseBody`.

V takovém případě přesměrování již neděláte pomocí předpony `redirect:` ale přímo přes `HttpServletResponse.sendRedirect` metody.

V případě přesunu požadavku na JSP soubor můžete využít metodu `request.getRequestDispatcher("/admin/findex.jsp").forward(request, response);`.

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
		out.println("<strong>" + prop.getText("findexer.indexing") + "</strong><br>");
		out.flush();
        ...
        request.setAttribute("indexedFiles", indexedFiles);
		request.getRequestDispatcher("/admin/findex.jsp").forward(request, response);
    }
```
