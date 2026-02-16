package sk.iway.iwcm.findexer;


/**
 *  Drzi result indexovania
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Streda, 2004, január 21
 *@modified     $Date: 2004/01/25 13:31:09 $
 */
public class ResultBean
{

	/**
	 *  Constructor for the ResultBean object
	 */
	public ResultBean() { }

	private String file;
	private String data;
	private int docId;

	/**
	 *  Gets the file attribute of the ResultBean object
	 *
	 *@return    The file value
	 */
	public String getFile()
	{
		return file;
	}

	/**
	 *  Sets the file attribute of the ResultBean object
	 *
	 *@param  file  The new file value
	 */
	public void setFile(String file)
	{
		this.file = file;
	}

	/**
	 *  Sets the data attribute of the ResultBean object
	 *
	 *@param  data  The new data value
	 */
	public void setData(String data)
	{
		this.data = data;
	}

	/**
	 *  Gets the data attribute of the ResultBean object
	 *
	 *@return    The data value
	 */
	public String getData()
	{
		return data;
	}
	public int getDocId()
	{
		return docId;
	}
	public void setDocId(int docId)
	{
		this.docId = docId;
	}
}
