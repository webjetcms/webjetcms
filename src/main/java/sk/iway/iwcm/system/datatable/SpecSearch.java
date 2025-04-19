package sk.iway.iwcm.system.datatable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UserDetails;

/**
 * Pomocne metody pre specialne/zlozite vyhladavanie v repozitaroch
 */
public class SpecSearch<T> {

    /**
     * Specialne vyhladavanie v password_protected stlpci, kde sa hlada zadany vyraz cez LIKE vo forme ID ID,% %,ID,% %,ID
     * cize vo vsetkych variantoch ako sa moze vyskytovat v ciarkou oddelenom zozname
     * @param userGroupName - MENO skupiny pouzivatelov
     * @param jpaProperty
     * @param predicates
     * @param root
     * @param builder
     */
    public void addSpecSearchPasswordProtected(String userGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
		addSpecSearchBySelect("SELECT DISTINCT user_group_id FROM user_groups WHERE user_group_name", userGroupName, jpaProperty, true, predicates, root, builder);
	}

    /**
     * Specialne vyhladavanie v password_protected stlpci, kde sa hlada zadany vyraz cez LIKE vo forme ID ID,% %,ID,% %,ID
     * cize vo vsetkych variantoch ako sa moze vyskytovat v ciarkou oddelenom zozname
     * @param userGroupId - ID skupiny pouzivatelov
     * @param jpaProperty
     * @param predicates
     * @param root
     * @param builder
     */
    public void addSpecSearchPasswordProtected(Integer userGroupId, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
        List<Predicate> idsList = new ArrayList<>();
        //hladame vsetky varianty ID ID, ,ID, ,ID
        idsList.add(builder.like(root.get(jpaProperty), String.valueOf(userGroupId)));
        idsList.add(builder.like(root.get(jpaProperty), String.valueOf(userGroupId)+",%"));
        idsList.add(builder.like(root.get(jpaProperty), "%,"+userGroupId+",%"));
        idsList.add(builder.like(root.get(jpaProperty), "%,"+userGroupId));
        predicates.add(builder.or(idsList.toArray(new Predicate[idsList.size()])));
    }

	/**
     * Specialne vyhladavanie v perex_group stlpci, kde sa hlada zadany vyraz cez LIKE vo forme ID ID,% %,ID,% %,ID
     * cize vo vsetkych variantoch ako sa moze vyskytovat v ciarkou oddelenom zozname
     * @param perexGroupName - MENO perex skupiny
     * @param jpaProperty
     * @param predicates
     * @param root
     * @param builder
     */
	public void addSpecSearchPerexGroup(String perexGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
		String idsSelectSql = "SELECT DISTINCT perex_group_id FROM perex_groups WHERE perex_group_name";
		addSpecSearchBySelect(idsSelectSql, perexGroupName, jpaProperty, false, predicates, root, builder);
	}

	/**
	 * Specialne vyhladavanie kde sa prevedie searchText na zoznam ID podla zadaneho idsSelectSql
	 * a nasledne sa hlada zadany vyraz cez LIKE vo forme ID ID,% %,ID,% %,ID
     * cize vo vsetkych variantoch ako sa moze vyskytovat v ciarkou oddelenom zozname
	 * @param idsSelectSql
	 * @param searchText
	 * @param jpaProperty
	 * @param equalsSingleResult - if it's true we will use equal instead of like for search
	 * @param predicates
	 * @param root
	 * @param builder
	 */
    private void addSpecSearchBySelect(String idsSelectSql, String searchText, String jpaProperty, boolean equalsSingleResult, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {

		String valueClean = DatatableRestControllerV2.getCleanValue(searchText);

		String operator = "LIKE";
		String prepend = "%";
		String append = "%";

		boolean isEqual = false;

		if (searchText.startsWith("^") && searchText.endsWith("$")) {
			operator = "=";
			prepend = "";
			append = "";
			isEqual = true;
		} else if (searchText.startsWith("^")) {
			prepend = "";
		} else if (searchText.endsWith("$")) {
			append = "";
		}

		List<Number> searchIds;
		if (Tools.isNotEmpty(idsSelectSql)) {
			searchIds = (new SimpleQuery()).forListNumber(idsSelectSql+" "+operator+" ?", prepend+valueClean+append);
		} else {
			//if SQL is empty use searchValue directly as number
			searchIds = new ArrayList<>();
			searchIds.add(Tools.getIntValue(searchText, -1));
		}
		if (searchIds.isEmpty()==false) {
            List<Predicate> idsList = new ArrayList<>();
            for (Number id : searchIds) {
				if (isEqual && equalsSingleResult) {
					//we are searching to match user with only this exact group (just single ID)
					idsList.add(builder.equal(root.get(jpaProperty), String.valueOf(id)));
				} else {
					//hladame vsetky varianty ID ID, ,ID, ,ID
					idsList.add(builder.like(root.get(jpaProperty), String.valueOf(id)));
					idsList.add(builder.like(root.get(jpaProperty), String.valueOf(id)+",%"));
					idsList.add(builder.like(root.get(jpaProperty), "%,"+id+",%"));
					idsList.add(builder.like(root.get(jpaProperty), "%,"+id));
				}
            }
            predicates.add(builder.or(idsList.toArray(new Predicate[idsList.size()])));
        }
		else {
            predicates.add(builder.like(root.get(jpaProperty), "xxxxxxxx-notfound"));
        }
	}

    /**
	 * Specialne vyhladavanie podla zadaneho mena/priezviska s konverziou na hladanie podla ID pouzivatela (v DB tabulke je zvycajne ulozene ID pouzivatela, nie jeho cele meno).
	 * Interne funguje tak, ze v users tabulke vyhlada zadane meno, vysledok skonvertuje na pole ID pouzivatelov a to nasledne hlada v jpaProperty.in()
	 * @param paramValue - hladane meno/priezvisko
	 * @param jpaProperty - meno JPA property, v ktorej sa nasledne hlada ID pouzivatela
	 * @param predicates
	 * @param root
	 * @param builder
	 */
	public void addSpecSearchUserFullName(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {

		String valueClean = DatatableRestControllerV2.getCleanValue(paramValue);

		String operator = "LIKE";
		String prepend = "%";
		String append = "%";

		if (paramValue.startsWith("^") && paramValue.endsWith("$")) {
			operator = "=";
			prepend = "";
			append = "";
		} else if (paramValue.startsWith("^")) {
			prepend = "";
		} else if (paramValue.endsWith("$")) {
			append = "";
		}

		List<Integer> userIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT user_id FROM users WHERE CONCAT(CONCAT(first_name, ' '), last_name) "+operator+" ? OR email "+operator+" ?", prepend+valueClean+append, prepend+valueClean+append);
		if (userIds.size()>0) predicates.add(root.get(jpaProperty).in(userIds));
		else predicates.add(builder.equal(root.get(jpaProperty), Integer.MAX_VALUE));
	}

    /**
	 * Specialne vyhladavanie podla stavovej ikony
	 * @param statusSearch
	 * @param predicates
	 * @param root
	 * @param builder
	 */
	public void addSpecSearchStatusIcons(String statusSearch, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
		//vyhladavanie na zaklade stavu
		if (Tools.isNotEmpty(statusSearch)) {
			int columnIndex = statusSearch.indexOf(":");
			if (columnIndex > 0) {
				String column = statusSearch.substring(0, columnIndex);
				String value = statusSearch.substring(columnIndex+1);

				if ("notEmpty".equals(value)) {
					List<Predicate> notEmptyList = new ArrayList<>();
					notEmptyList.add(builder.isNotNull(root.get(column)));
					notEmptyList.add(builder.notEqual(root.get(column), ""));
					//and ( NOT null AND !='')
					predicates.add(builder.and(notEmptyList.toArray(new Predicate[notEmptyList.size()])));
				} else if ("empty".equals(value)) {
					List<Predicate> emptyList = new ArrayList<>();
					emptyList.add(builder.isNull(root.get(column)));
					emptyList.add(builder.equal(root.get(column), ""));
					//and (NULL OR '')
					predicates.add(builder.or(emptyList.toArray(new Predicate[emptyList.size()])));
				} else if(value.startsWith("eq:")) {
					predicates.add(builder.equal(root.get(column), value.substring(3)));
				} else if(value.startsWith("gt:")) {
					predicates.add(builder.greaterThan(root.get(column), value.substring(3)));
				} else if(value.startsWith("gte:")) {
					predicates.add(builder.greaterThanOrEqualTo(root.get(column), value.substring(4)));
				} else if(value.startsWith("lt:")) {
					predicates.add(builder.lessThan(root.get(column), value.substring(3)));
				} else if(value.startsWith("lte:")) {
					predicates.add(builder.lessThanOrEqualTo(root.get(column), value.substring(4)));
				} else if (value.contains("%")) {
					if (value.startsWith("!")) predicates.add(builder.notLike(root.get(column), value.substring(1)));
					else predicates.add(builder.like(root.get(column), value));
				} else {
					predicates.add(builder.equal(root.get(column), "true".equals(value)));
				}
			}
		}
	}

	/**
	 * Search by ID in foreign table in column with name foreignColumnName by paramValue
	 * Useful in case of media search by mediaGroupName in media_group foreign table
	 * @param paramValue
	 * @param foreignTableName
	 * @param foreignTableId
	 * @param foreignColumnName
	 * @param jpaProperty
	 * @param predicates
	 * @param root
	 * @param builder
	 */
	public void addSpecSearchIdInForeignTable(String paramValue, String foreignTableName, String foreignTableId, String foreignColumnName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
		String valueClean = DatatableRestControllerV2.getCleanValue(paramValue);

		String operator = "LIKE";
		String prepend = "%";
		String append = "%";

		if (paramValue.startsWith("^") && paramValue.endsWith("$")) {
			operator = "=";
			prepend = "";
			append = "";
		} else if (paramValue.startsWith("^")) {
			prepend = "";
		} else if (paramValue.endsWith("$")) {
			append = "";
		}

		List<Integer> ids = (new SimpleQuery()).forListInteger("SELECT DISTINCT "+foreignTableId+" FROM "+foreignTableName+" WHERE "+foreignColumnName+" "+operator+" ?", prepend+valueClean+append);
		if (ids.size()>0) predicates.add(root.get(jpaProperty).in(ids));
		else predicates.add(builder.equal(root.get(jpaProperty), Integer.MAX_VALUE));
	}

	public void addSpecSearchIdInForeignTableInteger(int paramValue, String foreignTableName, String foreignTableId, String foreignColumnName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
		List<Integer> ids = (new SimpleQuery()).forListInteger("SELECT DISTINCT "+foreignTableId+" FROM "+foreignTableName+" WHERE "+foreignColumnName+" = ?", paramValue);
		if (ids.size()>0) predicates.add(root.get(jpaProperty).in(ids));
		else predicates.add(builder.equal(root.get(jpaProperty), Integer.MAX_VALUE));
	}

	/**
	 * Search by DocDetails.fullPath value, simulated by concating file_name/title in table documents
	 * @param paramValue
	 * @param jpaProperty
	 * @param predicates
	 * @param root
	 * @param builder
	 */
	public void addSpecSearchDocFullPath(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {

		String valueClean = DatatableRestControllerV2.getCleanValue(paramValue);

		String operator = "LIKE";
		String prepend = "%";
		String append = "%";

		if (paramValue.startsWith("^") && paramValue.endsWith("$")) {
			operator = "=";
			prepend = "";
			append = "";
		} else if (paramValue.startsWith("^")) {
			prepend = "";
		} else if (paramValue.endsWith("$")) {
			append = "";
		}

		List<Integer> docIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT doc_id FROM documents WHERE "+DB.fixAiCiCol("CONCAT(CONCAT(file_name, '/'), title)")+" "+operator+" ?", prepend+DB.fixAiCiValue(valueClean)+append);

		//if it's number add it as direct docid
		int paramDocId = Tools.getIntValue(paramValue, -1);
		if (paramDocId>0) docIds.add(paramDocId);

		if (docIds.size()>0) predicates.add(root.get(jpaProperty).in(docIds));
		else predicates.add(builder.equal(root.get(jpaProperty), Integer.MAX_VALUE));
	}

	/**
	 * Search by allowed user editable pages/groups
	 * @param user
	 * @param jpaProperty
	 * @param predicates
	 * @param root
	 * @param builder
	 */
	public void addSpecSearchByUserEditable(UserDetails user, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
		List<Integer> docIdsList = new ArrayList<>();

		if (Tools.isNotEmpty(user.getEditablePages())) {
            int[] docIds = Tools.getTokensInt(user.getEditablePages(), ",");
            if (docIds != null && docIds.length>0) {
                docIdsList.addAll(Arrays.stream(docIds).boxed().collect(Collectors.toList()));
            }
        }
        if (Tools.isNotEmpty(user.getEditableGroups())) {
            GroupsDB groupsDB = GroupsDB.getInstance();
            int[] expandedGroupIds = groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(), ","), true);
			String groupIdsJoined = Arrays.stream(expandedGroupIds)
			.mapToObj(String::valueOf)
			.collect(Collectors.joining(","));

			//Fix, if groupIdsJoined is empty ("=") sql is invalid
			if(!groupIdsJoined.isEmpty()) {
				List<Integer> docIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT doc_id FROM documents WHERE group_id IN ("+groupIdsJoined+")");
				docIdsList.addAll(docIds);
			}
        }

		if (docIdsList.isEmpty()==false) predicates.add(root.get(jpaProperty).in(docIdsList));
	}
}
