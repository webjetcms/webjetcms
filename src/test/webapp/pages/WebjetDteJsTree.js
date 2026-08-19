const modal = "webjet-dte-jstree .custom-modal.open-custom-modal";
const tree = `${modal} .jsTree-wrapper > div[id^="webjetJsTree-"]`;

module.exports = Object.freeze({
    modal,
    tree,
    anchors: `${tree} a.jstree-anchor`,
    loading: `${tree} li.jstree-loading`
});
