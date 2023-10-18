package sk.iway.iwcm.components.adminlog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import sk.iway.iwcm.Constants;

@Controller
@RequestMapping(path = "/admin/adminlog")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_adminlog')")
public class LoggingController {
    private static String VIEW = "/components/adminlog/logging";
    private static File logDir = new File(System.getProperty("catalina.base"),"logs");

    @GetMapping("/logging")
    public String index(Model model) {
        String logLevel = Constants.getString("logLevel");
        boolean isDebug = logLevel.equalsIgnoreCase("debug");
        model.addAttribute("logLevel", logLevel);
        model.addAttribute("isDebug", isDebug);
        model.addAttribute("logLevels", Constants.getString("logLevels"));

        model.addAttribute("logDirPath", logDir.getAbsolutePath());
        model.addAttribute("files", getFiles());

        return VIEW;
    }

    @PostMapping("/logging")
    public String save(@RequestParam(required = false) boolean isDebug, @RequestParam String logLevels) {
        Constants.setString("logLevel", isDebug ? "debug" : "normal");
        Constants.setString("logLevels", logLevels);

        return "redirect:/admin/adminlog/logging";
    }

    private List<TailFileWrapper> getFiles() {
        File logDir = new File(System.getProperty("catalina.base"),"logs");
        File[] files = logDir.listFiles();

        List<TailFileWrapper> result = new ArrayList<>();

        if (files != null)
        {
            try
            {
                //usortuj to podla abecedy
                Arrays.sort(files, (f1, f2) -> (Long.valueOf(f1.lastModified()-f2.lastModified()).intValue()));
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }

            for (File file : files) {
                result.add(new TailFileWrapper(file));
            }
        }

        return result;
    }
}
