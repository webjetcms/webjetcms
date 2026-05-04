package sk.iway.iwcm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *  Zakladna praca s JSON objektami
 */
public class JsonTools
{
	private JsonTools() {
		//private constructor
	}

	/**
	 * Vycisti html kod od includov a inych balastov
	 * @param html html kod
	 * @param request request
	 * @return
	 */
	public static String prepare4Json(String html, HttpServletRequest request)
	{
		if(html == null || request == null) throw new IllegalArgumentException("html nor request can't be null");
		html = html.replace("<p>&nbsp;</p>","");
		html = html.replaceAll("!INCLUDE.*?\\)!","");
		html = html.replaceAll("<img[\\s\\S]*?src=[\"'](\\/[\\s\\S]*?)[\"'][\\s\\S]*?>", "<img src=\""+Tools.getBaseHref(request)+"/thumb$1?w=320&amp;h=480\" thumb=\"http://"+Tools.getServerName(request)+"/thumb$1?w=300&amp;h=100\" />");
		html = html.replaceAll("<a[\\s\\S]*?name=['\"]([\\s\\S]*?)['\"].*<[\\S]*?a>", "");
		html = html.replaceAll("<a[\\s\\S]*?href=['\"](\\/[\\s\\S]*?)['\"]", "<a href=\""+Tools.getBaseHref(request)+"$1\"");
		html = html.replaceAll("<object[\\s\\S]*?src=[\"']http://www.youtube.com/v/([\\s\\S]*?)(&[\\s\\S]*?)??[\"'][\\s\\S]*?>[\\s\\S]*?</object>", "<youtube>http://www.youtube.com/watch?v=$1</youtube>");
		html = html.replaceAll("style=[\"']([\\s\\S]*?)[\"']", "");
		return html;
	}

	/**
	 * Upravi retazec aby mohol byt bezpecne vlozeny do uvodzoviek vramci bezneho JSP vypisu "premenna": "hodnota"
	 * @param text
	 * @return
	 */
	public static String escape(String text)
	{
		return Tools.replace(text, "\"", "\\\"");
	}

	/**
	 * Spravi z nested json objektu flat strukturu
	 * @param object
	 * @param flattened
	 * @return
	 */
	public static JSONObject flatten(JSONObject object, JSONObject flattened){
	    if(flattened == null){
	        flattened = new JSONObject();
	    }
	    Iterator<?> keys = object.keys();
	    while(keys.hasNext()){
	        String key = (String)keys.next();
	        try {
	            if(object.get(key) instanceof JSONObject){
	            	flatten(object.getJSONObject(key), flattened);
	            } else {
	                flattened.put(key, object.get(key));
	            }
	        } catch(JSONException e){
	           Logger.debug(JsonTools.class,e.getMessage());
	        }
	    }
	    return flattened;
	}

	public static String objectToJSON(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

	/**
	 * Zo zadaneho JSON retazca vytvori mapu kluc:hodnota pricom kluce zlozi aj do vnorenych atributov typu kluc.subkluc.tretiauroven:hodnota
	 * @param json
	 * @param baseKey
	 * @param myMap
	 * @return
	 */
	private static Map<String, String> getKeyValuesNested(JSONObject json, String baseKey, Map<String,String> myMap) {
		if (myMap == null) myMap = new HashMap<>();
		if (baseKey == null) baseKey = "";

		int curLen =  json.length();
		Iterator<String> keys;
		if(curLen>1) {
			keys = json.keys();
			while(keys.hasNext()){
				String nextKeys = keys.next();
				try {
					if(json.get(nextKeys) instanceof  JSONObject) {
						if(curLen>1) {
							getKeyValuesNested(json.getJSONObject(nextKeys), baseKey+nextKeys+".", myMap);
						}
					}
					else{
						Object ansKey =  json.get(nextKeys);
						myMap.put(baseKey+nextKeys,ansKey.toString());
					}
				}
				catch (Exception e) {
					Logger.error(JsonTools.class, e);
				}
			}
		}
		else if(curLen == 1) {
			try {
				//Dont know what is purpose of this ...
				//Yust adding has() control so it wont throu exception
				String key = json.toString();
				if(json.has(key)) {
					Object ansKey = json.get(key);
					myMap.put(baseKey+json.toString(),ansKey.toString());
				}
			}
			catch (Exception e) {
				Logger.error(JsonTools.class, e);
			}
		}
		return myMap;
	}

	/**
	 * Vrati hodnotu z JSON retazca podla zadaneho kluca
	 * @param json
	 * @param key - kluc vratane moznosti vnarania kluc.druhauroven.tretia
	 * @return
	 */
	public static String getValue(String json, String key) {
		try {
			JSONObject jsondata = new JSONObject(json);

			Map<String, String> keyValues = getKeyValuesNested(jsondata, null, null);

			return keyValues.get(key);
		} catch (Exception e) {
			Logger.error(JsonTools.class, e);
		}
		return null;
	}

	/**
	 * Vrati boolean hodnotu z JSON retazca podla zadaneho kluca
	 * @param json
	 * @param key - kluc vratane moznosti vnarania kluc.druhauroven.tretia
	 * @return
	 */
	public static boolean getBooleanValue(String json, String key) {
		String value = getValue(json, key);
		return "true".equals(value);
	}
}
