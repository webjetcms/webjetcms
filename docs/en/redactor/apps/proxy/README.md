# Proxy

The proxy application allows you to embed a page, or an entire section from another website, into a WebJET page. The condition of use is that when integrating the whole part of the remote web site, it can be identified by the URL prefix.

## Page settings

If you are not using a proxy to call REST services, but you are embedding the output into your site, you need to create a Web page on which to embed the part via the proxy application. Create it as a regular web page that will have **blank text**. If you are embedding a whole section from another web site, edit the virtual path of the page so that the character at the end is `*`. This will display the WebJET page for any URL starting with that address. For example, `/cobrand/poistenie/*`.

## Application settings

After the page is created, you need to set the address mapping parameters. Go to the Applications/Proxy menu. Click on the icon to add a new entry. Enter the following:
- Name - your identifying proxy name (free text)
- Local URL - the mapping address on your website, for example `/cobrand/poistenie/` (without end `*`). In the web pages, create a web page with such a URL, you can use the character `*` in the URL, e.g. `/cobrand/poistenie/*` to create a web page that will accept all URLs starting with the specified value.
- Remote server - address of the remote server (without http prefix), for example `reservations.bookhostels.com`
- Remote URL - the URL of a remote server, for example `/custom/index.php`
- Remote port - the port on which the remote server runs, by default `80`
- Character encoding - character encoding of the remote server (e.g. `windows-1250` or `utf-8`)
- Proxy type - select an option `ProxyByHttpClient4`, older version `ProxyBySocket` does not support all options (e.g. authorization).
- Extensions inserted into the page - a list of extensions that will be inserted into the web page (e.g. `.htm,.html,.php,.asp,.aspx,.jsp,.do,.action`), other files (images, PDF...) will be sent directly to the output. If you use the proxy to call the REST service enter a blank value, for this value the response will never be inserted into the web page, the response will be forwarded directly to the client.
- HTML trim code - start - if the received HTML code needs to be trimmed, enter the start of the trim here, for example `<body`
- Keep the beginning HTML code in the output - if you want the specified beginning of the clipping to be kept in the output, enable this option. E.g. if the clipping start code is specified as `<div id="content` and you need to have this code in the output as well.
- HTML clipping code - start - end of HTML code for clipping, for example `</body`
- Leave ending HTML code in output - similar to the start code, when enabled, the option will also insert the specified ending HTML code into the output.

Multiple URLs can be entered in the Local URL field (each on a new line) to be used for the proxy call, they have the following options:
- `/url-adresa/` - will be used for pages starting with the specified URL, e.g. also for `/url-adresa/25/`.
- `^/url-adresa/$` - is used for pages with a precisely specified URL, i.e. only for the URL `/url-adresa/`.
- `/url-adresa/$` - is used for pages with an address ending in the specified value, e.g. for `/nieco/ine//url-adresa/`.

## Security

In the security tab, it is possible to activate authorization against a remote server. When the REST service is called, the HTTP header `AUTH_USER_CMS` sends the login name of the currently logged in user (if logged in).

For the Basic authorization method, it is necessary to enter a name and password, and for NTLM authorization, the Host and Domain must also be entered.

If you need to enable only certain HTTP methods, you can enter a comma-separated list of them in the Allowed HTTP Methods field. For methods other than enabled, the HTTP status will be returned `403`.

## Advanced options

If you need to make some modifications in the output HTML code from the remote server, you can use the ready-made component `proxy.jsp`. First, make a copy of it in the directory `/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp`. Insert the component into the original page using the code:

`!INCLUDE(/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp)!`

You can then make replacements to the HTML code in the component, or make other modifications. You can also use the component if you need to insert other texts into the page besides the output from the remote server.
