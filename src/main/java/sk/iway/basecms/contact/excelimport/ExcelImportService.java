package sk.iway.basecms.contact.excelimport;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import sk.iway.basecms.contact.ContactEntity;
import sk.iway.iwcm.admin.xls.AbstractExcelImportService;

/**
 * Ukazkova service trieda pre import z Excel suboru do Spring DATA repozitara
 */
@Service
@RequestScope
public class ExcelImportService extends AbstractExcelImportService<ContactEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelImportService.class);

    public ExcelImportService(CrudRepository<ContactEntity, Long> repository, ConversionService conversionService) {
        super(repository, conversionService);
    }

    /**
     * Metoda sa vola pred konvertovanim udajov do beanu, umoznuje modifikovat data
     */
    @Override
    public void beforeRow(Row row) {
        LOGGER.debug("beforeRow cell: {}", row.getRowNum());

        int i = 0;
        for (Cell cell : row) {
            String columnName = getFirstRowNames().get(i);

            //vygeneruj nahodne telefonne cislo pre prvy riadok
            if (row.getRowNum() == 1 && "phone".equals(columnName)) {
                cell.setCellValue("+421 90" + RandomStringUtils.random(1, false, true) + " " + RandomStringUtils.random(3, false, true) + " " + RandomStringUtils.random(3, false, true));
            }

            //uprav format PSC, Excel sa nam to snazi dat ako cislo
            if ("zip".equals(columnName) && cell.getCellType().equals(CellType.NUMERIC)) {
                //ponechaj len celociselnu (int) cast cisla, lebo PSC 123456 importuje ako 123456.0
                cell.setCellValue(String.valueOf((int)cell.getNumericCellValue()));
            }

            i++;
        }
    }

    /**
     * Metoda sa vola po konvertovani riadku do beanu, pred jeho ulozenim do databazy
     */
    @Override
    public void afterRow(ContactEntity bean) {
        LOGGER.debug("afterRow: {}", bean.getName());
    }
}
