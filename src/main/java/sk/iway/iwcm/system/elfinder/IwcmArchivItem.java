package sk.iway.iwcm.system.elfinder;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;



public class IwcmArchivItem implements FsItem {
    FileArchivatorBean _fab;
    String _path;

    FsVolume _volumn;

    @Override
    public FsVolume getVolume() {
        return _volumn;
    }

    public void set_volumn(FsVolume _volumn) {
        this._volumn = _volumn;
    }

    public IwcmArchivItem(IwcmArchivFsVolume volumn, String path, FileArchivatorBean fab)
    {
        super();
        //Logger.debug(this,"new IwcmArchivItem ("+path+")"+(fab != null ? fab.getId():"-"));
        _volumn = volumn;
        _path = path;
        _fab = fab;
    }

    public IwcmArchivItem(IwcmArchivFsVolume volumn, FileArchivatorBean fab)
    {
        super();
        _volumn = volumn;
        if(fab != null)
        {
            _fab = fab;
            _path = fab.getVirtualPath();
        }
    }

    public String getName()
    {
        //nt lastIndex = _path.lastIndexOf("/");
        if (_fab != null)
            return _fab.getVirtualFileName();
        if(Tools.isNotEmpty(_path) && !_path.endsWith("/"))
            return _path.substring(_path.lastIndexOf("/")+1);
        else {
            String[] tokens = Tools.getTokens(_path, "/");
            return tokens[tokens.length - 1];
        }
    }

    public String getPath() {
        return _path;
    }

    public void setPath(String _path) {
        this._path = _path;
    }

    public FileArchivatorBean getFab() {
        return _fab;
    }

    public void setFab(FileArchivatorBean _fab) {
        this._fab = _fab;
    }

    public int getSortPriority()
    {
        if (getFab() != null && getFab().getId() > 0) {
            return getFab().getFileArchiveId();
        }

        return 10;
    }
}

