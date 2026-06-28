import { defineConfig } from 'astro/config';

const backendOrigin = process.env.HEADLESS_BACKEND_ORIGIN || 'http://iwcm.interway.sk';
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
  vite: {
    server: {
      proxy,
    },
    preview: {
      proxy,
    },
  },
});
