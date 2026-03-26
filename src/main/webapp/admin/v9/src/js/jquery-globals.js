// jQuery is loaded synchronously via <script> tag in head.pug.
// This module ensures window.jQuery/$ are the same instance used by ES modules
// (via jquery-window-global.js alias in vite.config.js).
// No queue replay is needed — jQuery is available before any inline scripts run.
