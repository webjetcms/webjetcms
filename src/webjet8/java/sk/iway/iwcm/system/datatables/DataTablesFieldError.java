package sk.iway.iwcm.system.datatables;

/**
 *  DataTablesFieldError.java
 *
 *  Field error class for displaying errors with DataTables Editor.
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


public class DataTablesFieldError {
    private String name;
    private String status;

    public DataTablesFieldError() {
    }

    public DataTablesFieldError(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
