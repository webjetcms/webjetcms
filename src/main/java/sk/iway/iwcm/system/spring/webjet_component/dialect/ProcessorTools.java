package sk.iway.iwcm.system.spring.webjet_component.dialect;

import javax.servlet.http.HttpServletRequest;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.tags.CombineTag;

public class ProcessorTools {

    private ProcessorTools() {
        //private konstruktor, trieda ma len staticke metody
    }

    /**
     * Vrati hodnotu zadaneho atributu na hlavnom tagu
     *
     * @param tag
     * @param context
     * @param name
     * @return
     */
    protected static String getAttributeValue(IProcessableElementTag tag, ITemplateContext context, String name) {
        String value = tag.getAttributeValue(name);
        StringBuilder processed = new StringBuilder();
        if (value.contains("\n")) {
            // je to multiline, musime to poskladat
            String[] lines = Tools.getTokens(value, "\n", true);
            for (String line : lines) {
                processed.append(processExpression(context, line)).append("\n");
            }
        } else {
            processed.append(processExpression(context, value));
        }
        return processed.toString();
    }

    /**
     * Vykona expression v retazci
     *
     * @param expression
     * @param context
     * @return
     */
    private static String processExpression(ITemplateContext context, String expression) {
        if (expression.contains("{") && expression.contains("}")) {
            final IEngineConfiguration configuration = context.getConfiguration();
            final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
            final IStandardExpression expressionToExecute = parser.parseExpression(context, expression);
            return (String) expressionToExecute.execute(context);
        }
        return expression;
    }

    /**
     * Vrati jazyk aktualne prihlaseneho usera, aby sa spravne nacitali cachovane
     * subory (sucast parametra)
     *
     * @return String
     */
    protected static String getLng(HttpServletRequest request) {
        return PageLng.getUserLng(request);
    }

    /**
     * Vrati verziu aktualnych suborov, aby sa spravne cachovali subory
     *
     * @return long
     */
    protected static long getVersion() {
        return CombineTag.getVersion();
    }
}
