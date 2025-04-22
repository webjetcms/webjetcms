package sk.iway.iwcm.system.elfinder;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.admin.jstree.JsTreeItemType;
import sk.iway.iwcm.io.IwcmFile;

/**
 * Entita pre jsTree reprezentujuca adresar v suborovom systeme
 */
@Getter
@Setter
public class DirTreeItem extends JsTreeItem {

    public DirTreeItem() {
        super();
    }

    public DirTreeItem(IwcmFile f) {
        this(f, false);
    }
        
    public DirTreeItem(IwcmFile f, boolean setParent) {
        super();
        setId(f.getVirtualPath());
        setText(f.getName());
        setVirtualPath(f.getVirtualPath());

        setIcon("ti ti-folder");
        setState(new JsTreeItemState());

        setType(JsTreeItemType.DIR);
        setChildren(hasChildren(f));
        
        if(setParent == true)
            setParent( f.getParentFile().getVirtualPath() );
        else
            setParent(null);
    }

    public DirTreeItem(String virtualPath) {
        this(new IwcmFile(Tools.getRealPath(virtualPath)));
    }

    private boolean hasChildren(IwcmFile f) {
        IwcmFile[] subfiles = f.listFiles();
        if (subfiles.length>0) return true;
        return false;
    }
}
