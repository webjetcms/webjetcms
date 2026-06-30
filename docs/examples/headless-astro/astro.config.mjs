import { defineConfig } from 'astro/config';

const backendOrigin = process.env.HEADLESS_BACKEND_ORIGIN || 'http://cms.iway.sk';
const proxyPrefixes = (process.env.HEADLESS_PROXY_PREFIXES || '/images/,/files/,/thumb/,/shared/,/components,/FormMailAjax.action,/rest/')
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
  server: {
    host: process.env.HEADLESS_HOST || '127.0.0.1',
    port: parseInt(process.env.HEADLESS_PORT || '3000'),
  },
  vite: {
    server: {
      proxy,
      host: process.env.HEADLESS_HOST || '127.0.0.1',
      port: parseInt(process.env.HEADLESS_PORT || '3000'),
      allowedHosts: [
        'headless.interway.sk',
        'iwcm.interway.sk',
        'localhost',
        '127.0.0.1',
      ],
    },
    preview: {
      proxy,
      host: process.env.HEADLESS_HOST || '127.0.0.1',
      port: parseInt(process.env.HEADLESS_PORT || '3000'),
    },
  },
});
