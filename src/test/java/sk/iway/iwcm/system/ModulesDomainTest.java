package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.withSettings;

import java.util.Hashtable;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;
import sk.iway.iwcm.users.PermissionGroupDB;
import sk.iway.iwcm.users.UsersDB;

class ModulesDomainTest {

    private MockedStatic<Constants> constants;
    private MockedStatic<CloudToolsForCore> cloudTools;
    private Modules modules;
    private ModuleInfo module;
    private Identity user;

    @BeforeEach
    void setUp() {
        constants = mockStatic(Constants.class);
        cloudTools = mockStatic(CloudToolsForCore.class);
        constants.when(() -> Constants.getString("wjVersion")).thenReturn("M");
        constants.when(() -> Constants.getBoolean("usersSplitByDomain")).thenReturn(true);
        cloudTools.when(CloudToolsForCore::getDomainName).thenReturn("other.example.com");

        module = new ModuleInfo("multiweb.title", "cmp_multiweb", "/apps/multiweb/", null,
            true, true, "/apps/multiweb/admin/", "M", 1000);
        module.setAvailable(true);
        module.setDomainName("admin.example.com");
        module.setGroup("components");
        module.setMenuIcon("ti ti-settings");

        modules = mock(Modules.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(modules, "modules", List.of(module));
        MockServletContext context = new MockServletContext();
        context.setAttribute(Modules.CONTEXT_KEY, modules);
        constants.when(Constants::getServletContext).thenReturn(context);

        user = new Identity();
        user.setDisabledItemsTable(new Hashtable<>());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cloudTools.close();
        constants.close();
    }

    @Test
    void hidesModuleFromPermissionListsOnAnotherDomainWhenUsersAreSplit() {
        assertTrue(modules.getUserEditItems(mock(Prop.class)).isEmpty());
        assertTrue(modules.getUserMenuItems(user).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, true",
        "true, false, false",
        "false, true, false",
        "true, true, false"
    })
    void distinguishesPermissionListsFromDomainSpecificLeftMenu(boolean usersSplitByDomain,
            boolean cloudInstallation, boolean expectedInPermissionLists) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/v9/");
        try (MockedStatic<Prop> props = mockStatic(Prop.class);
                MockedStatic<InitServlet> init = mockStatic(InitServlet.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<DocDB> documents = mockStatic(DocDB.class);
                MockedStatic<PageLng> languages = mockStatic(PageLng.class);
                MockedConstruction<PermissionGroupDB> groups = mockConstruction(PermissionGroupDB.class)) {
            props.when(Prop::getInstance).thenReturn(mock(Prop.class));
            props.when(() -> Prop.getInstance(request)).thenReturn(mock(Prop.class));
            props.when(() -> Prop.getLng(request, false)).thenReturn("sk");
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            documents.when(() -> DocDB.getDomain(request)).thenAnswer(invocation -> CloudToolsForCore.getDomainName());
            languages.when(() -> PageLng.getUserLngIso("sk")).thenReturn("sk-SK");
            constants.when(() -> Constants.getBoolean("usersSplitByDomain")).thenReturn(usersSplitByDomain);
            init.when(InitServlet::isTypeCloud).thenReturn(cloudInstallation);

            assertEquals(expectedInPermissionLists, modules.getUserEditItems(mock(Prop.class)).stream()
                .anyMatch(item -> item.getItemKey().equals("cmp_multiweb")));
            assertEquals(expectedInPermissionLists, hasModuleInPermissionTree());
            assertFalse(hasModuleInLeftMenu(request));
            assertFalse(user.isEnabledItem("cmp_multiweb"));

            cloudTools.when(CloudToolsForCore::getDomainName).thenReturn("admin.example.com");
            assertFalse(modules.getUserEditItems(mock(Prop.class)).isEmpty());
            assertTrue(hasModuleInPermissionTree());
            assertTrue(hasModuleInLeftMenu(request));
        }
    }

    @Test
    void deniesGrantedPermissionOnAnotherDomain() {
        String permission = MenuService.getPerms("/apps/multiweb/admin/");

        assertEquals("cmp_multiweb", permission);
        assertFalse(user.isEnabledItem(permission));
    }

    @Test
    void deniesDomainRestrictionEvenWithoutExplicitlyDisabledItems() {
        user.setDisabledItemsTable(null);

        assertFalse(user.isEnabledItem("cmp_multiweb"));
    }

    @Test
    void deniesSubmenuPermissionOnAnotherDomain() {
        ModuleInfo submenu = new ModuleInfo("multiweb.delete", "multiweb_delete", "/apps/multiweb/delete/", null,
            true, true, "/apps/multiweb/delete/", "M", 1001);
        submenu.setAvailable(true);
        module.addSubmenu(submenu);

        assertFalse(user.isEnabledItem("multiweb_delete"));
    }

    @Test
    void allowsModuleAfterSwitchingBackToItsDomain() {
        modules.disableModules(user);
        cloudTools.when(CloudToolsForCore::getDomainName).thenReturn("admin.example.com");

        assertTrue(user.isEnabledItem("cmp_multiweb"));
        assertTrue(modules.getAvailableModules().contains(module));
        assertFalse(modules.getUserEditItems(mock(Prop.class)).isEmpty());
        assertTrue(modules.getUserMenuItems(user).contains(module));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void preservesModulesWithoutDomainRestriction(String domainName) {
        module.setDomainName(domainName);

        assertTrue(user.isEnabledItem("cmp_multiweb"));
        assertTrue(modules.getAvailableModules().contains(module));
    }

    @Test
    void preservesGlobalAvailabilityForInitializationAndSharedCaches() {
        assertTrue(modules.isAvailable("cmp_multiweb"));
        assertTrue(modules.getAvailableModules().contains(module));
        assertTrue(module.isAvailable());
        assertTrue(user.getDisabledItemsTable().isEmpty());
    }

    @Test
    void supportsMultipleDomainsAndIgnoresCaseAndWhitespace() {
        module.setDomainName("admin.example.com, OTHER.example.com ");

        assertTrue(user.isEnabledItem("cmp_multiweb"));
        assertFalse(modules.getUserEditItems(mock(Prop.class)).isEmpty());
    }

    @Test
    void requiresAWholeDomainName() {
        cloudTools.when(CloudToolsForCore::getDomainName).thenReturn("example.com");

        assertFalse(user.isEnabledItem("cmp_multiweb"));
        assertTrue(Modules.filterDomain(List.of(module), "example.com").isEmpty());
    }

    @Test
    void checksCachedRestPermissionsAfterEveryDomainSwitch() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "admin", "password", List.of(new SimpleGrantedAuthority("ROLE_Group_admin"),
                new SimpleGrantedAuthority("ROLE_Permission_cmp_multiweb"),
                new SimpleGrantedAuthority("ROLE_Permission_cmp_external"),
                new SimpleGrantedAuthority("ROLE_Permission_editor"))));
        WebjetSecurityService security = new WebjetSecurityService(null, null);

        assertFalse(security.hasPermission("cmp_multiweb"));
        assertFalse(security.hasPermission("cmp_multiweb&editor"));
        assertTrue(security.hasPermission("cmp_multiweb|editor"));
        assertTrue(security.hasPermission("cmp_external"));

        cloudTools.when(CloudToolsForCore::getDomainName).thenReturn("admin.example.com");
        assertTrue(security.hasPermission("cmp_multiweb"));
        assertTrue(security.hasPermission("cmp_multiweb&editor"));

        cloudTools.when(CloudToolsForCore::getDomainName).thenReturn("other.example.com");
        assertFalse(security.hasPermission("cmp_multiweb"));
    }

    @Test
    void retainsGrantedRestPermissionsWhenLoggingInOnAnotherDomain() {
        user.setAdmin(true);
        user.setLogin("admin");
        user.setUserGroupsNames("");
        try (MockedConstruction<Identity> identities = mockConstruction(Identity.class,
                withSettings().defaultAnswer(CALLS_REAL_METHODS),
                (identity, context) -> identity.setDisabledItemsTable(new Hashtable<>()))) {
            Authentication authentication = WebjetAuthentificationProvider.authenticate(user);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            WebjetSecurityService security = new WebjetSecurityService(null, null);

            assertFalse(security.hasPermission("cmp_multiweb"));
            cloudTools.when(CloudToolsForCore::getDomainName).thenReturn("admin.example.com");
            assertTrue(security.hasPermission("cmp_multiweb"));
        }
    }

    @Test
    void preservesGlobalModuleAvailability() {
        cloudTools.when(CloudToolsForCore::getDomainName).thenReturn("admin.example.com");
        module.setAvailable(false);

        assertFalse(user.isEnabledItem("cmp_multiweb"));
        assertTrue(modules.getAvailableModules().isEmpty());
    }

    private boolean hasModuleInPermissionTree() {
        return MenuService.getAllPermissions().stream()
            .anyMatch(item -> item.getId().equals(MenuService.getPermsIdWithPrefix("cmp_multiweb")));
    }

    private boolean hasModuleInLeftMenu(MockHttpServletRequest request) {
        return new MenuService(request).getMenu().stream()
            .flatMap(root -> root.getChildrens().stream())
            .anyMatch(item -> item.getHref().equals(module.getLeftMenuLink()));
    }
}
