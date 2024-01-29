package sk.iway.iwcm.system;

import java.util.Date;

import javax.validation.constraints.NotBlank;

import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * Informacia o konfiguracnej premennej v DB / zoznam premennych
 *  ConfDetails.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.2.2010 10:43:46
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ConfDetails
{

   @NotBlank
	@DataTableColumn(
      inputType = DataTableColumnType.OPEN_EDITOR,
      title = "admin.conf_editor.name",
      tab = "basic",
      sortAfter = "id",
      editor = {
         @DataTableColumnEditor(
            type = "text",
            attr = {
               @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/configuration/autocomplete"),
               @DataTableColumnEditorAttr(key = "data-ac-click", value = "fillValue")
            }
         )
      }
   )
   private String name;

   @DataTableColumn(
      inputType = DataTableColumnType.TEXTAREA,
      title = "admin.conf_editor.value",
      tab = "basic",
      sortAfter = "name",
      className = "wrap show-html"
   )
   private String value;

   private String modules;

	@DataTableColumn(
      inputType = DataTableColumnType.TEXT,
      renderFormat = "dt-format-text-wrap",
      title = "admin.conf_editor.description",
      tab = "basic",
      sortAfter = "oldValue",
      className = "wrap",
      editor = {
         @DataTableColumnEditor(
            type = "textarea",
            attr = {
               @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
            }
            /*className: 'hide-on-create'*/
         )
      }
   )
   private String description;

   @DataTableColumn(
      inputType = DataTableColumnType.DATE,
      renderFormat = "dt-format-date-time",
      title = "admin.conf_editor.date_change",
      tab = "basic",
      sortAfter = "description",
      editor = {
         @DataTableColumnEditor(
            type = "datetime",
            attr = {
               @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
            }
            /*className: 'hide-on-create'*/
         )
      }
   )
   private Date dateChanged;

   public ConfDetails(){}

   public ConfDetails(String name, String value)
   {
      this.name = name;
      this.value = value;
   }

   public ConfDetails(String name, String value, Date dateChanged)
   {
      this.name = name;
      this.value = value;
      this.dateChanged = dateChanged;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String newName)
   {
      if (newName!=null)
      {
         name = newName;
      }
   }

   public String getValue()
   {
      return value;
   }

   public void setValue(String newValue)
   {
      if (newValue!=null)
      {
         value = newValue;
      }
   }

	public String getModules()
	{
		return modules;
	}

	public void setModules(String modules)
	{
		this.modules = modules;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Date getDateChanged()
	{
		return dateChanged;
	}

	public void setDateChanged(Date dateChanged)
	{
		this.dateChanged = dateChanged;
	}


}
