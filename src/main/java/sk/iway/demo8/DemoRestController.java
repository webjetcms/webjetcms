package sk.iway.demo8;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.spring.SpringSecurityConf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class DemoRestController
{
	@GetMapping(path={"/demo-test"})
	public String test(HttpServletRequest request)
	{
		final StringBuilder result = new StringBuilder();
		result.append("Demo OK ").append(Constants.getInstallName()).append("\n<br>").append("\n<br>");

		result.append("docList.size="+DocDB.getInstance().getDocByGroup(4).size()).append("\n<br>").append("\n<br>");

		result.append("Java 17 (<a href=\"https://www.baeldung.com/java-17-new-features\">https://www.baeldung.com/java-17-new-features</a>)").append("\n<br>");

		IntStream random = RandomGeneratorFactory.of("Random")
				.create()
				.ints(10, 0, 100);

		result.append("<b>2.2. Enhanced Pseudo-Random Number Generators</b>").append("\n<br>");
		random.forEach(i -> result.append(i).append("\n<br>"));
		result.append("\n<br>").append("\n<br>");

		result.append("Java 16 (<a href=\"https://www.baeldung.com/java-16-new-features\">https://www.baeldung.com/java-16-new-features</a>)").append("\n<br>");

		String r = "";
		Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HelloWorld.class },
				(prox, method, args) -> {
					if (method.isDefault()) {
						return InvocationHandler.invokeDefault(prox, method, args);
					}

					return "default not found";
				}
		);
		try {
			Method method = proxy.getClass().getMethod("hello");
			r = (String) method.invoke(proxy);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		result.append("<b>2. Invoke Default Methods From Proxy Instances</b>").append("\n<br>");
		result.append("Vysledok by mal byt world, je: ").append(r);
		result.append("\n<br>").append("\n<br>");

		String name = Tools.getParameter(request, "name");
		result.append("\n<p class='name-request'>").append(name).append("</p>");

		return result.toString();
	}

	/**
	 * Get current basic auth status. It's used in autotests api-auth.js
	 * @return
	 */
	@GetMapping(path={"/rest/basic-auth-enabled"})
	public String isBasicAuthEnabled()
	{
		return "{ \"result\": " + SpringSecurityConf.isBasicAuthEnabled() + " }";
	}

	/**
	 * Test private URL with basic auth in api-auth.js
	 * @return
	 */
	@GetMapping(path={"/private/rest/demo-test"})
	@PreAuthorize(value = "@WebjetSecurityService.isLogged()")
	public String testPrivateUrl()
	{
		return "{ \"result\": \"Demo OK\" }";
	}

	/**
	 * Test POST url referrers in api-auth.js
	 * @param id
	 * @return
	 */
	@PostMapping(value="/private/rest/demo-post", consumes = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize(value = "@WebjetSecurityService.isLogged()")
	public String testPrivatePostUrl(@RequestBody DocDetails doc)
	{
		return "{ \"result\": \"Demo OK\", \"id\": " + doc.getId() + " }";
	}
}