package sk.iway.iwcm.components.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.configuration.model.ConfPrefixDto;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Abstract class for configuration controllers with specified prefix of conf. variables
 */
public abstract class AbstractConfigurationController extends DatatableRestControllerV2<ConfPrefixDto, Long> {

    private final String confPrefix;
    private final ConfDetailsMapper confDetailsMapper;

    protected AbstractConfigurationController(String confPrefix, ConfDetailsMapper confDetailsMapper) {
        super(null);
        this.confPrefix = confPrefix;
        this.confDetailsMapper = confDetailsMapper;
    }

    @Override
    public Page<ConfPrefixDto> getAllItems(Pageable pageable) {
        return new DatatablePageImpl<>(getAllConfByPrefix());
    }

    @Override
    public ConfPrefixDto editItem(ConfPrefixDto entity, long id) {
        try {
            //The ID is not sent to us in the bean, so we count on the same as it was sent to us
            entity.setId(id);
            setForceReload(true);
            return save(entity);
        } catch (Exception e) {
            Logger.error(ConfigurationController.class, e);
        }
        return null;
    }

    private final List<ConfPrefixDto> getAllConfByPrefix() {
        List<ConfDetails> configurationData = new ArrayList<>();

        //All existing confs
        for(ConfDetails conf : sk.iway.iwcm.Constants.getAllValues())
            if(conf.getName().startsWith(confPrefix)) configurationData.add(conf);

        //Convert it
        List<ConfPrefixDto> configurationDataDtos = confDetailsMapper.entityListToPrefixDtoList(configurationData);

        //Get all changed data from DB -> update Dto's
        for(ConfDetails changedConf : ConfDB.getConfig(confPrefix)) {
            for(ConfPrefixDto baseConf : configurationDataDtos)
                if(baseConf.getName().equals(changedConf.getName())) {
                    //Base value is now old value
                    baseConf.setOldValue( baseConf.getValue() );
                    //Set new value
                    baseConf.setValue(changedConf.getValue());
                    //Set date changed
                    baseConf.setDateChanged(changedConf.getDateChanged());
                    break;
                }
        }

        return configurationDataDtos;
    }

    private final ConfPrefixDto save(ConfPrefixDto entity) throws Exception {
        //First is needed BE verify, that conf name wasn't changed (must be in array of all conf's by prefix)
        boolean isNameValid = false;
        for(ConfPrefixDto conf : getAllConfByPrefix())
            if(conf.getName().equals(entity.getName())) {
                isNameValid = true;
                break;
            }

        if(!isNameValid) {
            throwError(getProp().getText("config.conf_name_change_err"));
            return null;
        }

        if (entity.isEncrypt()) {
            Password password = new Password();
            entity.setValue("encrypted:" + password.encrypt(entity.getValue()));
        }

        //Set change
        ConfDB.setName(entity.getName(), entity.getValue());
        //zapis zmenu do historie
        ConfDB.setNamePrepared(entity.getName(), entity.getValue(), null); //Date prepared always null

        ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-" + entity.getName());

        //musime vratit aktualne nastavenu hodnotu, pretoze sa mohla dat sifrovat, alebo je v buducnosti
        ConfDetails actual = ConfDB.getVariable(entity.getName());
        ConfPrefixDto actualDto = confDetailsMapper.entityToPrefixDto(actual);
        if (entity.getId() != null && entity.getId() > 0) actualDto.setId(entity.getId());
        else actualDto.setId(Tools.getNow());

        setOldValueDescription(actualDto);

        return actualDto;
    }

    private final void setOldValueDescription(ConfPrefixDto conf) {
        List<ConfDetails> constantsData = Constants.getAllValues();
        for (ConfDetails c : constantsData) {
            if (c.getName().equals(conf.getName())) {
                conf.setOldValue(c.getValue());
                conf.setDescription(c.getDescription());
            }
        }
    }

    @Override
    public ConfPrefixDto insertItem(ConfPrefixDto entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
        return null;
    }

    @Override
    public boolean deleteItem(ConfPrefixDto entity, long id) {
        throwError(getProp().getText("config.not_permitted_action_err"));
        return false;
    }

    @Override
    public void beforeDuplicate(ConfPrefixDto entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
    }
}
