package sk.iway.iwcm.admin.xls;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.multipart.MultipartFile;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.xls.exception.ExcelImportServiceException;
import sk.iway.iwcm.i18n.Prop;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Abstraktna trieda pre import xlsx a xls do DB pomocou Spring repository.
 *
 * Data sa precitaju z Excl suboru, mapuju sa property podla prveho riadku.
 * Nacitane su do listu importedRows a nasledne zapisane do databazy volanim saveListToDB().
 *
 * @param <T> Entita pre zapis do DB
 */
public abstract class AbstractExcelImportService<T> implements ExcelImportServiceInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExcelImportService.class);

    private final CrudRepository<T, Long> repository;
    private MultipartFile file;

    private final ConversionService conversionService;
    private final Class<T> typeArgumentClass;
    private final Prop prop;
    private List<T> importedRows;
    private Map<String, Method> propertySetterMap;

    private List<String> firstRowNames;

    @SuppressWarnings("unchecked")
    protected AbstractExcelImportService(CrudRepository<T, Long> repository, ConversionService conversionService) {
        this.repository = repository;
        this.conversionService = conversionService;
        //noinspection unchecked
        this.typeArgumentClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), AbstractExcelImportService.class);
        this.prop = Prop.getInstance();
    }

    /**
     * Metoda pre import zaznamov do DB
     * @param file Subor so zaznamami
     */
    public void importFile(MultipartFile file) {
        if (file == null) {
            LOGGER.error("File cannot be empty");
            throw new ExcelImportServiceException(prop.getText("ExcelImportServiceAbstract.importFile.file_empty"));
        }

        this.file = file;
        if (Tools.isEmpty(this.file.getOriginalFilename()) && !this.file.getOriginalFilename().endsWith("xlsx") && !this.file.getOriginalFilename().endsWith("xls")) {
            LOGGER.error("Filename doesnt end with xls or xlsx. Filename: {}", this.file.getOriginalFilename());
            throw new ExcelImportServiceException(prop.getText("ExcelImportServiceAbstract.importFile.filename_not_xls"));
        }

        LOGGER.trace("Loaded file, filename: {}", this.file.getOriginalFilename());

        Optional<InputStream> inputStreamOptional = getInputStreamOptional();
        if (!inputStreamOptional.isPresent()) {
            LOGGER.error("InputStream is not readable");
            throw new ExcelImportServiceException(prop.getText("ExcelImportServiceAbstract.importFile.input_stream_not_readale"));
        }

        Optional<Workbook> workbookOptional = createWorkbookOptional(inputStreamOptional.get());
        if (!workbookOptional.isPresent()) {
            LOGGER.error("Workbook is not readable");
            throw new ExcelImportServiceException(prop.getText("ExcelImportServiceAbstract.importFile.workbook_not_readale"));
        }
        LOGGER.trace("Workbook loaded");

        setAndClearImportedRows();
        setPropertyToSetterMap();
        Workbook workbook = workbookOptional.get();

        beforeWorkbook();
        for (Sheet sheet : workbook) {
            beforeSheet(sheet);
            LOGGER.debug("Sheet name: {}", sheet.getSheetName());
            setFirstRowNames(sheet);

            if (firstRowNames.isEmpty()) {
                LOGGER.error("Nothing to import found on this sheet");
                continue;
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    LOGGER.trace("Skipping row number 0");
                    continue;
                }

                beforeRow(row);
                Optional<T> optionalBean = convertRowToBeanAndAddToList(row);
                optionalBean.ifPresent(this::afterRow);
            }

            afterSheet(sheet);
        }

        afterWorkbook(importedRows);


        try {
            saveListToDB();
        }
        catch (ConstraintViolationException e) {
            ExcelImportServiceException excelImportServiceException = new ExcelImportServiceException();
            Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
            for (ConstraintViolation<?> constraintViolation : constraintViolations) {
                excelImportServiceException.addError(constraintViolation.getMessage());
            }

            throw excelImportServiceException;
        }
    }

    /**
     * Metoda pre nastavenie, prip. zmazanie zoznamu entit
     */
    private void setAndClearImportedRows() {
        if (importedRows == null) {
            importedRows = new ArrayList<>();
        }

        if (!importedRows.isEmpty()) {
            importedRows.clear();
        }
    }

    /**
     * Metoda pre ulozenie entit do DB
     */
    public void saveListToDB() {
        if (importedRows.isEmpty()) {
            LOGGER.debug("saveListToDB - List is empty");
            return;
        }

        repository.saveAll(importedRows);
    }

    /**
     * Metoda pre nastavenie hash mapy s klucom, ktory je nazvov stlpca a hodnotou, ktora je pozadovany seter na entite
     */
    private void setPropertyToSetterMap() {
        if (propertySetterMap == null) {
            propertySetterMap = new HashMap<>();
        }

        if (!propertySetterMap.isEmpty()) {
            propertySetterMap.clear();
        }

        for (Method declaredMethod : typeArgumentClass.getDeclaredMethods()) {
            if (!declaredMethod.getName().startsWith("set")) {
                LOGGER.trace("setPropertySetterMap - Method name {} doesnt start with set, skipped", declaredMethod.getName());
                continue;
            }

            String propertyName = declaredMethod.getName().toLowerCase().substring(3);
            propertySetterMap.put(propertyName, declaredMethod);
        }
    }

    /**
     * Metoda pre nastavenie zoznamu nazvov stlpcov
     * @param sheet Sheet
     */
    private void setFirstRowNames(Sheet sheet) {
        if (firstRowNames == null) {
            firstRowNames = new ArrayList<>();
        }

        if (!firstRowNames.isEmpty()) {
            firstRowNames.clear();
        }

        List<String> missingProperties = new ArrayList<>();
        for (Cell cell : sheet.getRow(0)) {
            firstRowNames.add(cell.getStringCellValue());

            if (!propertySetterMap.containsKey(cell.getStringCellValue().toLowerCase())) {
                missingProperties.add(cell.getStringCellValue());
            }
        }

        if (!missingProperties.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("setFirstRowNames - Properties: [{}] not found on class {}", Tools.join(missingProperties, ", "), typeArgumentClass.getName());
        }
    }

    /**
     * Metoda pre konvertovanie riadku na entitu
     * @param row riadok excelu
     * @return Optional entitu
     */
    public Optional<T> convertRowToBeanAndAddToList(Row row) {
        try {
            T object = BeanUtils.instantiateClass(typeArgumentClass);
            int i = 0;
            int changesCounter = 0;
            for (Cell cell : row) {
                String property = firstRowNames.get(i);
                i++;
                Object value = getTypedValueFromCell(cell);
                if (!propertySetterMap.containsKey(property.toLowerCase())) {
                    LOGGER.trace("convertRowToBeanAndAddToList - Property {}, value: {} skipped", property, value);
                    continue;
                }

                Method method = propertySetterMap.get(property.toLowerCase());
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    LOGGER.error("convertRowToBeanAndAddToList - Method {} has more than one parameter types", method.getName());
                    continue;
                }

                Object retypedValue = retypeValue(value, parameterTypes[0]);
                if (retypedValue != null) {
                    method.invoke(object, retypedValue);
                    changesCounter++;
                }
            }

            if (changesCounter > 0) {
                importedRows.add(object);
            }

            return Optional.of(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return Optional.empty();
    }

    /**
     * Metoda pre navrat spravne typovej hodnoty z excelu
     * @param cell bunka
     * @return Objekt prisluchajuci hodnote bunky
     */
    private Object getTypedValueFromCell(Cell cell) {
        Object value;
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case BOOLEAN:
                value = cell.getBooleanCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue();
                } else {
                    value = cell.getNumericCellValue();
                }
                break;
            case ERROR:
                value = cell.getErrorCellValue();
                break;

            case BLANK:
                value = "";
                break;

            default:
                value = cell.getCellFormula();
                break;
        }

        return value;
    }

    /**
     * Pretypovanie hodnoty z excelu na pozadovanu hodnotu entity
     * @param value hodnota
     * @param parameterType typ premennej z entity
     * @return spravne pretypovany Objekt
     */
    private Object retypeValue(Object value, Class<?> parameterType) {
        if (conversionService.canConvert(value.getClass(), parameterType)) {
            return conversionService.convert(value, parameterType);
        }

        LOGGER.debug("retypeValue - Returning default value: {}, type: {}", value, value.getClass().getName());
        return value;
    }

    /**
     * Metoda pre vytvorenie workbooku
     * @param inputStream InputStream
     * @return Optional<Workbook>
     */
    private Optional<Workbook> createWorkbookOptional(InputStream inputStream) {
        try {
            return Optional.of(WorkbookFactory.create(inputStream));
        } catch (IOException e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return Optional.empty();
    }

    /**
     * Metoda pre vytvorenie InputStreamu zo suboru
     * @return Optional<InputStream>
     */
    private Optional<InputStream> getInputStreamOptional() {
        try {
            return Optional.of(new BufferedInputStream(this.file.getInputStream()));
        } catch (IOException e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return Optional.empty();
    }

    /**
     * Metoda urcena na Override, volana pred iteraciou harkov
     */
    public void beforeWorkbook() {
        LOGGER.debug("beforeSheets - Not implemented");
    }

    /**
     * Metoda urcena na Override, volana po iteracii harkov
     */
    public void afterWorkbook(List<T> list) {
        LOGGER.debug("afterSheets - Not implemented, list is empty: {}", list.isEmpty());
    }

    /**
     * Metoda urcena na Override, volana pred ziskanim kazdeho riadku excelu
     */
    public void beforeRow(Row row) {
        LOGGER.debug("beforeRow - Not implemented");
    }

    /**
     * Metoda urcena na Override, volana po ziskanim kazdeho riadku excelu a konvertovanim na entitu
     */
    public void afterRow(T bean) {
        LOGGER.debug("afterRow - Not implemented, bean: {}", bean);
    }

    /**
     * Metoda urcena na Override, volana pred importovanim harku
     */
    private void beforeSheet(Sheet sheet) {
        LOGGER.debug("beforeSheet - Not implemented, sheet: {}", sheet);
    }

    /**
     * Metoda urcena na Override, volana po importovani harku
     */
    private void afterSheet(Sheet sheet) {
        LOGGER.debug("afterSheet - Not implemented, sheet: {}", sheet);
    }

    /**
     * Metoda pre navrat nazvov stlpcov
     */
    public List<String> getFirstRowNames() {
        return firstRowNames;
    }
}
