// Function to get CSRF token from cookies
async function getCsrfToken() {
  return fetch('/admin/v9/')
    .then(response => response.text())
    .then(html => {
      const parser = new DOMParser();
      const doc = parser.parseFromString(html, 'text/html');
      const scriptTags = doc.getElementsByTagName('script');
      for (let script of scriptTags) {
        if (script.textContent.includes('window.csrfToken')) {
          const tokenMatch = script.textContent.match(/window\.csrfToken\s*=\s*['"]([^'"]+)['"]/);
          if (tokenMatch) {
            return tokenMatch[1];
          }
        }
      }
    });
}

async function initializeSwaggerUI() {
  let csrfToken = await getCsrfToken();
  if (typeof csrfToken === 'undefined') {
    console.error('CSRF token not found in the page, Try it out button will not work. You can set window.csrfToken="xxx" manually.');
  } else {
    window.csrfToken = csrfToken;
  }
  //console.debug('CSRF token:', window.csrfToken);
  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  window.ui = SwaggerUIBundle({
    url: "/admin/rest/openapi/api-docs",
    dom_id: '#swagger-ui',
    deepLinking: true,
    displayOperationId: true,
    filter: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout",
    validatorUrl: null,
    requestInterceptor: (req) => {
      req.headers['X-CSRF-Token'] = window.csrfToken;
      return req;
    }
  });
}

window.onload = function() {
  initializeSwaggerUI();
};
