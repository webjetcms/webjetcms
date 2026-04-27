# Proxies

The proxy application allows you to insert a page, or a whole section, from another website into a WebJET page. The condition for use is that when integrating the entire section of the remote website, it can be identified by the URL prefix.

## Page setup

If you are not using a proxy to call REST services, but you are embedding the output into your website, you need to create a Web page on which the part is to be embedded via the proxy application. Create it as a regular web page that will have **empty text**. If you are embedding the entire part from another website, edit the virtual path of the page so that the character ```*``` ends. This way, WebJET will display the page for any URL starting with the given address. For example ```/cobrand/poistenie/*```.

## Application settings

After creating the page, you need to set the address mapping parameters. Go to the Applications/Proxy menu. Click the icon to add a new record. Enter the following items:

- Name - your proxy identification name (any text)
- Local URL - the mapping address on your website, for example ```/cobrand/poistenie/``` (without the trailing ```*```). In websites, create a website with such a URL, you can use the `*` character in the URL, for example `/cobrand/poistenie/*` to create a website that will accept all URLs starting with the specified value.
- Remote server - remote server address (without http prefix), for example ```reservations.bookhostels.com```
- Remote URL - The URL of the remote server, for example ```/custom/index.php```
- Remote port - the port on which the remote server is running, by default ```80```
- Character encoding - character encoding of the remote server (e.g. ```windows-1250``` or ```utf-8```)
- Proxy type - select option `ProxyByHttpClient4`, the older version `ProxyBySocket` does not support all options (e.g. authorization).
- Extensions inserted into the page - a list of extensions that will be inserted into the web page (e.g. ```.htm,.html,.php,.asp,.aspx,.jsp,.do,.action```), other files (images, PDF...) will be directly sent to the output. If you use a proxy to call the REST service, enter an empty value, for this value the response will never be inserted into the web page, the response will be forwarded directly to the client.
- HTML trim code - start - if the received HTML code needs to be trimmed, enter the start of the trim here, for example ```<body```
- Keep starting HTML code in output - if you want the specified start of the trim to be kept in the output, enable this option. For example, if the starting trim code is specified as `<div id="content` and you need to have this code in the output as well.
- HTML trim code - start - end HTML trim code, for example ```</body```
- Leave the end HTML code in the output - similar to the start code, when this option is enabled, the specified end HTML code will also be inserted into the output.

In the Local URL field, you can enter multiple URLs (each on a new line) that will be used to call the proxy, with the following options:

- `/url-adresa/` - ​​will be used for pages starting with the specified URL address, e.g. also for `/url-adresa/25/`.
- `^/url-adresa/-  - ​​used for pages with a precisely specified URL address, i.e. only for the URL address ` /url-adresa/`.
- `/url-adresa/-  - ​​used for pages with an address ending in the specified value, e.g. for ` /nieco/ine//url-adresa/`.

## Security

In the security tab, you can activate authorization against a remote server. When calling a REST service, the login name of the currently logged in user (if logged in) is sent in the HTTP header `AUTH_USER_CMS`.

For the Basic authorization method, you need to enter a name and password, and for NTLM authorization, you need to enter a Host and Domain.

If you need to allow only some HTTP methods, you can enter a comma-separated list of them in the Allowed HTTP Methods field. For methods other than those allowed, an HTTP status of `403` will be returned.

## Advanced options

If you need to make some adjustments to the output HTML code from a remote server, you can use the ready-made component ```proxy.jsp```. First, make a copy of it in the directory
```/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp```.
Komponentu vložte do pôvodnej stránky pomocou kódu:

```!INCLUDE(/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp)!```

Následne môžete v komponente vykonať nahradenia HTML kódu, prípadne vykonať iné úpravy. Komponentu môžete použiť aj ak potrebujete do stránky okrem výstupu zo vzdialeného servera vkladať aj iné texty.