package sk.iway.iwcm.system.spring.webjet_component.dialect;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.model.ITemplateEvent;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Trieda CombineAttributeTagProcessor pre iwcm tag s nazvom combine
 * Tag <combine data-iwcm-combine=""/> sluzi na vlozenie JS a CSS do sablon
 * parsuje aj BODY tagu, kde hlada script a link tagy a berie z nich linky
 * je to tak preto, aby sa to dalo pouzit aj bez WJ s npm run start
 */
public class CombineAttributeModelProcessor extends AbstractIwcmAttributeModelProcessor {
    private static final String ATTR_NAME = "combine";
    private static final int PRECEDENCE = 10002;

    public CombineAttributeModelProcessor(String dialectPrefix) {
        super(dialectPrefix, ATTR_NAME, PRECEDENCE);
    }

    @Override
    protected void processTag(ITemplateContext context, IModel model, IProcessableElementTag baseTag, HttpServletRequest request) {

        final IModelFactory modelFactory = context.getModelFactory();

        //zozbieraj zoznam liniek
        List<String> links = new ArrayList<>();
        for (int i=0; i<model.size(); i++) {
            final ITemplateEvent event = model.get(i);
            //Logger.debug(CombineAttributeModelProcessor.class, "event="+event);

            if (event instanceof IStandaloneElementTag) {
                IStandaloneElementTag tag = (IStandaloneElementTag)event;
                if ("link".equalsIgnoreCase(tag.getElementCompleteName())) {
                    links.add(tag.getAttributeValue("href"));
                }
            } else if (event instanceof IOpenElementTag) {
                IOpenElementTag tag = (IOpenElementTag)event;
                if ("script".equalsIgnoreCase(tag.getElementCompleteName())) {
                    links.add(tag.getAttributeValue("src"));
                }
            }
        }

        model.reset();

        //mame zoznam liniek vnutri v tagu

        //Logger.debug(CombineAttributeModelProcessor.class, "======> LINKS.size="+links.size()+" words="+request.getParameter("words"));

        String lng = ProcessorTools.getLng(request);
        boolean combine = isCombine(request);
        long v = ProcessorTools.getVersion();

        List<String> set = getSetFiles(baseTag, context, request, links);
        if (set == null || set.isEmpty()) {
            Logger.error(CombineAttributeModelProcessor.class, "Set is null");
            return;
        }

        // pridanie systemovych JS A CSS suborov
        set = addSystemFiles(set);
        // vyfiltrovanie CSS suborov
        List<String> css = set.stream().filter(s -> s.endsWith(".css")).collect(Collectors.toList());
        // vyfiltrovanie JS suborov
        List<String> js = set.stream().filter(s -> s.endsWith(".js") || s.endsWith(".js.jsp")).collect(Collectors.toList());

        // ak je povoleny combine
        if (combine) {
            if (!css.isEmpty()) {
                addCssTag(model, modelFactory, "/components/_common/combine.jsp?t=css&amp;f="+String.join(",", css)+"&amp;v="+v+"&amp;lng="+lng);
            }
            if (!js.isEmpty()) {
                addScriptTag(model, modelFactory, "/components/_common/combine.jsp?t=js&amp;f="+String.join(",", js)+"&amp;v="+v+"&amp;lng="+lng);
            }
        }
        // ak nie je povoleny combine
        else
        {
            if (!css.isEmpty()) {
                for (String s : css) {
                    addCssTag(model, modelFactory, s+"?v="+v+"&amp;lng="+lng);
                }
            }
            if (!js.isEmpty()) {
                for (String s : js) {
                    addScriptTag(model, modelFactory, s+"?v="+v+"&amp;lng="+lng);
                }
            }
        }
    }

    /**
     * Metoda pre pridanie systemovych suborov do zoznamu na vlozenie
     * @param set List<String>
     * @return List<String>
     */
    private static List<String> addSystemFiles(List<String> set) {
        List<String> result = new ArrayList<>();
        for (String s : set) {
            // pridanie combine soborov z konstanty
            String constValue = Constants.getString("combine-" + s);
            if (Tools.isNotEmpty(constValue)) {
                result.add(constValue);
                continue;
            }

            result.add(s);
        }

        return result;
    }

    /**
     * Metoda pre overenie ci je povolene kombinovat subory cez combine.jsp
     * @param request HttpServletRequest
     * @return boolean
     */
    private static boolean isCombine(HttpServletRequest request) {
        boolean combine = true;
        if ("false".equals(request.getParameter("combineEnabled")))
        {
            combine = false;
            request.getSession().setAttribute("combineEnabled", "false");
        }
        if ("true".equals(request.getParameter("combineEnabled")))
        {
            combine = true;
            request.getSession().removeAttribute("combineEnabled");
        }
        if (request.getSession().getAttribute("combineEnabled")!=null) combine = false;
        return combine;
    }

    /**
     * Metoda pre ziskanie suborov na vlozenie
     * @return List<String>
     */
    private static List<String> getSetFiles(IProcessableElementTag combineTag, ITemplateContext context, HttpServletRequest request, List<String> bodyLinks) {

        final String basePath = ProcessorTools.getAttributeValue(combineTag, context, "basePath");
        final String baseCss = (String)request.getAttribute("base_css_link_nocombine");
        final String cssLink = (String)request.getAttribute("css_link_nocombine");
        String combineAttribute = ProcessorTools.getAttributeValue(combineTag, context, "data-iwcm-combine");

        List<String> list = new ArrayList<>();
        //prve musia ist body linky, tam je typicky jquery
        list.addAll(bodyLinks);
        //az potom specificke pre WJ
        list.addAll(Tools.getStringListValue(Tools.getTokens(combineAttribute, "\n")));

        list = list.stream().map(l -> {
            l = removeCrLf(l.trim());
            l = Tools.replace(l, "USERLANG", Tools.replace(ProcessorTools.getLng(request), "cz", "cs"));

            if (Tools.isNotEmpty(baseCss) && l.contains("base_css_link")) {
                l = Tools.replace(l, "base_css_link", removeCrLf(baseCss));
            }

            if (Tools.isNotEmpty(cssLink) && l.contains("css_link")) {
                l = Tools.replace(l, "css_link", removeCrLf(cssLink));
            }

            if (Tools.isNotEmpty(basePath) && (l.endsWith(".js") || l.endsWith(".css"))) {
                if (l.startsWith(basePath)==false && l.startsWith("/templates/")==false && l.startsWith("/components/")==false) l = basePath + l;
            }

            if (l.contains("page_functions")) {
                //ak obsahuje page_functions predpokladame, ze je uz vlozene aj jQuery, musime poznacit aby sa nevlozilo pri doc_data
                Tools.insertJQuery(request);
            }

            return l;
        }).collect(Collectors.toList());

        return list;
    }

    /**
     * Metoda pre odstranenie nechcenych znakov z vyrazu
     * @param str String
     * @return String
     */
    private static String removeCrLf(String str)
    {
        String result = Tools.replace(str, "\n", ",");
        result = Tools.replace(result, "\r", "");
        result = Tools.replace(result, " ", "");
        result = Tools.replace(result, "\t", "");
        result = Tools.replace(result, ",,", ",");
        //ked nie je pagefunctions na konci a je to vlozene cez EL tak sa spojja dve dokopy (odstrani sa enter) a vtedy sa to posaha
        result = Tools.replace(result, "page_functions.js.jsp/", "page_functions.js.jsp,/");
        return result;
    }
}