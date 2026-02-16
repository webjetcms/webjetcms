package cn.bluejoe.elfinder.controller.executors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.bluejoe.elfinder.controller.executor.AbstractCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;

public class TmbCommandExecutor extends AbstractCommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext) throws Exception
	{
		String target = request.getParameter("target");
		FsItemEx fsi = super.findItem(fsService, target);
		
		int width = fsService.getServiceConfig().getTmbWidth();
		
		String virtualPath = sk.iway.iwcm.system.elfinder.FsService.getVirtualPath(fsi);
		if (virtualPath.startsWith("/")==false) virtualPath = "/"+virtualPath;
		
		//JEEFF: preposleme na thumb server
		response.setStatus(302);
		response.setHeader("Location", "/thumb"+virtualPath+"?w="+width);
		
		/*
		InputStream is = fsi.openInputStream();
		BufferedImage image = ImageIO.read(is);
		int width = fsService.getServiceConfig().getTmbWidth();
		ResampleOp rop = new ResampleOp(DimensionConstrain.createMaxDimension(width, -1));
		rop.setNumberOfThreads(4);
		BufferedImage b = rop.filter(image, null);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(b, "png", baos);
		byte[] bytesOut = baos.toByteArray();
		is.close();

		response.setHeader("Last-Modified", DateUtils.addDays(Calendar.getInstance().getTime(), 2 * 360).toGMTString());
		response.setHeader("Expires", DateUtils.addDays(Calendar.getInstance().getTime(), 2 * 360).toGMTString());

		ImageIO.write(b, "png", response.getOutputStream());
		*/
	}
}
