package cn.bluejoe.elfinder.controller.executor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import cn.bluejoe.elfinder.controller.executors.ArchiveCommandExecutor;
import cn.bluejoe.elfinder.controller.executors.ExtractCommandExecutor;
import cn.bluejoe.elfinder.service.FsItemFilter;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsItemFilterUtils;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import sk.iway.iwcm.components.gallery.GalleryService;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.elfinder.FolderPropertiesService;

public abstract class AbstractCommandExecutor implements CommandExecutor
{
	protected void addChildren(Map<String, FsItemEx> map, FsItemEx fsi) throws IOException
	{
		for (FsItemEx f : fsi.listChildren()) {
			map.put(f.getHash(), f);
		}
	}

    protected FsItemFilter getRequestedFilter(HttpServletRequest request)
    {
        String[] onlyMimes = request.getParameterValues("mimes[]");
        if (onlyMimes == null)
            return FsItemFilterUtils.FILTER_ALL;

        return FsItemFilterUtils.createMimeFilter(onlyMimes);
    }

	protected void addSubfolders(Map<String, FsItemEx> map, FsItemEx fsi) throws IOException
	{
		for (FsItemEx f : fsi.listChildren())
		{
			if (f.isFolder()) {
				map.put(f.getHash(), f);
				//JEEFF addSubfolders(map, f);
			}
		}
	}

	/**
	 * Classic createAndCopy BUT set request is used to swap FOLDER PROPERTIES
	 * 
	 * @param src
	 * @param dst
	 * @param request
	 * @throws IOException
	 */
	protected void createAndCopy(FsItemEx src, FsItemEx dst, HttpServletRequest request) throws IOException
	{
		if (src.isFolder())
		{
			createAndCopyFolder(src, dst, request);
		}
		else
		{
			createAndCopyFile(src, dst);
		}
	}

	protected void createAndCopy(FsItemEx src, FsItemEx dst) throws IOException
	{
		createAndCopy(src, dst, null);
	}

	protected void createAndCopyFile(FsItemEx src, FsItemEx dst) throws IOException
	{
		dst.createFile();
		InputStream is = src.openInputStream();
		OutputStream os = dst.openOutputStream();
		IOUtils.copy(is, os);
		is.close();
		os.close();

		GalleryService.createOrUpdateGalleryEntity(src, dst);
	}

	protected void createAndCopyFolder(FsItemEx src, FsItemEx dst) throws IOException
	{
		createAndCopyFolder(src, dst, null);
	}

	/**
	 * Classic createAndCopyFolder BUT request is used to swap FOLDER PROPERTIES
	 * 
	 * @param src
	 * @param dst
	 * @param request
	 * @throws IOException
	 */
	protected void createAndCopyFolder(FsItemEx src, FsItemEx dst, HttpServletRequest request) throws IOException
	{
		dst.createFolder();

		if(request != null) {
			FolderPropertiesService.copyFolderProperties(src.getPath(), dst.getPath(), request);
		}

		for (FsItemEx c : src.listChildren())
		{
			if (c.isFolder())
			{
				createAndCopyFolder(c, new FsItemEx(dst, c.getName()), request);
			}
			else
			{
				createAndCopyFile(c, new FsItemEx(dst, c.getName()));
			}
		}
	}

	@Override
	public void execute(CommandExecutionContext ctx) throws Exception
	{
		FsService fileService = ctx.getFsServiceFactory().getFileService(ctx.getRequest(), ctx.getServletContext());
		execute(fileService, ctx.getRequest(), ctx.getResponse(), ctx.getServletContext());
	}

	public abstract void execute(FsService fsService, HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext) throws Exception;

	protected Object[] files2JsonArray(HttpServletRequest request, Collection<FsItemEx> list) throws IOException
	{
		List<Map<String, Object>> los = new ArrayList<Map<String, Object>>();
		for (FsItemEx fi : list)
		{
			los.add(getFsItemInfo(request, fi));
		}

		return los.toArray();
	}

	protected FsItemEx findCwd(FsService fsService, String target) throws IOException
	{
		//current selected directory
		FsItemEx cwd = null;
		if (target != null)
		{
			cwd = findItem(fsService, target);
		}

		if (cwd == null)
			cwd = new FsItemEx(fsService.getVolumes()[0].getRoot(), fsService);

		return cwd;
	}

	protected FsItemEx findItem(FsService fsService, String hash) throws IOException
	{
		return FsServiceUtils.findItem(fsService, hash);
	}

	protected Map<String, Object> getFsItemInfo(HttpServletRequest request, FsItemEx fsi) throws IOException
	{
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("hash", fsi.getHash());
		info.put("mime", fsi.getMimeType());
		info.put("ts", fsi.getLastModified());
		info.put("size", fsi.getSize());
		info.put("read", fsi.isReadable(fsi) ? 1 : 0);
		info.put("write", fsi.isWritable(fsi) ? 1 : 0);
		info.put("locked", fsi.isLocked(fsi) ? 1 : 0);

		if (fsi.isRoot())
		{
			info.put("name", fsi.getVolumnName());
			info.put("volumeid", fsi.getVolumeId());
		}
		else
		{
			info.put("name", fsi.getName());
			info.put("phash", fsi.getParent().getHash());
		}

		if (fsi.isFolder())
		{
			info.put("dirs", fsi.hasChildFolder() ? 1 : 0);
		}
		
		//jeeff - webjet - pridane URL pre integraciu s editorom
		info.put("url", fsi.getPath());
		String virtualPath = sk.iway.iwcm.system.elfinder.FsService.getVirtualPath(fsi);
		info.put("virtualPath", virtualPath);

		//jeeff - webjet URL pre thumb servlet
		if (fsi.getMimeType().startsWith("image"))
		{
			info.put("tmb", virtualPath+"?w="+fsi.getService().getServiceConfig().getTmbWidth()+"&v="+fsi.getLastModified());
		}
		
		
		info.put("sort_priority", sk.iway.iwcm.system.elfinder.FsService.getSortPriority(fsi));

		return info;
	}

	protected String getMimeDisposition(String mime)
	{
		String[] parts = mime.split("/");
		String disp = ("image".equals(parts[0]) || "text".equals(parts[0]) ? "inline" : "attachments");
		return disp;
	}

	protected Map<String, Object> getOptions(HttpServletRequest request, FsItemEx cwd)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("path", cwd.getName());
		map.put("disabled", new String[0]);
		map.put("separator", "/");
		map.put("copyOverwrite", 1);
		map.put("archivers", getArchivers());
		if (ContextFilter.isRunning(request)) map.put("tmbUrl", request.getContextPath() + "/thumb");
		else map.put("tmbUrl", "/thumb");

		return map;
	}
	
	protected Map<String, List<String>> getArchivers()
	{
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		result.put("create", ArchiveCommandExecutor.getAllowedTypes());
		result.put("extract", ExtractCommandExecutor.getAllowedTypes());
		return result;
	}
}