package sk.iway.iwcm.components.restaurant_menu.dto;

import java.util.Date;

/**
 * Date: 17.10.2017<br/>
 * Time: 13:25<br/>
 * Project: webjet8<br/>
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2017
 *
 * @author mpijak<br/>
 */
public class MenuDto {
    private int id;
    private MealDto meal;
    private Date day;
    private int priority;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MealDto getMeal() {
        return meal;
    }

    public void setMeal(MealDto meal) {
        this.meal = meal;
    }

    public Date getDay() {
        return day;
    }

    public void setDay(Date day) {
        this.day = day;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
