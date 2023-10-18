package sk.iway.iwcm.components.restaurant_menu;

import java.util.Date;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.restaurant_menu.dto.MenuDto;
import sk.iway.iwcm.components.restaurant_menu.mapper.MenuMapper;

/**
 * Date: 17.10.2017<br/>
 * Time: 12:27<br/>
 * Project: webjet8<br/>
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2017
 *
 * @author mpijak<br/>
 */
@RestController
@RequestMapping("/rest/restaurant-menu")
public class RestaurantMenuController {
    @RequestMapping(path = "/menus", method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    public List<MenuDto> getMeals() {
        List<MenuBean> menuBeans = MenuDB.getInstance().getByDate(new Date());
        return MenuMapper.INSTANCE.toMenuDtos(menuBeans);
    }
}
