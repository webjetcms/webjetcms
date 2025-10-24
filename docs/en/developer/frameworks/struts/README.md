# Transition from Struts to Spring

Struts is an old MVC technology (framework) that with the help of a configuration file `struts-config.xml` maps incoming requests to `Struts Action` objects. As of WebJET 2025, struts is replaced by Spring MVC/REST.

## Original mapping using Struts

URL mapping `*.do` is configured in the file `struts-config.xml`, in the sample mapping `/inquiry.answer` (the actual URL has the suffix `.do` that is to say `/inquiry.answer.do`) per class `InquiryAnswerAction`.

```xml
<action path="/inquiry.answer" type="sk.iway.iwcm.inquiry.InquiryAnswerAction">
		<forward name="ok" path="/components/inquiry/ok.jsp"/>
		<forward name="fail" path="/components/inquiry/fail.jsp"/>
</action>
```

Action class, in this case `InquiryAnswerAction`, processes the request using the method `execute` and may use data stored in `ActionForm` bean. It then uses the method `mapping.findForward()` to redirect the request to the output view in the JSP file. In the example, the redirection is used to display `fail`, which is defined in the file `struts-config.xml` Like `/components/inquiry/fail.jsp`.

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

## Switching from Struts to Spring mapping

To switch to Spring, delete the mapping from `struts-config.xml` and create a new one in Spring controllers. The problem is if you need to keep the original URLs (since they can be used in different JSP files). It is not possible to create a Spring mapping to `/cokolvek.do` as `*.do` handled by Struts `servlet`. The solution is a class `UnknownAction` which maps the unknown Struts `*.do` calls to `*.struts` that you can already map in Spring:

```xml
    <action path="/unknown" type="sk.iway.iwcm.sync.UnknownAction" unknown="true" />
```

Class `UnknownAction` the unknown requirements are modified so that the suffix `.do` is replaced by the suffix `.struts`:

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

So if you need to keep the URL `/cokolvek.do` in Spring class you set the mapping to `/cokolvek.struts`:

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

## Examples of implementation

### Move a request to a JSP file

In most cases, it will probably be sufficient for the controller method mapping the request to have a return type of `String` to move the request to the JSP file. For example, we can return a reference to a file `ok.jsp`, which served as an output display of the successful poll vote. Care should be taken that the line does not contain the suffix `.jsp`, this will ensure that Spring mapping is set to add a suffix to the returned link `.html` for search `Thymeleaf` template or if it does not find the suffix `.jsp` for use `jsp` file.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        return "/components/inquiry/ok";
    }
```

### Redirect to

If you need to redirect to another page, just add a prefix to the returned value `redirect:`, but we recommend to use the method `String SpringUrlMapping.redirect(String url)` to add a prefix so you don't make a typo. An example use case is when the user is not yet logged in, so it redirects them to the login page.

```java
    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer() {
        ...
        //return "redirect:/admin/logon/";
        return SpringUrlMapping.redirect("/admin/logon/");
    }
```

### Direct HTML code output

If you need to generate HTML code directly in the class, you can use `PrintWriter`. The method must have a return type `void` and annotation `@ResponseBody`.

In this case, you no longer do the redirection using the prefix `redirect:` but directly through `HttpServletResponse.sendRedirect` Methods.

In case of moving a request to a JSP file, you can use the method `request.getRequestDispatcher("/admin/findex.jsp").forward(request, response);`.

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

Brands like `<logic:present/iterate/...` substituted for the corresponding `<iwcm:present/iterate/...`, beware `<bean:write` For `<iwcm:beanWrite`.
