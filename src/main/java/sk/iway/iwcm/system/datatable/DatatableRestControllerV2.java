package sk.iway.iwcm.system.datatable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.json.JSONObject;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ActiveRecordBase;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.system.spring.NullAwareBeanUtils;
import sk.iway.iwcm.system.stripes.MultipartWrapper;
import sk.iway.iwcm.users.UsersDB;

/**
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 * @author       tmarcinkova $
 * @created      2019/05/10 12:50
 *
 *  Abstraktny univerzalny RestController na pracu s DataTables Editor-om
 *
 */
public abstract class DatatableRestControllerV2<T, ID extends Serializable>
{
	private final JpaRepository<T, Long> repo;
	private final Class<T> entityClass;

	//pozor: po zmene je potrebne opravit aj prefix v src/main/webapp/admin/v9/src/js/app.js
	private static final String REGEX_PREFIX = "regex:";

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private Validator validator;

	private static final ThreadLocal<ThreadBean> threadData = new ThreadLocal<>(); //NOSONAR

	boolean checkDomainId = false;

	protected DatatableRestControllerV2() {
		this(null, null);
	}

	protected DatatableRestControllerV2(JpaRepository<T, Long> repo) {
		this(repo, null);
	}

	/**
	 * Constructor for DatatableRestControllerV2.
	 * If entityClass is provided, it will be used to create new instances of the entity instead of using NULL value.
	 * So it will have properties set to default values for new item. Requires fetchOnCreate: true in WJ.DataTable config.
	 * @param repo
	 * @param entityClass
	 */
	protected DatatableRestControllerV2(JpaRepository<T, Long> repo, Class<T> entityClass) {
		this.repo = repo;

		//over, ci maju byt pouzite automaticke podmienky so stlpcom domain_id
		if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
			if (repo !=null && repo instanceof DomainIdRepository) checkDomainId = true;
		}

		this.entityClass = entityClass;
	}

	/***************************** CITANIE / ZAPIS DAT *****************************/

	/**
	 * Vlozi NOVU entitu do databazy
	 * @param entity
	 * @return
	 */
	public T insertItem(T entity) {
		//musime z editoFields najskor prepisat hodnoty do entity
		T processed = processToEntity(entity, ProcessItemAction.CREATE);
		//ulozime
		T saved = repo.save(processed);
		//nastavime editorFields atributy
		return processFromEntity(saved, ProcessItemAction.CREATE, 1);
	}

	/**
	 * Ulozi existujucu entitu do databazy
	 * @param entity
	 * @param id
	 * @return
	 */
	public T editItem(T entity, long id) {

		//zachovaj thread bean, lebo volanie getOne ho zmaze a moze to byt nastavene z beforeSave metody
		boolean forceReload = isForceReload();
		List<NotifyBean> notify = getThreadData().getNotify();
		boolean isImporting = isImporting();

		//toto nam zabezpeci aby sa nam nestratili udaje, ktore nemame v editore
		T one = getOne(id);

		if (isForceReload()) setForceReload(forceReload);
		if (notify!=null) addNotify(notify);
		setImporting(isImporting);

		copyEntityIntoOriginal(entity, one);

		//musime z editoFields najskor prepisat hodnoty do entity
		T processed = processToEntity(one, ProcessItemAction.EDIT);
		//ulozime
		T saved = repo.save(processed);
		//nastavime editorFields atributy
		return processFromEntity(saved, ProcessItemAction.EDIT, 1);
	}

	/**
	 * metoda pre ziskanie entity s rovnakou hodnotou v stlci propertyName ako hodnota v obj
	 * @param propertyName
	 * @param original
	 * @return
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings("unchecked")
	public List<T> findItemBy(String propertyName, T original) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

		//musime spravit najskor kopiu obj, aby sme na nej mohli zavolat processToEntity bez posahania povodnej entity
		T obj = (T)original.getClass().getDeclaredConstructor().newInstance();
		NullAwareBeanUtils.copyProperties(original, obj);
		processToEntity(obj, ProcessItemAction.EDIT);

		JpaEntityManager entityManager = JpaTools.getSpringEntityManager(original.getClass());
		ReadAllQuery raq = new ReadAllQuery(original.getClass());
		ExpressionBuilder builder = new ExpressionBuilder();

		BeanWrapperImpl bw = new BeanWrapperImpl(obj);
		Object value = bw.getPropertyValue(propertyName);
		Expression exp = builder.get(propertyName).equal(value);

		//pridaj domainId podmienku ak entita obsahuje domainId stlpec (aby sa neaktualizovali entity v inej domene)
		if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
			if (bw.getPropertyType("domainId")!=null) {
				exp = exp.and(builder.get("domainId").equal(CloudToolsForCore.getDomainId()));
			}
		}

		raq.setSelectionCriteria(exp);

		Query query = entityManager.createQuery(raq);
		List<T> list = query.getResultList();
		int rowCount = 1;
		for (T entity : list) {
			processFromEntity(entity, ProcessItemAction.FIND, rowCount);
			rowCount++;
		}
		return list;
	}

	@SuppressWarnings("rawtypes")
	private String getIdColumnName(T entity) {
		Class c = entity.getClass();
		for (Field field : c.getDeclaredFields()) {
			if (field.isAnnotationPresent(Id.class)) {
				return field.getName();
			}
		}

		return null;
	}

	/**
	 * metoda na upravu beanu v DB na zaklade nazvu stlpca v DB @updateByColumn.
	 * @param entity
	 * @param updateByColumn
	 * @return
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	private List<T> editItemByColumn(T entity, String updateByColumn) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
		beforeSave(entity);

		// ziskame list entit, ktore obsahuju v stlpci updateByColumn rovnaku hodnotu ako entita
		String idColumnName = getIdColumnName(entity);
		if ("id".equalsIgnoreCase(updateByColumn) && idColumnName!=null) updateByColumn = idColumnName;

		List<T> itemsBy = findItemBy(updateByColumn, entity);
		if (itemsBy.isEmpty()) {
			//zmaz ID column
			try {
				if (idColumnName == null) idColumnName = "id";
				BeanWrapperImpl bw = new BeanWrapperImpl(entity);
				Long id = null;
				try { bw.setPropertyValue(idColumnName, id); }
				catch (Exception ex) { bw.setPropertyValue(idColumnName, 0); }
			} catch (Exception ex) {
				//failsafe
			}
			T processed = insertItem(entity);
			afterSave(entity, processed);
			return Arrays.asList(processed);
		}

		List<T> savedList = new ArrayList<>();
		// nastavenie dat a ulozenie
		for (T itemBy : itemsBy) {
			long id = 0;
			try {
				BeanWrapperImpl bw = new BeanWrapperImpl(itemBy);
				Object value = bw.getPropertyValue(idColumnName != null ? idColumnName : "id");
				if (value instanceof Number) {
					id = ((Number)value).longValue();
				}
			} catch (Exception ex) {
				//failsafe
			}

			BeanWrapperImpl bw = new BeanWrapperImpl(entity);
			//setni ID hodnotu na povodnej entite, aby sa nasledne korektne vykonala processToEntity so spravnym ID
			try {
				bw.setPropertyValue("id", Long.valueOf(id));
			} catch (Exception e) {
				//failsafe
			}
			if (idColumnName!=null && "id".equals(idColumnName)==false) {
				try {
					bw.setPropertyValue(idColumnName, Long.valueOf(id));
				} catch (Exception e) {
					//failsafe
				}
			}

			beforeSave(entity);
			checkItemPermsThrows(entity, id);

			T saved = editItem(entity, id);

			afterSave(entity, saved);

			savedList.add(saved);
		}

		return savedList;
	}

	/**
	 * Zmaze danu entitu z databazy
	 * @param entity
	 * @param id
	 * @return
	 */
	public boolean deleteItem(T entity, long id) {
		if (beforeDelete(repo.getById(id))) {
			try {
				if (checkDomainId) {
					//zmazanie pri pouziti domain_id riesime ziskanim zaznamu cez getOneItem ktore overi aj domain_id stlpec a naslednym zmazanim entity
					T fromRepo = getOneItem(id);
					if (fromRepo != null) {
						DomainIdRepository<T, ID> domainRepo = getDomainRepo();
						if (domainRepo!=null) domainRepo.delete(fromRepo);
					}
				} else {
					T fromRepo = getOneItem(id);
					if (fromRepo != null) {
						repo.delete(fromRepo);
					}
				}
				return true;
			} catch (Exception e) {
				Logger.error(DatatableRestControllerV2.class, e);
			}
		}

		return false;
	}

	/**
	 * Ziska z databazy entitu so zadanym id
	 * @param id
	 * @return
	 */
	public T getOneItem(long id) {
		T result = null;

		//create new instance
		if (id == -1 && entityClass != null) {
			try {
				result = entityClass.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				Logger.error(DatatableRestControllerV2.class, e);
			}
		}

		if (result == null && repo.existsById(id)) {
			Optional<T> byId = Optional.empty();

			if (checkDomainId) {
				DomainIdRepository<T, ID> domainRepo = getDomainRepo();
				if (domainRepo!=null) byId = domainRepo.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId());
			} else {
				byId = repo.findById(id);
			}

			if (byId.isPresent()) {
				result = byId.get();
			}
		}

		return processFromEntity(result, ProcessItemAction.GETONE, 1);
	}

	/**
	 * Ziska z databazy vsetky zaznamy
	 * @param pageable
	 * @return
	 */
	public Page<T> getAllItems(Pageable pageable) {
		Page<T> page = null;

		if (checkDomainId) {
			//volame aj s podmienkami domain_id
			DomainIdRepository<T, ID> domainRepo = getDomainRepo();
			if (domainRepo!=null) {
				//ak nemame size parameter tak sa jedna o serverSide: false, takze pageable nemame pouzit
				if (getRequest().getParameter("size")==null) page = new DatatablePageImpl<>(domainRepo.findAllByDomainId(CloudToolsForCore.getDomainId()));
				else page = domainRepo.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable);
			}
		} else {
			//ak nemame size parameter tak sa jedna o serverSide: false, takze pageable nemame pouzit
			if (getRequest().getParameter("size")==null) page = new DatatablePageImpl<>(repo.findAll());
			else page = repo.findAll(pageable);
		}

		processFromEntity(page, ProcessItemAction.GETALL);

		return page;
	}

	/**
	 * Vrati vsetky zaznamy, pricom vykona volanie metody addSpecSearch,
	 * cize je mozne pouzit URL parametre na filtrovanie vsetkych zaznamov.
	 * @param empty - prazdny objekt (je potrebny kvoli vytvoreniu instance)
	 * @param pageable
	 * @return
	 */
	public Page<T> getAllItemsIncludeSpecSearch(T empty, Pageable pageable) {

		Map<String, String> params = getParamsMap(getRequest());

		return searchItem(params, pageable, empty);
	}

	/**
	 * Convert URL/request parameters to Map&lt;String paramName, String paramValue&gt;
	 * @param request
	 * @return
	 */
	public static Map<String, String> getParamsMap(HttpServletRequest request) {
		Map<String, String[]> paramsMulti = request.getParameterMap();
		Map<String, String> params = new HashMap<>();
		for (Map.Entry<String, String[]> entry : paramsMulti.entrySet()) {
			String[] value = entry.getValue();
			if (value != null && value.length>0) {
				if ("sort".equals(entry.getKey())) {
					//you can sort using shift for multiple columns, we need to handle it
					params.put(entry.getKey(), String.join("\n", value));
				} else {
 					params.put(entry.getKey(), value[0]);
				}
			}
		}
		return params;
	}

	/**
	 * Vykona zadanu akciu (napr. rotacia obrazku v galerii)
	 *
	 * @param entity
	 * @param action
	 * @return false ak nastane chyba
	 */
	public boolean processAction(T entity, String action) {
		return true;
	}

	/**
	 * Vykona upravy vo vsetkych entitach v page objekte pred vratenim cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from entity to editorFields)
	 * @param page
	 * @param action - typ zmeny - create,edit,getall...
	 */
	public void processFromEntity(Page<T> page, ProcessItemAction action) {
		if (page == null || page.getContent()==null) { //NOSONAR
			return;
		}

		//pri exporte potrebujeme vsetky data z editorFields, takze sa tvarime ako rezim GETONE
		if (isExporting()) action = ProcessItemAction.GETONE;

		int rowCount = 1;
		for (T entity : page.getContent()) {
			processFromEntity(entity, action, rowCount);
			rowCount++;
		}
	}

	/**
	 * Vykona upravy vo vsetkych entitach v page objekte pred vratenim cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from entity to editorFields)
	 * @param entities - list entit
	 * @param action - typ zmeny - create,edit,getall...
	 */
	public void processFromEntity(List<T> entities, ProcessItemAction action) {
		if(entities == null) return;

		//pri exporte potrebujeme vsetky data z editorFields, takze sa tvarime ako rezim GETONE
		if (isExporting()) action = ProcessItemAction.GETONE;

		int rowCount = 1;
		for (T entity : entities) {
			processFromEntity(entity, action, rowCount);
			rowCount++;
		}
	}

	/**
	 * Vykona upravy v entite pred vratenim cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from entity to editorFields)
	 * @param entity
	 * @param action - typ zmeny - create,edit,getall...
	 * @param rowCount - cislo riadka v tabulke
	 */
	public T processFromEntity(T entity, ProcessItemAction action, int rowCount) {
		return processFromEntity(entity, action);
	}

	/**
	 * Vykona upravy v entite pred vratenim cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from entity to editorFields)
	 * @param entity
	 * @param action - typ zmeny - create,edit,getall...
	 */
	public T processFromEntity(T entity, ProcessItemAction action) {
		return entity;
	}

	/**
	 * Vykona upravy v entite pri odpovedi (ulozeni) z REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from editorFields to entity)
	 * @param entity
	 * @param action - typ zmeny - create,edit,getall,
	 */
	public T processToEntity(T entity, ProcessItemAction action) {
		return entity;
	}


	/**
	 * Do objektu searchProperties naplni hladane vyrazy, vrati pripadne upraveny ExampleMatcher
	 * @param params
	 * @param searchProperties - vratena mapa request parametrov pre vyhladavanie
	 * @param searchWrapped
	 * @param matcher - ak sa jedna o exampleMatcher, moze byt null
	 * @param isExampleSearch
	 * @return
	 */
	public ExampleMatcher getSearchProperties(Map<String, String> params, Map<String, String> searchProperties, BeanWrapperImpl searchWrapped, ExampleMatcher matcher, boolean isExampleSearch) {

		//final Map<String, String> searchProperties = new HashMap<>();

		for (Map.Entry<String, String> paramsEntry : params.entrySet()) {
			String key = getCleanKey(paramsEntry.getKey());

			//this is not search property
			if ("size".equals(key) || "page".equals(key) || "sort".equals(key)) {
				continue;
			}

			if (!searchWrapped.isReadableProperty(key)) {
				Logger.debug(DatatableRestControllerV2.class, "Property is not readable, key; "+key);
				continue;
			}

			String value = paramsEntry.getValue();
			if (Tools.isEmpty(value)) continue;

			if (value.startsWith("range:") || value.startsWith("daterange:")) {
				searchProperties.put(key, value);
			}
			else {
				String cleanValue = getCleanValue(value);

				if (Tools.isEmpty(cleanValue)) {
					Logger.debug(DatatableRestControllerV2.class, "Value empty, key: "+key+", value: "+value);
					continue;
				}

				if (isExampleSearch) {
					ExampleMatcher.GenericPropertyMatcher genericPropertyMatcherFromValue = getGenericPropertyMatcherFromValue(value);
					matcher = matcher.withMatcher(key, genericPropertyMatcherFromValue);

					searchWrapped.setPropertyValue(key, getCleanValue(value));
				} else {
					searchProperties.put(key, value);
				}
			}
		}

		return matcher;
	}

	/**
	 * Vyhlada objekty podla zadaneho search objektu a pripadnych parametrov z requestu
	 * @param params
	 * @param pageable
	 * @param search
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Page<T> searchItem(@RequestParam Map<String, String> params, Pageable pageable, T search) {

		//urcenie sposobu hladania - by example alebo pomocou presnych parametrov
		boolean isExampleSearch = true;
		if (repo instanceof JpaSpecificationExecutor) {
			isExampleSearch = false;
		}

		ExampleMatcher matcher = ExampleMatcher.matchingAll();

		BeanWrapperImpl searchWrapped = new BeanWrapperImpl(search);

		final Map<String, String> searchProperties = new HashMap<>();
		matcher = getSearchProperties(params, searchProperties, searchWrapped, matcher, isExampleSearch);

		//Check pageable sort - remove editor fileds type of sort
		if(pageable != null) {
			Sort sort = pageable.getSort();
			//remove all sort for fields starting with editorFields.
			if(sort != null && sort.isSorted()) {
				Iterator<Order> iter = sort.iterator();
				while(iter.hasNext()) {
					Sort.Order order = iter.next();
					//remove sort, this editorFields sort will cause error
					if(order.getProperty().startsWith("editorFields.")) {
						iter.remove();
						iter = sort.iterator();
					}
				}
			}
		}

		Page<T> page;
		if (isExampleSearch) {
			matcher = matcher.withIgnoreCase().withIgnoreNullValues();
			Example<T> exampleQuery = Example.of(search, matcher);

			if (pageable != null) page = repo.findAll(exampleQuery, pageable);
			else page = new DatatablePageImpl<>(repo.findAll(exampleQuery));
		} else {
			Specification<T> spec  = getSearchConditions(searchProperties, params, search);
			if (pageable != null) page = ((JpaSpecificationExecutor<T>)repo).findAll(spec, pageable);
			else page = new DatatablePageImpl<>(((JpaSpecificationExecutor<T>)repo).findAll(spec));
		}

		ProcessItemAction action = ProcessItemAction.FIND;
		//pri exporte potrebujeme vsetky data z editorFields, takze sa tvarime ako rezim GETONE
		if (isExporting()) action = ProcessItemAction.GETONE;

		int rowCount = 1;
		for (T entity : page.getContent()) {
			processFromEntity(entity, action, rowCount);
			rowCount++;
		}

		return page;
	}

	/**
	 * Doplni pri volani getAllItems options polozky pre vyberove polia
	 * @param page
	 */
	public void getOptions(DatatablePageImpl<T> page) {
		//page.addOptions(field, options, labelProperty, valueProperty, includeOriginalObject);
	}

	/*************************** BEZPECNOST A VALIDACIA ****************************/

	/**
	 * Validate access to this rest controller, this is not per row/entity check
	 * @param request
	 * @return
	 */
	public boolean checkAccessAllowed(HttpServletRequest request) {
		return true;
	}

	/**
	 * Check item perms, it's called with every save/delete/getOne action
	 * @param entity - current entity
	 * @param id - entity ID
	 * @return false if permissions is not allowed
	 */
	public boolean checkItemPerms(T entity, Long id) {
		return true;
	}

	/**
	 * Check and throws exception if item is not allowed to edit
	 * @param entity
	 * @param id
	 */
	private void checkItemPermsThrows(T entity, Long id) {
		boolean valid = checkItemPerms(entity, id);
		if (valid==false) throwConstraintViolation(getProp().getText("components.file_archiv.file_rename.nemate_pravo_na_tuto_editaciu"));
	}

	/**
	 * Pripravena metoda, odporucame implementovat v child triede.
	 * Metoda je volana pre kazdy odoslaby objekt.
	 * Chyby pridava do error objeku pomocou {@link Errors}.rejectValue
	 *
	 * @param request
	 * @param user
	 * @param errors
	 * @param id
	 * @param entity
	 */
	public void validateEditor(HttpServletRequest request, DatatableRequest<Long, T> target, Identity user, Errors errors, Long id, T entity) {}

	/**
	 * Metoda volana pred zmazanim enity z DB, moze vykonat dodatocne akcie
	 * napr. zmazanie suborov z disku, ulozenie do archivu,
	 * alebo specialne kontroly prav
	 * @param entity
	 * @return
	 */
	public boolean beforeDelete(T entity) {
		return true;
	}

	/**
	 * Metoda volana pred insert/save danej entity,
	 * da sa pouzit na nastavenie udajov, napr. datum ulozenia, domainId a podobne
	 * @param entity
	 */
	public void beforeSave(T entity) {

	}

	/**
	 * Metoda volana pred duplikovanim danej entity,
	 * da sa pouzit na resetovanie udajov, napr. priradena default stranka adresara a podobne
	 * @param entity
	 */
	public void beforeDuplicate(T entity) {

	}

	/**
	 * Metoda volana po duplikovanim danej entity,
	 * da sa pouzit na dokopirovanie udajov, napr. media web stranky
	 * @param entity - novo ulozena (zduplikovana) entita
	 * @param originalId - ID povodneho zaznamu ktory sa duplikoval
	 */
	public void afterDuplicate(T entity, Long originalId) {

	}

	/**
	 * Metoda volana po ulozeni entity.
	 * POZOR: pre novo vytvaranu entitu bude jej ID ulozene len v saved entite, povodna entity bude mat ID=0
	 * @param entity - povodna odoslana entita
	 * @param saved - uz ulozena verzia entity
	 */
	public void afterSave(T entity, T saved) {

	}

	/**
	 * Metoda volana po zmazanim enity z DB, moze vykonat dodatocne akcie
	 * napr. zmazanie suborov z disku, ulozenie do archivu,
	 * alebo obnovu cache objektov
	 * @param entity
	 * @param id
	 */
	public void afterDelete(T entity, long id) {

	}

	/**
	 * Metoda sa vola pri importe po kazdom chunku
	 * @param chunk - aktualny chunk
	 * @param totalChunks - celkovy pocet chunkov
	 */
	public void afterImportChunk(int chunk, int totalChunks) {

	}


	/************************** PRIVATNE / SUPPORT metody***************************/

	private ExampleMatcher.GenericPropertyMatcher getGenericPropertyMatcherFromValue(String value) {
		if (value.startsWith("^") && value.endsWith("$")) {
			return ExampleMatcher.GenericPropertyMatchers.exact();
		} else if (value.startsWith("^")) {
			return ExampleMatcher.GenericPropertyMatchers.startsWith();
		} else if (value.endsWith("$")) {
			return ExampleMatcher.GenericPropertyMatchers.endsWith();
		} else if (value.startsWith(REGEX_PREFIX)) {
			return ExampleMatcher.GenericPropertyMatchers.regex();
		}
		return ExampleMatcher.GenericPropertyMatchers.contains();
	}

	public static String getCleanKey(String key) {
		return firstToLower(Tools.replace(key, "search", ""));
	}

	public static String getCleanValue(String value) {
		String result = value;
		if (result.length() >= 2) {
			if (result.startsWith("^"))
				result = result.substring(1);
			if (result.endsWith("$"))
				result = result.substring(0, result.length() - 1);
		}
		if (result.startsWith(REGEX_PREFIX) && result.length() > REGEX_PREFIX.length())
			result = result.substring(REGEX_PREFIX.length());

		return result;
	}

	private static String firstToLower(String value) {
		if (value == null || value.length() == 0) {
			return "";
		}

		char[] chArr = value.toCharArray();
		chArr[0] = Character.toLowerCase(chArr[0]);

		return new String(chArr);
	}

	/**
	 * metoda na validovanie dat z editora, vola metodu validateEditor nejprv ak existuje tak z child objektu, ak nie tak z tohto
	 * validateEditor sa vola pre kazdy objekt v requeste.
	 * @param request
	 * @param binder
	 * @throws IllegalAccessException
	 */
	@InitBinder
	protected void initBinder(HttpServletRequest request, WebDataBinder binder)
	{
		String requestURI = request.getRequestURI();
		if (requestURI.endsWith("/editor")) {
			@SuppressWarnings("unchecked")
			DatatableRequest<Long, T> target = (DatatableRequest<Long, T>) binder.getTarget();
			if (target != null) {
				//clear thread data
				getThreadData().setInvalidImportedRows(null);
				getThreadData().setInvalidImportedRowsErrors(null);
				getThreadData().clearNotifyList();

				Map<Long, T> data = target.getData();
				BindingResult bindingResult = binder.getBindingResult();
				Identity currentUser = UsersDB.getCurrentUser(request);
				if (data != null) {
					Set<Long> invalidImportedRows = new HashSet<>();
					setSkipWrongData( target.isSkipWrongData() );
					for (Map.Entry<Long, T> galleryEntityEntry : data.entrySet()) {
						Long key = galleryEntityEntry.getKey();
						T value = galleryEntityEntry.getValue();

						//zial, nefunguje inak nastavovanie errorov ako na konkretny field, takze to fejkujeme takymto objektom
						//moze to robit haluze pri editacii viacerych objektov naraz, u nas ale pouzivame len spolocne atributy, takze by to mohlo fungovat aj tam
						target.setErrorField(value);

						if (target.getDztotalchunkcount()>0) {
							setImporting(true);
							if (target.getDzchunkindex()>0) setLastImportedRow(target.getDzchunkindex()*Constants.getInt("chunksQuantity"));
							else setLastImportedRow(null);
						} else {
							setImporting(false);
						}

						setImportedColumns(target.getImportedColumns());

						//Use separe binding result
						BeanPropertyBindingResult entityBindingResult = new BeanPropertyBindingResult(binder.getTarget(), binder.getObjectName());
						validateEditor(request, target, currentUser, entityBindingResult, key, value);

						//If we DON'T WANT skip wrong data, push error back into main binding result
						if(isSkipWrongData() == false) {
							bindingResult.addAllErrors(entityBindingResult);
						} else {
							//We skipped wrong data, but use errors for user notification
							if(entityBindingResult.getErrorCount() > 0) {
								invalidImportedRows.add(key);

								addImportedColumnError( entityBindingResult.getFieldErrors().get(0), key.intValue() );
							}
						}
					}

					//Set which rows are invalid
					if(isSkipWrongData() == true) {
						setInvalidImportedRows(invalidImportedRows);
					}
				}

				if (bindingResult.hasFieldErrors()) {
					//vyhod este globalnu error hlasku, aby sa zobrazila aj pri tlacitkach a user si preklikal taby na konkretne chyby
					bindingResult.addError(new ObjectError("global", Prop.getInstance(request).getText("datatable.error.fieldErrorMessage")));
				}
			}
		}
	}


    private static java.lang.reflect.Field getDeclaredFiledRecursive(Class<?> initialClass, String fieldName) throws NoSuchFieldException {
        java.lang.reflect.Field field = null;
        int failsafe=0;
        Class<?> targetClass = initialClass;
        while (targetClass != null && failsafe++<15) {
            try {
                field = targetClass.getDeclaredField(fieldName);
                if(field != null) return field;
            } catch (NoSuchFieldException e) {}
            // Field not found in current class, continue to superclass
            targetClass = targetClass.getSuperclass();
        }

       throw new NoSuchFieldException("Field " + fieldName + " not found in class " + initialClass + " or in super classes");
    }

	private static boolean isFieldType(Class<?> initialClass, String fieldNam, DataTableColumnType type) {
		boolean isProvidedType = false;
		try {
			java.lang.reflect.Field field = getDeclaredFiledRecursive(initialClass, fieldNam);
			if (field.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class)) {
				DataTableColumnType[] inputType = field.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class).inputType();

				//Check if field inputType is equal with provided inputType
				if(inputType != null && inputType.length > 0)
					isProvidedType = inputType[0].equals(type);
			}
		} catch(Exception e) {
			//Do nothing
		}

		return isProvidedType;
	}

	/**
	 * Vytvori zoznam predikatov pre vyhladavanie
	 * @param properties - ocisteny zoznam params o atributy, ktore sa nechachadzaju v T
	 * @param params - kompletny zoznam request parametrov, vratane pagingu
	 * @param entity - entita, ktora sa ma hladat
	 * @return
	 */
	protected Specification<T> getSearchConditions(Map<String, String> properties, Map<String, String> params, T entity) {
		return (Specification<T>) (root, query, builder) -> {
			final List<Predicate> predicates = new ArrayList<>();

			for (Map.Entry<String, String> paramsEntry : properties.entrySet()) {
				String field = paramsEntry.getKey();
				String value = paramsEntry.getValue();

				//toto sa hlada v addSpecSearch
				if ("perexGroups".equals(field)) continue;

				if (value.startsWith("daterange:")) {
					Timestamp from = null;
					Timestamp to = null;
					String[] values = Tools.getTokens(value.substring(value.indexOf(":")+1), "-");
					if (values.length==2) {
						from = new Timestamp(Tools.getLongValue(values[0], 0));
						to = new Timestamp(Tools.getLongValue(values[1], 0));
					} else if (values.length==1) {
						//ked nemame from pride to ako: daterange:-1589666400000
						if (value.contains("range:-")) to = new Timestamp(Tools.getLongValue(values[0], 0));
						else from = new Timestamp(Tools.getLongValue(values[0], 0));
					}

					//Ak sa jedna o DATETIME, ta žiadnu úpravu nespravíme (používateľ nech si časovú zložku nastaví sám)
					//Ak sa jedná o DATE, tak nastavíme časovú zložku FROM na 00:00:00 a TO na 23:59:59
					boolean isDate = isFieldType(entity.getClass(), field, DataTableColumnType.DATE);

					if(isDate && from != null) {
						Calendar cal = Calendar.getInstance();
						cal.setTime(from);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						from = new Timestamp( cal.getTimeInMillis() );
					}

					if(isDate && to != null) {
						Calendar cal = Calendar.getInstance();
						cal.setTime(to);
						//set to begining of next day because we will use lessThan
						cal.add(Calendar.DATE, 1);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						to = new Timestamp( cal.getTimeInMillis() );
					}

					sk.iway.iwcm.Logger.debug(DatatableRestControllerV2.class, "Daterange from="+Tools.formatDateTimeSeconds(from)+" to="+Tools.formatDateTimeSeconds(to)+" original="+value);

					if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get(field), from));
					if (to != null) predicates.add(builder.lessThan(root.get(field), to));
				} else if (value.startsWith("range:")) {
					BigDecimal from = null;
					BigDecimal to = null;
					String[] values = Tools.getTokens(value.substring(value.indexOf(":")+1), "-");
					if (values.length==2) {
						from = Tools.getBigDecimalValue(values[0], "0");
						to = Tools.getBigDecimalValue(values[1], "0");
					} else if (values.length==1) {
						//ked nemame from pride to ako: daterange:-1589666400000
						if (value.contains("range:-")) to = Tools.getBigDecimalValue(values[0], "0");
						else from = Tools.getBigDecimalValue(values[0], "0");
					}

					sk.iway.iwcm.Logger.debug(DatatableRestControllerV2.class, "Range from="+from+" to="+to+" original="+value);

					if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get(field), from));
					if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get(field), to));
				} else {
					try {
						//skus ziskat field, ak to padne na IllegalArgumentException tak neexistuje, nevadi, ignorujeme
						@SuppressWarnings("rawtypes")
						Path path = root.get(field);

						//toto nefunguje dobre
						//path.getJavaType().isInstance(Boolean.class) - aj ked je Boolean vrati false
						//toto funguje
						//path.getJavaType().isAssignableFrom(Boolean.class)

						String simpleName = path.getJavaType().getSimpleName();

						if ("null".equals(value)) {
							predicates.add(builder.isNull(root.get(field)));
						} else if (simpleName.equalsIgnoreCase("Boolean")) {
							predicates.add(builder.equal(root.get(field), Boolean.valueOf(value)));
						} else if (simpleName.equalsIgnoreCase("Integer") || simpleName.equalsIgnoreCase("int")) {
							predicates.add(builder.equal(root.get(field), Integer.valueOf(value)));
						} else if (simpleName.equalsIgnoreCase("Long")) {
							predicates.add(builder.equal(root.get(field), Long.valueOf(value)));
						} else if (simpleName.equalsIgnoreCase("Double")) {
							predicates.add(builder.equal(root.get(field), Double.valueOf(value)));
						} else {

							if (value.startsWith("^") && value.endsWith("$")) predicates.add(builder.equal(root.get(field), value.substring(1, value.length()-1)));
							else {
								if (value.startsWith("^")) value = value.substring(1)+"%";
								else if (value.endsWith("$")) value = "%"+value.substring(0, value.length()-1);
								else value = "%"+value+"%";

								if (Constants.DB_TYPE==Constants.DB_ORACLE && isJpaLowerField(field)) {
									predicates.add(builder.like(builder.lower(root.get(field)), value.toLowerCase()));
								} else if (Constants.DB_TYPE==Constants.DB_PGSQL) {
									predicates.add(builder.like(builder.lower(builder.function("unaccent", String.class, root.get(field))), DB.internationalToEnglish(value).toLowerCase()));
								} else {
									predicates.add(builder.like(root.get(field), value));
								}
							}
						}
					} catch (IllegalArgumentException e) {
						//failsafe
					}
				}
			}

			//pridaj do vyhladavania automaticky podmienku podla domain_id ak je potrebna
			if (checkDomainId) predicates.add(builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()));

			addSpecSearch(params, predicates, root, builder);

			return builder.and(predicates.toArray(new Predicate[predicates.size()]));
		};
	}

	/**
	 * Doplnenie pecialneho vyhladavanie, interne vola:
	 * - addSpecSearchUserFullName(searchUserFullName, "userId", predicates, root, builder);
	 * @param params
	 * @param predicates
	 */
	public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {

		//v DB entite mame bezne len userId a pridavame tam entitu userFullName, defaultne ked existuje parameter searchUserFullName tak hladaj podla userId
		String searchUserFullName = params.get("searchUserFullName");
		if (Tools.isNotEmpty(searchUserFullName)) {
			SpecSearch<T> specSearch = new SpecSearch<>();
			//ziskaj zoznam IDecok userov, ktory maju dane meno
			specSearch.addSpecSearchUserFullName(searchUserFullName, "userId", predicates, root, builder);

		}

		//vyhladavanie na zaklade stavu
		String statusSearch = params.get("searchEditorFields.statusIcons");
		if (Tools.isNotEmpty(statusSearch)) {
			SpecSearch<T> specSearch = new SpecSearch<>();
			specSearch.addSpecSearchStatusIcons(statusSearch, predicates, root, builder);
		}

		//vyhladavanie podla perexSkupiny
		String searchPerexGroups = params.get("searchPerexGroups");
		if (Tools.isNotEmpty(searchPerexGroups)) {
			SpecSearch<T> specSearch = new SpecSearch<>();
			//ziskaj zoznam IDecok userov, ktory maju dane meno
			specSearch.addSpecSearchPerexGroup(searchPerexGroups, "perexGroups", predicates, root, builder);

		}
	}

	/**
	 * You can create new Pageable object where you can add custom sorting
	 * @param params
	 * @param pageable
	 */
	public Pageable addSpecSort(Map<String, String> params, Pageable pageable) {
		return pageable;
	}

	/********************************* REST METODY *********************************/

	/**
	 * Vrati vsetky zaznamy v datatabaze (serverovo strankovane a sortovane)
	 */
	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@GetMapping("/all")
	public Page<T> getAll(Pageable pageable) {
		clearThreadData();
		if ("true".equals(request.getParameter("export"))) {
			setExporting(true);
			Adminlog.add(Adminlog.TYPE_FORM_EXPORT, request.getRequestURI(),-1, -1);
		}
		else {
			setExporting(false);
		}

		pageable = addSpecSort(getParamsMap(getRequest()), pageable);

		Page<T> page = this.getAllItems(pageable);

		//napln options
		DatatablePageImpl<T> pageImpl;
		if (page instanceof DatatablePageImpl) {
			//uz je to impl, moze mat nejake options uz setnute
			pageImpl = (DatatablePageImpl<T>)page;
		} else {
			pageImpl = new DatatablePageImpl<>(page);
		}
		this.getOptions(pageImpl);

		pageImpl.setNotify(getThreadData().getNotify());

		return pageImpl;
	}

	/**
	 * Vyhlada zaznamy v databaze podla zadanych kriterii (serverovo strankovane a sortovane).
	 * Pouziva EampleMatcher, v Beane NESMU BYT pouzite primitivne typy (vsetko musia byt Objekty)
	 */
	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@GetMapping("/search/findByColumns")
	public Page<T> findByColumns(@RequestParam Map<String, String> params, Pageable pageable, T search) {
		clearThreadData();

		//fix multiple sort - replace params single value with multi value from request
		if (getRequest()!=null && getRequest().getParameter("sort")!=null) {
			String[] sort = getRequest().getParameterValues("sort");
			if (sort.length > 1) {
				params.put("sort", String.join("\n", sort));
			}
		}

		pageable = addSpecSort(params, pageable);

		if ("true".equals(request.getParameter("export"))) {
			setExporting(true);
			Adminlog.add(Adminlog.TYPE_FORM_EXPORT, request.getRequestURI(),-1, -1);
		}
		else {
			setExporting(false);
		}
		return searchItem(params, pageable, search);
	}

	/**
	 * Edit import data. For example, set id to -1 if you want to change update to create.
	 *
	 * @param request
	 * @param data
	 * @param importMode
	 * @return
	 */
	public Map<Long, T> preImportDataEdit(HttpServletRequest request, Map<Long, T> data, String importMode) {
		return data;
	}

	/**
	 * Ulozenie zaznamu do DB vo formate posielanom Datatables Editor, moze naraz zapisat viac zaznamov
	 */
	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@PostMapping(value = "/editor", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DatatableResponse<T>> handleEditor(HttpServletRequest request, @RequestBody DatatableRequest<Long, T> datatableRequest) {
		boolean isImporting = isImporting();
		Integer lastImportedRow = getLastImportedRow();
		List<NotifyBean> notifyListBeforeClear = getThreadData().getNotify();

		//SKIP wrong data support variables
		boolean skipWrongData = datatableRequest.isSkipWrongData();
		Set<Long> invalidImportedRows = getInvalidImportedRows();
		TreeMap<Integer, String> invalidImportedRowsErrors = getThreadData().getInvalidImportedRowsErrors();

		clearThreadData();
		if (isImporting) {
			setImporting(true);
			setSkipWrongData(skipWrongData);
			//pri importe moze vykonat converter nastavenie nejakych notifikacii, pre istotu takto zachovame
			if (notifyListBeforeClear!=null && notifyListBeforeClear.isEmpty()==false) addNotify(notifyListBeforeClear);
		}

		DatatableResponse<T> response = new DatatableResponse<>();

		if (datatableRequest.isDeleteOldData()) {
			//je potrebne sa zamysliet nad bezpecnostou, zatial schovane aj v UI
			//repo.deleteAll();
		}

		setForceReload(false);
		setImportedColumns(datatableRequest.getImportedColumns());

		String updateByColumn = datatableRequest.getUpdateByColumn();
		getThreadData().setUpdateByColumn(updateByColumn);

		String importMode = datatableRequest.getImportMode();
		getThreadData().setImportMode(importMode);

		if(isImporting == true) datatableRequest.setData( preImportDataEdit(request, datatableRequest.getData(), importMode) );

		int rowCounter = 0;
		if (isImporting && lastImportedRow!=null) rowCounter = lastImportedRow.intValue();
		for (Long id : datatableRequest.getData().keySet()) {
			rowCounter++;

			//This row was marked as invalid, skip it
			if(isImporting() && skipWrongData == true && invalidImportedRows.contains(id)) {
				//Mark row
				setLastImportedRow(rowCounter);
				continue;
			}

			T entity = datatableRequest.getData().get(id);

			if (entity instanceof ActiveRecordBase) {
				Integer rowNum = ((ActiveRecordBase)entity).get__rowNum__();
				setLastImportedRow(rowNum);
			} else {
				if (isImporting()) setLastImportedRow(rowCounter);
				else setLastImportedRow(null);
			}

			//tu nepouzijeme podmienku checkDomainId, aby sa domainId nastavilo vzdy a nezostalo NULL/0 aj ked je aktualne enableStaticFilesExternalDir vypnute (napr. na produkcii)
			if (repo instanceof DomainIdRepository) {
				//over, ci entita ma property domainId a ci sedi voci aktualnemu CloudToolsForCore.getDomainId()
				BeanWrapperImpl bw = new BeanWrapperImpl(entity);
				Integer domainId = (Integer)bw.getPropertyValue("domainId");
				if (domainId == null || domainId.intValue()<1 || datatableRequest.isInsert()) {
					//domainId nie je nastavene, setni na aktualnu hodnotu
					domainId = CloudToolsForCore.getDomainId();
					bw.setPropertyValue("domainId", domainId);
				} else {
					if (CloudToolsForCore.getDomainId() != domainId.intValue()) {
						//domainId nesedi, je to nejaka manipulacia s datami, vyhod chybu
						throwError("datatables.error.domainId");
					}
				}
			}

			boolean isDuplicate = false;
			if (datatableRequest.isInsert()) {
				try {
					if (id>0) {
						//jedna sa o duplikovanie, musime zrusit hodnotu ID property
						String propertyName = "id";
						for (Field field : entity.getClass().getDeclaredFields()) {
							if (field.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class)) {
								DataTableColumnType[] inputType = field.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class).inputType();
								if (inputType.length>0 && inputType[0]==DataTableColumnType.ID) {
									propertyName = field.getName();
									break;
								}
							}
						}

						Long lnull = null;
						Long inull = null;
						try {
							//musime ist takto, pretoze na lombok triedach BeanUtils nic nespravi
							String methodName = "set"+propertyName.substring(0,1).toUpperCase()+propertyName.substring(1);
							Method setId = entity.getClass().getMethod(methodName, Long.class);
							setId.invoke(entity, lnull);
						} catch (Exception e) {
							try {
								//na starych WJ triedach je potrebne nastavit integer hodnotu, napr. setTempId(0)
								String methodName = "set"+propertyName.substring(0,1).toUpperCase()+propertyName.substring(1);
								Method setId = entity.getClass().getMethod(methodName, Integer.class);
								setId.invoke(entity, 0);
							} catch (Exception e2) {
								try {
									//na starych WJ triedach je potrebne nastavit int hodnotu, napr. setTempId(0)
									String methodName = "set"+propertyName.substring(0,1).toUpperCase()+propertyName.substring(1);
									Method setId = entity.getClass().getMethod(methodName, int.class);
									setId.invoke(entity, 0);
								} catch (Exception e21) {
									BeanWrapperImpl bw = new BeanWrapperImpl(entity);
									//setni ID hodnotu na povodnej entite, aby sa nasledne korektne vykonala processToEntity so spravnym ID
									try {
										bw.setPropertyValue(propertyName, lnull);
									} catch (Exception e3) {
										try {
											bw.setPropertyValue(propertyName, inull);
										} catch (Exception e4) {
											try {
												bw.setPropertyValue(propertyName, 0);
											} catch (Exception e5) {

											}
										}
									}
								}
							}
						}

						isDuplicate = true;
						beforeDuplicate(entity);
					}

				} catch (Exception ex) {
					Logger.error(DatatableRestControllerV2.class, ex);
					throwError("datatables.error.system.js");
				}

				if (isImporting && "onlyNew".equals(importMode) && Tools.isNotEmpty(updateByColumn)) {
					try {
						List<T> itemsBy = findItemBy(updateByColumn, entity);
						if (itemsBy.isEmpty()==false) {
							//SKIP import, entity allready exists
							Logger.debug(DatatableRestControllerV2.class, "import SKIP entity - allready exists, entity="+entity+", "+updateByColumn+"="+updateByColumn);
							response.setForceReload(Boolean.TRUE);
							continue;
						}
					} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
						response.setError(String.format("Field: %s not found", updateByColumn));
						Logger.error(DatatableRestControllerV2.class, e);
						return ResponseEntity.ok(response);
					} catch(RuntimeException ex) {
						//Ignore error if skipWrongData is true
						if(skipWrongData == true) {
							addImportedColumnError(ex);
							continue;
						}
						throw ex;
					}
				}

				try {
					ResponseEntity<T> re = add(entity); //This method throws ConstraintViolationException
					response.add(re.getBody());

					if (isDuplicate) afterDuplicate(entity, id);
				} catch (ConstraintViolationException ex) {
					//Ignore error if skipWrongData is true
					if(skipWrongData == true) {
						List<ConstraintViolation<?>> violations = new ArrayList<>();
						for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
							violations.add(violation);
						}
						addImportedColumnError(violations);
						continue;
					}
					throw ex;
				} catch(RuntimeException ex) {
					//Ignore error if skipWrongData is true
					if(skipWrongData == true) {
						addImportedColumnError(ex);
						continue;
					}
					throw ex;
				}

			} else if (datatableRequest.isUpdate()) {

				if (isImporting) {
					setImporting(true);
				}

				ResponseEntity<T> re=null;
				// Ak updatujeme na zaklade stlpca v DB
				if (Tools.isNotEmpty(updateByColumn)) {
					try {
						response.setData(editItemByColumn(entity, updateByColumn));
					} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
						response.setError(String.format("Field: %s not found", updateByColumn));
						Logger.error(DatatableRestControllerV2.class, e);
						return ResponseEntity.ok(response);
					} catch(RuntimeException ex) {
						//Ignore error if skipWrongData is true
						if(skipWrongData == true) {
							addImportedColumnError(ex);
							continue;
						}
						throw ex;
					}
				}
				else {
					re = edit(id, entity);
					response.add(re.getBody());
				}
			} else if (datatableRequest.isDelete()) {

				ResponseEntity<Map<String, Object>> re = delete(id, entity);

				//delete(id, datatableRequest.getData().get(id));
				Map<String, Object> body = re.getBody();
				if (body == null || body.get("result") == null || body.get("result").equals(Boolean.TRUE)==false) {
					throwError("editor.delete_error");
				}
			}
		}

		//We skipped worng data, prepare and show errors notification
		if(skipWrongData == true) {

			if(invalidImportedRowsErrors == null) {
				invalidImportedRowsErrors = new TreeMap<>();
			}

			if(getThreadData().getInvalidImportedRowsErrors() != null) {
				invalidImportedRowsErrors.putAll( getThreadData().getInvalidImportedRowsErrors() );
			}

			if(invalidImportedRowsErrors.size() > 0) {
				StringBuilder allInsertErrors = new StringBuilder("");

				for (Map.Entry<Integer, String> set : invalidImportedRowsErrors.entrySet()) {
					allInsertErrors.append(set.getValue()).append("<br> <br>");
				}

				NotifyBean error = new NotifyBean(Prop.getInstance().getText("datatables.error.title.js"), allInsertErrors.toString(), NotifyType.ERROR);
				getThreadData().addNotify(error);
			}
		}

		//!! CLear SKIP wrong data support variables
		getThreadData().setInvalidImportedRows(null);
		getThreadData().setInvalidImportedRowsErrors(null);

		if (isForceReload()) {
			response.setForceReload(Boolean.TRUE);
		}

		if (isImporting) {
			afterImportChunk(datatableRequest.getDzchunkindex(), datatableRequest.getDztotalchunkcount());
		}

		//If thread notify list != null, set list into response
		if(hasNotify()) response.setNotify(getThreadData().getNotify());

		if (datatableRequest.getData().size()>5) {
			//aby nenastala chyba 429 pri importe musime spomalit download
			MultipartWrapper.slowdownUpload();
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Volanie specialnej akcie (napr. otocenie obrazku v galerii).
	 * V pug subore sa vola ako galleryTable.executeAction("rotate");
	 * @param action
	 * @param ids
	 * @return
	 */
	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@PostMapping(value = "/action/{action}")
	public ResponseEntity<DatatableResponse<T>> action(@PathVariable String action, @RequestParam(value = "ids[]") Long[] ids) {
		clearThreadData();
		DatatableResponse<T> response = new DatatableResponse<>();

		for (Long id : ids) {
			Logger.debug(DatatableRestControllerV2.class, "action=" + action + ", id=" + id);

			T entity = null;
			//id==-1 je v situacii ked sa nic neselectne, napr. pre refresh akciu
			if (id != -1) entity = getOneItem(id);
			if (entity != null || id==-1) {
				checkItemPermsThrows(entity, id);
				boolean success = processAction(entity, action);
				if (success == false) {
					response.setError(getProp().getText("datatable.error.unknown") + ": id=" + id);
				}
			}
		}

		//If thread notify list != null, set list into response
		if(hasNotify()) response.setNotify(getThreadData().getNotify());
		response.setForceReload(isForceReload());

		return ResponseEntity.ok(response);
	}

	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@GetMapping("/{id}")
	public T getOne(@PathVariable("id") long id) {
		clearThreadData();
		T result = getOneItem(id);
		checkItemPermsThrows(result, id);
		addNotifyToEditorFields(result);
		return result;
	}

	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@PostMapping("/add")
	public ResponseEntity<T> add(@Valid @RequestBody T entity) {
		beforeSave(entity);

		// validacia
		Set<ConstraintViolation<T>> violations = validator.validate(entity);

		//Error will be always thrown, but prepare error message for user is we skipping wrong data
		if (!violations.isEmpty()) {
			//convert violations to List<ConstraintViolation<?>>
			List<ConstraintViolation<?>> violationsList = new ArrayList<>();
			for (ConstraintViolation<T> violation : violations) {
				violationsList.add(violation);
			}
			addImportedColumnError(violationsList);
			throw new ConstraintViolationException("Invalid data", violations);
		} else {
			checkItemPermsThrows(entity, -1L);
			T newT = this.insertItem(entity);
			afterSave(entity, newT);
			return ResponseEntity.status(HttpStatus.CREATED).body(newT);
		}
	}

	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@PostMapping("/edit/{id}")
	public ResponseEntity<T> edit(@PathVariable("id") long id, @Valid @RequestBody T entity) {
		beforeSave(entity);

		// validacia
		Set<ConstraintViolation<T>> violations = validator.validate(entity);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException("Invalid data", violations);
		} else {
			checkItemPermsThrows(entity, id);
			T one = this.editItem(entity, id);
			afterSave(entity, one);
			return ResponseEntity.ok(one);
		}
	}

	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") long id, @RequestBody T entity) {
		clearThreadData();
		Map<String, Object> result = new HashMap<>();
		checkItemPermsThrows(entity, id);

		boolean deleted = false;
		if (beforeDelete(entity)) {
			deleted = this.deleteItem(entity, id);
		}
		result.put("result", deleted);
		if (deleted) {
			afterDelete(entity, id);
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
	 * Return SUM values of columns belonging to entity declared in parameter columns[].
	 * <p>
	 * If column do not exist for this entity, OR column is not subclass of Number, then empty string for column is returned.
	 * @param entity
	 * @param columns string arraya of column names to sum (aka entity field names), they must be numerical types (subclass of Number)
	 * @return
	 */
	@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
	@GetMapping("/sumAll")
	public String getSum(T entity, @RequestParam(value = "columns[]") String[] columns) {

		clearThreadData();

		JSONObject output = new JSONObject();

		//Get class
		try {
			Class<?> clazz = entity.getClass();
			String tableName;

			if (clazz.isAnnotationPresent(Table.class)) {
				tableName = clazz.getAnnotation(Table.class).name();
			} else {
				//Cant call SimpleQuery without table name
				return output.toString();
			}

			String whereClause = "";
			boolean hasDomainIdField = Arrays.stream(clazz.getDeclaredFields())
				.anyMatch(field -> field.getName().equals("domainId"));
			if (hasDomainIdField) {
				whereClause = " WHERE domain_id = " + CloudToolsForCore.getDomainId();
			}

			// Get all valid column names from the entity class, because columns[] is unsafe input/parameter
			Set<String> validColumns = new HashSet<>();
			for (Field field : clazz.getDeclaredFields()) {
				if (Number.class.isAssignableFrom(field.getType())) {
					validColumns.add(field.getName());
				}
			}

			//iterate over parameters and get sum for each column
			for(String column : columns) {
				if (validColumns.contains(column)) {
					//Ok, its numerical type
					output.put(column, new SimpleQuery().forNumber("SELECT SUM(" + DB.removeSlashes(column) + ") FROM " + DB.removeSlashes(tableName) + whereClause));
				} else {
					//Field is not numerical type, set empty string
					output.put(column, "");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();
	}

	public JpaRepository<T, Long> getRepo() {
		return repo;
	}

	public HttpServletRequest getRequest(){
		return this.request;
	}

	public Identity getUser(){
		return UsersDB.getCurrentUser(getRequest());
	}

	public Prop getProp() {
		//kedze nie sme thread safe vraciame takto, request je autowired, cize ten je OK
		return Prop.getInstance(getRequest());
	}

	/**
	 * Vyvola vseobecnu vynimku ulozenia (ked napr. v editItem nastane nejaka vseobecna chyba)
	 * Chybove hlasenie sa zobrazi v editore pri tlacitku odoslat
	 * @param errorKey
	 */
	@SuppressWarnings("all")
	public void throwError(String errorKey) {
		throwError(errorKey, false);
	}

	/**
	 * Vyvola vseobecnu vynimku ulozenia (ked napr. v editItem nastane nejaka vseobecna chyba)
	 * Chybove hlasenie sa zobrazi v editore pri tlacitku odoslat
	 * @param errorKey
	 * @param showNotifications - zobrazi notifikacie z vlakna
	 */
	@SuppressWarnings("all")
	public void throwError(String errorKey, boolean showNotifications) {
		String message = getProp().getText(errorKey);

		if(showNotifications) {
			throw new EditorException(message, getThreadData().getNotify());
		} else {
			throw new RuntimeException(message);
		}
	}

	/**
	 * Vyvola vseobecnu vynimku ulozenia (ked napr. v editItem nastane nejaka vseobecna chyba)
	 * Chybove hlasenie sa zobrazi v editore pri tlacitku odoslat
	 * @param errorKey
	 * @param params - parametre pre preklad
	 */
	@SuppressWarnings("all")
	public void throwError(String errorKey, String... params) {
		//no notification
		throwError(errorKey, false, params);
	}

	/**
	 * Vyvola vseobecnu vynimku ulozenia (ked napr. v editItem nastane nejaka vseobecna chyba)
	 * Chybove hlasenie sa zobrazi v editore pri tlacitku odoslat
	 * @param errorKey
	 * @param params - parametre pre preklad
	 * @param showNotifications - zobrazi notifikacie z vlakna
	 */
	@SuppressWarnings("all")
	public void throwError(String errorKey, boolean showNotifications, String... params) {
		String message = getProp().getTextWithParams(errorKey, params);

		if(showNotifications) {
			throw new EditorException(message, getThreadData().getNotify());
		} else {
			throw new RuntimeException(message);
		}
	}

	/**
	 * Vyvola vynimku platnosti typu pola (napr. kontrola email adresy)
	 * @param errorKey - prekladovy kluc chybovej spravy
	 */
	@SuppressWarnings("all")
	public void throwConstraintViolation(String errorKey) {
		String message = getProp().getText(errorKey);
		throw new ConstraintViolationException(message, null);
	}

	/**
	 * Vyvola vseobecnu vynimku ulozenia (ked napr. v editItem nastane nejaka vseobecna chyba)
	 * Chybove hlasenie sa zobrazi v editore pri tlacitku odoslat
	 * @param errorKeys
	 */
	@SuppressWarnings("all")
	public void throwError(List<String> errorKeys) {
		throwError(errorKeys, false);
	}

	/**
	 * Vyvola vseobecnu vynimku ulozenia (ked napr. v editItem nastane nejaka vseobecna chyba)
	 * Chybove hlasenie sa zobrazi v editore pri tlacitku odoslat
	 * @param errorKeys
	 * @param showNotifications - zobrazi notifikacie z vlakna
	 */
	@SuppressWarnings("all")
	public void throwError(List<String> errorKeys, boolean showNotifications) {
		//preved z klucov na texy
		Prop prop = Prop.getInstance();
		StringBuilder message = new StringBuilder();
		for (String key : errorKeys) {
			if (message.length()>0) message.append(";\n");
			message.append(prop.getText(key));
		}

		if(showNotifications) {
			throw new EditorException(message.toString(), getThreadData().getNotify());
		} else {
			throw new RuntimeException(message.toString());
		}
	}

	private static ThreadBean getThreadData() {
		ThreadBean data = threadData.get();
		if (data == null) {
			Logger.debug(DatatableRestControllerV2.class, "ThreadData.creating, id="+Thread.currentThread().getId());
			data = new ThreadBean();
			threadData.set(data);
		}
		//Logger.debug(DatatableRestControllerV2.class, "ThreadData.get, id="+Thread.currentThread().getId()+" data="+data.toString());
		return data;
	}

	private void clearThreadData() {
		getThreadData().clear();
	}

	/**
	 * Indikuje, ze dane volanie je pre export dat
	 * @return
	 */
	public boolean isExporting() {
		boolean exporting = getThreadData().isExporting();
		//Logger.debug(DatatableRestControllerV2.class, "isExporting, thread="+Thread.currentThread().getId()+" exporting="+exporting);
		return exporting;
	}

	private void setExporting(boolean exporting) {
		getThreadData().setExporting(exporting);
	}

	/**
	 * Indikuje, ze dane volanie je pre import dat
	 * @return
	 */
	public boolean isImporting() {
		return getThreadData().isImporting();
	}

	private void setImporting(boolean importing) {
		getThreadData().setImporting(importing);
	}

	/**
	 * Indikuje, ze sa ma vykonat reload tabulky
	 * @return
	 */
	public boolean isForceReload() {
		return getThreadData().isForceReload();
	}

	public void setForceReload(boolean forceReload) {
		getThreadData().setForceReload(forceReload);
	}

	public boolean hasNotify() {
		return getThreadData().getNotify() != null ? true : false;
	}

	/**
	 * Prida notifikaciu pre zobrazenie po odoslani dat
	 * @param notify
	 */
	public static void addNotify(NotifyBean notify) {
		getThreadData().addNotify(notify);
	}

	/**
	 * Prida zoznam notifikacii pre zobrazenie po odoslani dat
	 * @param notifyList
	 */
	public static void addNotify(List<NotifyBean> notifyList) {
		if (notifyList != null && notifyList.isEmpty()==false) {
			for (NotifyBean notify : notifyList) {
				getThreadData().addNotify(notify);
			}
		}
	}

	/**
	 * Nastavi cislo importovaneho riadku (ak sa nachadza v datach)
	 * @param lastImportedRow
	 * @return
	 */
	private void setLastImportedRow(Integer lastImportedRow) {
		getThreadData().setLastImportedRow(lastImportedRow);
	}

	/**
	 * Vrati cislo posledne importovaneho riadku
	 * @return
	 */
	public static Integer getLastImportedRow() {
		return getThreadData().getLastImportedRow();
	}

	private void setImportedColumns(Set<String> importedColumns) {
		getThreadData().setImportedColumns(importedColumns);
	}

	/**
	 * Returns Set<String> of imported columns from xlsx file.
	 * You can check which columns were in Excel file during import process.
	 * @return
	 */
	public static Set<String> getImportedColumns() {
		return getThreadData().getImportedColumns();
	}

	/**
	 * Vrati repo pretypovane na DomainIdRepository pre jednoduchsie pouzitie
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private DomainIdRepository<T, ID> getDomainRepo() {
		if (repo instanceof DomainIdRepository) {
			return (DomainIdRepository<T, ID>)repo;
		}
		return null;
	}

	/**
	 * Prida notify list do BaseEditorFields objektu (ak existuje)
	 * Toto je potrebne pri REST volaniach, kedy sa posiela nazad len zakladna entita
	 * @param entity
	 */
	private void addNotifyToEditorFields(T entity) {
		try {
			List<NotifyBean> notifyList = getThreadData().getNotify();
			if (notifyList!=null && notifyList.isEmpty()==false) {
				BeanWrapperImpl bw = new BeanWrapperImpl(entity);
				if (bw.getPropertyType("editorFields.notify")!=null) {
					bw.setPropertyValue("editorFields.notify", getThreadData().getNotify());
				}
			}
		} catch (Exception e) {
			Logger.error(DatatableRestControllerV2.class, e);
		}
	}

	/**
	 * There is problem to return single Boolean from JPA query,
	 * it's returned as Boolean in MariaDB/MSSQL and Number (0 or 1) on Oracle
	 * there we cast it correctly to boolean
	 * @param value
	 * @return
	 */
	public static boolean jpaToBoolean(Object value) {
		if (value == null) return false;
		if (value instanceof Boolean) return (Boolean)value;
		if (value instanceof Number) {
			return ((Number)value).intValue() == 1 ? true : false;
		}
		return false;
	}

	/**
	 * Test if field value should be lower cased
	 * @param field
	 * @return
	 */
	private boolean isJpaLowerField(String field)
    {
		String[] jpaToLowerFields = ConstantsV9.getArrayCached("jpaToLowerFields", 120);
        if (jpaToLowerFields==null || jpaToLowerFields.length==0 || field==null) return false;

        for (String one : jpaToLowerFields)
        {
           if (one.equals(field)) return true;
		   //support for descriptionLong* so we don't need to write all language codes
		   if (one.endsWith("*") && one.substring(0, one.length()-1) .equals(field)) return true;
        }
        return false;
    }

	/**
	 * column name which is used to update the row with import
	 * @return
	 */
	public String getUpdateByColumn() {
		return getThreadData().getUpdateByColumn();
	}

	/**
	 * mode of import (append, update, onlyNew)
	 * @return
	 */
	public String getImportMode() {
		return getThreadData().getImportMode();
	}

	/**
	 * Set invalid imported rows
	 * @param invalidImportedRows
	 */
	private void setInvalidImportedRows(Set<Long> invalidImportedRows) {
		getThreadData().setInvalidImportedRows(invalidImportedRows);
	}

	/**
	 * Get invalid imported rows
	 * @return
	 */
	public Set<Long> getInvalidImportedRows() {
		return getThreadData().getInvalidImportedRows();
	}

	/**
	 * Set skip wrong data.
	 * TRUE - wrong data during import will be skipped and process will continue
	 * @param skipWrongData
	 */
	private void setSkipWrongData(boolean skipWrongData) {
		getThreadData().setSkipWrongData(skipWrongData);
	}

	/**
	 * Get skip wrong data.
	 * TRUE - wrong data during import will be skipped and process will continue
	 * @return
	 */
	public boolean isSkipWrongData() {
		return getThreadData().isSkipWrongData();
	}

	/**
	 * List of violated constraints during import (invalid rows during import) will be prepared and ADDED inside of threadData.InvalidImportedRowsErrors
	 * This set of processed error's are used for Warning notification (FOR user). So user can by notified which rows are invalid and WHY.
	 * @param violations - Set of ConstraintViolations
	 */
	private void addImportedColumnError(List<ConstraintViolation<?>> violations) {
		if(violations == null || violations.size() < 1) return;
		ConstraintViolation<?> firstViolation = violations.iterator().next();
		String propertyName = firstViolation.getPropertyPath().toString();
		int dot = propertyName.indexOf(".");
		if (dot > 0 && propertyName.startsWith("editorFields") == false) propertyName = propertyName.substring(0, dot);

		String errCause = firstViolation.getMessageTemplate();
		if(Tools.isNotEmpty(errCause) && errCause.startsWith("{") && errCause.endsWith("}")) {
			//For example {javax.validation.constraints.NotBlank.message}
			errCause = errCause.substring(1, errCause.length() - 1);
			errCause = Prop.getInstance().getText( errCause );
		} else {
			errCause = firstViolation.getMessage();
		}

		StringBuilder errExplanation = new StringBuilder(propertyName);
		int lastImportedRow = getLastImportedRow() == null ? 0 : getLastImportedRow().intValue();

		errExplanation.append(" - ").append(firstViolation.getInvalidValue() == null ? "EMPTY" : firstViolation.getInvalidValue().toString()).append(" - ").append(errCause);
		String errMsg = Prop.getInstance().getText("datatable.error.importRow", String.valueOf(lastImportedRow) , errExplanation.toString());

		TreeMap<Integer, String> rowsErrors = getThreadData().getInvalidImportedRowsErrors();
		if(rowsErrors == null) rowsErrors = new TreeMap<>();
		rowsErrors.put(lastImportedRow, errMsg);
		getThreadData().setInvalidImportedRowsErrors(rowsErrors);
	}

	private void addImportedColumnError(RuntimeException ex) {
		String errExplanation = ex.getMessage();
		int lastImportedRow = getLastImportedRow() == null ? 0 : getLastImportedRow().intValue();

		String errMsg = Prop.getInstance().getText("datatable.error.importRow", String.valueOf(lastImportedRow) , errExplanation);

		TreeMap<Integer, String> rowsErrors = getThreadData().getInvalidImportedRowsErrors();
		if(rowsErrors == null) rowsErrors = new TreeMap<>();
		rowsErrors.put(lastImportedRow, errMsg);
		getThreadData().setInvalidImportedRowsErrors(rowsErrors);
	}


	/**
	 * This error will be prepared and ADDED inside of threadData.InvalidImportedRowsErrors.
	 * This set of processed error's are used for Warning notification (FOR user). So user can by notified which rows are invalid and WHY.
	 * @param err - FieldError
	 * @param rowNumber - imported row number
	 */
	private void addImportedColumnError(org.springframework.validation.FieldError err, Integer rowNumber) {
		String propertyName = err.getField();
		if(propertyName.startsWith("errorField.")) propertyName = propertyName.replace("errorField.", "");
		int dot = propertyName.indexOf(".");
		if (dot > 0 && propertyName.startsWith("editorFields") == false) propertyName = propertyName.substring(0, dot);

		StringBuilder errExplanation = new StringBuilder();
		errExplanation.append(propertyName);
		errExplanation.append(" - ").append( err.getRejectedValue() == null ? "EMPTY" : err.getRejectedValue().toString() );
		errExplanation.append(" - ").append( err.getDefaultMessage() );

		int lastImportedRow = getLastImportedRow() == null ? 0 : getLastImportedRow().intValue();
		String errMsg = Prop.getInstance().getText("datatable.error.importRow", String.valueOf(lastImportedRow + rowNumber + 1) , errExplanation.toString());

		TreeMap<Integer, String> rowsErrors = getThreadData().getInvalidImportedRowsErrors();
		if(rowsErrors == null) rowsErrors = new TreeMap<>();
		rowsErrors.put(lastImportedRow + rowNumber + 1, errMsg);
		getThreadData().setInvalidImportedRowsErrors(rowsErrors);
	}

	/**
	 * Copy fields from provided entity into original entity
	 * @param entity
	 * @param one
	 */
	protected void copyEntityIntoOriginal(T entity, T one) {
		List<String> alwaysCopyProperties = new ArrayList<>();
		List<String> ignoreProperties = new ArrayList<>();

		Field[] declaredFields = AuditEntityListener.getDeclaredFieldsTwoLevels(entity.getClass());
		for (Field field : declaredFields) {
			if (field.isAnnotationPresent(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class)) {
				sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation = field.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class);
				boolean[] hiddenEditor = annotation.hiddenEditor();
				if (hiddenEditor.length > 0) {
					//ak je hiddenEditor preskoc
					if (hiddenEditor[0]==true) {
						ignoreProperties.add(field.getName());
						continue;
					}
				}

				//also skip if editor.attr.disabled=disabled
				DataTableColumnEditor editor[] = annotation.editor();
				if (editor.length > 0) {
					boolean isDisabled = false;
					DataTableColumnEditorAttr attrs[] = editor[0].attr();
					if (attrs.length > 0) {
						for (DataTableColumnEditorAttr attr : attrs) {
							if ("disabled".equals(attr.key())) {
								isDisabled = true;
								break;
							}
						}
					}
					//we allow updateDate, for others you can set alwaysCopyProperties=true in annotation
					if (isDisabled && field.getName().equals("lastUpdate")==false) {
						ignoreProperties.add(field.getName());
						continue;
					}
				}

				boolean alwaysCopy = false;
				if (annotation.alwaysCopyProperties().length>0) {
					alwaysCopy = annotation.alwaysCopyProperties()[0];
					//implicit false value
					if (alwaysCopy==false) continue;
				}
				if (alwaysCopy || field.getType().isAssignableFrom(Date.class) || field.getType().isAssignableFrom(java.sql.Date.class) || field.getType().isAssignableFrom(LocalDate.class) || field.getType().isAssignableFrom(LocalDateTime.class)) {
					//ak je to datum tak ho dajme do ignore, aby isiel zadat v GUI prazdny datum
					alwaysCopyProperties.add(field.getName());
				}
			}
		}

		NullAwareBeanUtils.copyProperties(entity, one, alwaysCopyProperties, ignoreProperties.toArray(new String[0]));
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}
}