package sk.iway.iwcm.components.news;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.i18n.PropDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ConfDB;

public class NewsTemplateBean
{
	public static final String PAGING_POSITION_KEY = "_paging_position";
	public static final String PAGING_KEY = "_paging";
	private static final String IMAGE_PATH = "/components/news/images";
	private static final String PREFIX = "news.template";

	private String keyBeforeSave;
	private String key;
	private String keyShort;
	private String pagingKey;
	private String pagingPositionKey;
	private String image;
	private String value;
	private PagingPosition pagingPosition;
	private String pagingValue;
	private HttpServletRequest request;
	private boolean selected;
	private NewsTemplateBean selectedTemplate;
	private Prop prop;

	public static enum PagingPosition {
		NONE,
		BEFORE,
		AFTER,
		BEFORE_AND_AFTER
	}

	// prazdny constructor pretoze bez neho nefunguje stripes parameter binding
	public NewsTemplateBean()
	{

	}

	public NewsTemplateBean(String key)
	{
		this(null, key, null);
	}

	public NewsTemplateBean(String key, NewsTemplateBean selectedTemplate)
	{
		this(null, key, selectedTemplate);
	}

	public NewsTemplateBean(HttpServletRequest request, String key, NewsTemplateBean selectedTemplate)
	{
		this.request = request;
		this.key = key;
		this.selectedTemplate = selectedTemplate;

		fillTemplateBean();
	}

	public void delete()
	{
		try
		{
			IwayProperties iwprop = new IwayProperties();
			iwprop.setProperty(this.getKey(), "");

			saveProperty(iwprop);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	public void save()
	{
		IwayProperties iwprop = new IwayProperties();
		iwprop.setProperty(this.getKey(), this.getValue());
		if (Tools.isNotEmpty(getPagingValue())) iwprop.setProperty(this.getPagingKey(), this.getPagingValue());
		iwprop.setProperty(this.getPagingPositionKey(), String.valueOf(this.getPagingPosition().ordinal()));

		saveProperty(iwprop);

		Prop.getInstance(true);
	}

	private void saveProperty(IwayProperties iwprop) {
		//natvrdo sk lebo sablony zapisujeme do SK propertiesov (aby boli pre vsetky jazyky)
		String lng = "sk";

		int insertCounter = 0;
		int updateCounter = 0;

		for(Entry<String, String> property : iwprop.entrySet())
		{
			if(Tools.isEmpty(property.getKey())) continue;

			Logger.debug(PropDB.class, "Importing prop, key="+property.getKey()+" value="+property.getValue());

			if((new SimpleQuery().forInt("Select count(*) from "+ConfDB.PROPERTIES_TABLE_NAME+" where lng = ? and prop_key = ?", lng, property.getKey())) > 0)
			{
				new SimpleQuery().execute("UPDATE "+ConfDB.PROPERTIES_TABLE_NAME+" SET prop_value=? WHERE prop_key=? AND lng = ?",property.getValue(),property.getKey(),lng);
				updateCounter++;
			}
			else
			{
				new SimpleQuery().execute("INSERT INTO "+ConfDB.PROPERTIES_TABLE_NAME+" (prop_key,lng,prop_value) VALUES (?,?,?)",property.getKey(),lng,property.getValue());
				insertCounter++;
			}
		}
		Logger.debug(PropDB.class, "IwayProperties saved, inserted: " + insertCounter + " , updated: " + updateCounter);
	}

	public void fillTemplateBean()
	{
		if (keyShort != null) {
			key = PREFIX + "." + keyShort;
		}
		if (key != null) {
			pagingKey = key + PAGING_KEY;
			pagingPositionKey =  key + PAGING_POSITION_KEY;

			String[] imageKeys = Tools.getTokens(key, ".");
			keyShort = imageKeys[imageKeys.length - 1];

			//natvrdo sk lebo sablony zapisujeme do SK propertiesov (aby boli pre vsetky jazyky)
			prop = Prop.getInstance("sk");

			fillValue();
			fillPagingValue();
			fillPagingPositionValue();
			fillImage();
			fillSelected();

			if (Tools.isEmpty(getValue())) {

			}
		}


	}

	private void fillValue() {
		if (value == null) {
			String value = prop.getText(key);
			if (!value.equals(key)) {
				this.setValue(value);
			}
		}
	}

	private void fillPagingValue() {
		if (Tools.isNotEmpty(pagingKey) && Tools.isEmpty(pagingValue)) {
			String pagingValue = prop.getText(pagingKey);

			if (Tools.isNotEmpty(pagingValue) && !pagingKey.equals(pagingValue)) {
				this.setPagingValue(pagingValue);
			}
		}
	}

	private void fillPagingPositionValue() {
		if (Tools.isNotEmpty(pagingPositionKey)) {
			String pagingPositionString = prop.getText(pagingPositionKey);

			if (pagingPosition == null && !pagingPositionKey.equals(pagingPositionString)) {
				int pagingPosition = Tools.getIntValue(pagingPositionString, 0);

				if (pagingPosition > 0) {
					this.setPagingPosition(PagingPosition.values()[pagingPosition]);
				}
			}
		}
	}

	private void fillSelected()
	{
		if (Tools.isNotEmpty(key) && selectedTemplate != null) {
			selected = key.equals(selectedTemplate.getKey());
		}
	}

	private void fillImage()
	{
		List<String> extensions = Arrays.asList(new String[]{"jpg", "png"});

		String imagePath = "";

		for (String extension : extensions) {
			String path = IMAGE_PATH + "/" + keyShort + "." + extension;
			IwcmFile imageFile = new IwcmFile(Tools.getRealPath(WriteTagToolsForCore.getCustomPage(path, getRequest())));

			if (imageFile.isFile()) {
				imagePath = imageFile.getVirtualPath();
				break;
			}
		}

		if (Tools.isEmpty(imagePath)) {
			imagePath = IMAGE_PATH + "/placeholder-380-200.png";
		}


		this.image = imagePath;
	}

	public String getKey()
	{
		return key;
	}

	public void setKey(String key)
	{
		this.key = key;
		this.pagingKey = key + PAGING_KEY;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getPagingValue()
	{
		return pagingValue;
	}

	public void setPagingValue(String pagingValue)
	{
		this.pagingValue = pagingValue;
	}

	public HttpServletRequest getRequest()
	{
		return request;
	}

	public void setRequest(HttpServletRequest request)
	{
		this.request = request;
	}

	public String getPagingKey()
	{
		return pagingKey;
	}

	public void setPagingKey(String pagingKey)
	{
		this.pagingKey = pagingKey;
	}

	public String getImage()
	{
		return image;
	}

	public void setImage(String image)
	{
		this.image = image;
	}

	public String getKeyShort()
	{
		return keyShort;
	}

	public void setKeyShort(String keyShort)
	{
		this.keyShort = keyShort;
	}

	public String getKeyBeforeSave()
	{
		return keyBeforeSave;
	}

	public void setKeyBeforeSave(String keyBeforeSave)
	{
		this.keyBeforeSave = keyBeforeSave;
	}

	public boolean isSelected()
	{
		return selected;
	}

	public void setSelected(boolean selected)
	{
		this.selected = selected;
	}

	public PagingPosition getPagingPosition()
	{
		return pagingPosition;
	}

	public void setPagingPosition(PagingPosition pagingPosition)
	{
		this.pagingPosition = pagingPosition;
	}

	public String getPagingPositionKey()
	{
		return pagingPositionKey;
	}

	public void setPagingPositionKey(String pagingPositionKey)
	{
		this.pagingPositionKey = pagingPositionKey;
	}
}
