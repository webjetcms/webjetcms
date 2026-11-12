package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.List;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.system.spring.DatatableExceptionHandlerV2;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Map;
import java.util.Optional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Working version DataTable CRUD, switcher context and REST routing. */
class BranchRestControllerTest extends BaseWebjetTest {
    private static ValidatorFactory validation;
    private final BranchRepository repository=mock(BranchRepository.class);
    private final BranchService service=mock(BranchService.class);
    private final BranchAccess access=mock(BranchAccess.class);
    private final MockHttpServletRequest request=new MockHttpServletRequest();
    private BranchRestController controller;

    @BeforeAll static void validator() { validation=Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void closeValidator() { validation.close(); }

    @BeforeEach void setUp() {
        controller=new BranchRestController(repository,service,access);
        controller.setRequest(request);
        ReflectionTestUtils.setField(controller,"checkDomainId",true);
        ReflectionTestUtils.setField(controller,"validator",validation.getValidator());
        Identity user=new Identity();user.setAdmin(true);user.setUserId(7);
        request.getSession().setAttribute(Constants.USER_KEY,user);
    }

    @Test void inheritedReadsUseTheStandardDomainScope() {
        var fresh=controller.getOneItem(-1);
        assertNull(fresh.getId());assertNull(fresh.getDomainName());
        assertDoesNotThrow(()->new tools.jackson.databind.ObjectMapper().writeValueAsString(fresh));
        when(repository.existsById(42L)).thenReturn(true);
        when(repository.findFirstByIdAndDomainId(42L,1)).thenReturn(Optional.empty());
        assertNull(controller.getOneItem(42));
        verify(repository).findFirstByIdAndDomainId(42L,1);
        verify(repository,never()).findById(any());
    }

    @Test void standardEditorSetsDomainAndInitialValuesBeforeInheritedInsert() {
        var input=new BranchEntity();input.setId(99L);input.setDomainId(2);
        input.setName("  Campaign  ");input.setState("MERGED");input.setRevision(999L);
        when(repository.save(any())).thenAnswer(call->{BranchEntity row=call.getArgument(0);row.setId(42L);return row;});
        var edit=new DatatableRequest<Long,BranchEntity>();edit.setAction("create");edit.setData(Map.of(-1L,input));
        try(var readiness=mockStatic(BranchReadiness.class)) {
            controller.handleEditor(request,edit);
            readiness.verify(BranchReadiness::verifyActivation);
        }
        assertEquals(42L,input.getId());assertEquals(1,input.getDomainId());
        assertEquals("Campaign",input.getName());assertEquals("DRAFT",input.getState());assertEquals(0L,input.getRevision());
        assertEquals(7,input.getCreatedBy());assertEquals(7,input.getUpdatedBy());
        assertEquals(input.getCreatedAt(),input.getUpdatedAt());
        verify(repository).save(input);
    }

    @Test void inheritedEditCopiesMetadataAndPreservesOperationalFields() {
        var stored=new BranchEntity();stored.setId(42L);stored.setDomainId(1);stored.setName("Original");
        stored.setState("SCHEDULED");stored.setRevision(12L);stored.setCreatedBy(3);
        stored.setCreatedAt(new Timestamp(1000));stored.setUpdatedAt(new Timestamp(2000));stored.setPublishAt(new Timestamp(3000));
        var input=new BranchEntity();input.setId(42L);input.setDomainId(1);input.setName("  Campaign  ");input.setDescription("Note");input.setColor("lavender");
        input.setState("MERGED");input.setRevision(999L);input.setCreatedBy(99);input.setPublishAt(new Timestamp(9999));
        when(repository.existsById(42L)).thenReturn(true);
        when(repository.findFirstByIdAndDomainId(42L,1)).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(call->call.getArgument(0));
        BranchEntity saved;
        try(var readiness=mockStatic(BranchReadiness.class)) { saved=controller.edit(42,input).getBody(); }
        assertSame(stored,saved);assertEquals("Campaign",saved.getName());assertEquals("Note",saved.getDescription());assertEquals("lavender",saved.getColor());
        assertEquals("SCHEDULED",saved.getState());assertEquals(12L,saved.getRevision());assertEquals(3,saved.getCreatedBy());
        assertEquals(new Timestamp(1000),saved.getCreatedAt());assertEquals(new Timestamp(3000),saved.getPublishAt());
        assertEquals(7,saved.getUpdatedBy());assertTrue(saved.getUpdatedAt().after(new Timestamp(2000)));
        verify(repository).save(stored);
    }

    @Test void entityAnnotationsValidateNameAndColorBeforePersistence() {
        try(var readiness=mockStatic(BranchReadiness.class)) {
            var blank=new BranchEntity();blank.setName("   ");
            var error=assertThrows(ConstraintViolationException.class,()->controller.add(blank));
            assertTrue(error.getConstraintViolations().stream().anyMatch(v->"name".equals(v.getPropertyPath().toString())));
            var invalidColor=new BranchEntity();invalidColor.setName("Campaign");invalidColor.setColor("x".repeat(17));
            error=assertThrows(ConstraintViolationException.class,()->controller.add(invalidColor));
            assertTrue(error.getConstraintViolations().stream().anyMatch(v->"color".equals(v.getPropertyPath().toString())));
        }
        verify(repository,never()).save(any());
    }

    @Test void constructorRecognizesDomainIdRepository() {
        try(var constants=mockStatic(Constants.class);var cloud=mockStatic(CloudToolsForCore.class)) {
            constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
            controller=new BranchRestController(repository,service,access);
            assertEquals(true,ReflectionTestUtils.getField(controller,"checkDomainId"));
        }
    }

    @Test void standardEditorRejectsForgedDomainId() {
        controller=spy(controller);Prop prop=mock(Prop.class);doReturn(prop).when(controller).getProp();
        when(prop.getText("datatables.error.domainId")).thenReturn("Domain mismatch");
        var input=new BranchEntity();input.setDomainId(2);
        var edit=new DatatableRequest<Long,BranchEntity>();edit.setAction("edit");edit.setData(Map.of(42L,input));
        assertEquals("Domain mismatch",assertThrows(RuntimeException.class,()->controller.handleEditor(request,edit)).getMessage());
        verifyNoInteractions(repository);
    }

    @Test void unsupportedOperationsCannotWrite() {
        var input=new BranchEntity();
        assertThrows(ResponseStatusException.class,()->controller.beforeDelete(input));
        assertThrows(ResponseStatusException.class,()->controller.beforeDuplicate(input));
        controller=spy(controller);doReturn(true).when(controller).isImporting();
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED,assertThrows(ResponseStatusException.class,()->controller.beforeSave(input)).getStatusCode());
        verifyNoInteractions(repository);
    }

    private MockedStatic<BranchDomains> domains;
    @BeforeEach void domainIds() {
        domains = mockStatic(BranchDomains.class);
        domains.when(() -> BranchDomains.id("a.example.sk")).thenReturn(1);
        domains.when(() -> BranchDomains.name(1)).thenReturn("a.example.sk");
    }
    @AfterEach void closeDomainIds() { domains.close(); }


    @Test
    void listsOnlyServerDomainWithoutChangingSelection() {
        BranchService service = mock(BranchService.class);
        BranchAccess access = mock(BranchAccess.class);
        BranchRestController controller = new BranchRestController(repository, service, access);
        MockHttpServletRequest request = new MockHttpServletRequest();
        BranchContext branch = new BranchContext(12, "a.example.sk", 7, BranchContext.Mode.ADMIN);
        request.setAttribute(BranchContexts.ATTRIBUTE, branch);
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(access.requireUser(request)).thenReturn(user);
        when(access.requireDomain(request, user)).thenReturn("a.example.sk");
        BranchEntity entity = new BranchEntity();
        entity.setId(42L);entity.setName("Campaign");entity.setDomainId(1);
        entity.setState("DRAFT");entity.setColor("lavender");
        when(service.list("a.example.sk", false)).thenAnswer(invocation -> {
            assertEquals(BranchContext.Mode.LIVE, BranchContexts.current(request).mode());
            return List.of(entity);
        });
        try (var requests = mockStatic(SetCharacterEncodingFilter.class)) {
            assertEquals(List.of(new BranchRestController.Summary(42, "Campaign", 1, "a.example.sk", "DRAFT", "lavender")),
                controller.list(false, request));
            assertSame(branch, request.getAttribute(BranchContexts.ATTRIBUTE));
            verify(service).list("a.example.sk", false);
        }
    }

    @Test
    void collectionRejectsCreateRequests() throws Exception {
        BranchService service=mock(BranchService.class);
        BranchAccess access=mock(BranchAccess.class);
        var mvc=MockMvcBuilders.standaloneSetup(new BranchRestController(repository,service,access)).build();
        mvc.perform(post("/admin/rest/branches").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service,access);
    }

    /** Both controllers register together, with the standard ID route reserved for DataTable metadata. */
    @Test
    void dataTableAndPublicationRoutesDoNotCollide() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(controller,new BranchMergeRestController(mock(BranchFileRepository.class)))
            .setControllerAdvice(new DatatableExceptionHandlerV2()).build();
        var response=mvc.perform(get("/admin/rest/branches/-1"))
            .andExpect(status().isOk()).andReturn().getResponse();
        assertEquals("cyan",new org.json.JSONObject(response.getContentAsString()).getString("color"));
        verifyNoInteractions(repository);
    }

    /** Context selection retains its HTTP conflict status even with the DataTable exception advice installed. */
    @Test
    void staleContextSelectionRetainsItsHttpStatus() throws Exception {
        Identity user=new Identity();user.setUserId(7);
        when(access.requireUser(any())).thenReturn(user);
        when(access.requireDomain(any(),eq(user))).thenReturn("a.example.sk");
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,"Domain changed"))
            .when(access).requireExpectedDomain(1,2);
        var mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new DatatableExceptionHandlerV2()).build();
        var response=mvc.perform(post("/admin/rest/branches/context").contentType(MediaType.APPLICATION_JSON)
                .content("{\"branchId\":0,\"expectedDomainId\":2}"))
            .andExpect(status().isConflict()).andReturn().getResponse();
        assertEquals("Domain changed",new org.json.JSONObject(response.getContentAsString()).getString("detail"));
        verifyNoInteractions(repository,service);
    }
}
