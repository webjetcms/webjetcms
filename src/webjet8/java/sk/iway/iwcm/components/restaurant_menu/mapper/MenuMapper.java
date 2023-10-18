package sk.iway.iwcm.components.restaurant_menu.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.components.restaurant_menu.MealBean;
import sk.iway.iwcm.components.restaurant_menu.MenuBean;
import sk.iway.iwcm.components.restaurant_menu.dto.MealDto;
import sk.iway.iwcm.components.restaurant_menu.dto.MenuDto;

/**
 * Date: 17.10.2017<br/>
 * Time: 13:27<br/>
 * Project: webjet8<br/>
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2017
 *
 * @author mpijak<br/>
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper {
    MenuMapper INSTANCE = Mappers.getMapper(MenuMapper.class );

    @Mappings({
            @Mapping(target = "id", source = "menuBean.menuId")
    })
    MenuDto toMenuDto(MenuBean menuBean);
    List<MenuDto> toMenuDtos(List<MenuBean> menuBeans);

    @Mappings({
            @Mapping(target = "menuId", source = "menuDto.id")
    })
    MenuBean toMenuBean(MenuDto menuDto);
    List<MenuBean> toMenuBeans(List<MenuDto> menuDtos);

    @Mappings({
            @Mapping(target = "id", source = "mealId"),
            @Mapping(target = "category", source = "cathegory"),
            @Mapping(target = "weight", source = "weight")
    })
    MealDto toMealDto(MealBean mealBean);
    List<MealDto> toMealDtos(List<MealBean> mealBeans);
}
