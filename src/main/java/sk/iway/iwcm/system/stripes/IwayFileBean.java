package sk.iway.iwcm.system.stripes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.fileupload2.core.FileItem;

import net.sourceforge.stripes.action.FileBean;

/**
 *  IwayFileBean.java
 *  
 *  subclass of {@link FileBean} that overrides all the
 *  methods that rely on having a {@link File} present, to use the {@link FileItem}
 *  created by commons upload instead and is serializable
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.1.2013 17:16:13
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class IwayFileBean extends FileBean implements Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 8501190073097050241L;
	
	FileItem item;
	
	public IwayFileBean(File file, FileItem item)
	{
		super(file, item.getContentType(), item.getName());
		this.item = item;
	}
	
	@Override public long getSize() { return item.getSize(); }

   @Override public InputStream getInputStream() throws IOException {
       return item.getInputStream();
   }

   @Override public void save(File toFile) throws IOException {
       try {
           item.write(toFile);
           delete();
       }
       catch (Exception e) {
           if (e instanceof IOException) throw (IOException) e;
           else {
               IOException ioe = new IOException("Problem saving uploaded file.");
               ioe.initCause(e);
               throw ioe;
           }
       }
   }

   @Override
   public void delete() throws IOException { item.delete(); }
}
