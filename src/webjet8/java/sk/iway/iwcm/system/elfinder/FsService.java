package sk.iway.iwcm.system.elfinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsItemFilter;
import cn.bluejoe.elfinder.service.FsSecurityChecker;
import cn.bluejoe.elfinder.service.FsServiceConfig;
import cn.bluejoe.elfinder.service.FsVolume;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UsersDB;

public class FsService implements cn.bluejoe.elfinder.service.FsService //NOSONAR
{
	public static final int TYPE_ALL = 1;
	public static final int TYPE_IMAGES = 2;
	public static final int TYPE_LINK = 3;
	//file browser
	public static final int TYPE_FILES = 4;
	public static final int TYPE_PAGES = 5;

	FsSecurityChecker _securityChecker = new FsSecurityCheckerChain(); //NOSONAR
	FsServiceConfig _serviceConfig = new IwcmFsServiceConfig(); //NOSONAR

	public static Identity getCurrentUser()
	{
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb != null && rb.getRequest()!=null)
		{
			return UsersDB.getCurrentUser(rb.getRequest());
		}
		return null;
	}

	@Override
	public FsServiceConfig getServiceConfig()
	{
		return _serviceConfig;
	}

	public void setServiceConfig(FsServiceConfig serviceConfig)
	{
		_serviceConfig = serviceConfig;
	}

	Map<FsVolume, String> _volumeIds = new HashMap<FsVolume, String>(); //NOSONAR

	FsVolume[] _volumes; //NOSONAR

	static String[][] escapes = { { "+", "_P" }, { "-", "_M" }, { "/", "_S" }, { ".", "_D" }, { "=", "_E" } };

	public FsService()
	{
		initialize(TYPE_ALL);
	}

	public FsService(int type)
	{
		initialize(type);
	}

	/**
	 * Pre Volumes ktore poznaju virtualPath vrati priamo virtualPath inak vrati path
	 * @param fsi
	 * @return
	 * @throws IOException
	 */
	public static String getVirtualPath(FsItemEx fsi) throws IOException
	{
		String virtualPath = fsi.getPath();
		if (fsi.getVolumeId().equals(IwcmDocGroupFsVolume.VOLUME_ID))
		{
			virtualPath = ((IwcmDocGroupFsVolume)fsi.getVolume()).getVirtualPath(fsi);
		}
		if (fsi.getVolumeId().equals(IwcmFsVolume.VOLUME_ID_ACTUAL_PAGE))
		{
			virtualPath = ((IwcmFsVolume)fsi.getVolume()).getVirtualPath(fsi);
		}

		return virtualPath;
	}

	public static int getSortPriority(FsItemEx fsi) throws IOException
	{
		if (fsi.getVolumeId().equals(IwcmDocGroupFsVolume.VOLUME_ID))
		{
			return ((IwcmDocGroupFsVolume)fsi.getVolume()).getSortPriority(fsi);
		}

		/*
		if (fsi.getVolumeId().equals(IwcmFsVolume.VOLUME_ID_ACTUAL_PAGE))
		{
			return ((IwcmActualPageFsVolume)fsi.getVolume()).getSortPriority(fsi);
		}
		*/

		return -1;
	}

	/**
	 * Inicializacia volumes podla typu zobrazenia (obrazky, vsetko...)
	 * @param type
	 */
	private void initialize(int type)
	{
		Logger.debug(FsService.class, "IwcmFsService initialize, type="+type);

		int counter = 0;

		if (type == TYPE_IMAGES)
		{
			_volumes = new FsVolume[2];

			counter = addActualPageVolume(counter);
			counter = addLibraryVolume(counter);
		}
		else if (type == TYPE_LINK)
		{
			boolean elfinderEnableFileArchive = Constants.getBoolean("elfinderFileArchiveEnabled");
			int size = 3;
			if (elfinderEnableFileArchive) size = 4;

			_volumes = new FsVolume[size];

			counter = addActualPageVolume(counter);
			counter = addLibraryVolume(counter);
			counter = addDocGroupVolume(counter);
			if (elfinderEnableFileArchive)
			{
				counter = addArchivVolume(counter);
			}
		}
		else if (type == TYPE_FILES)
		{
			_volumes = new FsVolume[3];

			counter = addLibraryVolume(counter);
			counter = addFsVolume(counter);
			counter = addDocGroupVolume(counter);
		}
		else if (type == TYPE_PAGES)
		{
			_volumes = new FsVolume[1];

			counter = addDocGroupVolume(counter);
		}
		else
		{
			_volumes = new FsVolume[4];

			counter = addLibraryVolume(counter);
			counter = addFsVolume(counter);
			counter = addDocGroupVolume(counter);
			counter = addActualPageVolume(counter);
		}

		//moze tam byt null, prepocitaj
		int notNullSize = 0;
		FsVolume[] allItems = new FsVolume[_volumes.length];
		for (int i=0; i<_volumes.length; i++)
		{
			allItems[i] = _volumes[i];
			if (allItems[i]!=null) notNullSize++;
		}

		if (notNullSize != _volumes.length)
		{
			//musime shrinknut
			_volumes = new FsVolume[notNullSize];
			int notNullCounter = 0;
			for (int i=0; i<allItems.length; i++)
			{
				if (allItems[i]!=null)
				{
					_volumes[notNullCounter++] = allItems[i];
				}
			}
		}
	}

	private int addLibraryVolume(int counter)
	{
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		String domainAliasAppend = "";
		if (rb != null)
		{
			String domain = rb.getDomain();
			String domainAlias = MultiDomainFilter.getDomainAlias(domain);
			if (Tools.isNotEmpty(domainAlias))
			{
				domainAliasAppend = " ("+domainAlias+")";
			}
		}

		_volumes[counter] = new IwcmLibraryFsVolume(Prop.getTxt("elfinder.library")+domainAliasAppend, "/");
		_volumeIds.put(_volumes[counter], "iwcm_1");
		counter++;
		return counter;
	}

	private int addFsVolume(int counter)
	{
		_volumes[counter] = new IwcmFsVolume(Prop.getTxt("elfinder.allFiles"), "/");
		_volumeIds.put(_volumes[counter], "iwcm_2");
		counter++;
		return counter;
	}

	private int addActualPageVolume(int counter)
	{
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		int docId = Tools.getIntValue(rb.getRequest().getParameter("docId"), 1);
		int groupId = Tools.getIntValue(rb.getRequest().getParameter("groupId"), -1);

		if (groupId > 0)
		{

			StringBuilder rootDirs = new StringBuilder();

			String[] prefixes = {"/images", "/files", "/images/gallery"};
			for (String prefix : prefixes)
			{
				String dir = UploadFileTools.getPageUploadSubDir(docId, groupId, prefix);
				//Logger.debug(FsService.class, "uploadSubdir: docId="+docId+", groupId="+groupId+", dir="+dir);
				IwcmFile dirFile = new IwcmFile(Tools.getRealPath(dir));

				if (dirFile.exists()==false && prefix.equals("/images/gallery"))
				{
					//galeriu vynechavame, zobrazi sa ako folder len ked existuje
					continue;
				}

				if (dirFile.exists()==false)
				{
					boolean ret = dirFile.mkdirs();
					Logger.debug(FsService.class, "CREATING DIR: "+dirFile.getAbsolutePath()+" ret="+ret);
				}

				if (rootDirs.length()>0) rootDirs.append(",");
				rootDirs.append(dir);
			}

			_volumes[counter] = new IwcmActualPageFsVolume(Prop.getTxt("elfinder.actualPage"), "/", rootDirs.toString());
			_volumeIds.put(_volumes[counter], IwcmFsVolume.VOLUME_ID_ACTUAL_PAGE);
			counter++;
		}
		return counter;
	}

	private int addDocGroupVolume(int counter)
	{
		Identity user = getCurrentUser();
		if (user == null || user.isDisabledItem("menuWebpages")) return counter;

		_volumes[counter] = new IwcmDocGroupFsVolume(Prop.getTxt("menu.web_sites"));
		_volumeIds.put(_volumes[counter], IwcmDocGroupFsVolume.VOLUME_ID);
		counter++;
		return counter;
	}

	private int addArchivVolume(int counter)
    {
        _volumes[counter] = new IwcmArchivFsVolume(Prop.getTxt("components.file_archiv.name"));
        _volumeIds.put(_volumes[counter], IwcmArchivFsVolume.VOLUME_ID);
        counter++;
        return counter;
    }

	@Override
	public FsItem fromHash(String hash)
	{
		if (hash != null)
		{
			for (FsVolume v : _volumes)
			{
				String prefix = getVolumeId(v) + "_";

				if (hash.equals(prefix))
				{
					return v.getRoot();
				}

				if (hash.startsWith(prefix))
				{
					String localHash = hash.substring(prefix.length());

					for (String[] pair : escapes)
					{
						localHash = localHash.replace(pair[1], pair[0]);
					}

					//JEEFF String relativePath = new String(Base64.decodeBase64(localHash));
					String relativePath = new String(Base64.decodeBase64(localHash.getBytes()));
					return v.fromPath(relativePath);
				}
			}
		}

		return null;
	}

	@Override
	public String getHash(FsItem item) throws IOException
	{
		String relativePath = item.getVolume().getPath(item);
		if ("/group:0".equals(relativePath)) relativePath = "";
		String base = new String(Base64.encodeBase64(relativePath.getBytes()));

		for (String[] pair : escapes)
		{
			base = base.replace(pair[0], pair[1]);
		}

		return getVolumeId(item.getVolume()) + "_" + base;
	}

	/**
	 * Vrati hash pre zadanu cestu (pouzivane v JS kode pre znovaotvorenie cesty)
	 * @param relativePath
	 * @return
	 */
	public static String getHash(String relativePath)
	{
		String base = new String(Base64.encodeBase64(relativePath.getBytes()));

		for (String[] pair : escapes)
		{
			base = base.replace(pair[0], pair[1]);
		}

		String volumeId = "iwcm_1";

		return volumeId + "_" + base;
	}

	@Override
	public FsSecurityChecker getSecurityChecker()
	{
		return _securityChecker;
	}

	@Override
	public String getVolumeId(FsVolume volume)
	{
		return _volumeIds.get(volume);
	}

	@Override
	public FsVolume[] getVolumes()
	{
		return _volumes;
	}

	public void setSecurityChecker(FsSecurityChecker securityChecker)
	{
		_securityChecker = securityChecker;
	}

	public void setVolumes(FsVolume[] volumes)
	{
		_volumes = volumes;
		char vid = 'A';
		for (FsVolume volume : volumes)
		{
			_volumeIds.put(volume, "" + vid);
			vid++;
		}
	}

	@Override
    public FsItemEx[] find(FsItemFilter filter)
    {
        List<FsItemEx> listFsItemsEx = new ArrayList<>();
        if(getVolumes() != null && getVolumes().length >0)
        {
            for(FsVolume volume:getVolumes())
            {
                for(FsItem item: volume.listChildren(volume.getRoot()))
                {
                    if(item instanceof IwcmArchivItem)
                    {
                        listFsFiles(item,volume,listFsItemsEx,filter,this);
                    }
                }
            }
        }
        //Logger.debug(this,"listFsItemsEx.size() "+listFsItemsEx.size());
        FsItemEx[] cwd = new FsItemEx[listFsItemsEx.size()];
        cwd = listFsItemsEx.toArray(cwd);
        return cwd;
    }

    private static List<FsItemEx> listFsFiles(FsItem item,FsVolume volume, List<FsItemEx> listFsItemsEx,FsItemFilter filter, FsService fsService)
    {
        for (FsItem file : volume.listChildren(item))
        {
            if (!volume.isFolder(file) && filter.accepts(new FsItemEx(file, fsService)))
            {
                FsItemEx fix = new FsItemEx((IwcmArchivItem)file, fsService); //NOSONAR
                listFsItemsEx.add(fix);
            }
        }

        return listFsItemsEx;
    }
}
