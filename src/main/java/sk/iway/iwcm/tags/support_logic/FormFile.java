package sk.iway.iwcm.tags.support_logic;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public interface FormFile {
   String getContentType();

   void setContentType(String var1);

   int getFileSize();

   void setFileSize(int var1);

   String getFileName();

   void setFileName(String var1);

   byte[] getFileData() throws FileNotFoundException, IOException;

   InputStream getInputStream() throws FileNotFoundException, IOException;

   void destroy();
}