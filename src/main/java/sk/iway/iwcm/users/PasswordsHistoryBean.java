package sk.iway.iwcm.users;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import sk.iway.iwcm.database.ActiveRecord;


/**
 *  #23471 - Password security - pri zmene hesla sa musi kontrolovat, ci heslo nie je v historii pouzitych hesiel usera.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 10.01.2018 14:52:01
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="passwords_history")
public class PasswordsHistoryBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_passwords_history")
	@TableGenerator(name="WJGen_passwords_history",pkColumnValue="passwords_history")	
	@Column(name="passwords_history_id")
	private int passwordsHistoryId;
	@Column(name="user_id")
	int userId;
	@Column(name="password")
	String password;
	@Column(name="salt")
	String salt;
	@Column(name="save_date")
	@Temporal(TemporalType.TIMESTAMP)
    Date saveDate;

    public PasswordsHistoryBean(){
        super();
    }

    public PasswordsHistoryBean(int userId, String password, String salt) {
        this.userId = userId;
        this.password = password;
        this.salt = salt;
    }

    /**Ak taky zaznam neexistuje, tak ho ulozi
     * 
     */
    public static PasswordsHistoryBean insertAndSaveNew(int userId, String hash, String salt)
    {
        PasswordsHistoryBean passwordsHistoryBean = new PasswordsHistoryBean();
        passwordsHistoryBean.setUserId(userId);
        passwordsHistoryBean.setPassword(hash);
        passwordsHistoryBean.setSalt(salt);
        passwordsHistoryBean.saveIfNotExistsAndDeleteOld();
        return passwordsHistoryBean;
    }

    public int getPasswordsHistoryId()
	{
		return passwordsHistoryId;
	}
	
	public void setPasswordsHistoryId(int passwordsHistoryId)
	{
		this.passwordsHistoryId = passwordsHistoryId;
	}
	
	@Override
	public void setId(int id)
	{
		setPasswordsHistoryId(id);
	}
	
	@Override
	public int getId()
	{
		return getPasswordsHistoryId();
	}
	
	public int getUserId()
	{
		return userId;
	}

	public void setUserId(int userId)
	{
		this.userId = userId;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getSalt()
	{
		return salt;
	}

	public void setSalt(String salt)
	{
		this.salt = salt;
	}

	public Date getSaveDate()
	{
		return saveDate;
	}

	public void setSaveDate(Date saveDate)
	{
		this.saveDate = saveDate;
	}

	/** Ulozi a nastavi datum ulozenia (ak nebol nastaveny)
	 * 
	 */
    public boolean save()
    {
        if(getSaveDate() == null)
            setSaveDate(new Date());
        return super.save();
    }
    
    /** Ulozi ho iba ak uz neexistuje taky password v historii
     * 
     * @return
     */
    public boolean saveIfNotExists()
    {    	
    	if(PasswordsHistoryDB.existsPassword(getUserId(), getPassword()))
    		return false;
        return save();
    }

    /** Ak neexistuje taky zaznam, tak ho ulozi a zmaze najstarsi
     * 
     * @return
     */
    public boolean saveIfNotExistsAndDeleteOld()
    {
    	boolean isSaved = saveIfNotExists(); 
        if(isSaved)
        	PasswordsHistoryDB.getInstance().deleteOld(getUserId());
        return isSaved;
    }
}

/*
mysql
java.sql.SQLSyntaxErrorException: ORA-00900: neplatný príkaz SQL

CREATE TABLE passwords_history (
passwords_history_id INT NOT NULL PRIMARY KEY,
user_id INT,
password VARCHAR(128),
salt VARCHAR(64),
save_date DATETIME);
INSERT INTO pkey_generator VALUES('passwords_history', 1 , 'passwords_history', 'passwords_history_id');

mssql
java.sql.SQLSyntaxErrorException: ORA-00900: neplatný príkaz SQL

CREATE TABLE passwords_history (
[passwords_history_id] [INT] NOT NULL PRIMARY KEY,
user_id [INT],
password [NVARCHAR](128),
salt [NVARCHAR](64),
save_date [DATETIME]);
INSERT INTO pkey_generator VALUES('passwords_history', 1 , 'passwords_history', 'passwords_history_id');

oracle
java.sql.SQLSyntaxErrorException: ORA-00900: neplatný príkaz SQL

CREATE TABLE passwords_history (
passwords_history_id INT NOT NULL PRIMARY KEY,
user_id INTEGER,
password NVARCHAR2(128),
salt NVARCHAR2(64),
save_date DATE);
INSERT INTO pkey_generator VALUES('passwords_history', 1 , 'passwords_history', 'passwords_history_id');
PKEY:
INSERT INTO pkey_generator VALUES('passwords_history', 1 , 'passwords_history', 'passwords_history_id');
*/