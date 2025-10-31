package sk.iway.iwcm.system.datatable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.test.TestRequest;
import sk.iway.spring.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;

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

}