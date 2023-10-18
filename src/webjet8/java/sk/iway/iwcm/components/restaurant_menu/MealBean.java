package sk.iway.iwcm.components.restaurant_menu;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

@Entity
@Table(name="restaurant_menu_meals")
public class MealBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_restaurant_menu_meals")
	@TableGenerator(name="WJGen_restaurant_menu_meals",pkColumnValue="restaurant_menu_meals")
	@Column(name="meals_id")
	private int mealId;
	@Column(name="cathegory")
	private String cathegory;
	@Column(name="name")
	private String name;
	@Column(name="description")
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String description;
	@Column(name="weight")
	private String weight;
	@Column(name="price")
	private BigDecimal price;
	@Column(name="alergens")
	private String alergens;
	@Column(name="domain_id")
	private int domainId;

	@OneToMany(fetch = FetchType.LAZY,mappedBy="meal")
	private List<MenuBean> menu;

	@Override
	public int getId() {
		return getMealId();
	}

	@Override
	public void setId(int id) {
		setMealId(id);
	}

	public int getMealId() {
		return mealId;
	}

	public void setMealId(int mealId) {
		this.mealId = mealId;
	}

	public String getCathegory() {
		if(Character.isDigit(cathegory.charAt(0)))
			return cathegory.substring(1);
		else
			return cathegory;
	}

	public void setCathegory(String cathegory) {
		this.cathegory = DB.filterHtml(cathegory);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = DB.filterHtml(name);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = DB.filterHtml(description);
	}

	public String getWeight() {
		return weight;
	}

	public void setWeight(String weight) {
		this.weight = weight;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getAlergens() {
		return alergens;
	}

	public void setAlergens(String alergens) {
		this.alergens = DB.filterHtml(alergens);
	}

	public int getDomainId() {
		return domainId;
	}

	public void setDomainId(int domainId) {
		this.domainId = domainId;
	}

	public List<MenuBean> getMenu() {
		return menu;
	}

	public void setMenu(List<MenuBean> menu) {
		this.menu = menu;
	}

}
