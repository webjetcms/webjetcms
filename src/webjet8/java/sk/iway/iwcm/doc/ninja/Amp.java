package sk.iway.iwcm.doc.ninja;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.helpers.RequestHelper;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Amp {
    private final HttpServletRequest request;

    public Amp(HttpServletRequest request) {
        this.request = request;
        request.setAttribute("jQueryInserted", "true");
    }

    private void replaceInObject(String object) {
        String html = (String) request.getAttribute(object);
        if (Tools.isNotEmpty(html)) {
            html = replaceInString(html);
            request.setAttribute(object, html);
        }
    }

    public void replaceInRequest() {
        String[] objects = Tools.getTokens(Constants.getString("ampObjects","doc_data"), ",");
        for (String object : objects) {
            replaceInObject(object);
        }
    }

    public String replaceInString(String html) {
        if (Tools.isNotEmpty(html)) {
            Document document = Jsoup.parseBodyFragment(html);
            replaceImages(document);
            removeScripts(document);
            removeIncludes(document);

            return document.body().html();
        }

        return "";
    }

    private void replaceImages(Document document) {
        Elements elements = document.select("img");
        boolean ampImageRealDimensions = Constants.getBoolean("ampImageRealDimensions");
        int ampImageRealDimensionsCache = Constants.getInt("ampImageRealDimensionsCache", 0);

        Attributes attributes = getAttributes();
        for (Element element : elements) {
            Element ampImg = new Element(Tag.valueOf("amp-img"), "", attributes.clone());
            int width = 0;
            int height = 0;
            String src = "";

            for (Attribute attribute : element.attributes()) {
                if ("width".equalsIgnoreCase(attribute.getKey())) {
                    width = Tools.getIntValue(attribute.getValue(), 0);
                    continue;
                }
                if ("height".equalsIgnoreCase(attribute.getKey())) {
                    height = Tools.getIntValue(attribute.getValue(), 0);
                    continue;
                }

                if ("src".equalsIgnoreCase(attribute.getKey())) {
                    src = attribute.getValue();
                }

                if ("style".equalsIgnoreCase(attribute.getKey())) {
                    continue;
                }

                ampImg.attr(attribute.getKey(), attribute.getValue());
            }

            if (ampImageRealDimensions && Tools.isNotEmpty(src)) {

                if (ampImageRealDimensionsCache > 0) {
                    Cache c = Cache.getInstance();
                    String dimensions = c.getObject("ampImageCache-" + src, String.class);
                    if (Tools.isNotEmpty(dimensions) && dimensions.contains("x")) {
                        String[] dimesnionsTokens = Tools.getTokens(dimensions, "x");
                        width = Tools.getIntValue(dimesnionsTokens[0], 0);
                        height = Tools.getIntValue(dimesnionsTokens[1], 0);
                    }
                }

                if (width == 0 || height == 0) {
                    try {
                        BufferedImage bimg = ImageIO.read(new File(Tools.getRealPath(src)));
                        if (bimg != null) {
                            width = bimg.getWidth();
                            height = bimg.getHeight();

                            if (ampImageRealDimensionsCache > 0) {
                                Cache c = Cache.getInstance();
                                c.setObject("ampImageCache-" + src, width + "x" + height, ampImageRealDimensionsCache);
                            }
                        }
                    } catch (IOException e) {
                        sk.iway.iwcm.Logger.error(e);
                    }
                }
            }

            if (width > 0) {
                ampImg.attr("width", "" + width);
            }

            if (height > 0) {
                ampImg.attr("height", "" + height);
            }

            element.replaceWith(ampImg);
        }
    }

    private void removeScripts(Document document) {
        document.select("script").remove();
    }

    private void removeIncludes(Document document) {
        List<String> allowedIncludes = Tools.getStringListValue(Tools.getTokens(Constants.getString("ampAllowedIncludes", "/components/news/news-velocity.jsp"), ","));
        String html = document.body().html();

        final String regex = "\\!INCLUDE.*\\)!";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(html);

        outerloop: while (matcher.find()) {
            String match = matcher.group(0);
            if (!allowedIncludes.isEmpty()) {
                for (String allowedInclude : allowedIncludes) {
                    if (match.contains(allowedInclude)) {
                        Logger.debug(Amp.class, String.format("Nechavam include: %s", match));
                        continue outerloop;
                    }
                }
            }

            Logger.debug(Amp.class, String.format("Odstranujem include: %s", match));
            html = Tools.replace(html, match, "");
        }

        document.body().html(html);
    }

    private Attributes getAttributes() {
        Attributes attributes = new Attributes();
        RequestHelper requestHelper = new RequestHelper(request);
        TemplateDetails template = requestHelper.getTemplate();

        if (template == null) {
            return attributes;
        }
        int templateId = template.getTempId();

        if (templateId < 1) {
            return attributes;
        }

        String constantKey = "ampImgAttributes";
        String[] attributesArray = getAttributesArray(Constants.getString(constantKey + templateId, ""));

        if (attributesArray.length == 0) {
            attributesArray = getAttributesArray(Constants.getString(constantKey, ""));
        }

        if (attributesArray.length > 0) {
            for (String s : attributesArray) {
                String[] tokens = Tools.getTokens(s, "=");
                attributes.put(tokens[0], Tools.replace(tokens[1], "\"", ""));
            }
        }

        return attributes;
    }

    private String[] getAttributesArray(String att) {
        String attPmd = att;
        if (attPmd.contains("\n")) {
            attPmd = Tools.replace(attPmd, "\n", " ");
        }
        return Tools.getTokens(attPmd, " ");
    }
}
