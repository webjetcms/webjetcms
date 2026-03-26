package sk.iway.iwcm.sync.export;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.gallery.GalleryBean;
import sk.iway.iwcm.gallery.GalleryDimension;
import sk.iway.iwcm.inquiry.InquiryBean;

/**
 * Obsah stranok urceny na export.
 * Zoznam suborov s informaciou, ktory subor je z ktorej stranky;
 * zoznam bannerov; zoznam ankiet.
 * Jedna stranka moze obsahovat viacero suborov, jeden subor moze byt z viacerych stranok.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.6.2012 9:33:24
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class Content
{

	/**
	 * Zoznam suborov patriacich k jednotlivym strankam.
	 * Kluc je docId, hodnota je zoznam suborov (ich virtualnych ciest) v danej stranke.
	 */
	private Map<Integer, Collection<String>> docFiles = new HashMap<Integer, Collection<String>>();

	/**
	 * Zoznam popisov exportovanych suborov.
	 */
	private List<File> files = new ArrayList<File>();

	private List<BannerBean> banners = new ArrayList<BannerBean>();

	private List<InquiryBean> inquiries = new ArrayList<InquiryBean>();
	
	private List<Map<String, GalleryBean>> galleryImages = new ArrayList<Map<String, GalleryBean>>();
	private List<GalleryInfo> galleryInfos = new ArrayList<GalleryInfo>();

	/**
	 * Vrati zoznam suborov patriacich k jednotlivym strankam.
	 * 
	 * @return
	 */
	public Map<Integer, Collection<String>> getDocFiles()
	{
		return docFiles;
	}

	/**
	 * Vrati popisy jednotlivych suborov v ZIP archive.
	 * 
	 * @return
	 */
	public List<File> getFiles()
	{
		return files;
	}

	/**
	 * Vrati zoznam bannerov.
	 * 
	 * @return
	 */
	public List<BannerBean> getBanners()
	{
		return banners;
	}

	/**
	 * Vrati zoznam ankiet.
	 * 
	 * @return
	 */
	public List<InquiryBean> getInquiries()
	{
		return inquiries;
	}

	/**
	 * Vrati zoznam obrazkov v galerii, zoskupene rozne jazykove verzie jedneho obrazku.
	 * 
	 * @return
	 */
	public List<Map<String, GalleryBean>> getGalleryImages()
	{
		return galleryImages;
	}

	/**
	 * Vrati zoznam adresarov v galerii.
	 * 
	 * @return
	 */
	public List<GalleryInfo> getGalleryInfos()
	{
		return galleryInfos;
	}

	// settery pre XML enkoder
	public void setDocFiles(Map<Integer, Collection<String>> docFiles) { this.docFiles = docFiles; }
	public void setFiles(List<File> files) { this.files = files; }
	public void setBanners(List<BannerBean> banners) { this.banners = banners; }
	public void setInquiries(List<InquiryBean> inquiries) { this.inquiries = inquiries; }
	public void setGalleryImages(List<Map<String, GalleryBean>> galleryImages) { this.galleryImages = galleryImages; }
	public void setGalleryInfos(List<GalleryInfo> galleryInfos) { this.galleryInfos = galleryInfos; }

	/**
	 * Vrati, ci obsahuje dany subor.
	 * 
	 * @param virtualPath  virtualna adresa suboru
	 * @return             true ak obsahuje, inak false
	 */
	public boolean containsFile(String virtualPath)
	{
		for (File file : files)
		{
			if (virtualPath.equals(file.virtualPath)) return true;
		}
		return false;
	}

	/**
	 * Informacie o subore: povodna cesta, nazov v archive, cas modifikacie, velkost.
	 */
	public static class File
	{

		private String virtualPath;
		private String zipPath;
		private long time;
		private long size;

		public String getVirtualPath() { return virtualPath; }
		public String getZipPath    () { return zipPath    ; }
		public long   getTime       () { return time       ; }
		public long   getSize       () { return size       ; }

		public void setVirtualPath(String virtualPath) { this.virtualPath = virtualPath; }
		public void setZipPath    (String zipPath    ) { this.zipPath     = zipPath    ; }
		public void setTime       (long   time       ) { this.time        = time       ; }
		public void setSize       (long   size       ) { this.size        = size       ; }

	}

	/**
	 * Informacie o galerii obrazkov.
	 */
	public static class GalleryInfo
	{
		private GalleryDimension info;
		private Dimension dim;
		private Dimension dimNormal;

		public GalleryDimension getInfo     () { return info     ; }
		public Dimension        getDim      () { return dim      ; }
		public Dimension        getDimNormal() { return dimNormal; }

		public void setInfo     (GalleryDimension info     ) { this.info      = info     ; }
		public void setDim      (Dimension        dim      ) { this.dim       = dim      ; }
		public void setDimNormal(Dimension        dimNormal) { this.dimNormal = dimNormal; }
	}

}
