package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Tests for TemplatesDB.getTemplate(TemplateDetails original, BrowserDetector bd)
 * Verifies that device template matching correctly filters by domain-specific properties
 * (availableGroups, templatesGroupId, templateInstallName) so that templates
 * from another domain are not incorrectly used.
 */
class TemplatesDBDeviceTest extends BaseWebjetTest {

	private static final String IPHONE_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";

	private TemplatesDB templatesDB;
	private List<TemplateDetails> temps;

	@BeforeEach
	void setUp() throws Exception {
		temps = new ArrayList<>();

		// Use reflection to create TemplatesDB instance without DB access
		// The constructor catches exceptions from reload(), so it should succeed even without DB
		Constructor<TemplatesDB> constructor = TemplatesDB.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		templatesDB = constructor.newInstance();

		// Set the temps field via reflection
		Field tempsField = TemplatesDB.class.getDeclaredField("temps");
		tempsField.setAccessible(true);
		tempsField.set(templatesDB, temps);
	}

	private TemplateDetails createTemplate(int id, String name, String availableGroups, Long groupId, String installName) {
		TemplateDetails td = new TemplateDetails();
		td.setTempId(id);
		td.setTempName(name);
		td.setAvailableGroups(availableGroups);
		td.setTemplatesGroupId(groupId);
		td.setTemplateInstallName(installName);
		return td;
	}

	@Test
	void testNullOriginalReturnsNull() {
		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		assertNull(templatesDB.getTemplate((TemplateDetails) null, bd));
	}

	@Test
	void testNullBrowserDetectorReturnsNull() {
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");
		assertNull(templatesDB.getTemplate(original, (BrowserDetector) null));
	}

	@Test
	void testNormalDeviceReturnsNull() {
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");
		// Desktop browser has null browserDeviceType
		BrowserDetector bd = new BrowserDetector("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");
		assertNull(templatesDB.getTemplate(original, bd));
	}

	@Test
	void testEmptyTempNameReturnsNull() {
		TemplateDetails original = createTemplate(1, "", "1,2,3", 1L, "test");
		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		assertNull(templatesDB.getTemplate(original, bd));
	}

	@Test
	void testMatchingDeviceTemplateFound() {
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");

		// Add a matching device template to the list
		TemplateDetails deviceTemplate = createTemplate(-1, "Homepage device=phone", "1,2,3", 1L, "test");
		temps.add(deviceTemplate);

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		assertNotNull(result);
		assertEquals("Homepage device=phone", result.getTempName());
	}

	@Test
	void testDeviceTemplateFromDifferentDomainNotMatched() {
		// Original template for domain with availableGroups "1,2,3"
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");

		// Device template exists but belongs to a different domain (different availableGroups)
		TemplateDetails otherDomainDevice = createTemplate(-1, "Homepage device=phone", "4,5,6", 2L, "other-install");
		temps.add(otherDomainDevice);

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		// Should NOT match the template from another domain
		// Result is null because creation of a new device template requires DocDB
		if (result != null) {
			// If a template was returned (e.g. runtime created one), verify it's NOT the wrong domain one
			assertEquals("1,2,3", result.getAvailableGroups());
			assertEquals(1L, result.getTemplatesGroupId());
			assertEquals("test", result.getTemplateInstallName());
		}
	}

	@Test
	void testDeviceTemplateFromDifferentDomainNotMatchedWithCorrectAlsoPresent() {
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");

		// Wrong domain template
		TemplateDetails otherDomainDevice = createTemplate(-1, "Homepage device=phone", "4,5,6", 2L, "other-install");
		// Correct domain template
		TemplateDetails correctDevice = createTemplate(-1, "Homepage device=phone", "1,2,3", 1L, "test");
		temps.add(otherDomainDevice);
		temps.add(correctDevice);

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		assertNotNull(result);
		assertEquals("1,2,3", result.getAvailableGroups());
		assertEquals(1L, result.getTemplatesGroupId());
		assertEquals("test", result.getTemplateInstallName());
	}

	@Test
	void testDeviceTemplateWithDifferentAvailableGroupsNotMatched() {
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");

		// Same name pattern, same groupId, same installName, but different availableGroups
		TemplateDetails differentGroups = createTemplate(-1, "Homepage device=phone", "10,20,30", 1L, "test");
		// Correct match
		TemplateDetails correctMatch = createTemplate(-1, "Homepage device=phone", "1,2,3", 1L, "test");
		temps.add(differentGroups);
		temps.add(correctMatch);

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		// Should skip the one with different groups and match the correct one
		assertNotNull(result);
		assertEquals("1,2,3", result.getAvailableGroups());
	}

	@Test
	void testDeviceTemplateWithDifferentGroupIdNotMatched() {
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");

		// Same name pattern, same availableGroups, same installName, but different templatesGroupId
		TemplateDetails differentGroupId = createTemplate(-1, "Homepage device=phone", "1,2,3", 99L, "test");
		// Correct match
		TemplateDetails correctMatch = createTemplate(-1, "Homepage device=phone", "1,2,3", 1L, "test");
		temps.add(differentGroupId);
		temps.add(correctMatch);

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		// Should skip the one with different groupId and match the correct one
		assertNotNull(result);
		assertEquals(1L, result.getTemplatesGroupId());
	}

	@Test
	void testDeviceTemplateWithDifferentInstallNameNotMatched() {
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");

		// Same name pattern, same availableGroups, same groupId, but different installName
		TemplateDetails differentInstall = createTemplate(-1, "Homepage device=phone", "1,2,3", 1L, "other-install");
		// Correct match
		TemplateDetails correctMatch = createTemplate(-1, "Homepage device=phone", "1,2,3", 1L, "test");
		temps.add(differentInstall);
		temps.add(correctMatch);

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		// Should skip the one with different installName and match the correct one
		assertNotNull(result);
		assertEquals("test", result.getTemplateInstallName());
	}

	@Test
	void testDeviceTemplateMatchWithNullAvailableGroups() {
		TemplateDetails original = createTemplate(1, "Homepage", null, 1L, null);

		// Device template with same null values
		TemplateDetails deviceTemplate = createTemplate(-1, "Homepage device=phone", null, 1L, null);
		temps.add(deviceTemplate);

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		assertNotNull(result);
		assertEquals("Homepage device=phone", result.getTempName());
	}

	@Test
	void testDeviceTemplateNotMatchedWhenOnlyNameMatches() {
		// Two templates with same name but different domain properties
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "domain-a");

		// This device template has the right name pattern but wrong domain properties
		TemplateDetails wrongDomain = createTemplate(-1, "Homepage device=phone", "7,8,9", 3L, "domain-b");
		// Correct domain template
		TemplateDetails correctDomain = createTemplate(-1, "Homepage device=phone", "1,2,3", 1L, "domain-a");
		temps.add(wrongDomain);
		temps.add(correctDomain);

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		// Should NOT return the wrong domain template, should return the correct one
		assertNotNull(result);
		assertEquals("domain-a", result.getTemplateInstallName());
		assertEquals("1,2,3", result.getAvailableGroups());
	}

	@Test
	void testTabletDeviceTemplate() {
		String ipadUa = "Mozilla/5.0 (iPad; CPU OS 16_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.3 Mobile/15E148 Safari/604.1";

		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");

		TemplateDetails tabletTemplate = createTemplate(-1, "Homepage device=tablet", "1,2,3", 1L, "test");
		temps.add(tabletTemplate);

		BrowserDetector bd = new BrowserDetector(ipadUa);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		assertNotNull(result);
		assertEquals("Homepage device=tablet", result.getTempName());
	}

	@Test
	void testMultipleDeviceTemplatesCorrectOneSelected() {
		TemplateDetails original = createTemplate(1, "Homepage", "1,2,3", 1L, "test");

		// Template from domain A
		TemplateDetails domainADevice = createTemplate(-1, "Homepage device=phone", "1,2,3", 1L, "test");
		// Template from domain B (should NOT be matched for original from domain A)
		TemplateDetails domainBDevice = createTemplate(-1, "Homepage device=phone", "10,20,30", 2L, "other");

		temps.add(domainBDevice); // add wrong one first
		temps.add(domainADevice); // correct one second

		BrowserDetector bd = new BrowserDetector(IPHONE_UA);
		TemplateDetails result = templatesDB.getTemplate(original, bd);

		assertNotNull(result);
		assertEquals("1,2,3", result.getAvailableGroups());
		assertEquals(1L, result.getTemplatesGroupId());
		assertEquals("test", result.getTemplateInstallName());
	}
}
