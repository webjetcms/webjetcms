const { I } = inject();

/**
 * WebJet selectors with dynamic generation
 *
 * This code dynamically generates CSS selectors for buttons based on specified contexts and actions.
 * It supports different button contexts (`#datatableInit_wrapper`, `.tree-col`)
 * and button actions (e.g., `add`, `delete`, `edit`, etc.). This eliminates duplicate code and
 * makes it easy to add new contexts or actions.
 *
 * How to use:
 * 1. Access specific selectors by context and action:
 *    - For a specific context, use: `DT.btn.add_button` for an "add" button in `#datatableInit_wrapper`.
 *    - For a specific context, use: `DT.btn.gallery_add_button` for an "add" button in `#galleryTable_wrapper`.
 *    - For global (no context), use: `DT.btn.add_button` for an "add" button with no specific context.
 * 2. Example usage in tests:
 *    - `I.click(DT.btn.add_button);` - clicks "add" in `#datatableInit_wrapper`
 *    - `I.click(DT.btn.gallery_add_button);` - clicks "add" in `#galleryTable_wrapper`
 *    - `I.click(DT.btn.tree_delete_button);` - clicks "delete" in `.tree-col`
 *    - `I.click(DT.btn.refresh_button);` - clicks "refresh" globally
 */

const contexts = {
  '': '#datatableInit_wrapper',
  gallery: '#galleryTable_wrapper',
  tree: '.tree-col'
};

const actions = {
  add_button: 'buttons-create',
  delete_button: 'buttons-remove',
  edit_button: 'buttons-edit',
  refresh_button: 'buttons-refresh',
  duplicate_button: 'btn-duplicate',
  recovery_button: 'buttons-recover',
  stat_button: 'buttons-statistics',
  settings_button: 'buttons-settings',
  import_export_button: 'buttons-import-export',
  refund_button: 'buttons-refund-payment',
  import_button: 'btn-import-dialog',
  export_button: 'btn-export-dialog',
  resend_button: 'buttons-resend',
  group_button :'btn-add-group',
  remove_group_button: 'buttons-removeGroupFromAll',
  preview_button: 'buttons-history-preview'
};

const getButtonSelector = (context, action) => {
  if (!context) {
    return locate(`button.${action}`);
  }
  return locate(context).find(`button.${action}`);
};

const buttons = {};

for (const [contextKey, contextValue] of Object.entries(contexts)) {
  for (const [actionKey, actionClass] of Object.entries(actions)) {
    const selectorKey = contextKey ? `${contextKey}_${actionKey}` : actionKey;
    buttons[selectorKey] = getButtonSelector(contextValue, actionClass);
  }
}

function _addContext(newContextKey, newContextSelector) {
  let exist = false;
  for (const [contextKey, contextValue] of Object.entries(contexts)) {
    if(contextKey == newContextKey) {
      exist = true;
      break;
    }
  }

  if(exist) {
    console.log(`--- Context ${newContextKey} already exists`);
  } else {
    console.log(`--- Adding new context: ${newContextKey}`);
    for (const [actionKey, actionClass] of Object.entries(actions)) {
      const selectorKey = newContextKey ? `${newContextKey}_${actionKey}` : actionKey;
      buttons[selectorKey] = getButtonSelector(newContextSelector, actionClass);
    }
  }
}

module.exports = {
  btn: buttons,
  addContext(newContextKey, newContextSelector) {
    _addContext(newContextKey, newContextSelector);
  }
};