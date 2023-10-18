package sk.iway.iwcm.filebrowser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuFbrowser')")
public class FbrowserController
{
	/**
	 * Set request data/attributes for File/Dir properties in elfinder
	 * @param request
	 * @param response
	 * @return
	 */
	@GetMapping("/admin/fbrowser/fileprop/")
	public String executeEdit(@RequestParam String dir, Model model, HttpServletRequest request, HttpServletResponse response)
	{
		return FileBrowserService.editFile(dir, model, request);
	}

	/**
	 * Execute rename file/save file attributes on file properties in elfinder
	 * @param editForm
	 * @param request
	 * @param response
	 * @return
	 */
    @PostMapping("/admin/fbrowser/fileprop/")
	public String executeSave(@ModelAttribute("fbrowserEditForm") EditForm editForm, HttpServletRequest request, HttpServletResponse response) {
		return FileBrowserService.saveFile(editForm, request);
	}
}