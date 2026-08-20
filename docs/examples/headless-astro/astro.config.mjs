import { defineConfig } from 'astro/config';
import node from '@astrojs/node';
import fs from 'node:fs';
import { loadEnv } from 'vite';

// Load all variables from .env (prefix '' = load everything, not just PUBLIC_)
const env = loadEnv(process.env.NODE_ENV || 'development', process.cwd(), '');

// Apply non-PUBLIC_ variables to process.env so Node.js runtime settings work
// (e.g. NODE_TLS_REJECT_UNAUTHORIZED for self-signed certificates in local dev)
for (const [key, value] of Object.entries(env)) {
  if (!key.startsWith('PUBLIC_') && !(key in process.env)) {
    process.env[key] = value;
  }
}

// Extract backend origin from PUBLIC_API_BASE or use environment variable
function getBackendOrigin() {
  const apiBase = env.PUBLIC_API_BASE;
  return apiBase.substring(0, apiBase.indexOf('/', 8)); // Extract origin from URL
}

const backendOrigin = getBackendOrigin();
const isHttpsEnabled = ['1', 'true', 'yes', 'on'].includes((env.HEADLESS_HTTPS || '').toLowerCase());

function getHttpsOptions() {
  const keyPath = env.HEADLESS_HTTPS_KEY || './.cert/localhost-key.pem';
  const certPath = env.HEADLESS_HTTPS_CERT || './.cert/localhost.pem';

  if (fs.existsSync(keyPath) && fs.existsSync(certPath)) {
    return {
      key: fs.readFileSync(keyPath),
      cert: fs.readFileSync(certPath),
    };
  }

  // Fallback to Vite's default HTTPS behavior when cert files are not present.
  return {};
}

const httpsOptions = getHttpsOptions();
const proxyPrefixes = (env.HEADLESS_PROXY_PREFIXES || '/images/,/files/,/thumb/,/shared/,/components,/FormMailAjax.action,/rest/,/apps/form/mvc/,/captcha.jpg')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean)
  .map((value) => (value.startsWith('/') ? value : `/${value}`));

const proxy = Object.fromEntries(
  proxyPrefixes.map((prefix) => [
    prefix,
    {
      target: backendOrigin,
      changeOrigin: true,
      secure: false,
    },
  ])
);

export default defineConfig({
  output: 'server',
  adapter: node({
    mode: 'standalone',
  }),
  server: {
    host: env.HEADLESS_HOST || '127.0.0.1',
    port: Number.parseInt(env.HEADLESS_PORT || '3000'),
    https: isHttpsEnabled == true ? httpsOptions : false,
  },
  vite: {
    server: {
      proxy,
      host: env.HEADLESS_HOST || '127.0.0.1',
      port: Number.parseInt(env.HEADLESS_PORT || '3000'),
      https: isHttpsEnabled == true ? httpsOptions : false,
      allowedHosts: [
        'headless.interway.sk',
        'iwcm.interway.sk',
        'localhost',
        '127.0.0.1',
      ],
    },
    preview: {
      proxy,
      host: env.HEADLESS_HOST || '127.0.0.1',
      port: Number.parseInt(env.HEADLESS_PORT || '3000'),
      https: isHttpsEnabled == true ? httpsOptions : false,
    },
  },
});

