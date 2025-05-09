package sk.iway.iwcm.tags;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 * Implementation of <code>TagExtraInfo</code> for the <b>iterate</b> tag,
 * identifying the scripting object(s) to be made visible.
 *
 * @version $Rev$ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class IterateTei extends TagExtraInfo {
    /**
     * Return information about the scripting variables to be created.
     */
    public VariableInfo[] getVariableInfo(TagData data) {
        // prime this array with the maximum potential variables.
        // will be arraycopy'd out to the final array based on results.
        VariableInfo[] variables = new VariableInfo[2];

        // counter for matched results.
        int counter = 0;

        /* id : object of the current iteration */
        String id = data.getAttributeString("id");
        String type = data.getAttributeString("type");

        if (id != null) {
            if (type == null) {
                type = "java.lang.Object";
            }

            variables[counter++] =
                new VariableInfo(data.getAttributeString("id"), type, true,
                    VariableInfo.NESTED);
        }

        /* indexId : number value of the current iteration */
        String indexId = data.getAttributeString("indexId");

        if (indexId != null) {
            variables[counter++] =
                new VariableInfo(indexId, "java.lang.Integer", true,
                    VariableInfo.NESTED);
        }

        /* create returning array, and copy results */
        VariableInfo[] result;

        if (counter > 0) {
            result = new VariableInfo[counter];
            System.arraycopy(variables, 0, result, 0, counter);
        } else {
            result = new VariableInfo[0];
        }

        return result;
    }
}