/** Adds language navigation beside folder anchors, keeping jsTree selection and dragging separate. */
export function bindMirrorLinks() {
    const treeElement = document.getElementById("SomStromcek");
    const tree = $(treeElement).jstree(true);
    if (!tree) return;

    const back = document.createElement("button");
    back.type = "button";
    back.className = "btn btn-sm btn-outline-secondary buttons-mirror-back";
    back.innerHTML = '<i class="ti ti-arrow-left" aria-hidden="true"></i>';
    back.hidden = true;
    document.querySelector(".wp-header-tree .dt-buttons").append(back);
    const status = document.createElement("span");
    status.className = "visually-hidden";
    status.setAttribute("aria-live", "polite");
    treeElement.after(status);

    let previousId = null;
    let pendingId = null;
    const dropdowns = new Set();

    function highlight() {
        treeElement.querySelectorAll(".jstree-mirror-peer").forEach(row => row.classList.remove("jstree-mirror-peer"));
        for (const node of tree.get_selected(true)) {
            for (const mirror of node.original?.mirrors || []) {
                const row = tree.get_node(String(mirror.groupId), true);
                if (row) row.addClass("jstree-mirror-peer");
            }
        }
        if (pendingId && tree.is_selected(pendingId)) {
            const anchor = tree.get_node(pendingId, true).children(".jstree-anchor")[0];
            anchor?.focus({ preventScroll: true });
            anchor?.scrollIntoView({ block: "nearest", inline: "nearest" });
            status.textContent = tree.get_node(pendingId).original.groupDetails.fullPath;
            pendingId = null;
        }
    }

    function render() {
        for (const button of dropdowns) {
            if (!treeElement.contains(button)) {
                bootstrap.Dropdown.getInstance(button)?.dispose();
                dropdowns.delete(button);
            }
        }
        treeElement.querySelectorAll(".jstree-anchor").forEach(anchor => {
            if (anchor.nextElementSibling?.classList.contains("jstree-mirrors")) return;
            const node = tree.get_node(anchor);
            const mirrors = node.original?.mirrors;
            if (!mirrors?.length) return;
            const group = document.createElement("span");
            group.className = "jstree-mirrors dropdown";
            group.setAttribute("role", "group");
            group.setAttribute("aria-label", treeElement.dataset.mirrorsLabel);
            group.innerHTML = '<i class="ti ti-link" aria-hidden="true"></i>';
            const menu = document.createElement("div");
            menu.className = "dropdown-menu jstree-mirror-menu";
            mirrors.forEach((mirror, index) => {
                const link = document.createElement("a");
                link.className = "jstree-mirror-link";
                link.href = `?groupid=${mirror.groupId}`;
                link.dataset.groupId = mirror.groupId;
                link.textContent = mirror.language.toUpperCase();
                const label = `${treeElement.dataset.mirrorsOpen} ${link.textContent}: ${mirror.path} (#${mirror.groupId})`;
                link.title = label;
                link.setAttribute("aria-label", label);
                if (index < 2) group.append(link);
                else {
                    link.classList.add("dropdown-item");
                    link.textContent += ` · ${mirror.path}`;
                    menu.append(link);
                }
            });
            if (mirrors.length > 2) {
                const more = document.createElement("button");
                more.type = "button";
                more.className = "jstree-mirror-more";
                more.textContent = `+${mirrors.length - 2}`;
                more.setAttribute("aria-expanded", "false");
                more.dataset.bsToggle = "dropdown";
                more.setAttribute("aria-label", treeElement.dataset.mirrorsMore);
                group.append(more, menu);
                new bootstrap.Dropdown(more, { popperConfig: { strategy: "fixed" } });
                dropdowns.add(more);
            }
            anchor.after(group);
            anchor.parentElement.classList.add("jstree-has-mirrors");
        });
        highlight();
    }

    /** Uses the existing folder-ID input workflow to load ancestors, switch tabs and report missing folders. */
    function navigate(id) {
        pendingId = String(id);
        // A server-side tree filter can hide the destination and its ancestors.
        if ($("#tree-folder-search-input").val()) $("#tree-folder-search-clear-button").trigger("click");
        $("#tree-folder-id").val(id).trigger($.Event("keyup", { key: "Enter", keyCode: 13 }));
    }

    treeElement.addEventListener("click", event => {
        const link = event.target.closest(".jstree-mirror-link");
        if (!link || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return;
        event.preventDefault();
        event.stopPropagation();
        const more = link.closest(".jstree-mirrors").querySelector(".jstree-mirror-more");
        if (more) bootstrap.Dropdown.getInstance(more)?.hide();
        const source = tree.get_node(link.closest(".jstree-node"));
        previousId = source.id;
        const label = `${treeElement.dataset.mirrorsBack} ${source.original.groupDetails.fullPath}`;
        back.title = label;
        back.setAttribute("aria-label", label);
        back.hidden = false;
        navigate(link.dataset.groupId);
    });
    back.addEventListener("click", () => {
        if (previousId) navigate(previousId);
        previousId = null;
        back.hidden = true;
    });
    $(treeElement).on("ready.jstree redraw.jstree after_open.jstree rename_node.jstree", render);
    $(treeElement).on("changed.jstree", highlight);
    render();
}
