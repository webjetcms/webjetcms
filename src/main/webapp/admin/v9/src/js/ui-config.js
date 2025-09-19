import $ from 'jquery';

// Nastav back-compat pred načítaním akýchkoľvek jquery-ui widgetov, potrebuje to elfinder
$.uiBackCompat = true;

export default $; // voliteľné, aby sme mali istotu že bundler nezahodí súbor