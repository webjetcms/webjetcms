package sk.iway.iwcm.components.multistep_form.support;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import sk.iway.iwcm.Constants;

public class CustommStepInterceptor implements StepInterceptorInterface {

    @Override
    public void runInterceptor(String formName, Long currentStepId, JSONObject currentReceived, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        List<String> fieldsNames = Arrays.stream( Constants.getArray("multistepform_emailFields") )
                                    .map(s -> s.toLowerCase())
                                    .toList();

        // Try get any mail from currentRecived object
        String pes = "";
    }
}
