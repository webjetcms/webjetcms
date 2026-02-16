package cn.bluejoe.elfinder.controller.executors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsItemFilterUtils;
import sk.iway.iwcm.DB;

public class SearchCommandExecutor extends AbstractJsonCommandExecutor
{
    @Override
    public void execute(FsService fsService, HttpServletRequest request,
                        ServletContext servletContext, JSONObject json) throws Exception
    {
        String target = request.getParameter("target");
        boolean recursive = "true".equals(request.getParameter("recursive"));
        FsItemEx fsi = super.findItem(fsService, target);
        String keyword = DB.internationalToEnglish(request.getParameter("q")).toLowerCase();
        json.put(
                "files",
                files2JsonArray(request,
                        FsItemFilterUtils.filterFiles(fsService.find(FsItemFilterUtils.createFileNameKeywordFilter(keyword), fsi, recursive), super.getRequestedFilter(request))
                )
        );
    }
}