package sk.iway.iwcm.system.stripes;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import net.sourceforge.stripes.controller.FileUploadLimitExceededException;
import net.sourceforge.stripes.controller.multipart.DefaultMultipartWrapperFactory;
import net.sourceforge.stripes.controller.multipart.MultipartWrapper;

public class MultipartWrapperFactory extends DefaultMultipartWrapperFactory {

    @Override
    public MultipartWrapper wrap(HttpServletRequest request) throws IOException, FileUploadLimitExceededException {

        MultipartWrapper wrapper = super.wrap(request);

        if (wrapper instanceof sk.iway.iwcm.system.stripes.MultipartWrapper customWrapper) {
            if (customWrapper.isRequestParsed()) {
                request.setAttribute("sk.iway.iwcm.system.stripes.MultipartWrapper", true);
                return customWrapper;
            }
            //set multipart to null to prevent duplicating of param in request.getParameterValues(name) in Spring Apps
            return null;
        }
        return wrapper;
    }

    public static boolean isStripesMultipartParsed(HttpServletRequest request) {
        Object attr = request.getAttribute("sk.iway.iwcm.system.stripes.MultipartWrapper");
        return attr != null && (Boolean) attr;
    }

}
