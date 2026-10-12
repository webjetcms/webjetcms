import { registerSessionWidgets } from './session-widgets';
import { registerNewsWidget } from './news-widget';

/** Registers session widgets and release news. */
export function registerUtilityWidgets() {
    registerSessionWidgets();
    registerNewsWidget();
}
