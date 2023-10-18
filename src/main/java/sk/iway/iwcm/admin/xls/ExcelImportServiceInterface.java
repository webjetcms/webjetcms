package sk.iway.iwcm.admin.xls;

import org.springframework.web.multipart.MultipartFile;

public interface ExcelImportServiceInterface {
    void importFile(MultipartFile file);
}