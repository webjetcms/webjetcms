package sk.iway.iwcm.components.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SelectionFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.helpers.DataSanitizer;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.ConfigurationModulePath;
import sk.iway.iwcm.system.cluster.ClusterDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ConfigurationService {

    public static final String VIEW_CHANGED = "changed";
    public static final String VIEW_CUSTOM = "custom";
    public static final String VIEW_ALL = "all";
    public static final String VIEW_MODULE = "module";

    private static final String ENCRYPTED_VALUE_PREFIX = "encrypted:";

    private final ConfDetailsMapper confDetailsMapper;

    @Autowired
    public ConfigurationService(ConfDetailsMapper confDetailsMapper) {
        this.confDetailsMapper = confDetailsMapper;
    }

    /**
     * Returns configuration values stored in the database.
     *
     * @param user current user
     * @return changed configuration values visible to the user
     */
    public List<ConfDetailsDto> getAll(Identity user) {
        return getAll(user, VIEW_CHANGED, null);
    }

    /**
     * Returns the requested configuration catalog view.
     *
     * @param user current user
     * @param view one of {@link #VIEW_CHANGED}, {@link #VIEW_CUSTOM}, {@link #VIEW_ALL}, or {@link #VIEW_MODULE}
     * @param modulePath module branch used for the module view
     * @return configuration values visible to the user
     */
    public List<ConfDetailsDto> getAll(Identity user, String view, String modulePath) {
        ConfigurationCatalog catalog = getConfigurationCatalog(user);
        List<ConfDetails> selected;

        if (VIEW_ALL.equals(view)) {
            selected = catalog.getAll();
        } else if (VIEW_CUSTOM.equals(view)) {
            selected = getCustom(catalog);
        } else if (VIEW_MODULE.equals(view)) {
            selected = getByModule(catalog, modulePath);
        } else {
            selected = catalog.getChanged();
        }

        return prepareDtos(selected, catalog);
    }

    /**
     * Returns all module paths used by configuration values visible to the user.
     *
     * @param user current user
     * @return sorted full module paths
     */
    public List<String> getVisibleModulePaths(Identity user) {
        if (user == null) return List.of();

        List<ConfDetails> catalogued = filterStatSessionsCluster(
            getVisibleConfiguration(user, Constants.getAllValues())
        );
        Set<String> paths = new TreeSet<>();

        for (ConfDetails conf : catalogued) {
            paths.addAll(ConfigurationModulePath.parse(conf.getModules()));
        }

        return new ArrayList<>(paths);
    }

    public ConfDetailsDto getOne(Identity user, long id) {
        if (id < 1) {
            return new ConfDetailsDto();
        }

        ConfigurationCatalog catalog = getConfigurationCatalog(user);
        for (ConfDetailsDto configurationDto : prepareDtos(catalog.getAll(), catalog)) {
            if (configurationDto.getId() != null && configurationDto.getId().longValue() == id) {
                return configurationDto;
            }
        }

        return null;
    }

    private List<ConfDetails> getByModule(ConfigurationCatalog catalog, String modulePath) {
        if (ConfigurationModulePath.isValidPath(modulePath) == false) return List.of();

        List<ConfDetails> selected = new ArrayList<>();
        Set<String> addedNames = new LinkedHashSet<>();
        for (ConfDetails conf : catalog.getCatalogued()) {
            if (ConfigurationModulePath.isInBranch(conf.getModules(), modulePath) && addedNames.add(conf.getName())) {
                selected.add(conf);
            }
        }
        return selected;
    }

    private List<ConfDetails> getCustom(ConfigurationCatalog catalog) {
        String installName = Constants.getInstallName();
        boolean hasInstallName = Tools.isNotEmpty(installName);
        List<ConfDetails> selected = new ArrayList<>();
        Set<String> addedNames = new LinkedHashSet<>();

        for (ConfDetails conf : catalog.getAll()) {
            String name = conf.getName();
            boolean databaseOnly = catalog.getDatabaseNames().contains(name)
                && catalog.getDefaultValues().containsKey(name) == false;
            boolean installPrefixed = hasInstallName && name.startsWith(installName);
            if ((databaseOnly || installPrefixed) && addedNames.add(name)) selected.add(conf);
        }

        return selected;
    }

    private ConfigurationCatalog getConfigurationCatalog(Identity user) {
        if (user == null) return ConfigurationCatalog.empty();

        List<ConfDetails> constantsData = getVisibleConfiguration(user, Constants.getAllValues());
        List<ConfDetails> databaseData = getVisibleConfiguration(user, ConfDB.getConfig());

        Map<String, ConfDetails> defaultByName = new LinkedHashMap<>();
        Map<String, String> defaultValues = new HashMap<>();
        for (ConfDetails defaultConf : constantsData) {
            if (defaultConf == null || Tools.isEmpty(defaultConf.getName())) continue;

            ConfDetails copy = copyDefault(defaultConf);
            defaultByName.put(copy.getName(), copy);
            defaultValues.put(copy.getName(), copy.getValue());
        }

        Map<String, ConfDetails> allByName = new LinkedHashMap<>(defaultByName);
        Set<String> databaseNames = new LinkedHashSet<>();
        List<ConfDetails> changed = new ArrayList<>();
        for (ConfDetails databaseConf : databaseData) {
            if (databaseConf == null || Tools.isEmpty(databaseConf.getName())) continue;

            String name = databaseConf.getName();
            ConfDetails merged = mergeDatabaseValue(defaultByName.get(name), databaseConf);
            allByName.put(name, merged);
            databaseNames.add(name);
            changed.add(merged);
        }

        List<ConfDetails> catalogued = new ArrayList<>();
        for (String name : defaultByName.keySet()) {
            ConfDetails conf = allByName.get(name);
            if (conf != null) catalogued.add(conf);
        }

        return new ConfigurationCatalog(
            filterStatSessionsCluster(changed),
            filterStatSessionsCluster(new ArrayList<>(allByName.values())),
            filterStatSessionsCluster(catalogued),
            databaseNames,
            defaultValues
        );
    }

    private List<ConfDetails> getVisibleConfiguration(Identity user, List<ConfDetails> configuration) {
        if (configuration == null || configuration.isEmpty()) return List.of();

        List<ConfDetails> visible = ConfDB.filterConfDetailsByPerms(user, configuration);
        return visible == null ? List.of() : visible;
    }

    private ConfDetails copyDefault(ConfDetails source) {
        ConfDetails copy = new ConfDetails(source.getName(), source.getValue());
        copy.setDescription(source.getDescription());
        copy.setModules(source.getModules());
        copy.setDateChanged(null);
        return copy;
    }

    private ConfDetails mergeDatabaseValue(ConfDetails defaultConf, ConfDetails databaseConf) {
        ConfDetails merged = new ConfDetails(databaseConf.getName(), databaseConf.getValue(), databaseConf.getDateChanged());
        if (defaultConf != null) {
            merged.setDescription(defaultConf.getDescription());
            merged.setModules(defaultConf.getModules());
        }
        return merged;
    }

    private List<ConfDetailsDto> prepareDtos(List<ConfDetails> configuration, ConfigurationCatalog catalog) {
        List<ConfDetailsDto> configurationDtos = confDetailsMapper.entityListToDtoList(configuration);

        for (ConfDetailsDto configurationDto : configurationDtos) {
            configurationDto.setOldValue(catalog.getDefaultValues().get(configurationDto.getName()));
            configurationDto.setDatabaseValuePresent(catalog.getDatabaseNames().contains(configurationDto.getName()));
            setDisplayValue(configurationDto);
        }

        return configurationDtos;
    }

    private void setDisplayValue(ConfDetailsDto configurationDto) {
        String storedOrDefaultValue = Objects.toString(configurationDto.getValue(), "");
        String currentValue = Objects.toString(Constants.getString(configurationDto.getName()), "");
        String comparableStoredOrDefaultValue = Objects.toString(
            ConfDB.normalizeRuntimeValue(configurationDto.getName(), storedOrDefaultValue), ""
        );

        boolean runtimeValueDifferent = Objects.equals(currentValue, comparableStoredOrDefaultValue) == false;
        configurationDto.setRuntimeValueDifferent(runtimeValueDifferent);

        if (runtimeValueDifferent == false) {
            configurationDto.setDisplayValue(storedOrDefaultValue);
        } else {
            String displayedCurrentValue = DataSanitizer.sanitizeIfNameIsSensitive(configurationDto.getName(), currentValue);
            if (storedOrDefaultValue.startsWith(ENCRYPTED_VALUE_PREFIX)) {
                displayedCurrentValue = "********";
            }
            configurationDto.setDisplayValue(displayedCurrentValue);
        }
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
            if (conf == null || Tools.isEmpty(conf.getName())) continue;
            if (conf.getName().startsWith("statDistinctUsers-") || conf.getName().startsWith("statSessions-")) continue;

            filtered.add(conf);
        }
        return filtered;
    }

    ConfDetailsDto save(Identity currentUser, ConfDetailsDto confDetailsDto) throws Exception {
        if (null == currentUser) {
            return null;
        }

        if (confDetailsDto.isTemporary()) {
            ConfDB.setRuntimeValue(confDetailsDto.getName(), confDetailsDto.getValue());
            return confDetailsDto;
        }

        if (confDetailsDto.isEncrypt()) {
            Password password = new Password();
            confDetailsDto.setValue(ENCRYPTED_VALUE_PREFIX + password.encrypt(confDetailsDto.getValue()));
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
        actualDto.setDatabaseValuePresent(actual != null);

        return actualDto;
    }

    /**
     * Removes a stored override so the default configuration value becomes active again.
     *
     * @param user current user
     * @param name configuration name
     * @return true when a visible database override was removed
     */
    public boolean deleteConfDetails(Identity user, String name) {
        if (user == null || ConfDB.isKeyVisibleToUser(user, name) == false) return false;
        if (ConfDB.getVariable(name) == null) return false;

        boolean deleted = ConfDB.deleteName(name);
        if (deleted && ConfDB.isOnlyLocalConfig(name) == false) {
            ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-" + name);
        }
        return deleted;
    }

    /**
     * Vrati ConfDetails pre zadane name pre autocomplete
     * @param name
     * @return
     */
    public ConfDetailsDto getAutocompleteDetail(Identity user, String name) {
        ConfigurationCatalog catalog = getConfigurationCatalog(user);
        for (ConfDetailsDto conf : prepareDtos(catalog.getAll(), catalog)) {
            if (conf.getName().equals(name)) return conf;
        }

        if (ConfDB.isKeyVisibleToUser(user, name)) {
            //nenaslo sa, takze este nie je nastavena, posli hodnotu
            ConfDetailsDto conf = new ConfDetailsDto();
            conf.setName(name);
            conf.setValue(Constants.getString(name));
            conf.setOldValue(ConfDB.getOldValue(name));
            conf.setDescription(Constants.getDescription(name));
            conf.setDatabaseValuePresent(false);
            return conf;
        }

        return null;
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
                conf.setModules(c.getModules());
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
        ConfigurationCatalog catalog = getConfigurationCatalog(user);
        List<ConfDetailsDto> result = new ArrayList<>();

        for (ConfDetailsDto conf : prepareDtos(catalog.getAll(), catalog)) {
            if (propertyName.equals("id") && conf.getName().equals(original.getName())) {
                result.add(conf);
            } else if (propertyName.equals("name") && conf.getName().equals(original.getName())) {
                result.add(conf);
            } else if (propertyName.equals("value") && Objects.equals(conf.getValue(), original.getValue())) {
                result.add(conf);
            } else if (propertyName.equals("description") && Objects.equals(conf.getDescription(), original.getDescription())) {
                result.add(conf);
            }
        }

        return result;
    }

    private static class ConfigurationCatalog {
        private final List<ConfDetails> changed;
        private final List<ConfDetails> all;
        private final List<ConfDetails> catalogued;
        private final Set<String> databaseNames;
        private final Map<String, String> defaultValues;

        ConfigurationCatalog(List<ConfDetails> changed, List<ConfDetails> all, List<ConfDetails> catalogued,
                Set<String> databaseNames, Map<String, String> defaultValues) {
            this.changed = changed;
            this.all = all;
            this.catalogued = catalogued;
            this.databaseNames = databaseNames;
            this.defaultValues = defaultValues;
        }

        static ConfigurationCatalog empty() {
            return new ConfigurationCatalog(List.of(), List.of(), List.of(), Set.of(), Map.of());
        }

        List<ConfDetails> getChanged() {
            return changed;
        }

        List<ConfDetails> getAll() {
            return all;
        }

        List<ConfDetails> getCatalogued() {
            return catalogued;
        }

        Set<String> getDatabaseNames() {
            return databaseNames;
        }

        Map<String, String> getDefaultValues() {
            return defaultValues;
        }
    }
}
