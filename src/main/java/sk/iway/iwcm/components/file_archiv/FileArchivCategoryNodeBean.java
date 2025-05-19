package sk.iway.iwcm.components.file_archiv;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;


/**
 *  FileArchivCategoryNodeBean.java - Bean pre uchovavanie informacie o danej kategorii
 *
 * Title        webjet7
 * Company      Interway s.r.o. (www.interway.sk)
 * Copyright    Interway s.r.o. (c) 2001-2010
 * @author       $Author: prau $
 * @version      $Revision: 1.3 $
 * created      Date: 03.01.2019 08:59:21
 * modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="file_archiv_category_node")
public class FileArchivCategoryNodeBean extends sk.iway.iwcm.database.nestedsets.CommonNestedSetBean<FileArchivCategoryNodeBean> implements Serializable
{
	private static final long serialVersionUID = -1L;

    public FileArchivCategoryNodeBean() {
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name="category_type")
    FileArchivNodeTypeEnum categoryType;
	@Column(name="category_name")
	String categoryName;
	@Column(name="date_insert")
	@Temporal(TemporalType.TIMESTAMP)
    Date dateInsert;
	@Column(name="string1Name")
	String string1Name;
	@Column(name="string2Name")
	String string2Name;
	@Column(name="string3Name")
	String string3Name;
	@Column(name="int1Val")
	int int1Val;
	@Column(name="int2Val")
	int int2Val;
	@Column(name="int3Val")
	int int3Val;
    @Column(name="sort_priority")
    int priority;
    @Column(name="is_show")
    boolean isShow;

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isShow() {
        return isShow;
    }

    public void setShow(boolean show) {
        isShow = show;
    }

    public String getCategoryName()
	{
		return categoryName;
	}

	public void setCategoryName(String categoryName)
	{
        setDateInsert(new Date());
		this.categoryName = categoryName;
	}

	public Date getDateInsert()
	{
		return dateInsert;
	}

	public void setDateInsert(Date dateInsert)
	{
		this.dateInsert = dateInsert;
	}

	public String getString1Name()
	{
		return string1Name;
	}

	public void setString1Name(String string1Name)
	{
		this.string1Name = string1Name;
	}

	public String getString2Name()
	{
		return string2Name;
	}

	public void setString2Name(String string2Name)
	{
		this.string2Name = string2Name;
	}

	public String getString3Name()
	{
		return string3Name;
	}

	public void setString3Name(String string3Name)
	{
		this.string3Name = string3Name;
	}

	public int getInt1Val()
	{
		return int1Val;
	}

	public void setInt1Val(int int1Val)
	{
		this.int1Val = int1Val;
	}

	public int getInt2Val()
	{
		return int2Val;
	}

	public void setInt2Val(int int2Val)
	{
		this.int2Val = int2Val;
	}

	public int getInt3Val()
	{
		return int3Val;
	}

	public void setInt3Val(int int3Val)
	{
		this.int3Val = int3Val;
	}

    public FileArchivNodeTypeEnum getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(FileArchivNodeTypeEnum categoryType) {
        this.categoryType = categoryType;
    }

    @Override
    public boolean save()
    {
        setDateInsert(new Date());
        return super.save();
    }
}

/*
    jpa file_archiv_category_node sk.iway.iwcm.components.file_archiv.FileArchivCategoryNodeBean lft:int rgt:int rgt:int  level:int rootid:int category_name:string date_insert:date string1Name:String string2Name:String string3Name:String int1Val:int int2Val:int int3Val:int

    mysql
    java.sql.SQLException: Could not find stored procedure 'SHOW'.
    CREATE TABLE file_archiv_category_node (
    id INT NOT NULL PRIMARY KEY,
    lft INT NOT NULL ,
    rgt INT NOT NULL ,
    level INT NOT NULL ,
    rootid INT NOT NULL ,
    category_name VARCHAR(500),
    date_insert DATETIME,
    string1Name VARCHAR(500),
    string2Name VARCHAR(500),
    string3Name VARCHAR(500),
    int1Val INT,
    int2Val INT,
    int3Val INT,
    category_type INT);
    INSERT INTO pkey_generator VALUES('file_archiv_category_node', 1 , 'file_archiv_category_node', 'file_archiv_category_node_id')

    mssql
    java.sql.SQLException: Could not find stored procedure 'SHOW'.
    CREATE TABLE file_archiv_category_node (
    [id] [INT] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    lft [INT],
    rgt [INT],
    level [INT],
    rootid [INT],
    category_name [NVARCHAR](255),
    date_insert [DATETIME],
    string1Name [NVARCHAR](500),
    string2Name [NVARCHAR](500),
    string3Name [NVARCHAR](500),
    int1Val [INT],
    int2Val [INT],
    int3Val [INT],
    category_type [INT]);
    INSERT INTO pkey_generator VALUES('file_archiv_category_node', 1 , 'file_archiv_category_node', 'file_archiv_category_node_id')

    ALTER TABLE file_archiv_category_node ADD
    priority [INT],
    is_show [INT];

    oracle
    java.sql.SQLException: Could not find stored procedure 'SHOW'.
    CREATE TABLE file_archiv_category_node (
    file_archiv_category_node_id INT NOT NULL PRIMARY KEY,
    lft INTEGER,
    rgt INTEGER,
    level INTEGER,
    rootid INTEGER,
    category_name NVARCHAR2(255),
    date_insert DATE,
    string1Name String,
    string2Name String,
    string3Name String,
    int1Val INTEGER,
    int2Val INTEGER,
    int3Val INTEGER);
    INSERT INTO pkey_generator VALUES('file_archiv_category_node', 1 , 'file_archiv_category_node', 'file_archiv_category_node_id')
    PKEY:
    INSERT INTO pkey_generator (name, value, table_name, table_pkey_name) VALUES('file_archiv_category_node', 1 , 'file_archiv_category_node', 'id');

    */