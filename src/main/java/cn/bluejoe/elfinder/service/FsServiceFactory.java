package cn.bluejoe.elfinder.service;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

public interface FsServiceFactory
{

	FsService getFileService(HttpServletRequest request, ServletContext servletContext);

}
