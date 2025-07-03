package sk.iway.iwcm.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.export.ExportDatBean;
import sk.iway.iwcm.components.export.ExportDatRepository;
import sk.iway.iwcm.components.gallery.GalleryEntity;
import sk.iway.iwcm.components.gallery.GalleryRepository;
import sk.iway.iwcm.components.perex_groups.PerexGroupsEntity;
import sk.iway.iwcm.components.perex_groups.PerexGroupsRepository;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.UpdateDatabase;

/**
 * Update domainID in the database for various services
 */
public class DomainIdUpdateService {

	/**
	 * Update exportDat domainId based on first group in exportDat
	 */
	public static void updateExportDatDomainId() {
		try {
			String note = "29.05.2025 [sivan] nastavenie pridaneho stlpca domain_id podla zvoleneho adresara";
			if(UpdateDatabase.isAllreadyUpdated(note)) return;

			ExportDatRepository edr = Tools.getSpringBean("exportDatRepository", ExportDatRepository.class);
			if(edr == null) {
				Logger.error(UpdateDatabase.class, "ExportDatRepository bean not found");
				return;
			}

			GroupsDB groupsDB = GroupsDB.getInstance();

			// Get all no matter the domain
			for(ExportDatBean exportDat : edr.findAll()) {
				int[] groupIds = Tools.getTokensInt(exportDat.getGroupIds(), ",+");
				if(groupIds.length > 0) {
					//set domainId based on first group
					//this is not optimal, but we assume that all groups in exportDat are from the same domain
					String domainName = groupsDB.getDomain(groupIds[0]);
					exportDat.setDomainId(GroupsDB.getDomainId(domainName));
					edr.save(exportDat);
					Logger.warn(UpdateDatabase.class, "ExportDat updated: " + exportDat.getId() + ":" + exportDat.getUrlAddress() + ", domainId=" + exportDat.getDomainId() + ", groupIds=" + exportDat.getGroupIds());
				}
			}
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Update domainId for perexGroups and all dependencies (documents, galleries)
	 */
	public static void updatePerexGroupDomainId() {
		try {
			String note = "03.06.2025 [sivan] pridanie podpory domainId pre perexGroups + uprava všetky zavislosti k tomu";
			PerexGroupsRepository pgr = Tools.getSpringBean("perexGroupsRepository", PerexGroupsRepository.class);
			DocDetailsRepository ddr = Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class);
			GalleryRepository gr = Tools.getSpringBean("galleryRepository", GalleryRepository.class);

			if(pgr == null) { Logger.error(UpdateDatabase.class, "Inicializacia DocDetailsRepository zlyhala"); return; }
			if(ddr == null) { Logger.error(UpdateDatabase.class, "Inicializacia PerexGroupsRepository alebo DocDetailsRepository zlyhala"); return; }
			if(gr == null) { Logger.error(UpdateDatabase.class, "Inicializacia GalleryRepository zlyhala"); return; }

			DebugTimer dt = new DebugTimer("Updating perex groups and dependencies");

			Integer defaultDomainId = CloudToolsForCore.getDomainId();

			//Prepare domain name - id combination map
			Map<String, Integer> domainsMap = new Hashtable<>();
			for(String domainName : GroupsDB.getInstance().getAllDomainsList()) {
				domainsMap.putIfAbsent(domainName, GroupsDB.getDomainId(domainName));
			}

			//Perex groups for every domain
			duplicatePerexGroupsForEveryDomain(pgr, defaultDomainId, domainsMap);

			// prepare map
			Map<String, PerexGroupsEntity> domainPerexGroupsMap = new Hashtable<>();
			for(PerexGroupsEntity perex : pgr.findAll()) {
				if(perex.getDomainId().equals(defaultDomainId)) {
					perex.setAvailableGroups(null);
				}
				domainPerexGroupsMap.put(perex.getPerexGroupName() + "-" + perex.getDomainId(), perex);
			}

			// Handle documents perex groups (if doc have perex group from another domain, find and set equivalent perex group for its domain)
			updatePerexGroupDomainIdInDocuments(domainsMap, domainPerexGroupsMap, ddr);

			// Handle gallery perex groups (if gallery have perex group from another domain, find and set equivalent perex group for its domain)
			updatePerexGroupDomainIdInGallery(domainsMap, domainPerexGroupsMap, gr);

			dt.diffInfo("DONE");

			UpdateDatabase.saveSuccessUpdate(note);
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Loop through all gallery and check if perex groups are valid.
	 * If not, find equivalent perexGroups for gallery domain and set them.
	 * If there is change, save the gallery.
	 */
	private static void updatePerexGroupDomainIdInGallery(Map<String, Integer> domainsMap, Map<String, PerexGroupsEntity> domainPerexGroupsMap, GalleryRepository gr) {
		DocDB docDB = DocDB.getInstance();

		int pageSize = 100;
		int pageNumber = 0;
		int failsafe = 0;
		Page<GalleryEntity> page;
		do {
			page = gr.findAllByPerexGroupIsNotNull(PageRequest.of(pageNumber, pageSize));
			List<GalleryEntity> items = page.getContent();

			for(GalleryEntity galleryEntity : items) {
				String oldPerexGroups = galleryEntity.getPerexGroup();
				boolean needSave = getValidPerexGroupsForGallery(galleryEntity, domainsMap, domainPerexGroupsMap, docDB);
				if(needSave == true) {
					gr.save(galleryEntity);
					Logger.warn(UpdateDatabase.class, "Updated perex groups for gallery: " + galleryEntity.getId() + ":" + galleryEntity.getImagePath() + ", oldPerexGroups=" + oldPerexGroups + ", newPerexGroups=" + galleryEntity.getPerexGroup());
				}
			}

			pageNumber++;
		} while (!page.isLast() && failsafe++ < 5000);
	}

	/**
	 * Loop through all documents and check if perex groups are valid.
	 * If not, find equivalent perexGroups for doc domain and set them.
	 * If there is change, save the document.
	 */
	private static void updatePerexGroupDomainIdInDocuments(Map<String, Integer> domainsMap, Map<String, PerexGroupsEntity> domainPerexGroupsMap, DocDetailsRepository ddr) {
		int pageSize = 100;
		int pageNumber = 0;
		int failsafe = 0;
		Page<DocDetails> page;
		do {
			page = ddr.findAllByPerexGroupsIsNotNull(PageRequest.of(pageNumber, pageSize));
			List<DocDetails> items = page.getContent();

			for(DocDetails doc : items) {
				String oldPerexGroups = doc.getPerexGroupIdsString();
				boolean needSave = getValidPerexGroupsForDoc(doc, domainsMap, domainPerexGroupsMap);
				if(needSave == true) {
					ddr.save(doc);
					Logger.warn(UpdateDatabase.class, "Updated perex groups for doc: " + doc.getId() + ":" + doc.getVirtualPath() + ", oldPerexGroups=" + oldPerexGroups + ", newPerexGroups=" + doc.getPerexGroupIdsString());
				}
			}

			pageNumber++;
		} while (!page.isLast() && failsafe++ < 5000);
	}

	/**
	 * Check if perex groups for gallery are valid (are in same domain as gallery).
	 * If not, find equivalent perexGroups for gallery domain and set them.
	 * If there is change, return TRUE.
	 */
	private static boolean getValidPerexGroupsForGallery(GalleryEntity ge, Map<String, Integer> domainsMap, Map<String, PerexGroupsEntity> domainPerexGroupsMap, DocDB docDB) {
		if(ge.getPerexGroup().isEmpty() == true) return false; // dont neeed save

		Integer domainId = ge.getDomainId();
		Integer[] perexGroupIds = Tools.getTokensInteger(ge.getPerexGroup(), ",");

		List<Integer> newPerexGroups = new ArrayList<>();
		for(int perexGroupId : perexGroupIds) {
			//find perexGroup by id in domainPerexGroupsMap
			String perexGroupName = domainPerexGroupsMap.entrySet().stream()
				.filter(entry -> entry.getValue().getId().intValue() == perexGroupId)
				.map(entry -> entry.getValue().getPerexGroupName())
				.findFirst()
				.orElse(null);
			// Find perexGroup with same name but for domain of doc

			if(domainPerexGroupsMap.get(perexGroupName + "-" + domainId) == null) {
				Logger.error(UpdateDatabase.class, "Perex group: " + perexGroupId + ":" + perexGroupName + " was not found in domain: " + domainId);
			} else {
				newPerexGroups.add( domainPerexGroupsMap.get(perexGroupName + "-" + domainId).getId().intValue() );
			}
		}

		//Compare old and new ids, if there is change
		if(comapreListAndArray(newPerexGroups, perexGroupIds) == false) {
			//Replace values and return TRUE, so change will be saved
			ge.setPerexGroup(
				String.join(",", newPerexGroups.stream()
					.map(String::valueOf)
					.toArray(String[]::new))
			);
			return true;
		}

		return false;
	}

	/**
	 * Check if perex groups for doc are valid (are in same domain as doc).
	 * If not, find equivalent perexGroups for doc domain and set them.
	 * If there is change, return TRUE.
	 */
	private static boolean getValidPerexGroupsForDoc(DocDetails doc, Map<String, Integer> domainsMap, Map<String, PerexGroupsEntity> domainPerexGroupsMap) {
		if(doc.getPerexGroups().length < 1) return false; // dont neeed save

		Integer domainId = domainsMap.get( doc.getGroup().getDomainName() );

		List<Integer> newPerexGroups = new ArrayList<>();
		for(int perexGroupId : doc.getPerexGroups()) {
			// Find perexGroup with same name but for domain of doc
			String perexGroupName = domainPerexGroupsMap.entrySet().stream()
				.filter(entry -> entry.getValue().getId().intValue() == perexGroupId)
				.map(entry -> entry.getValue().getPerexGroupName())
				.findFirst()
				.orElse(null);

			if(domainPerexGroupsMap.get(perexGroupName + "-" + domainId) == null) {
				Logger.error(UpdateDatabase.class, "Perex group: " + perexGroupId + ":" + perexGroupName + " was not found in domain: " + domainId);
				return false;
			} else {
				newPerexGroups.add( domainPerexGroupsMap.get(perexGroupName + "-" + domainId).getId().intValue() );
			}
		}

		//Compare old and new ids, if there is change
		if(comapreListAndArray(newPerexGroups, doc.getPerexGroups()) == false) {
			//Replace values and return TRUE, so change will be saved
			doc.setPerexGroups( newPerexGroups.toArray(new Integer[0]) );
			return true;
		}

		return false;
	}

	/**
	 * Prepare perex groups for every domain.
	 * This will create deep copy of original perex groups and set domainId to new one.
	 * Also handle available groups for each perex group.
	 */
	private static void duplicatePerexGroupsForEveryDomain(PerexGroupsRepository pgr, Integer defaultDomainId, Map<String, Integer> domainsMap) {
		List<PerexGroupsEntity> originalPerexGroups = pgr.findAll(); //Find all no matter the domain

		GroupsDB groupsDB = GroupsDB.getInstance();

		// Set all existing perexes to default domain AND save them
		originalPerexGroups.forEach(perex -> perex.setDomainId(defaultDomainId));
		pgr.saveAll(originalPerexGroups);

		// Create and save duplicated for every other domain (set correct available groups)
		domainsMap.forEach((domainName, domainId) -> {
			if(domainId.equals(defaultDomainId) == false) {
				pgr.saveAll( clonePerexGroupDeepCopy(originalPerexGroups, domainId, domainsMap, groupsDB) );
			}
		});

		// At end, filter available groups for default domain
		for(PerexGroupsEntity originalPerex : originalPerexGroups)
			filterPerexGroupAvailableGroupsByDomain(originalPerex, domainsMap, groupsDB);
		pgr.saveAll(originalPerexGroups);
	}

	/**
	 * Create deep copy of original perex groups and set domainId to new one.
	 * Also handle available groups for each perex group.
	 */
	private static List<PerexGroupsEntity> clonePerexGroupDeepCopy(List<PerexGroupsEntity> originalPerexGroups, Integer domainId, Map<String, Integer> domainsMap, GroupsDB groupsDB) {
		List<PerexGroupsEntity> newPerexGroups = new ArrayList<>();
		for(PerexGroupsEntity origPerex : originalPerexGroups) {
			PerexGroupsEntity newPerex = new PerexGroupsEntity();

			// Must be set before handling available groups
			newPerex.setDomainId(domainId);

			newPerex.setPerexGroupName( origPerex.getPerexGroupName() );
			newPerex.setPerexGroupNameSk( origPerex.getPerexGroupNameSk() );
			newPerex.setPerexGroupNameEn( origPerex.getPerexGroupNameEn() );
			newPerex.setPerexGroupNameCz( origPerex.getPerexGroupNameCz() );
			newPerex.setPerexGroupNameDe( origPerex.getPerexGroupNameDe() );
			newPerex.setPerexGroupNameHu( origPerex.getPerexGroupNameHu() );
			newPerex.setPerexGroupNamePl( origPerex.getPerexGroupNamePl() );
			newPerex.setPerexGroupNameRu( origPerex.getPerexGroupNameRu() );
			newPerex.setPerexGroupNameEsp( origPerex.getPerexGroupNameEsp() );
			newPerex.setPerexGroupNameCho( origPerex.getPerexGroupNameCho() );

			newPerex.setFieldA( origPerex.getFieldA() );
			newPerex.setFieldB( origPerex.getFieldB() );
			newPerex.setFieldC( origPerex.getFieldC() );
			newPerex.setFieldD( origPerex.getFieldD() );
			newPerex.setFieldE( origPerex.getFieldE() );
			newPerex.setFieldF( origPerex.getFieldF() );

			// Set available groups for this perex
			newPerex.setAvailableGroups( origPerex.getAvailableGroups() );
			filterPerexGroupAvailableGroupsByDomain(newPerex, domainsMap, groupsDB);

			newPerexGroups.add(newPerex);

			Logger.warn(UpdateDatabase.class, "Duplicating perex group: " + origPerex.getId() + ":" + origPerex.getPerexGroupName() + ", domainId: " + domainId + ", availableGroups: " + newPerex.getAvailableGroups());
		}
		return newPerexGroups;
	}

	/**
	 * Loop perexGroup available groups and check if they are in the same domain as perexGroup. If yes keep them, if not remove them.
	 */
	private static void filterPerexGroupAvailableGroupsByDomain(PerexGroupsEntity perexGroup, Map<String, Integer> domainsMap, GroupsDB groupsDB) {
		int[] availableGroups = Tools.getTokensInt(perexGroup.getAvailableGroups(), ",");
		perexGroup.setAvailableGroups(null);

		for(int availableGroup : availableGroups) {
			String groupDomainName = groupsDB.getDomain(availableGroup);
			Integer groupDomainId = domainsMap.get(groupDomainName);

			if(perexGroup.getDomainId().equals(groupDomainId))
				perexGroup.addAvailableGroup(availableGroup);
		}
	}

	/**
	 * Transform array to list, sort both lists and compare them.

	 * @return true if both lists are equal, false otherwise.
	 */
	private static boolean comapreListAndArray(List<Integer> list, Integer[] array) {
		if(list == null && array == null) return true;
		else if(list == null || array == null) return false;

		List<Integer> arrayAsList = new ArrayList<>();
		for (int num : array)
			arrayAsList.add(num);

		Collections.sort(arrayAsList);
		Collections.sort(list);

		return arrayAsList.equals(list);
	}

}
