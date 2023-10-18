package cn.bluejoe.elfinder.controller.executors;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UsersDB;

public class LockfileCommandExecutor extends AbstractJsonCommandExecutor
{

	private static final long EVICT_TIME_SEC = 60;

	private static final Map<String,LockedFileInfoHolder> LOCKED_FILES = new ConcurrentHashMap<String,LockedFileInfoHolder>();//LinkedList<LockedFileInfoHolder>();


	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String result = "ok";
		String target = request.getParameter("target");

		FsItemEx fsi = super.findItem(fsService, target);

		// skontrolujem prava aby neprihlaseny uzivatel nemohol hlupo zablokovat cely filesystem

		lockFile(fsi);

		updateLockedFiles();

		json.put("result", result);
	}

	public static class LockedFileInfoHolder
	{
		protected String file;
		protected Map<Integer,Long> userIds = new ConcurrentHashMap<Integer,Long>();;

		public String getFile()
		{
			return file;
		}



	}

	private static void updateLockedFiles()
	{
		for (LockedFileInfoHolder l : LOCKED_FILES.values())
		{
			if (!l.userIds.isEmpty())
			{
				for (Integer i : l.userIds.keySet())
				{
					if ((l.userIds.get(i)+(EVICT_TIME_SEC*1000)-Tools.getNow())<0)
					{
						l.userIds.remove(i);

					}
				}
				if (l.userIds.isEmpty()) LOCKED_FILES.remove(l.file);
			}
		}
	}

	public static boolean checkAndLock(FsItemEx fsi)
	{
		updateLockedFiles();
		try
		{
			return lockFile(fsi);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return false;
		}

	}
	public static LockedFileInfoHolder check(FsItemEx fsi) throws Exception
	{
		updateLockedFiles();
		LockedFileInfoHolder lockedFile = LOCKED_FILES.get(fsi.getPath());
		return lockedFile;
	}

	public static void unlock(FsItemEx fsi) throws IOException
	{
		updateLockedFiles();
		FsItemEx parent = fsi.getParent();
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && parent!=null && UsersDB.isFolderWritable(user.getWritableFolders(), parent.getPath()))
		{
			// uzivatel mam pravo na zapis
			LockedFileInfoHolder lockedFile = LOCKED_FILES.get(fsi.getPath());
			if (lockedFile!=null && lockedFile.userIds.containsKey(user.getUserId()))
			{
				lockedFile.userIds.remove(user.getUserId());
				//ak ho uz nikto needituje, odstranim holder
				if (lockedFile.userIds.isEmpty())
					LOCKED_FILES.remove(fsi.getPath());
			}
		}
	}

	private static boolean lockFile(FsItemEx fsi) throws Exception
	{
		FsItemEx parent = fsi.getParent();
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user!=null && parent!=null && UsersDB.isFolderWritable(user.getWritableFolders(), parent.getPath()))
		{
			// uzivatel mam pravo na zapis
			LockedFileInfoHolder lockedFile = LOCKED_FILES.get(fsi.getPath());
			if (lockedFile==null)
			{
				lockedFile = new LockedFileInfoHolder();
				lockedFile.file = fsi.getPath();

			}
			//skontrolujem ci ho naozaj on edituje

			lockedFile.userIds.put(user.getUserId(), Tools.getNow());
			LOCKED_FILES.put(fsi.getPath(), lockedFile);
			Logger.debug(LockfileCommandExecutor.class, "Updating file lock for file:"+fsi.getPath()+" userId:"+user.getUserId());

		}
		else
		{
			return false;
		}
		return true;
	}

}
