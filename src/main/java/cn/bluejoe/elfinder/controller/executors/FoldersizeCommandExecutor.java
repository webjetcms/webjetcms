package cn.bluejoe.elfinder.controller.executors;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.service.FsService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.elfinder.FolderSizeService;
import sk.iway.iwcm.users.UsersDB;

public class FoldersizeCommandExecutor extends AbstractJsonCommandExecutor {
    @Override
    protected void execute(FsService fs, HttpServletRequest request, ServletContext context, JSONObject json) {
        Identity user = UsersDB.getCurrentUser(request);
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (user == null || !user.isAdmin() || rb == null || !"POST".equals(request.getMethod())) {
            json.put("error", "errPerm");
            return;
        }
        FolderSizeService service = Tools.getSpringBean("folderSizeService", FolderSizeService.class);
        if (service == null) {
            json.put("error", "wjFolderSizeFailed");
            return;
        }
        FolderSizeService.Owner owner = new FolderSizeService.Owner(request.getSession().getId(), user.getUserId(), rb.getDomain());
        String scanId = request.getParameter("scanId");
        try {
            FolderSizeService.Progress progress;
            switch (Tools.getStringValue(request.getParameter("action"), "")) {
                case "start" -> progress = service.start(owner, user, fs, fs.fromHash(request.getParameter("target")));
                case "status" -> progress = service.status(owner, scanId);
                case "cancel" -> {
                    service.cancel(owner, scanId);
                    json.put("state", "cancelled");
                    return;
                }
                default -> {
                    json.put("error", "errCmdParams");
                    return;
                }
            }
            if (progress == null) json.put("error", "errFileNotFound");
            else {
                json.put("scanId", progress.scanId());
                json.put("state", progress.state());
                json.put("bytes", progress.bytes());
                json.put("folders", progress.folders());
            }
        } catch (IOException | IllegalArgumentException | RejectedExecutionException ex) {
            json.put("error", "wjFolderSizeFailed");
        }
    }
}
