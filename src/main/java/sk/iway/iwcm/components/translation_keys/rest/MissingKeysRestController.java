package sk.iway.iwcm.components.translation_keys.rest;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.translation_keys.jpa.MissingKeysDto;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping(value = "/admin/v9/settings/missing-keys")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('edit_text')")
public class MissingKeysRestController extends DatatableRestControllerV2<MissingKeysDto, Long> {

    private TranslationKeyService translationKeyService;

    @Autowired
    public MissingKeysRestController(TranslationKeyService translationKeyService) {
        super(null);
        this.translationKeyService = translationKeyService;
    }

    @Override
    public Page<MissingKeysDto> getAllItems(Pageable pageable) {

        Prop prop = Prop.getInstance();

        //Get all missing keys (under all languages)
        DatatablePageImpl<MissingKeysDto> page = new DatatablePageImpl<>(new ArrayList<>(Prop.getMissingTexts()));

        long id = 1;
        for (MissingKeysDto dto : page.getContent()) {
            dto.setId(id++);
        }

        for(String lng : ConstantsV9.getArray("languages"))
            page.addOption("language", prop.getText("language." + lng.toLowerCase()), lng, false);

        return page;
    }

    @Override
	public MissingKeysDto editItem(MissingKeysDto entity, long logId) {
        translationKeyService.saveTranslation(entity.getKey(), entity.getTranslation(), entity.getLanguage());
        setForceReload(true);
        return entity;
	}

	@Override
    public boolean beforeDelete(MissingKeysDto entity) {
        return false;
    }

    @Override
    public boolean processAction(MissingKeysDto entity, String action) {
        if ("deleteAllCacheObjects".equals(action)) {
            Prop.clearMissingTexts();
            return true;
        }
        return false;
    }


}
