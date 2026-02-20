package sk.iway.iwcm.filebrowser;

import java.util.StringTokenizer;

import sk.iway.iwcm.users.UserDetails;
import sk.iway.upload.UploadedFile;


/**
 *  Formular pre editaciu suboru
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Streda, 2003, december 24
 *@modified     $Date: 2004/01/25 13:31:18 $
 */
public class EditForm
{
	private String dir;
	private String file;
	private String origFile;
	private String data;
	private UploadedFile uploadFile;
	private String origDir;
	private int[] passwordProtected;
	private boolean indexFulltext;
	private int logonDocId = -1;
	private String[] selectedFilesToDelete;

	public boolean isAccessibleFor(UserDetails user)
	{
		//ak nie je nic nastavene, je pristupne
		if (passwordProtected==null || passwordProtected.length<1) return true;
		//ak je user null a je nieco nastavene nie je pristupne
		if (user == null) return false;

		boolean isAccesible = false;
		int i;
		int size = passwordProtected.length;

		for (i=0; i<size; i++)
		{
			if (user.isInUserGroup(passwordProtected[i]))
			{
				isAccesible = true;
				break;
			}
		}
		return isAccesible;
	}

	public String[] getSelectedFilesToDelete()
	{
		return selectedFilesToDelete;
	}

	public void setSelectedFilesToDelete(String[] selectedFilesToDelete)
	{
		this.selectedFilesToDelete = selectedFilesToDelete;
	}

	/**
	 *  Gets the dir attribute of the EditForm object
	 *
	 *@return    The dir value
	 */
	public String getDir()
	{
		return dir;
	}

	/**
	 *  Sets the dir attribute of the EditForm object
	 *
	 *@param  dir  The new dir value
	 */
	public void setDir(String dir)
	{
		this.dir = dir;
	}

	/**
	 *  Sets the file attribute of the EditForm object
	 *
	 *@param  file  The new file value
	 */
	public void setFile(String file)
	{
		this.file = file;
	}

	/**
	 *  Gets the file attribute of the EditForm object
	 *
	 *@return    The file value
	 */
	public String getFile()
	{
		return file;
	}

	/**
	 *  Sets the file attribute of the EditForm object
	 *
	 *@param  file  The new file value
	 */
	public void setOrigFile(String file)
	{
		this.origFile = file;
	}

	/**
	 *  Gets the file attribute of the EditForm object
	 *
	 *@return    The file value
	 */
	public String getOrigFile()
	{
		return origFile;
	}

	/**
	 *  Sets the data attribute of the EditForm object
	 *
	 *@param  data  The new data value
	 */
	public void setData(String data)
	{
		this.data = data;
	}

	/**
	 *  Gets the data attribute of the EditForm object
	 *
	 *@return    The data value
	 */
	public String getData()
	{
		return data;
	}

	/**
	 *  Sets the fileUpload attribute of the EditForm object
	 *
	 *
	 */
	public void setUploadFile(UploadedFile uploadFile)
	{
		this.uploadFile = uploadFile;
	}

	/**
	 *  Gets the fileUpload attribute of the EditForm object
	 *
	 *@return    The fileUpload value
	 */
	public UploadedFile getUploadFile()
	{
		return uploadFile;
	}
	public String getOrigDir()
	{
		return origDir;
	}
	public void setOrigDir(String origDir)
	{
		this.origDir = origDir;
	}

	/**
	 *  Sets the passwordProtected attribute of the EditorForm object
	 *
	 *@param  passwordProtected  The new passwordProtected value
	 */
	public void setPasswordProtected(int[] passwordProtected)
	{
		this.passwordProtected = passwordProtected;
	}

	/**
	 *  Sets the passwordProtectedString attribute of the EditorForm object
	 *
	 *@param  passwordProtectedString  The new passwordProtectedString value
	 */
	public void setPasswordProtectedString(String passwordProtectedString)
	{
		if (passwordProtectedString == null || passwordProtectedString.length() < 1)
		{
			passwordProtected = new int[0];
			return;
		}
		//najskor zisti pocet
		try
		{
			StringTokenizer st = new StringTokenizer(passwordProtectedString, ",");
			int len = st.countTokens();
			passwordProtected = new int[len];
			int i = 0;
			while (st.hasMoreTokens())
			{
				passwordProtected[i] = Integer.parseInt(st.nextToken());
				i++;
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 *  Gets the passwordProtected attribute of the EditorForm object
	 *
	 *@return    The passwordProtected value
	 */
	public int[] getPasswordProtected()
	{
		return passwordProtected;
	}

	/**
	 *  Gets the passwordProtectedString attribute of the EditorForm object
	 *
	 *@return    The passwordProtectedString value
	 */
	public String getPasswordProtectedString()
	{
		if (passwordProtected == null)
		{
			return(null);
		}
		int size = passwordProtected.length;
		if (size == 0)
		{
			return (null);
		}
		String out = null;
		int i;
		for (i = 0; i < size; i++)
		{
			if (out == null)
			{
				out = Integer.toString(passwordProtected[i]);
			}
			else
			{
				out = out + "," + passwordProtected[i];
			}
		}
		return (out);
	}
	public boolean isIndexFulltext()
	{
		return indexFulltext;
	}
	public void setIndexFulltext(boolean indexFulltext)
	{
		this.indexFulltext = indexFulltext;
	}
	public int getLogonDocId()
	{
		return logonDocId;
	}
	public void setLogonDocId(int logonDocId)
	{
		this.logonDocId = logonDocId;
	}
}
