package sk.iway.iwcm.system.datatable;

/**
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 * @author       tmarcinkova $
 * @created      2019/05/10 12:14
 *
 *  Jeden prvok pola fieldErrors, ktory pouziva Datatable Editor
 *  	name = nazov pola
 *  	status = error message
 */
public class DatatableFieldError
{
	 private String name;
	 private String status;

	 public DatatableFieldError() {
	 }

	 public DatatableFieldError(String name, String status) {
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
