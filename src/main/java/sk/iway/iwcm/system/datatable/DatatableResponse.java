package sk.iway.iwcm.system.datatable;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.tags.support_logic.CustomResponseUtils;

/**
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 * @author       tmarcinkova $
 * @created      2019/05/10 12:43
 *
 * This is the structure we send back to the jQuery DataTables Editor plugin
 * when the user is inserting/updating/deleting things.
 *
 * @param <T> the class being wrapped.
 */

@Getter
@Setter
public class DatatableResponse<T> {
	private List<T> data;
	private String error;
	private List<DatatableFieldError> fieldErrors;
	private Boolean forceReload;
    private List<NotifyBean> notify;

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	public String getError() {
		return CustomResponseUtils.filter(error);
	}

	public void setError(String error) {
		this.error = error;
	}

	public List<DatatableFieldError> getFieldErrors() {
		return fieldErrors;
	}

	public void setFieldErrors(List<DatatableFieldError> fieldErrors) {
		this.fieldErrors = fieldErrors;
	}

	public void add(T newData) {
		if (newData == null) return;
		if (data == null)
			data = new ArrayList<T>();
		data.add(newData);
	}

	public Boolean getForceReload() {
		return forceReload;
	}

	public void setForceReload(Boolean forceReload) {
		this.forceReload = forceReload;
	}
}
