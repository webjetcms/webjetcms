package sk.iway.iwcm.editor;

import sk.iway.iwcm.doc.DocDetails;


/**
 * <p>Title: WebJET Content Management Server</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2001-2003</p>
 * <p>Company: Interway s.r.o. (www.interway.sk)</p>
 * @author unascribed
 * @version 1.0
 */

public class PublicableForm extends DocDetails
{

  private java.sql.Timestamp file_change;
  private int views_month;
  private int views_total;
  private String data_asc;
  private boolean done = false;


  public void setFile_change(java.sql.Timestamp newFileChange)
   {
      file_change = newFileChange;

   }

  public java.sql.Timestamp getFile_change()
  {
     return file_change;
  }

  public void setViews_month(int newViews_month)
  {
     views_month = newViews_month;
  }

  public int getViews_month()
  {
    return views_month;
  }

  public void setViews_total(int newViews_total)
  {
     views_total = newViews_total;
  }

  public int getViews_total()
  {
    return views_total;
  }

  public void setData_asc(String newData_asc)
  {
     data_asc = newData_asc;
  }

  public String getData_asc()
  {
     return data_asc;
  }

public boolean isDone()
{
	return done;
}

public void setDone(boolean done)
{
	this.done = done;
}




}