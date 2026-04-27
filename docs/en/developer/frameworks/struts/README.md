# Transitioning from Struts to Spring

Struts is an old MVC technology (framework) that maps incoming requests to `struts-config.xml` objects using a configuration file. Since WebJET 2025, struts has been replaced by Spring MVC/REST.

## Original mapping using Struts

The mapping of URL addresses `*.do` is configured in the file `struts-config.xml`, in the example the mapping of `/inquiry.answer` (the actual URL has the suffix `.do`, i.e. `/inquiry.answer.do`) to the class `InquiryAnswerAction`.

```xml
<action path="/inquiry.answer" type="sk.iway.iwcm.inquiry.InquiryAnswerAction">
		<forward name="ok" path="/components/inquiry/ok.jsp"/>
		<forward name="fail" path="/components/inquiry/fail.jsp"/>
</action>
```

The Action class, in this case `InquiryAnswerAction`, processes the request using the method `execute` and can use data stored in the `ActionForm` bean. It then uses the method `mapping.findForward()` to redirect the request to the output view in the JSP file. In the example, the redirection is used to the view `fail`, which is defined in the file `struts-config.xml` as `/components/inquiry/fail.jsp`.

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

## Transitioning from Struts to Spring mapping

To switch to Spring, delete the mapping from `struts-config.xml` and create a new one in the Spring controller. The problem is if you need to keep the original URLs (since they can be used in different JSP files). It is not possible to create a Spring mapping to `/cokolvek.do`, since `*.do` handles Struts `servlet`. The solution is a class `UnknownAction` that maps the unknown Struts `*.do` calls to `*.struts`, which you can already map in Spring:

```xml
    <action path="/unknown" type="sk.iway.iwcm.sync.UnknownAction" unknown="true" />
```

The unknown requirements of the `UnknownAction` class are modified so that the `.do` suffix is ​​replaced with the `.struts` suffix:

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

So if you need to keep the URL address `/cokolvek.do` in the Spring class, you set the mapping to `/cokolvek.struts`:

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

## Implementation examples

### Moving a request to a JSP file

In most cases, it will probably be enough for the controller method mapping the request to have a return type of `String` to transfer the request to the JSP file. For example, we can return a link to the file `ok.jsp`, which served as the output display of a successful vote in the poll. It is necessary to be careful that the line does not contain the extension `.jsp`, this will be ensured by the configured Spring mapping, which will add the extension `.html` to the returned link to search for the `Thymeleaf` template or, if it does not find it, the extension `.jsp` to use the `jsp` file.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        return "/components/inquiry/ok";
    }
```

### Redirection

If you need to redirect to another page, just add the prefix `redirect:` to the returned value, but we recommend using the `String SpringUrlMapping.redirect(String url)` method to add the prefix to avoid typos. An example of use is when the user is not yet logged in, so redirect them to the login page.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        //return "redirect:/admin/logon/";
        return SpringUrlMapping.redirect("/admin/logon/");
    }
```

### Direct HTML code output

If you need to generate HTML code directly in the class, you can use `PrintWriter`. The method must have a return type of `void` and an annotation of `@ResponseBody`.

In this case, you no longer redirect using the `redirect:` prefix but directly via the `HttpServletResponse.sendRedirect` method.

If you want to move the request to a JSP file, you can use the `request.getRequestDispatcher("/admin/findex.jsp").forward(request, response);` method.

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

### JSP tags

Tags like `<logic:present/iterate/...` replaced with corresponding `<iwcm:present/iterate/...`, be careful `<bean:write` for `<iwcm:beanWrite`.