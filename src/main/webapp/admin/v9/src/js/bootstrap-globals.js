// This module MUST be imported before bootstrap-select and any code referencing window.bootstrap.
// ES module imports are hoisted — all imports execute before any statements.
// By placing bootstrap global assignment in a separate module imported first,
// we ensure window.bootstrap exists before bootstrap-select evaluates.
import * as bootstrapModule from 'bootstrap';
window.bootstrap = bootstrapModule;
