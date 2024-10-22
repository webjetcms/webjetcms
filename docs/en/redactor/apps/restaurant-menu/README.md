# Restaurant menu

The Restaurant Menu app allows you to define dishes, create menus using the dishes and then display the menus in different styles. You can add the Restaurant Menu application to a web page via the application selector, or by adding the code directly to the body of the web page. Example: `!INCLUDE(/components/restaurant_menu/menu.jsp, style=02, mena=&euro;)!`

![](menu-app-dialog.png)

The application dialog consists of tabs:
- Settings
- Dishes
- Creating menus

## Settings

In the Settings tab, you can select the style in which the created restaurant menu will be displayed in the web page. As you can see in the previous image, there are 4 different display types available. Type 01, 02 and 03 display the whole menu (the whole week). Type 04 displays only the menu for the specific current day.

As an example, let's see how the generated restaurant menu type 02 looks like in a web page.

![](menu-app-frontend.png)

## Dishes

The Meals tab offers a nested date table for managing the list of supported meals. The full documentation for that table can be found here [Dishes](./meals.md).

![](menu-app-dialog-meals.png)

## Creating menus

The Menu Creation tab offers a nested date table for creating and managing menus, for a specific day/week. The full documentation for that table can be found here [Creating menus](./menu.md).

![](menu-app-dialog-menu.png)
