package cn.bluejoe.elfinder.controller.executors;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.system.elfinder.IwcmDocGroupFsVolume;

public class DuplicateCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String[] targets = request.getParameterValues("targets[]");

		List<FsItemEx> added = new ArrayList<FsItemEx>();

		for (String target : targets)
		{
			FsItemEx fsi = super.findItem(fsService, target);
			String name = fsi.getName();
			String baseName = FilenameUtils.getBaseName(name);
			String extension = FilenameUtils.getExtension(name);
			FsItemEx newFile = null;

			//JEEFF: upravene pre podporu nasho DocGroup
			if (fsi.getVolumeId().equals(IwcmDocGroupFsVolume.VOLUME_ID))
			{
				((IwcmDocGroupFsVolume)fsi.getVolume()).createAndCopy(fsi, fsi.getParent(), false);
				//toto realne neviem vykonstruovat newFile = new FsItemEx(fsi.getParent(), fsi.getPath());
				//musim nieco poslat, aby sa vyvolal added event a nasledne reload
				newFile = fsi;
				RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
			}
			else
			{

				int i = 1;
				baseName = baseName.replaceAll("-\\d+$", "");

				while (true)
				{
					String newName = String.format("%s-%d%s", baseName, i, (extension == null || extension.isEmpty() ? ""
							: "." + extension));
					newFile = new FsItemEx(fsi.getParent(), newName);
					if (!newFile.exists())
					{
						break;
					}
					i++;
				}

				createAndCopy(fsi, newFile);
			}
			added.add(newFile);
		}

		json.put("added", files2JsonArray(request, added));
	}

}
