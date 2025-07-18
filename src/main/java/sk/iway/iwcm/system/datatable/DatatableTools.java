package sk.iway.iwcm.system.datatable;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

/**
 * Utility class with various support methods for Datatables
 */
public class DatatableTools {

    private DatatableTools() {
        //utility class
    }

    /**
     * Create list of options for DataTableColumnType.IMAGE_RADIO
     * @param rootPath - path to directory with images, e.g. /components/inquiry/admin-styles/
     * @return
     */
    public static List<OptionDto> getImageRadioOptions(String rootPath) {
        IwcmFile dir = new IwcmFile(Tools.getRealPath(rootPath));
        List<OptionDto> options = new ArrayList<>();
        if (dir.exists() && dir.canRead())
        {
            IwcmFile files[] = dir.listFiles();
            files = FileTools.sortFilesByName(files);
            for (IwcmFile file : files)
            {
                if (file.isFile()==false || file.canRead()==false) continue;
                if (file.getName().endsWith(".png")==false && file.getName().endsWith(".jpg")==false && file.getName().endsWith(".gif")==false) continue;
                //skip screenshots and system/hidden files
                if (file.getName().startsWith("screenshot") || file.getName().startsWith("editoricon") || file.getName().startsWith("_")) continue;

                String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                options.add(new OptionDto(fileName, fileName, rootPath+file.getName()));
            }
        }
        return options;
    }

}
