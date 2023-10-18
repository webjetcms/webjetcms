package sk.iway.iwcm.components.file_archiv;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.beanutils.PropertyUtils;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  FileArchivatorBean.java - Sluzi na archivaciu suborov. Mame cestu k suboru a ostatne parametre si drzime v databaze
 *
 * Title        webjet7
 * Company      Interway s.r.o. (www.interway.sk)
 * Copyright    Interway s.r.o. (c) 2001-2010
 * @author       $Author: prau $
 * @version      $Revision: 1.3 $
 * created      Date: 04.02.2015 15:51:21
 * modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="file_archiv")
public class FileArchivatorBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_file_archiv")
	@TableGenerator(name="WJGen_file_archiv",pkColumnValue="file_archiv")
	@Column(name="file_archiv_id")
	private int nnFileArchiveId;
	@Column(name="user_id")
	int userId;//id usera
	@Column(name="file_path")
	String filePath;//cesta k suboru
	@Column(name="file_name")
	String fileName;//meno suboru
	@Column(name="virtual_file_name")
	String virtualFileName;//meno suboru ktore zobrazujeme
	@Column(name="show_file")
	boolean showFile;//zobrazujeme subor na frontende ?
	@Column(name="date_insert")
	@Temporal(TemporalType.TIMESTAMP)
	Date dateInsert;//kedy bol subor vlozeny / pridany do archivatoru
	@Column(name="valid_from")
	@Temporal(TemporalType.TIMESTAMP)
	Date validFrom;//datum platnosti suboru od
	@Column(name="valid_to")
	@Temporal(TemporalType.TIMESTAMP)
	Date validTo;//datum platnosti suboru do
	@Column(name="domain")
	String domain;//domena pod ktorou budeme (mozeme, nemusime) zobrazovat. Domena ku ktorej subor patri
	@Column(name="reference_id")
	int referenceId;// odkaz na id hlavneho suboru -> nn_file_archive_id. Ak je toto hlavny subor tak referenceId = -1
	@Column(name="order_id")
	int orderId;// index poradia pri zobrazovani
	@Column(name="product_code")
	String productCode;// kod produktu suboru
	@Column(name="product")
	String product;// produkt ku ktoremu patri subor
	@Column(name="category")
	String category;//kategoria do ktorej subor patri
	@Column(name="md5")//MD5 - ak bude treba mozeme prepisat, nepozadovali to.//field_a
	String md5;//field_a
	@Column(name="reference_to_main")//odkaz na hlavny subor - ma vyplneny iba Vzor//field_b
	String referenceToMain;//field_b
	@Column(name="priority")//priorita//field_c
	int priority;//field_c
	@Column(name="field_a")
	String fieldA;
	@Column(name="field_b")
	String fieldB;
	@Column(name="field_c")
	int fieldC;
	@Column(name="field_d")
	long fieldD;//volne pole ak bude treba mozem pouzit neskorej
	@Column(name="field_e")
	String fieldE;//volne pole ak bude treba mozem pouzit neskorej
	@Column(name="note")
	String note;//volne pole ak bude treba mozem pouzit neskorej
	@Column(name="global_id")
	int globalId;// globalne id pre cele "vlakno" suborov, primarne urcene pre zobrazovanie podla globalneho id
	@Column(name="date_upload_later")//datum a cas kedy sa ma subor nahrat
	@Temporal(TemporalType.TIMESTAMP)
	Date dateUploadLater;
	@Column(name="uploaded")// -1 - uspesne nahrany, 0 - caka na nahranie
	int uploaded;
	@Column(name="emails")//zoznam emailov oddelenych ciarkov, na ktore sa posle notifikacia po uspesnom oneskorenom nahrati
	String emails;
	@Column(name="file_size")
	long fileSize;
	@Column(name="domain_id")
	int domainId;
    @Column(name="extended_data_id")
	int extendedDataId;

	public FileArchivatorBean()
	{
		uploaded = -1;
		referenceId = -1;
		orderId = -1;
	}

	public FileArchivatorBean(int userId,String filePath, String fileName, String virtualFileName, boolean showFile, Date validFrom, Date validTo, String domain, String productCode, String product, String category, int referenceId, int orderId, String md5)
	{
		init(userId, filePath, fileName, virtualFileName, showFile, validFrom, validTo, domain, productCode, product, category, referenceId, orderId, md5, -1, null, null);
	}

	public FileArchivatorBean(int userId,String filePath, String fileName, String virtualFileName, boolean showFile, Date validFrom, Date validTo, String domain, String productCode, String product, String category, String md5, int priority, String patern_ref, String note)
	{
		init(userId, filePath, fileName, virtualFileName, showFile, validFrom, validTo, domain, productCode, product, category, -1, -1, md5, priority, patern_ref, note);
	}

	public FileArchivatorBean(int userId,String filePath, String fileName, String virtualFileName, boolean showFile )
	{
		init(userId, filePath, fileName, virtualFileName, showFile, null, null, null, null, null, null, -1, -1, null, -1, null, null);
	}

	public void init(int userId,String filePath, String fileName, String virtualFileName, boolean showFile, Date validFrom, Date validTo, String domain, String productCode, String product, String category, int referenceId, int orderId, String md5, int priority, String patern_ref, String note)
	{
		this.userId = userId;
		this.filePath = filePath;
		this.fileName = fileName;
		this.virtualFileName = virtualFileName;
		this.showFile = showFile;
		this.dateInsert = new Date();
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.domain = domain;
		this.referenceId = referenceId;
		this.orderId = orderId;
		this.productCode = productCode;
		this.product = product;
		this.category = category;
		this.md5 = md5;
		this.priority = priority;
		this.referenceToMain = patern_ref;
		this.note = note;
		this.globalId = -1;
	}

	/**
	 * Vrati true ak su platne datumy (alebo su null) pre aktualny cas
	 */
	public boolean isValidDates()
	{
		if (validFrom == null || validTo == null) return true;

		Date now = new Date(Tools.getNow());

		if (validFrom != null && validFrom.after(now)) return false;
		else if (validTo != null && validTo.before(now)) return false;
		else if (validFrom!=null && validTo!=null && (validFrom.before(now) && validTo.after(now))==false) return false;

		return false;
	}

	public int getNnFileArchiveId()
	{
		return nnFileArchiveId;
	}

	public void setNnFileArchiveId(int nnFileArchiveId)
	{
		this.nnFileArchiveId = nnFileArchiveId;
	}

	@Override
	public void setId(int id)
	{
		setNnFileArchiveId(id);
	}

	@Override
	public int getId()
	{
		return getNnFileArchiveId();
	}

	public int getUserId()
	{
		return userId;
	}

	public void setUserId(int userId)
	{
		this.userId = userId;
	}

	public String getFilePath()
	{
		return filePath;
	}

	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public String getVirtualFileName()
	{
		return virtualFileName;
	}

	public void setVirtualFileName(String virtualFileName)
	{
		this.virtualFileName = virtualFileName;
	}

	public boolean getShowFile()
	{
		return showFile;
	}

	public void setShowFile(boolean showFile)
	{
		this.showFile = showFile;
	}

	public Date getDateInsert()
	{
		return dateInsert;
	}

	public void setDateInsert(Date dateInsert)
	{
		this.dateInsert = dateInsert;
	}

	public Date getValidFrom()
	{
		return validFrom;
	}

	public void setValidFrom(Date validFrom)
	{
		this.validFrom = validFrom;
	}

	public Date getValidTo()
	{
		return validTo;
	}

	public void setValidTo(Date validTo)
	{
		this.validTo = validTo;
	}

	public String getDomain()
	{
		return domain;
	}

	public void setDomain(String domain)
	{
		this.domain = domain;
	}

	public int getReferenceId()
	{
		return referenceId;
	}

	public void setReferenceId(int referenceId)
	{
		this.referenceId = referenceId;
	}

	public int getOrderId()
	{
		return orderId;
	}

	public void setOrderId(int orderId)
	{
		this.orderId = orderId;
	}

	public String getProductCode()
	{
		return productCode;
	}

	public void setProductCode(String productCode)
	{
		this.productCode = productCode;
	}

	public String getProduct()
	{
		return product;
	}

	public void setProduct(String product)
	{
		this.product = product;
	}

	public String getCategory()
	{
		return category;
	}

	public void setCategory(String category)
	{
		this.category = category;
	}

	public String getMd5()
	{
		return md5;
	}

	public void setMd5(String md5)
	{
		this.md5 = md5;
	}

	public String getReferenceToMain()
	{
		return referenceToMain;
	}

	public void setReferenceToMain(String referenceToMain)
	{
		this.referenceToMain = referenceToMain != null ? (referenceToMain.trim().startsWith("/") ? referenceToMain.trim().substring(1) : referenceToMain.trim()) : null;
	}

	public int getPriority()
	{
		return priority;
	}

	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	public String getFieldA()
	{
		return fieldA;
	}

	public void setFieldA(String fieldA)
	{
		this.fieldA = fieldA;
	}

	public String getFieldB()
	{
		return fieldB;
	}

	public void setFieldB(String fieldB)
	{
		this.fieldB = fieldB;
	}

	public int getFieldC()
	{
		return fieldC;
	}

	public void setFieldC(int fieldC)
	{
		this.fieldC = fieldC;
	}

	public long getFieldD()
	{
		return fieldD;
	}

	public void setFieldD(long fieldD)
	{
		this.fieldD = fieldD;
	}

	public String getFieldE()
	{
		return fieldE;
	}

	public void setFieldE(String fieldE)
	{
		this.fieldE = fieldE;
	}

	public String getNote()
	{
		return note;
	}

	public void setNote(String note)
	{
		this.note = note;
	}

	public int getGlobalId()
	{
		return globalId;
	}

	public void setGlobalId(int globalId)
	{
		this.globalId = globalId;
	}

	private void deleteIndexedFile(String url)
	{
		int docId = DocDB.getInstance().getDocIdFromURLImpl(url+".html", GroupsDB.getInstance().getGroup(getDomainId()).getDomainName());
		if (docId > 0)
			DocDB.deleteDoc(docId, null);
	}

	@Override
	public boolean save()
	{
		if(dateInsert == null)
			dateInsert = new Date();
		//musim tu vymazat cache
		FileArchivatorKit.deleteFileArchiveCache();

		UserDetails user = UsersDB.getUser(userId);
		//najprv zmazem stary index
		if(getId() > 0)
			deleteIndexedFile("/"+FileArchivatorDB.getInstance().getById(getId()).getVirtualPath());

		boolean save = super.save();

		if(save)
		{
			 //ak je subor validny a je ako zobrazovat
			 if(getShowFile() && isValidDates())
			 {
			 	 //indexujem bud iba hlavny subor, alebo ked je povolene, tak vsetky
			 	 boolean index = getReferenceId() < 1 || Constants.getBoolean("fileArchivIndexOnlyMainFiles") == false;
				 if(index)
						FileIndexerTools.indexFile("/"+getVirtualPath(), user);
			 }
		}

		return save;
	}

	@Override
	public boolean delete()
	{
		//musim tu vymazat cache
		FileArchivatorKit.deleteFileArchiveCache();

		//zmazem index
		deleteIndexedFile("/"+getVirtualPath());

      Adminlog.add(Adminlog.TYPE_FILE_DELETE,"File Archiv mazeme subor: "+getVirtualPath()+" "+this,-1,-1);
		return super.delete();
	}


	@Override
	public String toString()
	{
		StringBuilder toString = new StringBuilder();
		for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(this.getClass()))
		{
			try{
				String property = descriptor.getName();
				if("historyFiles".equals(property))
				    continue;
				Object value = PropertyUtils.getProperty(this, property);
				toString.append(property).append(" = ");
				toString.append(value).append(',');
			}
			catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return toString.toString();
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

	public long getFileSizeKB() {
		return fileSize / 1024;
	}

	public double getFileSizeMB() {
		return getFileSizeKB() / 1024d;
	}

	public int getDomainId() {
		return domainId;
	}

	public void setDomainId(int domainId) {
		this.domainId = domainId;
	}

	public List<FileArchivatorBean> getHistoryFiles()
    {
        return FileArchivatorDB.getByReferenceId(this.getId());
    }

    public String getVirtualPath()
    {
        return getFilePath()+getFileName();
    }

    public int getExtendedDataId() {
        return extendedDataId;
    }

    public void setExtendedDataId(int extendedDataId) {
        this.extendedDataId = extendedDataId;
    }

    /*

	jpa nn_file_archives sk.iway.nn.manage_files.FileArchivatorBean user_id:int file_path:string file_name:string virtual_file_name:string show_file:boolean date_insert:date valid_from:date valid_to:date domain:string reference_id:int order_id:int product_code:string product:string category:string field_a:string field_b:string field_c:int field_d:int field_e:date

	--!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	ALTER TABLE nn_file_archives ADD(global_id INT); --8.7.2015
	INSERT INTO pkey_generator VALUES('nn_file_archives_global_id' , 'nn_file_archives', 'global_id',1);

	ALTER TABLE nn_file_archives ADD(note NVARCHAR2(1100));
	--!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

	mysql
	java.sql.SQLException: ORA-00900: neplatný príkaz SQL

	CREATE TABLE nn_file_archives (
	nn_file_archive_id INT NOT NULL PRIMARY KEY,
	user_id INT,
	file_path VARCHAR(255),
	file_name VARCHAR(255),
	virtual_file_name VARCHAR(255),
	show_file BIT(1),
	date_insert DATETIME,
	valid_from DATETIME,
	valid_to DATETIME,
	domain VARCHAR(255),
	reference_id INT,
	order_id INT,
	product_code VARCHAR(255),
	product VARCHAR(255),
	category VARCHAR(255),
	field_a VARCHAR(255),
	field_b VARCHAR(255),
	field_c INT,
	field_d INT,
	field_e DATETIME);
	INSERT INTO pkey_generator VALUES('nn_file_archives', 1 , 'nn_file_archives', 'nn_file_archive_id')

	mssql
	java.sql.SQLException: ORA-00900: neplatný príkaz SQL

	CREATE TABLE nn_file_archives (
	[nn_file_archive_id] [INT] NOT NULL PRIMARY KEY,
	user_id [INT],
	file_path [NVARCHAR](255),
	file_name [NVARCHAR](255),
	virtual_file_name [NVARCHAR](255),
	show_file [BIT],
	date_insert [DATETIME],
	valid_from [DATETIME],
	valid_to [DATETIME],
	domain [NVARCHAR](255),
	reference_id [INT],
	order_id [INT],
	product_code [NVARCHAR](255),
	product [NVARCHAR](255),
	category [NVARCHAR](255),
	field_a [NVARCHAR](255),
	field_b [NVARCHAR](255),
	field_c [INT],
	field_d [INT],
	field_e [DATETIME]);
	INSERT INTO pkey_generator VALUES('nn_file_archives', 1 , 'nn_file_archives', 'nn_file_archive_id')

	oracle
	java.sql.SQLException: ORA-00900: neplatný príkaz SQL

	CREATE TABLE nn_file_archives (
	nn_file_archive_id INT NOT NULL PRIMARY KEY,
	user_id INTEGER,
	file_path NVARCHAR2(255),
	file_name NVARCHAR2(255),
	virtual_file_name NVARCHAR2(255),
	show_file SMALLINT,
	date_insert DATE,
	valid_from DATE,
	valid_to DATE,
	domain NVARCHAR2(255),
	reference_id INTEGER,
	order_id INTEGER,
	product_code NVARCHAR2(255),
	product NVARCHAR2(255),
	category NVARCHAR2(255),
	field_a NVARCHAR2(255),
	field_b NVARCHAR2(255),
	field_c INTEGER,
	field_d INTEGER,
	field_e DATE);
	INSERT INTO pkey_generator VALUES('nn_file_archives','nn_file_archives', 'nn_file_archive_id', 1);
	*/

	/*
	ALTER TABLE NN_FILE_ARCHIVES
	  ADD (date_upload_later DATE,
	       uploaded NUMBER DEFAULT -1,
				 emails NVARCHAR2(1100));
	 */
}
