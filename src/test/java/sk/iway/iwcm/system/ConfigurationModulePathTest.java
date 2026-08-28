package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;

class ConfigurationModulePathTest {

	private static final Set<String> ROOT_MODULES = Set.of(
		"apps", "content", "files", "system", "security", "integrations", "users", "ai"
	);

	@BeforeEach
	void initializeConfigurationCatalog() {
		Constants.clearValues();
		ConstantsV9.clearValuesWebJet9();
	}

	@Test
	void parseNormalizesAndDeduplicatesValidPaths() {
		assertEquals(
			List.of("apps.gallery", "security.oauth2", "files"),
			ConfigurationModulePath.parse(" apps.gallery ;security.oauth2;apps.gallery;not valid;files ")
		);
		assertEquals("apps.gallery;security.oauth2", ConfigurationModulePath.normalize("apps.gallery;;security.oauth2"));
		assertTrue(ConfigurationModulePath.parse(null).isEmpty());
		assertTrue(ConfigurationModulePath.parse(" ").isEmpty());
	}

	@Test
	void branchMatchingUsesCompletePathSegments() {
		String modules = "apps.gallery;security.oauth2";

		assertTrue(ConfigurationModulePath.isInBranch(modules, "apps"));
		assertTrue(ConfigurationModulePath.isInBranch(modules, "apps.gallery"));
		assertTrue(ConfigurationModulePath.isInBranch(modules, "security"));
		assertFalse(ConfigurationModulePath.isInBranch(modules, "app"));
		assertFalse(ConfigurationModulePath.isInBranch(modules, "security.oauth"));
	}

	@Test
	void legacyMatchingAcceptsCompleteFlatSegmentsOnly() {
		String modules = "apps.gallery;security.oauth2;external";

		assertTrue(ConfigurationModulePath.matchesLegacyModule(modules, "apps.gallery"));
		assertTrue(ConfigurationModulePath.matchesLegacyModule(modules, "gallery"));
		assertTrue(ConfigurationModulePath.matchesLegacyModule(modules, "security"));
		assertTrue(ConfigurationModulePath.matchesLegacyModule(modules, "oauth2"));
		assertTrue(ConfigurationModulePath.matchesLegacyModule(modules, "external"));
		assertFalse(ConfigurationModulePath.matchesLegacyModule("apps.formmail", "form"));
	}

	@Test
	void coreConfigurationCatalogUsesCanonicalTaxonomy() {
		assertFalse(Constants.getAllValues().isEmpty());
		for (ConfDetails configuration : Constants.getAllValues()) {
			String modules = configuration.getModules();
			assertNotNull(modules, configuration.getName());
			assertFalse(modules.isBlank(), configuration.getName());
			assertEquals(modules, ConfigurationModulePath.normalize(modules), configuration.getName());

			for (String path : ConfigurationModulePath.parse(modules)) {
				assertTrue(ConfigurationModulePath.isValidPath(path), configuration.getName() + ": " + path);
				String root = path.substring(0, path.indexOf('.') == -1 ? path.length() : path.indexOf('.'));
				assertTrue(ROOT_MODULES.contains(root), configuration.getName() + ": " + path);
			}
		}
	}

	@Test
	void everyCatalogModuleHasDeclaredConstant() throws IllegalAccessException {
		Set<String> declaredModules = new HashSet<>();
		for (Field field : Constants.class.getFields()) {
			if (field.getType() == String.class && Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("MOD_")) {
				declaredModules.add((String) field.get(null));
			}
		}

		for (ConfDetails configuration : Constants.getAllValues()) {
			for (String module : ConfigurationModulePath.parse(configuration.getModules())) {
				assertTrue(declaredModules.contains(module), configuration.getName() + ": missing MOD_* constant for " + module);
			}
		}
	}

	@Test
	void representativeVariablesUseExpectedPaths() {
		assertEquals("security.oauth2", modulesOf("oauth2_githubClientId"));
		assertEquals("apps.gallery", modulesOf("galleryImageQuality"));
		assertEquals("system.email;apps.dmail;apps.reservation;apps.form", modulesOf("defaultSenderName"));
		assertEquals("apps.reservation", modulesOf("reservationAllDayStartTime"));
		assertTrue(ConfigurationModulePath.parse(modulesOf("usrLogonRequireSMS")).contains("integrations.sms"));
		assertEquals("files.metadata;files.gfs", modulesOf("metadataWaitTime"));
		assertEquals("files.imageeditor", modulesOf("imageEditorDefaultTool"));
		assertEquals("apps.structuremirroring", modulesOf("structureMirroringConfig"));
		assertEquals("system.admin", modulesOf("dashboardRecentSize"));
		assertTrue(ConfigurationModulePath.parse(modulesOf("xRobotsTagValue")).contains("content.seo"));
		assertEquals("content.properties;security", modulesOf("propertiesEnabledKeys"));
		assertEquals("security.password", modulesOf("password_passKeyEnabled"));
		assertEquals("security.xss", modulesOf("xssProtection"));
		assertEquals("security.cors", modulesOf("accessControlAllowedOrigins"));
		assertEquals("files.indexing", modulesOf("fileIndexerMaxFileSize"));
		assertEquals("files.thumb", modulesOf("imageMagickCustomParams"));
		assertEquals("system.database", modulesOf("mariaDbDefaultEngine"));
		assertEquals("system.sync", modulesOf("exportArchivePath"));
		assertEquals("system.audit", modulesOf("loggingInMemoryEnabled"));
		assertEquals("apps.gdpr", modulesOf("gdprDeleteDocAndGroupsAfterDays"));
	}

	private String modulesOf(String name) {
		for (ConfDetails configuration : Constants.getAllValues()) {
			if (name.equals(configuration.getName())) return configuration.getModules();
		}
		throw new AssertionError("Missing configuration variable: " + name);
	}
}
