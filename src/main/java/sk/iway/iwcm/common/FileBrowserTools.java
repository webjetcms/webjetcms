package sk.iway.iwcm.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.metadata.MetadataCleaner;

public class FileBrowserTools {
    private static List<String> forbiddenSymbols = null; //NOSONAR
    static
    {
        //forbiddenSymbols = new Hashtable();
        forbiddenSymbols = new ArrayList<>();

        // hardcoded forbidden symbols
        forbiddenSymbols.add(":");
        forbiddenSymbols.add("..");
        forbiddenSymbols.add("^");
        forbiddenSymbols.add("~");
        forbiddenSymbols.add("`");
        forbiddenSymbols.add("'");
        forbiddenSymbols.add("&");
        forbiddenSymbols.add("%");
        forbiddenSymbols.add("$");
        forbiddenSymbols.add("!");
        forbiddenSymbols.add("|");
        forbiddenSymbols.add("\"");
        forbiddenSymbols.add(";");
        forbiddenSymbols.add(",");
        forbiddenSymbols.add("?");
        forbiddenSymbols.add("*");
        forbiddenSymbols.add(">");
        forbiddenSymbols.add("<");

        // configured forbidden symbols
        addForbidenSymbolsFromConfig();

        // hardcoded forbidden strings
        forbiddenSymbols.add(".java");
        forbiddenSymbols.add(".lib");
        forbiddenSymbols.add("cvs");
        forbiddenSymbols.add(".svn");
        forbiddenSymbols.add("lost+found");
    }

    protected FileBrowserTools() {
        //utility class
    }

    /**
     * Checks whether a file name or path contains a forbidden character or substring.
     *
     * @param name file name or path to check; may be {@code null}
     * @return {@code true} when a forbidden character or substring is found
     */
    public static boolean hasForbiddenSymbol(String name)
    {
        return getForbiddenSymbol(name) != null;
    }

    /**
     * Finds a forbidden character or substring using the same exceptions as filename validation.
     *
     * @param name file name or path to check; may be {@code null}
     * @return the first matching token in the forbidden list, or {@code null} when none is found
     */
    public static String getForbiddenSymbol(String name)
    {
        if (name == null)
        {
            return null;
        }
        //in html folder we have valid files with name showdoc.dodocid=1234.html, allow this
        name = name.replace("showdoc.dodocid=" , "");

        name = name.toLowerCase();
        for (String fSymbol : forbiddenSymbols)
        {
            if ("$".equals(fSymbol) && name.endsWith(".class"))
            {
                // Allow JVM inner class names, but keep dollar signs forbidden in parent directories.
                int fileNameStart = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1;
                if (name.substring(0, fileNameStart).indexOf('$') == -1) continue;
            }
            if (name.indexOf(fSymbol) != -1)
            {
                return fSymbol;
            }
        }
        return null;
    }

    public static IwcmFile cleanMetadata(InputStream is, int length , String filename, boolean closeIs)
    {


        String metadataRemoverCommand = Constants.getString("metadataRemoverCommand");
        if (Tools.isNotEmpty(metadataRemoverCommand))
        {

            try
            {
                File tempFile = File.createTempFile("dragDropUpload", filename);
                IwcmFile tempIwcm = new IwcmFile(tempFile);
                IwcmFsDB.writeFiletoDest(is, tempFile, length, closeIs);
                MetadataCleaner.removeMetadata(tempIwcm);
                return tempIwcm;

            } catch (IOException e)
            {
                sk.iway.iwcm.Logger.error(e);
            }
        }
        return null;
    }

    /**
     * Prida zakazane znaky z konfiguracie do zoznamu zakazanych znakov
     * Znaky cita z konfiguracnej premennje FileBrowserTools.forbiddenSymbols, ktora je zoznam oddeleny ciarkou
     */
    private static void addForbidenSymbolsFromConfig() {
        String[] configuredForbiddenSymbols = Constants.getArray("FileBrowserTools.forbiddenSymbols");
        if(configuredForbiddenSymbols != null && configuredForbiddenSymbols.length > 0) {
            for (String configuredForbiddenSymbol : configuredForbiddenSymbols)
            {
                forbiddenSymbols.add(configuredForbiddenSymbol);
            }
        }
    }

}
