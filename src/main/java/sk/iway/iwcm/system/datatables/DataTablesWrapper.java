package sk.iway.iwcm.system.datatables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

/**
 *  DataTablesWrapper.java
 *
 *	Wrapper class for displaying data in DataTables Editor.
 *
 *
 *@Title        webjet8
 *@Company      Interway a.s. (www.interway.sk)
 *@Copyright    Interway a.s (c) 2001-2018
 *@author       $Author: mhruby $
 *@version      $Revision: 1.0 $
 *@created      Date: 27.03.2018 12:00:00
 *@modified     $Date: 27.03.2018 12:0:00 $
 */

public class DataTablesWrapper {
    private List<Object> data;
    private List<Object> options;
    private Files files;
    private Upload upload;
    private List<DataTablesFieldError> fieldErrors;
    private String error;

    public List<Object> getData() {
        return data;
    }

    public DataTablesWrapper setData(List<Object> data) {
        this.data = data;
        return this;
    }

    public List<Object> getOptions() {
        return options;
    }

    public DataTablesWrapper setOptions(List<Object> options) {
        this.options = options;
        return this;
    }

    public Files getFiles() {
        return files;
    }

    public Upload getUpload() { return upload; }

    public void addUpload(String fileUrl)
    {
        if (files==null) files = new Files();
        if (upload==null) upload = new Upload();

        files.addFile(fileUrl);
    }

    public List<DataTablesFieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<DataTablesFieldError> fieldErrors, Prop prop) {
        Map<String, DataTablesFieldError> temp = new HashMap<>();
        for (DataTablesFieldError error : fieldErrors){
            error.setStatus(prop.getText(error.getStatus()));
            if (temp.containsKey(error.getName())) {
                DataTablesFieldError dataTablesFieldError = temp.get(error.getName());
                dataTablesFieldError.setStatus(dataTablesFieldError.getStatus() + error.getStatus());
            } else {
                temp.put(error.getName(), error);
            }
        }
        this.fieldErrors = new ArrayList<DataTablesFieldError>(temp.values());
    }

    public void setFieldErrors(List<DataTablesFieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public String getError() {
        return error;
    }

    public void setError(String error, Prop prop) {
        if (Tools.isNotEmpty(error))
            this.error = prop.getText(error);
    }

    public void setError(String error) {
        this.error = error;
    }


    /**
     * Toto je sialene, je potrebne spravne zapuzdrit objekty pre upload podla dokumentacie na https://editor.datatables.net/manual/server
     */
    public class Upload {
        String id;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }
    }

    public class UploadFile {
        String id;
        String filename;
        String web_path;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getFilename()
        {
            return filename;
        }

        public void setFilename(String filename)
        {
            this.filename = filename;
        }

        public String getWeb_path()
        {
            return web_path;
        }

        public void setWeb_path(String web_path)
        {
            this.web_path = web_path;
        }
    }

    public class Files {
        List<UploadFile> files;

        public List<UploadFile> getFiles()
        {
            return files;
        }

        public void setFiles(List<UploadFile> files)
        {
            this.files = files;
        }

        public void addFile(String fileUrl)
        {
            if (files == null) files = new ArrayList<>();
            if (upload==null) upload = new Upload();

            UploadFile file = new UploadFile();
            file.setId(fileUrl);

            String fileName = "";
            int i = fileUrl.lastIndexOf("/");
            if (i>0) fileName = fileUrl.substring(i+1);

            file.setFilename(fileName);
            file.setWeb_path(fileUrl);
            files.add(file);

            upload.setId(fileUrl);
        }
    }
}
