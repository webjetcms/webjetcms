package sk.iway.iwcm.update;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.editor.rest.WebPagesListener;
import sk.iway.iwcm.i18n.Prop;

@Controller
@RequestMapping("/admin/update")
@PreAuthorize("@WebjetSecurityService.hasPermission('modUpdate')")
public class UpdateController {

	@GetMapping("/versions")
	@ResponseBody
    public List<VersionBean> getUpdateVersions(HttpServletRequest request) {
        try {
			return UpdateService.getUpdateVersionsData(request);
        }  catch (Exception ex) {
            Logger.error(WebPagesListener.class, ex);
        }
		return (new ArrayList<VersionBean>());
    }

    @GetMapping("/prepareUpdate")
    @ResponseBody
    public void prepareUpdate(@RequestParam(name = "version") String version, HttpServletRequest request, HttpServletResponse response) {
        try {
            UpdateService.prepareUpdate(version, request, response, false);
        }  catch (Exception ex) {
            Logger.error(WebPagesListener.class, ex);

            try {
                flushError(request, response);
            } catch(Exception e) {
                Logger.error(WebPagesListener.class, e);
            }
        }
    }

    @ResponseBody
    @GetMapping("/prepareFileUpdate")
    public void prepareFileUpdate(@RequestParam(name = "version") String version, HttpServletRequest request, HttpServletResponse response) {
        try {
            UpdateService.prepareUpdate(version, request, response, true);
        }  catch (Exception ex) {
            Logger.error(WebPagesListener.class, ex);

            try {
                flushError(request, response);
            } catch(Exception e) {
                Logger.error(WebPagesListener.class, e);
            }
        }
    }

    @GetMapping("/restart")
    public String doRestart(@RequestParam(name = "version") String version, HttpServletRequest request, HttpServletResponse response) {
        try {
            return UpdateService.doRestart(version, request, response);
        }  catch (Exception ex) {
            Logger.error(WebPagesListener.class, ex);
        }

        return null;
    }

    private void flushError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        Prop prop = Prop.getInstance(request);
        out.println("<span style='color: red'>" + prop.getText("update.failed") + "</span>");
        out.flush();
    }
}
