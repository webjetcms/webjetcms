package sk.iway.iwcm.components.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SelectionFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.cluster.ClusterDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfigurationService {

    private final ConfDetailsMapper confDetailsMapper;

    @Autowired
    public ConfigurationService(ConfDetailsMapper confDetailsMapper) {
        this.confDetailsMapper = confDetailsMapper;
    }

    public List<ConfDetailsDto> getAll(Identity user) {
        List<ConfDetails> configurationData = ConfDB.filterConfDetailsByPerms(user, ConfDB.getConfig());
        configurationData = filterStatSessionsCluster(configurationData);
        List<ConfDetailsDto> configurationDataDtos = confDetailsMapper.entityListToDtoList(configurationData);

        List<ConfDetails> constantsData = Constants.getAllValues();
        Map<String, String> constantsMapWithValue = new HashMap<>();
        Map<String, String> constantsMapWIthDescription = new HashMap<>();

        for (ConfDetails singleConstantsData : constantsData) {
            constantsMapWithValue.put(singleConstantsData.getName(), singleConstantsData.getValue());
            constantsMapWIthDescription.put(singleConstantsData.getName(), singleConstantsData.getDescription());
        }

        for (ConfDetailsDto constantsDto : configurationDataDtos) {
            constantsDto.setOldValue(constantsMapWithValue.get(constantsDto.getName()));
            constantsDto.setDescription(constantsMapWIthDescription.get(constantsDto.getName()));
        }
        return configurationDataDtos;
    }

    /**
     * Odflitruje v zozname hodnoty statDistinctUser- a statSessions- co su len hodnoty potrebne pre vypocet navstevnosti v clustri
     * standardne ich nie je potrebne vidiet
     * @param all
     * @return
     */
    private List<ConfDetails> filterStatSessionsCluster(List<ConfDetails> all) {
        List<ConfDetails> filtered = new ArrayList<>();
        for (ConfDetails conf : all) {
            if (conf.getName().startsWith("statDistinctUsers-") || conf.getName().startsWith("statSessions-")) continue;

            filtered.add(conf);
        }
        return filtered;
    }

    ConfDetailsDto save(Identity currentUser, ConfDetailsDto confDetailsDto) throws Exception {
        if (null == currentUser) {
            return null;
        }

        if (confDetailsDto.isEncrypt()) {
            Password password = new Password();
            confDetailsDto.setValue("encrypted:" + password.encrypt(confDetailsDto.getValue()));
        }

        if (null == confDetailsDto.getDatePrepared()) {
            ConfDB.setName(confDetailsDto.getName(), confDetailsDto.getValue());
            //zapis zmenu do historie
            ConfDB.setNamePrepared(confDetailsDto.getName(), confDetailsDto.getValue(), null);
        } else {
            ConfDB.setNamePrepared(confDetailsDto.getName(), confDetailsDto.getValue(), confDetailsDto.getDatePrepared());
        }

        if (ConfDB.isOnlyLocalConfig(confDetailsDto.getName())==false) ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-" + confDetailsDto.getName());

        //musime vratit aktualne nastavenu hodnotu, pretoze sa mohla dat sifrovat, alebo je v buducnosti
        ConfDetails actual = ConfDB.getVariable(confDetailsDto.getName());
        ConfDetailsDto actualDto = confDetailsMapper.entityToDto(actual);
        if (confDetailsDto.getId()!=null && confDetailsDto.getId()>0) actualDto.setId(confDetailsDto.getId());
        else actualDto.setId(Tools.getNow());

        setOldValueDescription(actualDto);

        return actualDto;
    }

    void deleteConfDetails(String name) {
        ConfDB.deleteName(name);
    }

    /**
     * Vrati ConfDetails pre zadane name pre autocomplete
     * @param name
     * @return
     */
    public ConfDetailsDto getAutocompleteDetail(Identity user, String name) {

        List<ConfDetailsDto> all = getAll(user);
        for (ConfDetailsDto c : all) {
            if (c.getName().equals(name)) {
                return c;
            }
        }

        if (ConfDB.isKeyVisibleToUser(user, name)) {
            //nenaslo sa, takze este nie je nastavena, posli hodnotu
            ConfDetailsDto c = new ConfDetailsDto();
            c.setName(name);
            c.setValue(Constants.getString(name));

            //old value sa neda inak ziskat ako takto
            c.setOldValue(getOldValue(name));
            c.setDescription(Constants.getDescription(name));
            return c;
        }

        return null;
    }

    private String getOldValue(String name) {
        List<ConfDetails> constantsData = Constants.getAllValues();
        for (ConfDetails c : constantsData) {
            if (c.getName().equals(name)) return c.getValue();
        }
        return "";
    }

    /**
     * Doplni do DTO objektu oroginalnu hodnotu z Constants triedy a description
     * @param conf
     */
    private void setOldValueDescription(ConfDetailsDto conf) {
        List<ConfDetails> constantsData = Constants.getAllValues();
        for (ConfDetails c : constantsData) {
            if (c.getName().equals(conf.getName())) {
                conf.setOldValue(c.getValue());
                conf.setDescription(c.getDescription());
            }
        }
    }

    public List<String> getAutocomplete(Identity user, String term) {
        final String termLC = term.toLowerCase();
        List<String> allKeys = Constants.getAllKeys();
        List<String> keysToSort = Tools.filter(allKeys, new SelectionFilter <String>(){

            public boolean fullfilsConditions(String key){
                return key != null && key.toLowerCase().contains(termLC);
            }
        });
        Collections.sort(keysToSort, new Comparator<String>(){
            public int compare(String key1, String key2){
                key1 = key1.toLowerCase();
                key2 = key2.toLowerCase();
                if (key1.startsWith(termLC) && key2.startsWith(termLC)) return key1.compareTo(key2);
                if (key1.startsWith(termLC)) return -1;
                if (key2.startsWith(termLC)) return 1;
                return key1.compareTo(key2);
            }
        });

        return ConfDB.filterByPerms(user, keysToSort);
    }

    public List<ConfDetailsDto> findConfDetailsBy(String propertyName, ConfDetailsDto original, Identity user) {

        List<ConfDetailsDto> all = getAll(user);
        List<ConfDetailsDto> result = new ArrayList<>();

        //iterate all and filted by propertyName
        for (ConfDetailsDto c : all) {
            if (propertyName.equals("id") && c.getName().equals(original.getName())) {
                result.add(c);
            } else if (propertyName.equals("name") && c.getName().equals(original.getName())) {
                result.add(c);
            } else if (propertyName.equals("value") && c.getValue().equals(original.getValue())) {
                result.add(c);
            } else if (propertyName.equals("description") && c.getDescription().equals(original.getDescription())) {
                result.add(c);
            }
        }

        return result;
    }
}
