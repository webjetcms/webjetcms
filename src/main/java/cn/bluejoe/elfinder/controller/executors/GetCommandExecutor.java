package cn.bluejoe.elfinder.controller.executors;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.controller.executors.LockfileCommandExecutor.LockedFileInfoHolder;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.users.UsersDB;

public class GetCommandExecutor extends AbstractJsonCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String target = request.getParameter("target");

		FsItemEx fsi = super.findItem(fsService, target);
		LockedFileInfoHolder lockedFileHolder = LockfileCommandExecutor.check(fsi);
		if (lockedFileHolder!=null)
		{
			JSONArray array = new JSONArray();
			for (Integer i : lockedFileHolder.userIds.keySet())
			{
				if (i.equals(SetCharacterEncodingFilter.getCurrentRequestBean().getUserId())) continue;
				array.put(UsersDB.getUser(i).getFullName());
			}
			//json.put("alert", "Fuj-to, baci-baci, editovat subor, ktory upravuje "+UsersDB.getUser(lockedFileHolder.getUserId()).getFullName()+".");
			json.put("editedBy", array);

			//return;

		}
		LockfileCommandExecutor.checkAndLock(fsi);

		String encoding = getEncoding(fsi, null);
		InputStream is = fsi.openInputStream();
		String content = IOUtils.toString(is, encoding);

		is.close();
		json.put("content", content);
	}

	public static String getEncoding(FsItemEx fsi, String content) {
		Charset readFileCharset = Charset.forName(Constants.getString("defaultEncoding"));//default

		IwcmInputStream fis = null;
		BufferedInputStream bis = null;
		try {

			IwcmFile file = new IwcmFile(PathFilter.getRealPath(fsi.getPath()));
			//json subory vzdy citaj/ukladaj ako utf-8
			if (file.getName().endsWith(".json")) return "utf-8";

			if (file.getName().endsWith(".jsp") && content != null) {
				//pre JSP obsahujuce pageEncoding="utf-8"
				if (content.contains("pageEncoding=\"utf-8\"") || content.contains("pageEncoding='utf-8'")) return "utf-8";
				if (content.contains("pageEncoding=\"windows-1250\"") || content.contains("pageEncoding='windows-1250'")) return "windows-1250";
			}

			if (content != null && content.contains("#encoding=utf-8")) return "utf-8";
			else if (content!=null && content.contains("#encoding=windows-1250")) return "windows-1250";

			if (file.exists() && file.canRead()) {

				Charset[] testedCharsets;
				//zoznam charsetov na testovanie, ked mame content (=zapisujeme) tak skus preverit windows-1250 a az potom utf-8, pri citani preferuj utf-8
				//ak tam bolo len lsctz tak to nepadlo pri windows-1250 aj ked sa jednalo o utf-8 subor
				if (content != null) testedCharsets = new Charset[]{Charset.forName("windows-1250"),StandardCharsets.UTF_8};
				else testedCharsets = new Charset[]{StandardCharsets.UTF_8};

				for (Charset charset:testedCharsets)
				{
					fis = new IwcmInputStream(file);
					bis = new BufferedInputStream(fis); //fsi.openInputStream());

					CharsetDecoder decoder = charset.newDecoder();
					decoder.reset();
					decoder.onMalformedInput(CodingErrorAction.REPORT);
					byte[] buffer = new byte[512];
					boolean identified = false;
					if ((bis.read(buffer) != -1))
					{
						String text = new String(buffer);
						if (text.contains("#encoding=utf-8")) {
							readFileCharset = StandardCharsets.UTF_8;
							break;
						}
						else if (text.contains("#encoding=windows-1250")) {
							readFileCharset = Charset.forName("windows-1250");
							break;
						}

						identified = identify(buffer, decoder);
					}
					while ((bis.read(buffer) != -1) && (identified))
					{
						identified = identify(buffer, decoder);
					}

					fis.close();
					fis = null;
					bis.close();
					bis = null;

					if (identified)
					{
						readFileCharset = charset;
						break;
					}
				}
			}

		} catch (Exception ex)  {

		} finally {
			if (bis != null) try { bis.close(); } catch (Exception e) {}
			if (fis != null) try { fis.close(); } catch (Exception e) {}
		}

		return readFileCharset.toString();
	}

	private static boolean identify(byte[] bytes, CharsetDecoder decoder) {
		try
		{
			decoder.decode(ByteBuffer.wrap(bytes));
		}
		catch (CharacterCodingException e) {
			//sk.iway.iwcm.Logger.error(e);
			return false;
		}
		return true;
	}
}
