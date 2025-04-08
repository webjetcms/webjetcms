package sk.iway.iwcm.components.restaurant_menu.jpa;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "restaurant_menu")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_RESTAURANT_MENU)
public class RestaurantMenuEntity {

    @Id
    @Column(name = "menu_id")
    @GeneratedValue(generator="WJGen_restaurant_menu")
	@TableGenerator(name="WJGen_restaurant_menu",pkColumnValue="restaurant_menu")
    @DataTableColumn(inputType = DataTableColumnType.ID, orderable = false)
    private Long id;

    @Column(name = "day")
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.stat.seo.google.position.searching.day",
        sortAfter = "editorFields.dayOfWeek",
        orderable = false
    )
	private Date dayDate;

    @Column(name = "priority")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.stat.seo.google.position.searching.position",
        orderable = false
    )
    private Integer priority;

    @Column(name = "domain_id")
    @NotNull
    private Integer domainId;

    @Column(name = "menu_meals_id", insertable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long mealId;

    @Transient
    @DataTableColumnNested
	private RestaurantMenuEditorFields editorFields = null;

    @ManyToOne
	@JsonBackReference(value="restaurantMenuMealsEntity")
	@JoinColumn(name="menu_meals_id")
	private RestaurantMenuMealsEntity meal;

    //For FE old version -> for jsp
	@Transient
	public int getDayNumber() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dayDate);
		return cal.get(Calendar.DAY_OF_WEEK);
	}

	@Transient
	public String getDayFormated() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		return dateFormat.format(dayDate);
	}

}
