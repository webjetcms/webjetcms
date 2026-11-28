function open(I) {
    I.waitForEnabled('#branch-switcher', 30);
    I.click('#branch-switcher');
    I.waitForVisible('#branch-picker', 10);
}

function select(I, id) {
    open(I);
    I.click(`#branch-picker [data-branch-id="${id}"]`);
}

function action(I, actionName) {
    open(I);
    I.click(`#branch-picker [data-branch-action="${actionName}"]`);
}

module.exports = {open, select, action};
