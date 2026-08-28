package sk.iway.iwcm.system.datatable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ch.qos.logback.classic.Level;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.upload.UploadSpringConfig;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.test.TestRequest;
import sk.iway.spring.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Id;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

/**
 * Test REST controller methods
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SpringApplication.class, UploadSpringConfig.class})
@WebAppConfiguration
class DatatableRestControllerV2Test extends BaseWebjetTest {

    RestControllerMock controller = new RestControllerMock();

    @Autowired
	private Validator validator;

    @BeforeEach
    public void setUp() {
        controller.resetCounters();
    }

    @BeforeAll
    public void setup() {
        Logger.setLevel(Level.ERROR);
        Logger.setLevel("sk.iway.basecms", Level.DEBUG);
        Logger.setLevel("sk.iway.iwcm.admin.upload", Level.TRACE);

        Constants.setServletContext(new MockServletContext("Webjet"){
            @Override
            public String getRealPath(@NonNull String path)
            {
                String basePath = System.getProperty("webjetTestBasepath");
                if (Tools.isEmpty(basePath)) {
                    basePath = "./src/main/webapp";
                }
                return new File(new File(basePath), path).getAbsolutePath();
            }
        });

        controller.setValidator(validator);
        TestRequest request = new TestRequest();
        controller.setRequest(request);
    }

    @Test
    void testEdit() {
        Object entity = new Object();
        Long id = 1L;

        // Call the REST method that triggers beforeSave and afterSave
        controller.edit(id, entity);

        // Verify that beforeSave and afterSave were called ONLY once
        assertEquals(1, controller.getBeforeSaveCounter(), "beforeSave should be called once");
        assertEquals(1, controller.getAfterSaveCounter(), "afterSave should be called once");
    }

    @Test
    void testAdd() {
        // Create a mock entity and ID
        Object entity = new Object();

        // Call the REST method that triggers beforeSave and afterSave
        controller.add(entity);

        // Verify that beforeSave and afterSave were called ONLY once
        assertEquals(1, controller.getBeforeSaveCounter(), "beforeSave should be called once");
        assertEquals(1, controller.getAfterSaveCounter(), "afterSave should be called once");
    }

    @Test
    void testDelete() {
        Object entity = new Object();
        Long id = 1L;

        // Call the REST method that triggers beforeDelete and afterDelete
        controller.delete(id, entity);

        // Verify that beforeDelete and afterDelete were called ONLY once
        assertEquals(1, controller.getBeforeDeleteCounter(), "beforeDelete should be called once");
        assertEquals(1, controller.getAfterDeleteCounter(), "afterDelete should be called once");
    }

    @Test
    void testGetOneItem() {
        Long id = 1L;

        // Call the REST method to get one item
        Object result = controller.getOne(id);

        // Verify that the result is not null
        assertEquals(true, result != null, "getOne should return a non-null result");
    }

    @Test
    void testGetAllItems() {
        // Call the REST method to get all items
        Page<Object> result = controller.getAll(Pageable.unpaged());

        // Verify that the result is not null
        assertEquals(true, result != null, "getAll should return a non-null result");
    }

    @Test
    void testHandleEditor() {
        // Call the REST method to handle the editor
        DatatableRequest<Long, Object> datatableRequest = new DatatableRequest<>();
        Map<Long, Object> data = new HashMap<>();
        data.put(1L, new Object());
        datatableRequest.setData(data);
        Object result;

        datatableRequest.setAction("edit");
        result = controller.handleEditor(controller.getRequest(), datatableRequest);

        // Verify that the result is not null
        assertEquals(true, result != null, "handleEditor should return a non-null result");

        assertEquals(1, controller.getEditItemCounter(), "editItem should be called once");
        assertEquals(0, controller.getInsertItemCounter(), "insertItem should not be called");
        assertEquals(0, controller.getDeleteItemCounter(), "deleteItem should not be called");

        assertEquals(1, controller.getBeforeSaveCounter(), "beforeSave should be called once");
        assertEquals(1, controller.getAfterSaveCounter(), "afterSave should be called once");
        assertEquals(0, controller.getBeforeDeleteCounter(), "beforeDelete should not be called");
        assertEquals(0, controller.getAfterDeleteCounter(), "afterDelete should not be called");

        data.clear();
        data.put(-1L, new Object());
        datatableRequest.setData(data);
        datatableRequest.setAction("create");
        result = controller.handleEditor(controller.getRequest(), datatableRequest);

        assertEquals(1, controller.getEditItemCounter(), "editItem should be called once");
        assertEquals(1, controller.getInsertItemCounter(), "insertItem should be called once");
        assertEquals(0, controller.getDeleteItemCounter(), "deleteItem should not be called");

        assertEquals(2, controller.getBeforeSaveCounter(), "beforeSave should be called twice");
        assertEquals(2, controller.getAfterSaveCounter(), "afterSave should be called twice");
        assertEquals(0, controller.getBeforeDeleteCounter(), "beforeDelete should not be called");
        assertEquals(0, controller.getAfterDeleteCounter(), "afterDelete should not be called");

        datatableRequest.setAction("remove");
        result = controller.handleEditor(controller.getRequest(), datatableRequest);

        assertEquals(1, controller.getEditItemCounter(), "editItem should be called once");
        assertEquals(1, controller.getInsertItemCounter(), "insertItem should be called once");
        assertEquals(1, controller.getDeleteItemCounter(), "deleteItem should be called once");

        assertEquals(2, controller.getBeforeSaveCounter(), "beforeSave should be called twice");
        assertEquals(2, controller.getAfterSaveCounter(), "afterSave should be called twice");
        assertEquals(1, controller.getBeforeDeleteCounter(), "beforeDelete should be called once");
        assertEquals(1, controller.getAfterDeleteCounter(), "afterDelete should be called once");
    }

    @Test
    void testHandleEditorUpdateByColumn() {
        // Call the REST method to handle the editor
        DatatableRequest<Long, Object> datatableRequest = new DatatableRequest<>();
        Map<Long, Object> data = new HashMap<>();
        data.put(1L, new Object());
        datatableRequest.setData(data);

        //verify editItemByColumn
        datatableRequest.setAction("edit");
        datatableRequest.setImportMode("update");
        datatableRequest.setUpdateByColumn("id");
        controller.handleEditor(controller.getRequest(), datatableRequest);

        assertEquals(0, controller.getEditItemCounter(), "editItem should be called once");
        assertEquals(1, controller.getInsertItemCounter(), "insertItem should be called once");
        assertEquals(0, controller.getDeleteItemCounter(), "deleteItem should be called once");

        assertEquals(1, controller.getBeforeSaveCounter(), "beforeSave should be called twice");
        assertEquals(1, controller.getAfterSaveCounter(), "afterSave should be called twice");
        assertEquals(0, controller.getBeforeDeleteCounter(), "beforeDelete should be called once");
        assertEquals(0, controller.getAfterDeleteCounter(), "afterDelete should be called once");
    }

    @Test
    void testRowReorderChecksItemPermissionsBeforeModification() {
        @SuppressWarnings("unchecked")
        JpaRepository<RowReorderTestEntity, Long> repository = mock(JpaRepository.class);
        RowReorderTestEntity entity = new RowReorderTestEntity(1L, 10);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(entity));

        DatatableRestControllerV2<RowReorderTestEntity, Long> restrictedController =
                new DatatableRestControllerV2<>(repository) {
                    @Override
                    public boolean checkItemPerms(RowReorderTestEntity checkedEntity, Long id) {
                        return false;
                    }
                };
        restrictedController.setRequest(controller.getRequest());

        RowReorderDto request = new RowReorderDto();
        request.setDataSrc("position");
        request.setValues(List.of(new RowReorderDto.RowReorderValue(1L, 10, 20)));

        assertThrows(ConstraintViolationException.class,
                () -> restrictedController.rowReorder(controller.getRequest(), request));
        assertEquals(10, entity.getPosition());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testRowReorderRejectsUnannotatedNumericProperty() {
        @SuppressWarnings("unchecked")
        JpaRepository<RowReorderTestEntity, Long> repository = mock(JpaRepository.class);
        RowReorderTestEntity entity = new RowReorderTestEntity(1L, 10);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(entity));

        DatatableRestControllerV2<RowReorderTestEntity, Long> unrestrictedController =
                new DatatableRestControllerV2<>(repository) {};
        unrestrictedController.setRequest(controller.getRequest());

        RowReorderDto request = new RowReorderDto();
        request.setDataSrc("unrestrictedValue");
        request.setValues(List.of(new RowReorderDto.RowReorderValue(1L, 0, 99)));

        assertEquals(Boolean.FALSE, unrestrictedController.rowReorder(controller.getRequest(), request).getBody());
        assertEquals(0, entity.getUnrestrictedValue());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testRowReorderUpdatesAnnotatedProperty() {
        @SuppressWarnings("unchecked")
        JpaRepository<RowReorderTestEntity, Long> repository = mock(JpaRepository.class);
        RowReorderTestEntity entity = new RowReorderTestEntity(1L, 10);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(entity));

        DatatableRestControllerV2<RowReorderTestEntity, Long> unrestrictedController =
                new DatatableRestControllerV2<>(repository) {};
        unrestrictedController.setRequest(controller.getRequest());

        RowReorderDto request = new RowReorderDto();
        request.setDataSrc("position");
        request.setValues(List.of(new RowReorderDto.RowReorderValue(1L, 10, 20)));

        assertEquals(Boolean.TRUE, unrestrictedController.rowReorder(controller.getRequest(), request).getBody());
        assertEquals(20, entity.getPosition());
        verify(repository).saveAll(List.of(entity));
    }

    @Test
    void testRowReorderRejectsIdProperty() {
        assertRejectedRowReorderProperty("id");
    }

    @Test
    void testRowReorderRejectsDomainIdPropertyEvenWhenAnnotated() {
        assertRejectedRowReorderProperty("domainId");
    }

    @Test
    void testRowReorderDoesNotModifyOrSaveWhenAnyValueIsInvalid() {
        @SuppressWarnings("unchecked")
        JpaRepository<RowReorderTestEntity, Long> repository = mock(JpaRepository.class);
        RowReorderTestEntity first = new RowReorderTestEntity(1L, 10);
        RowReorderTestEntity second = new RowReorderTestEntity(2L, 20);
        when(repository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        DatatableRestControllerV2<RowReorderTestEntity, Long> unrestrictedController =
                new DatatableRestControllerV2<>(repository) {};
        unrestrictedController.setRequest(controller.getRequest());

        RowReorderDto request = new RowReorderDto();
        request.setDataSrc("position");
        request.setValues(List.of(
            new RowReorderDto.RowReorderValue(1L, 10, 20),
            new RowReorderDto.RowReorderValue(2L, 20, null)
        ));

        assertEquals(Boolean.FALSE, unrestrictedController.rowReorder(controller.getRequest(), request).getBody());
        assertEquals(10, first.getPosition());
        assertEquals(20, second.getPosition());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testRowReorderRejectsRequestWhenAnyEntityIsMissing() {
        @SuppressWarnings("unchecked")
        JpaRepository<RowReorderTestEntity, Long> repository = mock(JpaRepository.class);
        RowReorderTestEntity entity = new RowReorderTestEntity(1L, 10);
        when(repository.findAllById(List.of(1L, 2L))).thenReturn(List.of(entity));

        DatatableRestControllerV2<RowReorderTestEntity, Long> unrestrictedController =
                new DatatableRestControllerV2<>(repository) {};
        unrestrictedController.setRequest(controller.getRequest());

        RowReorderDto request = new RowReorderDto();
        request.setDataSrc("position");
        request.setValues(List.of(
            new RowReorderDto.RowReorderValue(1L, 10, 20),
            new RowReorderDto.RowReorderValue(2L, 20, 10)
        ));

        assertEquals(Boolean.FALSE, unrestrictedController.rowReorder(controller.getRequest(), request).getBody());
        assertEquals(10, entity.getPosition());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testRowReorderRejectsRequestContainingEntityFromAnotherDomain() {
        int currentDomainId = 1;
        int foreignDomainId = 2;
        List<Long> requestedIds = List.of(1L, 2L);

        @SuppressWarnings("unchecked")
        DomainIdRepository<RowReorderTestEntity, Long> repository = mock(DomainIdRepository.class);
        RowReorderTestEntity currentDomainEntity = new RowReorderTestEntity(1L, 10);
        currentDomainEntity.setDomainId(currentDomainId);
        RowReorderTestEntity foreignDomainEntity = new RowReorderTestEntity(2L, 20);
        foreignDomainEntity.setDomainId(foreignDomainId);

        when(repository.findAllByIdInAndDomainId(requestedIds, currentDomainId))
            .thenReturn(List.of(currentDomainEntity));
        // This is what the unscoped implementation would load and subsequently modify.
        when(repository.findAllById(requestedIds)).thenReturn(List.of(currentDomainEntity, foreignDomainEntity));

        DatatableRestControllerV2<RowReorderTestEntity, Long> domainController =
                new DatatableRestControllerV2<>(repository) {};
        domainController.checkDomainId = true;
        domainController.setRequest(controller.getRequest());

        RowReorderDto request = new RowReorderDto();
        request.setDataSrc("position");
        request.setValues(List.of(
            new RowReorderDto.RowReorderValue(1L, 10, 20),
            new RowReorderDto.RowReorderValue(2L, 20, 10)
        ));

        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(currentDomainId);

            assertEquals(Boolean.FALSE, domainController.rowReorder(controller.getRequest(), request).getBody());
        }

        assertEquals(10, currentDomainEntity.getPosition());
        assertEquals(20, foreignDomainEntity.getPosition());
        verify(repository).findAllByIdInAndDomainId(requestedIds, currentDomainId);
        verify(repository, never()).findAllById(any());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testRowReorderAllowsAnnotatedPropertyInheritedFromSuperclass() {
        @SuppressWarnings("unchecked")
        JpaRepository<InheritedRowReorderTestEntity, Long> repository = mock(JpaRepository.class);
        InheritedRowReorderTestEntity entity = new InheritedRowReorderTestEntity(1L, 10);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(entity));

        DatatableRestControllerV2<InheritedRowReorderTestEntity, Long> unrestrictedController =
                new DatatableRestControllerV2<>(repository) {};
        unrestrictedController.setRequest(controller.getRequest());

        RowReorderDto request = new RowReorderDto();
        request.setDataSrc("position");
        request.setValues(List.of(new RowReorderDto.RowReorderValue(1L, 10, 20)));

        assertEquals(Boolean.TRUE, unrestrictedController.rowReorder(controller.getRequest(), request).getBody());
        assertEquals(20, entity.getPosition());
        verify(repository).saveAll(List.of(entity));
    }

    private void assertRejectedRowReorderProperty(String dataSrc) {
        @SuppressWarnings("unchecked")
        JpaRepository<RowReorderTestEntity, Long> repository = mock(JpaRepository.class);
        RowReorderTestEntity entity = new RowReorderTestEntity(1L, 10);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(entity));

        DatatableRestControllerV2<RowReorderTestEntity, Long> unrestrictedController =
                new DatatableRestControllerV2<>(repository) {};
        unrestrictedController.setRequest(controller.getRequest());

        RowReorderDto request = new RowReorderDto();
        request.setDataSrc(dataSrc);
        request.setValues(List.of(new RowReorderDto.RowReorderValue(1L, 1, 2)));

        assertEquals(Boolean.FALSE, unrestrictedController.rowReorder(controller.getRequest(), request).getBody());
        assertEquals(1L, entity.getId());
        assertEquals(1, entity.getDomainId());
        verify(repository, never()).saveAll(any());
    }

    private static class RowReorderTestEntity {

        @Id
        @DataTableColumn(inputType = {DataTableColumnType.ID, DataTableColumnType.ROW_REORDER})
        private Long id;
        @DataTableColumn(inputType = DataTableColumnType.ROW_REORDER)
        private Integer position;
        @DataTableColumn(inputType = DataTableColumnType.ROW_REORDER)
        private Integer domainId = 1;
        private Integer unrestrictedValue = 0;

        RowReorderTestEntity(Long id, Integer position) {
            this.id = id;
            this.position = position;
        }

        public Long getId() {
            return id;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }

        public Integer getDomainId() {
            return domainId;
        }

        public void setDomainId(Integer domainId) {
            this.domainId = domainId;
        }

        public Integer getUnrestrictedValue() {
            return unrestrictedValue;
        }

        public void setUnrestrictedValue(Integer unrestrictedValue) {
            this.unrestrictedValue = unrestrictedValue;
        }
    }

    private static class RowReorderTestEntityBase {

        @DataTableColumn(inputType = DataTableColumnType.ROW_REORDER)
        private Integer position;

        RowReorderTestEntityBase(Integer position) {
            this.position = position;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }
    }

    private static class InheritedRowReorderTestEntity extends RowReorderTestEntityBase {

        @Id
        private Long id;

        InheritedRowReorderTestEntity(Long id, Integer position) {
            super(position);
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }

}
