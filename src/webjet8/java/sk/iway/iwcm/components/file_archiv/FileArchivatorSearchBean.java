package sk.iway.iwcm.components.file_archiv;

import org.apache.poi.util.NotImplemented;

import java.util.Collection;
import java.util.Date;

public class FileArchivatorSearchBean {

    private int fileArchiveId;
    private int userId;//id usera
    private String filePath;//cesta k suboru
    private String fileName;//meno suboru
    private String virtualFileName;//meno suboru ktore zobrazujeme
    private Boolean showFile;//zobrazujeme subor na frontende ?
    //private Date dateInsert;//kedy bol subor vlozeny / pridany do archivatoru
    private Date dateInsertFrom;
    private Date dateInsertTo;
    private Date validFrom;//datum platnosti suboru od
    private Date validTo;//datum platnosti suboru do
    private Collection<String> domain;//domena pod ktorou budeme (mozeme, nemusime) zobrazovat. Domena ku ktorej subor patri
    private int referenceId;// odkaz na id hlavneho suboru -> nn_file_archive_id. Ak je toto hlavny subor tak referenceId = -1
    private int orderId;// index poradia pri zobrazovani
    private Collection<String> productCode;// kod produktu suboru
    private Collection<String> product;// produkt ku ktoremu patri subor
    private Collection<String> category;//kategoria do ktorej subor patri
    private String md5;
    private String referenceToMain;//odkaz na hlavny subor - ma vyplneny iba Vzor//field_b
    private int priority;//priorita
    private String fieldA;
    private String fieldB;
    private int fieldC;
    private long fieldD;//volne pole ak bude treba mozem pouzit neskorej
    private String fieldE;//volne pole ak bude treba mozem pouzit neskorej
    private String note;//volne pole ak bude treba mozem pouzit neskorej
    private int globalId;// globalne id pre cele "vlakno" suborov, primarne urcene pre zobrazovanie podla globalneho id
    private Date dateUploadLater;//datum a cas kedy sa ma subor nahrat
    private int uploaded;// -1 - uspesne nahrany, 0 - caka na nahranie
    private String emails;//zoznam emailov oddelenych ciarkov, na ktore sa posle notifikacia po uspesnom oneskorenom nahrati
    private long fileSize;
    private int domainId;
    private int extendedDataId;

    private Collection<String> excludeCategory;

    private String dirPath;
    private boolean includeSubdirs;
    private boolean asc;
    private boolean useCache;
    private boolean onlyMain;


    public FileArchivatorSearchBean() {
        globalId = -1;

        showFile = true;
        includeSubdirs = false;
        asc = true;
        useCache = true;
        onlyMain = true;

        filePath = null;
        fileName = null;
        virtualFileName = null;

        dateInsertFrom = null;
        dateInsertTo = null;
        validFrom = null;
        validTo = null;

        domain = null;
        productCode = null;
        product = null;
        category = null;
        md5 = null;
        referenceToMain = null;
        fieldA = null;
        fieldB = null;
        fieldC = -1;
        fieldE = null;
        note = null;
        dateUploadLater = null;
        emails = null;
        dirPath = null;

        excludeCategory = null;
    }

    public int getFileArchiveId() {
        return fileArchiveId;
    }

    public void setFileArchiveId(int fileArchiveId) {
        this.fileArchiveId = fileArchiveId;
    }

    public Collection<String> getExcludeCategory() {
        return excludeCategory;
    }

    public void setExcludeCategory(Collection<String> excludeCategory) {
        this.excludeCategory = excludeCategory;
    }

    @NotImplemented
    public int getUserId() {
        return userId;
    }

    @NotImplemented
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getVirtualFileName() {
        return virtualFileName;
    }

    public void setVirtualFileName(String virtualFileName) {
        this.virtualFileName = virtualFileName;
    }

    public Date getDateInsertFrom() {
        return dateInsertFrom;
    }

    public void setDateInsertFrom(Date dateInsertFrom) {
        this.dateInsertFrom = dateInsertFrom;
    }

    public Date getDateInsertTo() {
        return dateInsertTo;
    }

    public void setDateInsertTo(Date dateInsertTo) {
        this.dateInsertTo = dateInsertTo;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Collection<String> getDomain() {
        return domain;
    }

    public void setDomain(Collection<String> domain) {
        this.domain = domain;
    }

    public int getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(int referenceId) {
        this.referenceId = referenceId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Collection<String> getProductCode() {
        return productCode;
    }

    public void setProductCode(Collection<String> productCode) {
        this.productCode = productCode;
    }

    public Collection<String> getProduct() {
        return product;
    }

    public void setProduct(Collection<String> product) {
        this.product = product;
    }

    public Collection<String> getCategory() {
        return category;
    }

    public void setCategory(Collection<String> category) {
        this.category = category;
    }

    @NotImplemented
    public String getMd5() {
        return md5;
    }

    @NotImplemented
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @NotImplemented
    public String getReferenceToMain() {
        return referenceToMain;
    }

    @NotImplemented
    public void setReferenceToMain(String referenceToMain) {
        this.referenceToMain = referenceToMain;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getFieldA() {
        return fieldA;
    }

    public void setFieldA(String fieldA) {
        this.fieldA = fieldA;
    }

    public String getFieldB() {
        return fieldB;
    }

    public void setFieldB(String fieldB) {
        this.fieldB = fieldB;
    }

    @NotImplemented
    public int getFieldC() {
        return fieldC;
    }

    @NotImplemented
    public void setFieldC(int fieldC) {
        this.fieldC = fieldC;
    }

    @NotImplemented
    public long getFieldD() {
        return fieldD;
    }

    @NotImplemented
    public void setFieldD(long fieldD) {
        this.fieldD = fieldD;
    }

    @NotImplemented
    public String getFieldE() {
        return fieldE;
    }

    @NotImplemented
    public void setFieldE(String fieldE) {
        this.fieldE = fieldE;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getGlobalId() {
        return globalId;
    }

    public void setGlobalId(int globalId) {
        this.globalId = globalId;
    }

    public Date getDateUploadLater() {
        return dateUploadLater;
    }

    public void setDateUploadLater(Date dateUploadLater) {
        this.dateUploadLater = dateUploadLater;
    }

    public int getUploaded() {
        return uploaded;
    }

    public void setUploaded(int uploaded) {
        this.uploaded = uploaded;
    }

    public String getEmails() {
        return emails;
    }

    public void setEmails(String emails) {
        this.emails = emails;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    @NotImplemented
    public int getExtendedDataId() {
        return extendedDataId;
    }

    @NotImplemented
    public void setExtendedDataId(int extendedDataId) {
        this.extendedDataId = extendedDataId;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public boolean isIncludeSubdirs() {
        return includeSubdirs;
    }

    public void setIncludeSubdirs(boolean includeSubdirs) {
        this.includeSubdirs = includeSubdirs;
    }

    public boolean isAsc() {
        return asc;
    }

    public void setAsc(boolean asc) {
        this.asc = asc;
    }

    public Boolean getShowFile() {
        return showFile;
    }

    public void setShowFile(Boolean showFile) {
        this.showFile = showFile;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public boolean isOnlyMain() {
        return onlyMain;
    }

    public void setOnlyMain(boolean onlyMain) {
        this.onlyMain = onlyMain;
    }
}
