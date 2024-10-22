# Přechod ze systému Struts na Spring

Struts je stará technologie MVC (framework), která pomocí konfiguračního souboru `struts-config.xml` mapuje příchozí požadavky na `Struts Action` objekty. Ve WebJETu postupně nahrazujeme Struts systémem Spring MVC/REST.

## Původní mapování pomocí Struts

Mapování adres URL `*.do` je nakonfigurován v souboru `struts-config.xml`, ve vzorovém mapování `/inquiry.answer` (skutečná adresa URL má příponu `.do` to znamená. `/inquiry.answer.do`) na třídu `InquiryAnswerAction`.

```xml
<action path="/inquiry.answer" type="sk.iway.iwcm.inquiry.InquiryAnswerAction">
		<forward name="ok" path="/components/inquiry/ok.jsp"/>
		<forward name="fail" path="/components/inquiry/fail.jsp"/>
</action>
```

Akční třída, v tomto případě `InquiryAnswerAction`, zpracuje požadavek pomocí metody `execute` a může používat data uložená v `ActionForm` fazole. Poté použije metodu `mapping.findForward()` přesměrovat požadavek na výstupní zobrazení v souboru JSP. V příkladu je přesměrování použito k zobrazení `fail`, který je definován v souboru `struts-config.xml` Stejně jako `/components/inquiry/fail.jsp`.

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

Chcete-li přejít na Spring, odstraňte mapování z položky `struts-config.xml` a vytvořit nový v řídicích jednotkách Spring. Problém je, pokud potřebujete zachovat původní adresy URL (protože je lze použít v různých souborech JSP). Není možné vytvořit mapování Spring na `/cokolvek.do` jako `*.do` zpracovává Struts `servlet`. Řešením je třída `UnknownAction` který mapuje neznámý server Struts `*.do` volání na `*.struts` které můžete mapovat již na jaře:

```xml
    <action path="/unknown" type="sk.iway.iwcm.sync.UnknownAction" unknown="true" />
```

Třída `UnknownAction` neznámé požadavky jsou upraveny tak, aby přípona `.do` je nahrazen koncovkou `.struts`:

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

Pokud tedy potřebujete zachovat adresu URL `/cokolvek.do` ve třídě Spring nastavíte mapování na `/cokolvek.struts`:

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

## Příklady provádění

### Přesunutí požadavku do souboru JSP

Ve většině případů bude pravděpodobně stačit, když metoda řadiče mapující požadavek bude mít návratový typ `String` přesunout požadavek do souboru JSP. Můžeme například vrátit odkaz na soubor `ok.jsp`, který sloužil jako výstupní zobrazení úspěšného hlasování v anketě. Je třeba dbát na to, aby řádek neobsahoval příponu `.jsp`, což zajistí, že mapování Spring bude nastaveno tak, aby k vrácenému odkazu přidávalo příponu. `.html` pro vyhledávání `Thymeleaf` nebo pokud nenajde příponu `.jsp` pro použití `jsp` soubor.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        return "/components/inquiry/ok";
    }
```

### Přesměrování na

Pokud potřebujete přesměrovat na jinou stránku, stačí přidat k vrácené hodnotě předponu `redirect:`, ale doporučujeme použít metodu `String SpringUrlMapping.redirect(String url)` přidat předponu, aby nedošlo k překlepu. Příkladem použití je situace, kdy uživatel ještě není přihlášen, a proto ho přesměruje na přihlašovací stránku.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        //return "redirect:/admin/logon/";
        return SpringUrlMapping.redirect("/admin/logon/");
    }
```

### Přímý výstup kódu HTML

Pokud potřebujete generovat kód HTML přímo ve třídě, můžete použít příkaz `PrintWriter`. Metoda musí mít návratový typ `void` a anotace `@ResponseBody`.

V tomto případě již přesměrování neprovádíte pomocí předpony `redirect:` ale přímo prostřednictvím `HttpServletResponse.sendRedirect` Metody.

V případě přesunu požadavku do souboru JSP můžete použít metodu `request.getRequestDispatcher("/admin/findex.jsp").forward(request, response);`.

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
