package sk.iway.iwcm.editor.appstore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  AppBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2014
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 17.3.2014 14:59:52
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class AppBean
{
	private String nameKey;
	private String componentClickAction;
	private String imagePath;
	private BigDecimal priceEur;
	private List<String> galleryImages = null;
	private String domainName = null;
	private String itemKey = ""; //pouziva sa v AppManager kvoli filtracii prav
	//variant of the app, for multiple apps with same itemKey set different variant to keep them separate
	private String variant = "";
    private String descKey;
	private boolean custom = false;
	private String componentPath;

	public boolean isFree()
	{
		if (priceEur == null || priceEur.intValue()==0) return true;

		return false;
	}

	public String getDescriptionKey()
	{
        if (Tools.isNotEmpty(descKey)) return descKey;

		int index = nameKey.lastIndexOf(".title");
		if (index > 0) return nameKey.substring(0, index)+".desc";

		return nameKey+".desc";
	}

	/**
	 * Vrati zoznam obrazkov do galerie, ktore su zhodne z imagePath ale cislovane ako ...-0.jpg, ...-1.jpg atd
	 * @return
	 */
	public List<String> getGalleryImages()
	{
		if (galleryImages != null) return galleryImages;

		List<String> images = new ArrayList<>();

		int lastSlash = imagePath.lastIndexOf('/');
		if (lastSlash > 0)
		{
			String basePath = imagePath.substring(0, lastSlash);
            RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
            String lng = rb.getLng();
            String lngSuffix = switch (lng) {
                case "en" -> "-en";
                case "cz" -> "-cs";
                default -> "";
            };

            for (int i = 0; i < 10; i++) {
                String[] extensions = {".jpg", ".gif", ".png"};
                boolean found = false;

                for (String ext : extensions) {
                    String imagePathWithSuffix = basePath + "/screenshot-" + i + lngSuffix + ext;
                    if (FileTools.isFile(imagePathWithSuffix)) {
                        images.add(imagePathWithSuffix);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    for (String ext : extensions) {
                        String imagePathWithoutSuffix = basePath + "/screenshot-" + i + ext;
                        if (FileTools.isFile(imagePathWithoutSuffix)) {
                            images.add(imagePathWithoutSuffix);
                            break;
                        }
                    }
                }
            }
		}

		galleryImages = images;

		return images;
	}

	public String getNameKey()
	{
		return nameKey;
	}
	public void setNameKey(String nameKey)
	{
		this.nameKey = nameKey;
	}
	public String getComponentClickAction()
	{
		return componentClickAction;
	}
	public void setComponentClickAction(String componentClickAction)
	{
		this.componentClickAction = componentClickAction;
	}
	public String getImagePath()
	{
		return imagePath;
	}
	public void setImagePath(String imagePath)
	{
		this.imagePath = imagePath;
	}
	public BigDecimal getPriceEur()
	{
		return priceEur;
	}
	public void setPriceEur(BigDecimal priceEur)
	{
		this.priceEur = priceEur;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("AppBean [nameKey=").append(nameKey)
				.append(", componentClickAction=").append(componentClickAction)
				.append(", imagePath=").append(imagePath).append(", priceEur=")
				.append(priceEur).append(", galleryImages=")
				.append(galleryImages).append(", domainName=")
				.append(domainName).append(", itemKey=").append(itemKey)
				.append("]");
		return builder.toString();
	}

	public String getDomainName()
	{
		return domainName;
	}

	public void setDomainName(String domainName)
	{
		this.domainName = domainName;
	}

	public String getItemKey()
	{
		return itemKey;
	}

	public void setItemKey(String itemKey)
	{
		this.itemKey = itemKey;
	}

    public String getDescKey()
    {
        return descKey;
    }

    public void setDescKey(String descKey)
    {
        this.descKey = descKey;
    }

    public void setGalleryImages(String images)
    {
		if (images.endsWith("/")) {
			//list all images with name screenshot-X.jpg in images folder
			IwcmFile dir = new IwcmFile(Tools.getRealPath(images));
			IwcmFile[] files = FileTools.sortFilesByName(dir.listFiles());
			this.galleryImages = new ArrayList<>();
			for (IwcmFile file : files) {
				if (file.getName().startsWith("screenshot-")) {
					this.galleryImages.add(images + file.getName());
				}
			}
		} else if (Tools.isNotEmpty(images)) {
            String[] galleryImagesArr = Tools.getTokens(images, ",", true);
            this.galleryImages = Arrays.asList(galleryImagesArr);
        }
    }

	public boolean getCustom() {
		return custom;
	}

	public void setCustom(boolean custom) {
		this.custom = custom;
	}

	public String getComponentPath() {
		return componentPath;
	}

	public void setComponentPath(String componentPath) {
		this.componentPath = componentPath;
	}

	public String getVariant() {
		return variant;
	}
	public void setVariant(String variant) {
		this.variant = variant;
	}
}

