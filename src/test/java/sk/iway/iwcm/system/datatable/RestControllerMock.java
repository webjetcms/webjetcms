package sk.iway.iwcm.system.datatable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Mock REST controller for testing purposes
 */
public class RestControllerMock extends DatatableRestControllerV2<Object, Long> {

    private int beforeSaveCounter;
    private int afterSaveCounter;
    private int beforeDeleteCounter;
    private int afterDeleteCounter;

    private int deleteItemCounter;
    private int editItemCounter;
    private int insertItemCounter;

    @Override
    public boolean deleteItem(Object entity, long id) {
        deleteItemCounter++;
        return true;
    }

    @Override
    public Object editItem(Object entity, long id) {
        editItemCounter++;
        return entity;
    }

    @Override
    public Object insertItem(Object entity) {
        insertItemCounter++;
        return entity;
    }

    @Override
    public void beforeSave(Object entity) {
        beforeSaveCounter++;
    }

    @Override
    public boolean beforeDelete(Object entity) {
        beforeDeleteCounter++;
        return true;
    }

    @Override
    public void afterDelete(Object entity, long id) {
        afterDeleteCounter++;
    }

    @Override
    public void afterSave(Object entity, Object entity2) {
        afterSaveCounter++;
    }

    @Override
    public Page<Object> getAllItems(Pageable pageable) {
        List<Object> list = new ArrayList<>();
        list.add(new Object());
        return new DatatablePageImpl<>(list);
    }

    @Override
    public List<Object> findItemBy(String propertyName, Object original)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        return new ArrayList<>();
    }

    @Override
    public Object getOneItem(long id) {
        return new Object();
    }

    public void resetCounters() {
        beforeSaveCounter = 0;
        afterSaveCounter = 0;
        beforeDeleteCounter = 0;
        afterDeleteCounter = 0;
        deleteItemCounter = 0;
        editItemCounter = 0;
        insertItemCounter = 0;
    }

    public int getBeforeSaveCounter() {
        return beforeSaveCounter;
    }

    public int getAfterSaveCounter() {
        return afterSaveCounter;
    }

    public int getBeforeDeleteCounter() {
        return beforeDeleteCounter;
    }

    public int getAfterDeleteCounter() {
        return afterDeleteCounter;
    }

    public int getInsertItemCounter() {
        return insertItemCounter;
    }

    public int getEditItemCounter() {
        return editItemCounter;
    }

    public int getDeleteItemCounter() {
        return deleteItemCounter;
    }

}
