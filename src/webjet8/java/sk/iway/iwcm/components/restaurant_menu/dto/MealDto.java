package sk.iway.iwcm.components.restaurant_menu.dto;

import java.math.BigDecimal;

/**
 * Date: 17.10.2017<br/>
 * Time: 12:29<br/>
 * Project: webjet8<br/>
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2017
 *
 * @author mpijak<br/>
 */
public class MealDto {
    private int id;
    private String name;
    private String category;
    private String description;
    private String alergens;
    private String weight;
    private BigDecimal price;

    public String getAlergens() {
        return alergens;
    }

    public void setAlergens(String alergens) {
        this.alergens = alergens;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
