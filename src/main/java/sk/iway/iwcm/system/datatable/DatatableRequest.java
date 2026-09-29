package sk.iway.iwcm.system.datatable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 * @author       tmarcinkova $
 * @created      2019/05/10 12:41
 *
 * This is the structure we get from the jQuery DataTables Editor plugin when
 * the user is inserting/updating/deleting things.
 *
 * @param <K> the type of key used by the class being wrapped.
 * @param <V> the class being wrapped.
 */

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatatableRequest<K, V>
{
	private String action;
	private Map<K, V> data;
	private int dztotalchunkcount;
	private int dzchunkindex;
	private double dzchunksize;
	private double dztotalfilesize;
	private String name;
	private DatatableRequestOverwriteMode overwriteMode;
	private String uploadType;
	private boolean writeDirectlyToDestination;
	private String updateByColumn;
	private boolean deleteOldData;
	private String importMode;

	//if true, wrong data will be skipped during import
	private boolean skipWrongData;

	//set of columns in excel import (filled in export-import.js during xlsx parsing)
	private Set<String> importedColumns;

	//sem sa ukaldaju error hlasenia v initBinder
	private V errorField;
	private String globalError;

	 public boolean isInsert() {
		  return "create".equals(action);
	 }

	 public boolean isUpdate() {
		  return "edit".equals(action);
	 }

	 public boolean isDelete() {
		  return "remove".equals(action);
	 }
}
