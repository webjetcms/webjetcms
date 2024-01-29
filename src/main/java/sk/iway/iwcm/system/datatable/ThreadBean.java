package sk.iway.iwcm.system.datatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import sk.iway.iwcm.Logger;

/**
 * Tento bean drzi udaje potrebne pre thread local, cize udaje ktore sa menia s kazdym requestom
 */
@Getter
@Setter
@ToString
public class ThreadBean {

    //ak je true nastavi sa do vystupu forceReload: true
    private boolean forceReload = false;

	//urcuje, ci sa jedna o volanie exportu dat
	private boolean exporting = false;

    //urcuje, ze sa jedna o import
	private boolean importing = false;

    //list upozorneni
    private List<NotifyBean> notify = null;

    //cislo riadku pri importe z Excelu
    private Integer lastImportedRow = null;

    //set of columns in excel import (filled in export-import.js during xlsx parsing)
    private Set<String> importedColumns = null;

    //column name which is used to update the row with import
    private String updateByColumn;

    //mode of import (append, update, onlyNew)
    private String importMode;

    public void clear() {
        Logger.debug(ThreadBean.class, "clearing data, thread="+Thread.currentThread().getId());
        forceReload = false;
        exporting = false;
        importing = false;
        notify = null;
        lastImportedRow = null;
        importedColumns = null;
        updateByColumn = null;
        importMode = null;
    }

    public void addNotify(NotifyBean notify) {

        if(this.notify == null) this.notify = new ArrayList<>();

        //over, ci tam uz nemame rovnaky notify, aby sme nespamovali usera
        for (NotifyBean current : this.notify) {
            if (current.getTitle().equals(notify.getTitle()) &&
                current.getText().equals(notify.getText()) &&
                current.getType().equals(notify.getType())
                ) {
                return;
            }
        }

        this.notify.add(notify);
    }
}
