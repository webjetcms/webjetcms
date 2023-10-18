package sk.iway.iwcm.components.restaurant_menu;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import sk.iway.iwcm.database.ActiveRecord;

@Entity
@Table(name="restaurant_menu")
public class MenuBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;
	
	@Id
	@GeneratedValue(generator="WJGen_restaurant_menu")
	@TableGenerator(name="WJGen_restaurant_menu",pkColumnValue="restaurant_menu")	
	@Column(name="menu_id")
	private int menuId;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="menu_meals_id")
	MealBean meal;
	
	@Column(name="day")
	@Temporal(TemporalType.TIMESTAMP)
	private Date day;
	@Column(name="priority")
	private Integer priority;
	@Column(name="domain_id")
	private int domainId;

	@Override
	public int getId() {
		return getMenuId();
	}

	@Override
	public void setId(int id) {
		setMenuId(id);		
	}

	public int getMenuId() {
		return menuId;
	}

	public void setMenuId(int menuId) {
		this.menuId = menuId;
	}

	public MealBean getMeal() {
		return meal;
	}

	public void setMeal(MealBean meal) {
		this.meal = meal;
	}

	public Date getDay() {
		return day;
	}

	public void setDay(Date day) {
		this.day = day;
	}

	public int getDomainId() {
		return domainId;
	}

	public void setDomainId(int domainId) {
		this.domainId = domainId;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	@Transient
	public int getDayNumber() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(day);
		return cal.get(Calendar.DAY_OF_WEEK);
	}

	@Transient
	public String getDayFormated() {
		SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
		return format1.format(day);
	}
}
