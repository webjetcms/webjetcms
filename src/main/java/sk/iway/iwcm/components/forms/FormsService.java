package sk.iway.iwcm.components.forms;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.form.FormAttributeDB;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class FormsService<R extends FormsRepositoryInterface<E>, E extends FormsEntityBasic> {

    private final R formsRepository;

    public FormsService(R formsRepository) {
        this.formsRepository = formsRepository;
    }

    /**
     * Vrati zoznam vsetkych formularov, vyfiltruje len take, na ktore ma pouzivatel prava
     * @param user
     * @return
     */
    public List<E> getFormsList(UserDetails user) {
        Integer domainId = CloudToolsForCore.getDomainId();
        List<E> formsEntities = formsRepository.findAllByCreateDateIsNullAndDomainId(domainId);
        for (E entity : formsEntities) {
            entity.setCount(formsRepository.countAllByFormNameAndDomainId(entity.getFormName(), domainId) - 1);
            E lastOne = formsRepository.findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(entity.getFormName(), domainId);
            if (lastOne != null) {
                entity.setCreateDate(lastOne.getCreateDate());
                entity.setDocId(lastOne.getDocId());
            }
        }

        formsEntities = filterDistinct(formsEntities);

        return filterFormsByUser(user, formsEntities);
    }

    /**
     * Zrusi zo zoznamu duplicitne nazvy, tie su tam len ked je zle formular v DB vyplneny (typicky programovo)
     * @param allForms
     * @return
     */
    private List<E> filterDistinct(List<E> allForms) {
        List<E> ret = new ArrayList<>();
        Set<String> distinct = new HashSet<>();
        for (E f : allForms) {
            String key = f.getFormName();
            if (distinct.contains(key)==false) {
                ret.add(f);
                distinct.add(key);
            }
        }

        return ret;
    }

    /**
	 * Vyfiltruje formulare na zaklade prav pouzivatela na pristup k adresarom a strankam a docId formularu
	 * @param user
	 * @param allForms
	 * @return
	 */
	private List<E> filterFormsByUser(UserDetails user, List<E> allForms) {
		List<E> ret = new ArrayList<>(allForms.size());

		GroupsDB groupsDB = GroupsDB.getInstance();

		int[] userEditableGroups = groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(), ","), true);
		int[] userEditablePages = Tools.getTokensInt(user.getEditablePages(), ",");
		if ((userEditableGroups == null || userEditableGroups.length<1) && (userEditablePages==null || userEditablePages.length<1)) return allForms;

		DocDB docDB = DocDB.getInstance();
		for (E form : allForms)
		{
			boolean pridaj = isFormAccessible(form, userEditableGroups, userEditablePages, docDB);
			if (pridaj) ret.add(form);
		}

		return ret;
	}

    /**
     * Overi, ci pouzivatel ma pravo na dany formular
     * @param formName
     * @param user
     * @return
     */
    public boolean isFormAccessible(String formName, UserDetails user) {
        Integer domainId = CloudToolsForCore.getDomainId();
        GroupsDB groupsDB = GroupsDB.getInstance();

		int[] userEditableGroups = groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(), ","), true);
		int[] userEditablePages = Tools.getTokensInt(user.getEditablePages(), ",");
		if ((userEditableGroups == null || userEditableGroups.length<1) && (userEditablePages==null || userEditablePages.length<1)) return true;

		DocDB docDB = DocDB.getInstance();

        E lastOne = formsRepository.findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(formName, domainId);

        return isFormAccessible(lastOne, userEditableGroups, userEditablePages, docDB);
    }

    /**
     * Overi, ci konkretny formular je dostupny pre pouzivatela
     * @param form
     * @param user
     * @param userEditableGroups
     * @param userEditablePages
     * @param docDB
     * @return
     */
    private boolean isFormAccessible(E form, int[] userEditableGroups, int[] userEditablePages, DocDB docDB) {
        if (userEditableGroups!=null && userEditableGroups.length>0)
        {
            DocDetails doc = docDB.getBasicDocDetails(form.getDocId(), false);
            if (doc != null)
            {
                for (int groupId : userEditableGroups)
                {
                    if (doc.getGroupId()==groupId)
                    {
                        return true;
                    }
                }
            }
        }
        if (userEditablePages!=null && userEditablePages.length>0)
        {
            for (int docId : userEditablePages)
            {
                if (form.getDocId()==docId)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Vrati zoznam stlpcov formulara
     * @param formName
     * @return
     */
    public FormColumns getColumnNames(String formName, UserDetails user) {

        if (isFormAccessible(formName, user)==false) return null;

        E entity = formsRepository.findFirstByFormNameAndDomainIdAndCreateDateIsNullOrderByIdAsc(formName, CloudToolsForCore.getDomainId());
        String[] formsColumns = entity.getData().split("~");
        List<LabelValue> columns = new ArrayList<>();
        for (String column : formsColumns) {
            if (!column.contains("|")) {
                column += "|";
            }

            String[] columnNames = column.split("\\|", -1);
            if (Tools.isEmpty(columnNames[1])) {
                columnNames[1] = Tools.replace(columnNames[0], "-", " ");
                columnNames[1] = Tools.replace(columnNames[1], "_", " ");
                columnNames[1] = Tools.replace(columnNames[1], "e mail", "e-mail");
            }
            columns.add(new LabelValue(columnNames[1], columnNames[0]));
        }
        FormColumns formColumns = new FormColumns();
        formColumns.setColumns(columns);
        formColumns.setCount(formsRepository.countAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId()));
        Map<String, String> attributes = new FormAttributeDB().load(formName);
        if (attributes != null) {
            formColumns.setDoubleOptIn(attributes.containsKey("doubleOptIn") && attributes.get("doubleOptIn").equalsIgnoreCase("true"));
        }
        return formColumns;
    }

    /**
     * Vrati zaznamy v databaze pre dany formular (zaznamy formularu)
     * @param formName
     * @param pageable
     * @return
     */
    Page<E> getFormsData(String formName, UserDetails user, Pageable pageable) {

        if (isFormAccessible(formName, user)==false) return null;

        Page<E> formsEntities = formsRepository.findAllByFormNameAndDomainIdAndCreateDateNotNull(formName, CloudToolsForCore.getDomainId(), pageable);
        parseDataColumnInFormsEntities(formsEntities);
        return formsEntities;
    }

    /**
     * Vyhlada (serverovo) v udajoch formularu (jednotlive zaznamy formularu)
     * @param formName
     * @param user
     * @param params
     * @param pageable
     * @return
     */
    public Page<E> findInDataByColumns(String formName, UserDetails user, Map<String, String> params, Pageable pageable) {

        if (isFormAccessible(formName, user)==false) return null;

        Integer domainId = CloudToolsForCore.getDomainId();

        Specification<E> spec = null;
        spec = getSearchConditions(formName, domainId, params);

        Page<E> filteredForms;
        if (spec != null && formsRepository instanceof JpaSpecificationExecutor) filteredForms = formsRepository.findAll(spec, pageable);
        else filteredForms = formsRepository.findAllByFormNameAndDomainIdAndCreateDateNotNull(formName, domainId, pageable);

        parseDataColumnInFormsEntities(filteredForms);

        return filteredForms;
    }

    protected Specification<E> getSearchConditions(String formName, Integer domainId, Map<String, String> params) {
		return (Specification<E>) (root, query, builder) -> {
			final List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("formName"), formName));
            predicates.add(builder.equal(root.get("domainId"), domainId));
            predicates.add(builder.isNotNull(root.get("createDate")));

            for (Map.Entry<String, String> paramsEntry : params.entrySet()) {
                String key = paramsEntry.getKey();
                if ("fromLastExport".equals(key)) {
                    String value = paramsEntry.getValue();
                    if (Tools.isNotEmpty(value) && "false".equals(value)==false) {
                        predicates.add(builder.isNull(root.get(value)));
                    }
                } else if ("id".equals(key)) {
                        String value = DatatableRestControllerV2.getCleanValue(paramsEntry.getValue());
                        int[] ids = Tools.getTokensInt(value, ",");
                        List<Integer> idsList = Arrays.stream(ids).boxed().collect(Collectors.toList());

                        //ak nic neoznacil, nic mu neexportneme
                        if (idsList.isEmpty()) idsList.add(Integer.valueOf(-1));

                        predicates.add(root.get("id").in(idsList));
                } else if (checkSearchParam(key)) {
                    key = DatatableRestControllerV2.getCleanKey(paramsEntry.getKey());
                    if ("createDate".equals(key) || "lastExportDate".equals(key) || "doubleOptinConfirmationDate".equals(key)) {
                        String dateRange = paramsEntry.getValue();
                        Pair<Date, Date> datePair = parseDate(dateRange);
                        if (datePair != null) {
                            if (datePair.first != null) predicates.add(builder.greaterThanOrEqualTo(root.get(key), datePair.first));
                            if (datePair.second != null) predicates.add(builder.lessThanOrEqualTo(root.get(key), datePair.second));
                        }

                    } else if ("note".equals(key) || "files".equals(key)) {
                        String value = paramsEntry.getValue();
                        if (value.startsWith("^") && value.endsWith("$")) predicates.add(builder.equal(root.get(key), value.substring(1, value.length()-1)));
                        else {
                            if (value.startsWith("^")) value = value.substring(1)+"%";
                            else if (value.endsWith("$")) value = "%"+value.substring(0, value.length()-1);
                            else value = "%"+value+"%";
                            if (Constants.DB_TYPE==Constants.DB_ORACLE) {
                                predicates.add(builder.like(builder.lower(root.get(key)), value.toLowerCase()));
                            } else if (Constants.DB_TYPE==Constants.DB_PGSQL) {
                                predicates.add(builder.like(builder.lower(builder.function("unaccent", String.class, root.get(key))), DB.internationalToEnglish(value).toLowerCase()));
                            } else {
                                predicates.add(builder.like(root.get(key), value));
                            }
                        }
                    } else {
                        String value = DatatableRestControllerV2.getCleanValue(paramsEntry.getValue());
                        if (key.startsWith("col_")) key = key.substring(4);
                        String searchParam = "%" + key + "~" + value + "%";
                        if (Constants.DB_TYPE==Constants.DB_ORACLE) {
                            predicates.add(builder.like(builder.lower(root.get("data")), searchParam.toLowerCase()));
                        } else if (Constants.DB_TYPE==Constants.DB_PGSQL) {
                            predicates.add(builder.like(builder.lower(builder.function("unaccent", String.class, root.get("data"))), DB.internationalToEnglish(searchParam).toLowerCase()));
                        } else {
                            predicates.add(builder.like(root.get("data"), searchParam));
                        }
                    }
                }
            }

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    /**
     * Skonvertuje retazec daterange:tsfrom-tsto na par Date objektov
     * @param dateRange - par Date objektov, pre nezadany datum obsahuje null
     * @return
     */
    protected Pair<Date, Date> parseDate(String dateRange) {
        Pair<Date, Date> dateRangePair = null;
        String[] dates = null;
        if (Tools.isNotEmpty(dateRange)) {
            dates = Tools.getTokens(dateRange.substring(dateRange.indexOf(":") + 1), "-");

            if (dates.length == 1) {
                if (dateRange.contains("-")) {
                    dateRangePair = new Pair<>(null, new Date(Tools.getLongValue(dates[0], new Date().getTime())));
                } else {
                    dateRangePair = new Pair<>(new Date(Tools.getLongValue(dates[0], 0)), null);
                }
            } else {
                dateRangePair = new Pair<>(new Date(Tools.getLongValue(dates[0], 0)), new Date(Tools.getLongValue(dates[1], new Date().getTime())));
            }

        }
        return dateRangePair;
    }

    private void parseDataColumnInFormsEntities(Page<E> formsEntities) {
        for (E entity : formsEntities) {
            String[] columns = (entity.getData().split("\\|", -1));
            Map<String, String> columnNamesAndValues = new HashMap<>();

            boolean containsWysiwyg = false;
            //didnt find better way to check if HTML is enabled in the column
            String WYSIWYG_HTML = ResponseUtils.filter("<span class='form-control emailInput-textarea formsimple-wysiwyg' style='height: auto;'>");
            if (entity.getHtml()!=null && entity.getHtml().contains(WYSIWYG_HTML)) {
                containsWysiwyg = true;
            }

            for (String c : columns) {
                String[] nameAndValueArray = c.split("~");
                if (nameAndValueArray.length == 1) {
                    columnNamesAndValues.put(nameAndValueArray[0], "");
                } else {
                    columnNamesAndValues.put(nameAndValueArray[0], CryptoFactory.decrypt(nameAndValueArray[1]));
                }

                //allow HTML for cleditor - unescape entities
                if (containsWysiwyg) {
                    String value = columnNamesAndValues.get(nameAndValueArray[0]);
                    if (Tools.isNotEmpty(value) && entity.getHtml().contains(WYSIWYG_HTML+value)) {
                        //unescape entities
                        value = value.replace("&lt;", "<");
                        value = value.replace("&gt;", ">");
                        value = value.replace("&amp;", "&");
                        value = value.replace("&quot;", "\"");
                        value = value.replace("&#39;", "'");
                        columnNamesAndValues.put(nameAndValueArray[0], value);
                    }
                }
                //unescape double escape
                String value = columnNamesAndValues.get(nameAndValueArray[0]);
                if (Tools.isNotEmpty(value)) {
                    value = value.replace("&amp;", "&");
                    columnNamesAndValues.put(nameAndValueArray[0], value);
                }
            }

            entity.setColumnNamesAndValues(columnNamesAndValues);
            entity.setHtml("");
        }
    }

    boolean checkSearchParam(String param) {
        return param.startsWith("search");
    }

    /**
     * Ziska zaznam z repozitara podla ID
     * @param id
     * @return
     */
    public E getById(long id) {
        return formsRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
    }

    /**
     * Aktualizuje poznamku formulara
     * @param note
     * @param id
     */
    public void updateNote(String note, long id) {
        E form = getById(id);
        if (form != null) {
            form.setNote(note);
            formsRepository.save(form);
        }
    }

    /**
     * Nastavi datum posledneho exportu pre zadane formulare
     * @param forms
     */
    public void setExportDate(List<E> forms) {
        int counter = 0;
        List<Long> ids = new ArrayList<>();
        Date now = new Date(Tools.getNow());
        for (E form : forms) {
            ids.add(form.getId());
            form.setLastExportDate(now);
            if (counter++ % 100 == 0) {

                formsRepository.updateLastExportDate(now, ids);
                ids = new ArrayList<>();
            }
        }

        if (ids.isEmpty()==false) {
            formsRepository.updateLastExportDate(now, ids);
        }
    }

    /**
     * Zmaze zaznam z databazy
     * + ak je to posledny zaznam vo formulari, zmaze aj definiciu formularu
     * + ak maze riadiaci zaznam (createDate je null) tak zmaze vsetky zaznamy
     * @param entity
     * @param id
     * @return
     */
    public boolean deleteItem(E entity, long id) {
        try {
            String formName = entity.getFormName();
            E entityDb = getById(id);

            int domainId = CloudToolsForCore.getDomainId();
            if (domainId != entityDb.getDomainId()) return false;

            int count = formsRepository.countAllByFormNameAndDomainId(formName, domainId);

            if (entityDb.getCreateDate()==null || count <= 2) {
                //zmaz vsetky podla mena formu, ak su uz len 2 zaznamy (cize riadiaci + jeden form) zmaz tiez vsetko
                formsRepository.deleteByFormNameAndDomainId(formName, domainId);
            } else {
                formsRepository.deleteById(id);
            }

            return true;
        } catch (Exception e) {
            Logger.error(getClass(), e);
        }
        return false;
    }

    public String downloadAttachment(String name, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //Something wrong
        if(request == null || response == null) return null;

        //Redirect to /admin/logon.jsp
        HttpSession session = request.getSession();
        if (session == null) return SpringUrlMapping.redirectToLogon();

        //Test actual user or redirect
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) return SpringUrlMapping.redirectToLogon();

        if (user.isEnabledItem("cmp_form")==false) return SpringUrlMapping.redirectToLogon();

        //ak je zle poslana linka typu /WEB-INF/formfiles/23882_pdf.pdf;23882_pdf.pdf oprav
        if (name.contains(";")) name = name.substring(name.lastIndexOf(";")+1);

        //ochrana voci hackerom ;-)
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) return null;

        //get form name and check perms
        String formname = null;
        //get ID of row
        int i = name.indexOf("_");
        if (i > 0) {
            int id = Tools.getIntValue(name.substring(0, i), 0);
            if (id > 0) {
                formname = (new SimpleQuery()).forString("SELECT form_name FROM forms WHERE id=?", Integer.valueOf(id));
            }
        }

        //if we have formname check access, otherwise for safety reason (e.g. forms_archive different ID) allow access
        if (Tools.isNotEmpty(formname) && isFormAccessible(formname, user)==false) return SpringUrlMapping.redirectToLogon();

        //fix na lomitko v Tomcate
        String filePath = Tools.getRealPath(FormMailAction.FORM_FILE_DIR);
        if (filePath.endsWith(Character.toString(File.separatorChar))) filePath = filePath + name;
        else filePath = filePath + File.separatorChar + name;

        if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) {
           //kedze sme zmenili umiestnenie suborov na externy adresar, ak subor bol vytvoreny pred zmenou tak je v centralnom adresari
           IwcmFile f = new IwcmFile(filePath);
           if (f.exists() == false) {
              String globalPath = Constants.getServletContext().getRealPath(FormMailAction.FORM_FILE_DIR);
              if (globalPath.endsWith(Character.toString(File.separatorChar))) globalPath = globalPath + name;
              else globalPath = globalPath + File.separatorChar + globalPath;

              f = new IwcmFile(globalPath);
              if (f.exists()) filePath = globalPath;
           }
        }

        ServletOutputStream out = response.getOutputStream();
        //citaj subor a posielaj na vystup
        byte buff[] = new byte[8000];
        IwcmInputStream fis = new IwcmInputStream(filePath);
        int len;

        String mimeType = "application/octet-stream";
        try {
	    	mimeType = Constants.getServletContext().getMimeType(filePath.toLowerCase());
        } catch (Exception ex) {
	    	sk.iway.iwcm.Logger.error(ex);
        }

	    if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";

        response.setContentType(mimeType);
        name = name.substring(name.indexOf('_') + 1);
        response.setHeader("Content-Disposition", Tools.sanitizeHttpHeaderParam("attachment; filename=\"" + name + "\""));

        while ((len = fis.read(buff)) != -1) out.write(buff, 0, len);

        fis.close();
        out.flush();
        out.close();
        return null;
    }
}
