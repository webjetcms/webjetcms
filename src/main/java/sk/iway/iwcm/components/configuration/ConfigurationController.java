package sk.iway.iwcm.components.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/configuration")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuConfig')")
public class ConfigurationController extends DatatableRestControllerV2<ConfDetailsDto, Long> {

    private final ConfigurationService configurationService;

    @Autowired
    public ConfigurationController(ConfigurationService configurationService) {
        super(null);
        this.configurationService = configurationService;
    }

    @Override
    public Page<ConfDetailsDto> getAllItems(Pageable pageable) {
        Identity user = getUser();
        if (null == user) {
            return null;
        }
        return new sk.iway.iwcm.system.datatable.DatatablePageImpl<>(configurationService.getAll(user));
    }

    @Override
    public ConfDetailsDto insertItem(ConfDetailsDto confDetailsDto) {
        try {
            //musime setnut nejake ID
            confDetailsDto.setId(Tools.getNow());
            setForceReload(true);
            return configurationService.save(getUser(), confDetailsDto);
        } catch (Exception e) {
            Logger.error(ConfigurationController.class, e);
        }
        return null;
    }

    @Override
    public ConfDetailsDto editItem(ConfDetailsDto confDetailsDto, long id) {
        try {
            //ID sa nam v beane neposiela, takze setnime na rovnake ako bolo poslane
            confDetailsDto.setId(id);
            setForceReload(true);
            return configurationService.save(getUser(), confDetailsDto);
        } catch (Exception e) {
            Logger.error(ConfigurationController.class, e);
        }
        return null;
    }

    @Override
    public boolean deleteItem(ConfDetailsDto confDetailsDto, long id) {
        configurationService.deleteConfDetails(confDetailsDto.getName());
        return true;
    }

    @Override
    public List<ConfDetailsDto> findItemBy(String propertyName, ConfDetailsDto original)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

        //used for XLS import
        List<ConfDetailsDto> list = configurationService.findConfDetailsBy(propertyName, original, getUser());
        return list;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, ConfDetailsDto> target, Identity currentUser, Errors errors, Long id, ConfDetailsDto confDetailsDto) {
        String configEnabledKeys = Constants.getStringExecuteMacro("configEnabledKeys");
        String[] enabledKeys = Tools.getTokens(configEnabledKeys, ",");
        if(!currentUser.isEnabledItem("conf.show_all_variables")){
            if (!ConfDB.isKeyVisibleToUser(currentUser, enabledKeys, confDetailsDto.getName())) {
                errors.rejectValue("errorField.name", null, Prop.getInstance().getText("user.rights.configuration_rights"));
            }
        }
    }

    @GetMapping("/autocomplete/detail")
    public ConfDetailsDto getAutocompleteDetail(@RequestParam String name) {
        Identity user = getUser();
        if (null == user) {
            return null;
        }
        ConfDetailsDto c = configurationService.getAutocompleteDetail(user, name);
        if (c == null) c = new ConfDetailsDto();

        return c;
    }

    @GetMapping("/autocomplete")
    public List<String> getAutocomplete(@RequestParam String term) {
        Identity user = getUser();
        if (null == user) {
            return null;
        }

        return configurationService.getAutocomplete(user, term);
    }

    @PostMapping("/restart")
    public void restart() {
        Identity user = getUser();
        if (null == user) {
            return;
        }

        InitServlet.restart();
    }
}
