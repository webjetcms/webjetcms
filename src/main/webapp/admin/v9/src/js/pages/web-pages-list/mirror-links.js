/** Adds language navigation beside folder and page anchors, keeping jsTree selection and dragging separate. */
export function bindMirrorLinks() {
    const treeElement = document.getElementById("SomStromcek");
    const tree = $(treeElement).jstree(true);
    if (!tree) return;

    const status = document.createElement("span");
    status.className = "visually-hidden";
    status.setAttribute("aria-live", "polite");
    treeElement.after(status);

    let pendingId = null;
    const dropdowns = new Set();
    const tooltips = new Set();

    function highlight() {
        treeElement.querySelectorAll(".jstree-mirror-peer").forEach(row => row.classList.remove("jstree-mirror-peer"));
        for (const node of tree.get_selected(true)) {
            for (const mirror of node.original?.mirrors || []) {
                const row = tree.get_node(mirror.docId ? `docId-${mirror.docId}` : String(mirror.groupId), true);
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
        for (const element of tooltips) {
            if (!treeElement.contains(element)) {
                bootstrap.Tooltip.getInstance(element)?.dispose();
                tooltips.delete(element);
            }
        }
        for (const button of dropdowns) {
            if (!treeElement.contains(button)) {
                bootstrap.Dropdown.getInstance(button)?.dispose();
                dropdowns.delete(button);
            }
        }
        if (window.jstreeSettings.isShowMirrorings() == false) {
            pendingId = null;
            return;
        }
        treeElement.querySelectorAll(".jstree-anchor").forEach(anchor => {
            if (anchor.nextElementSibling?.classList.contains("jstree-mirrors")) return;
            const node = tree.get_node(anchor);
            const isPage = !!node.original?.docDetails;
            const mirrors = node.original?.mirrors || [];
            const problem = node.original?.mirroringProblem;
            if (!mirrors.length && !problem) return;
            const group = document.createElement("span");
            group.className = "jstree-mirrors dropdown";
            group.setAttribute("role", "group");
            group.setAttribute("aria-label", isPage ? treeElement.dataset.mirrorsPagesLabel : treeElement.dataset.mirrorsLabel);
            if (problem) {
                const link = document.createElement("a");
                link.className = "jstree-mirror-problem";
                link.href = `/admin/v9/webpages/mirroring/?id=${problem.syncId}${isPage ? "#pills-docs" : ""}`;
                link.dataset.bsTitle = problem.title;
                link.setAttribute("aria-label", problem.title);
                const icon = document.createElement("i");
                icon.className = problem.icon;
                icon.setAttribute("aria-hidden", "true");
                link.append(icon);
                group.append(link);
                tooltips.add(link);
            }
            const menu = document.createElement("div");
            menu.className = "dropdown-menu jstree-mirror-menu";
            mirrors.forEach((mirror, index) => {
                const link = document.createElement("a");
                link.className = "jstree-mirror-link";
                const id = isPage ? mirror.docId : mirror.groupId;
                if (isPage) {
                    link.href = `?docid=${id}`;
                    link.dataset.docId = id;
                } else {
                    link.href = `?groupid=${id}`;
                    link.dataset.groupId = id;
                }
                link.textContent = mirror.language.toUpperCase();
                const openLabel = isPage ? treeElement.dataset.mirrorsPagesOpen : treeElement.dataset.mirrorsOpen;
                const label = `${openLabel} ${link.textContent}: ${mirror.path} (#${id})`;
                link.dataset.bsTitle = `${mirror.path} (#${id})`;
                link.setAttribute("aria-label", label);
                tooltips.add(link);
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
                more.setAttribute("aria-label", isPage ? treeElement.dataset.mirrorsPagesMore : treeElement.dataset.mirrorsMore);
                group.append(more, menu);
                new bootstrap.Dropdown(more, { popperConfig: { strategy: "fixed" } });
                dropdowns.add(more);
            }
            anchor.after(group);
            WJ.initTooltip($(group).find(".jstree-mirror-problem, .jstree-mirror-link"));
            anchor.parentElement.classList.add("jstree-has-mirrors");
        });
        highlight();
    }

    /** Uses the existing ID input workflows to open folders or edit pages, including unloaded ancestors. */
    function navigate(id, isPage = false) {
        pendingId = isPage ? null : String(id);
        // A server-side tree filter can hide the destination and its ancestors.
        if ($("#tree-folder-search-input").val()) {
            // Finish restoring the tree before its events can advance the folder opener.
            $(treeElement).one("refresh.jstree", () => navigate(id, isPage));
            $("#tree-folder-search-clear-button").trigger("click");
            return;
        }
        $(isPage ? "#tree-doc-id" : "#tree-folder-id").val(id).trigger($.Event("keyup", { key: "Enter", keyCode: 13 }));
    }

    treeElement.addEventListener("click", event => {
        const link = event.target.closest(".jstree-mirror-link");
        if (!link || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return;
        event.preventDefault();
        event.stopPropagation();
        bootstrap.Tooltip.getInstance(link)?.hide();
        const more = link.closest(".jstree-mirrors").querySelector(".jstree-mirror-more");
        if (more) bootstrap.Dropdown.getInstance(more)?.hide();
        if (link.dataset.docId) navigate(link.dataset.docId, true);
        else navigate(link.dataset.groupId);
    });
    $(treeElement).on("ready.jstree redraw.jstree after_open.jstree rename_node.jstree", render);
    $(treeElement).on("changed.jstree", highlight);
    render();
}
