package sk.iway.iwcm.components.adminlog;

import java.io.File;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;

public class TailFileWrapper {
    private File file;

    public TailFileWrapper(File file) {
        this.file = file;
    }

    public String getName() {
        return this.file.getName();
    }

    public String getAbsolutePath() {
        return this.file.getAbsolutePath();
    }

    public String getFormatedFileSize() {
        return FileTools.getFormatFileSize(this.file.length(),false );
    }

    public String getFormatedDateTime() {
        return Tools.formatDateTimeSeconds(this.file.lastModified());
    }
}
