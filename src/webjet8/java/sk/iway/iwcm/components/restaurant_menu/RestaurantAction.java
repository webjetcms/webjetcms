package sk.iway.iwcm.components.restaurant_menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.enumerations.EnumerationDataDB;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.stripes.WebJETActionBean;

public class RestaurantAction extends WebJETActionBean
{
	private MealBean meal;
	private List<String> allergens;
	private List<Integer> pon;
	private List<Integer> uto;
	private List<Integer> str;
	private List<Integer> stv;
	private List<Integer> pia;
	private List<Integer> sob;
	private List<Integer> ned;
	private String week;
	private int menuToDelete;

	@Override
	public void setContext(ActionBeanContext context)
	{
		super.setContext(context);

		setDays();

		int mealId = Tools.getIntValue(context.getRequest().getParameter("meal.mealId"), -1);
		if (mealId > 0)
		{
			meal = MealDB.getInstance().getById(mealId);
			allergens = Arrays.asList(meal.getAlergens().split("\\s*,\\s*"));
		}
		else
		{
			meal = new MealBean();
		}
	}

	public Resolution bSave()
	{
		String lng = PageLng.getUserLng(getRequest());
		Prop prop = Prop.getInstance(lng);

		if (isAdminLogged()==false) return new ForwardResolution(RESOLUTION_NOT_LOGGED);

		meal.setDomainId(CloudToolsForCore.getDomainId());

		String al="";
		if (allergens.get(0).equals(" ")) allergens=null;
		if(allergens!=null)
		{
			for(String s:allergens)
			{
				al=al+s+",";
			}
			al=al.substring(0, al.length()-3);
		}
		meal.setAlergens(al);
		List<EnumerationDataBean> enumerationDataBeans = EnumerationDataDB.getEnumerationDataByType("restauracne menu kategorie");

		if (enumerationDataBeans.isEmpty()) {
			if (meal.getCathegory().equals(prop.getText("components.restaurant_menu.polievka")))
				meal.setCathegory("1" + meal.getCathegory());
			else if (meal.getCathegory().equals(prop.getText("components.restaurant_menu.hlavne_jedlo")))
				meal.setCathegory("2" + meal.getCathegory());
			else if (meal.getCathegory().equals(prop.getText("components.restaurant_menu.priloha")))
				meal.setCathegory("3" + meal.getCathegory());
			else if (meal.getCathegory().equals(prop.getText("components.restaurant_menu.dezert")))
				meal.setCathegory("4" + meal.getCathegory());
		} else {
			EnumerationDataBean enumerationDataBean = EnumerationDataDB.getInstance().findFirst("string1", meal.getCathegory());
			if (enumerationDataBean != null)
				meal.setCathegory(enumerationDataBean.getDecimal1().intValue() + meal.getCathegory());
		}

		try
		{
			MealDB.getInstance().save(meal);
		}
		catch(Exception e)
		{
			getRequest().setAttribute("errorText", prop.getText("admin.conf_editor.update_alert"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		return (new ForwardResolution("/components/reloadParentClose.jsp"));
	}

	public Resolution bDelete()
	{
		String lng = PageLng.getUserLng(getRequest());
		Prop prop = Prop.getInstance(lng);
		if (isAdminLogged()==false) return new ForwardResolution(RESOLUTION_NOT_LOGGED);

		try
		{
			MealDB.getInstance().deleteByIds(meal.getMealId());
		}
		catch(Exception e)
		{
			getRequest().setAttribute("errorText", prop.getText("admin.conf_editor.update_alert"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}

		return (new ForwardResolution("/components/maybeError.jsp"));
	}

	public Resolution menuSave()
	{
		if (isAdminLogged()==false) return new ForwardResolution(RESOLUTION_NOT_LOGGED);

		List<MenuBean> menuToDB = new ArrayList<>();
		List< List<MealBean> > menu = new ArrayList<>();
		menu.add(getMeals(pon));
		menu.add(getMeals(uto));
		menu.add(getMeals(str));
		menu.add(getMeals(stv));
		menu.add(getMeals(pia));
		menu.add(getMeals(sob));
		menu.add(getMeals(ned));

		int w=Integer.parseInt(week.substring(0, week.indexOf("-")));
		int y=Integer.parseInt(week.substring(week.indexOf("-")+1, week.length()));

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, y);
		cal.set(Calendar.WEEK_OF_YEAR, w);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		Date day = cal.getTime();

		for(List<MealBean> l : menu)
		{
			int mealCounter = 0;
			if(l!=null)
			{
				for(MealBean i : l)
				{
					MenuBean mb = new MenuBean();
					mb.setDay(day);
					mb.setPriority(mealCounter);
					mb.setMeal(i);
					mb.setDomainId(CloudToolsForCore.getDomainId());
					menuToDB.add(mb);
					mealCounter++;
				}
			}
			cal.add(Calendar.DATE, 1);
			day = cal.getTime();
		}

		cal.set(Calendar.YEAR, y);
		cal.set(Calendar.WEEK_OF_YEAR, w);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		day = cal.getTime();
		for(int i=0;i<7;i++)
		{
			deleteOld(day);
			cal.add(Calendar.DATE, 1);
			day = cal.getTime();
		}

		MenuBean[] stockArr = new MenuBean[menuToDB.size()];
		stockArr = menuToDB.toArray(stockArr);
		MenuDB.getInstance().save(stockArr);

		return (new ForwardResolution("/components/maybeError.jsp"));
	}

	/**
	 * vrati list jedal podla ich id
	 *
	 * @param ids
	 * @return
	 */
	private List<MealBean> getMeals(List<Integer> ids)
	{
		if(ids==null) return null;

		List<MealBean> result = new ArrayList<>();
		for(int i:ids)
		{
			result.add(MealDB.getInstance().getById(i));
		}
		return result;
	}

	/**
	 * vymaze vsetky jedla z daneho dna
	 *
	 * @param day
	 */
	private void deleteOld(Date day)
	{
		List<MenuBean> list = MenuDB.getInstance().getByDate(day);
		MenuBean[] array = new MenuBean[list.size()];
		array = list.toArray(array);

		MenuDB.getInstance().delete(array);
	}

	/**
	 * zisti tyzdenne menu (idcka jedal), aby mohlo byt nastavene v select boxe
	 *
	 */
	private void setDays()
	{
		int w,y;
		Calendar cal = Calendar.getInstance();
		String requestWeek=getRequest().getParameter("week");
		if(requestWeek!=null && requestWeek.matches("[0-9]+-[0-9]+"))
		{
			w=Integer.parseInt(requestWeek.substring(0, requestWeek.indexOf("-")));
			y=Integer.parseInt(requestWeek.substring(requestWeek.indexOf("-")+1, requestWeek.length()));

		}
		else
		{
			w = cal.get(Calendar.WEEK_OF_YEAR);
			y = cal.get(Calendar.YEAR);
		}
		cal.set(Calendar.YEAR, y);
		cal.set(Calendar.WEEK_OF_YEAR, w);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		Date day = cal.getTime();

		List<List<Integer>> days = new ArrayList<>();

		for(int i=0;i<7;i++)
		{
			days.add(new ArrayList<>());
			List<MenuBean> meals = MenuDB.getInstance().getByDate(day);

			for(MenuBean m:meals)
			{
				days.get(i).add(m.getMeal().getId());
			}
			cal.add(Calendar.DATE, 1);
			day = cal.getTime();
		}

		pon=days.get(0);
		uto=days.get(1);
		str=days.get(2);
		stv=days.get(3);
		pia=days.get(4);
		sob=days.get(5);
		ned=days.get(6);
	}

	public Resolution deleteFromMenu()
	{
		String lng = PageLng.getUserLng(getRequest());
		Prop prop = Prop.getInstance(lng);
		if (isAdminLogged()==false) return new ForwardResolution(RESOLUTION_NOT_LOGGED);

		try
		{
			MenuDB.getInstance().deleteByIds(menuToDelete);
		}
		catch(Exception e)
		{
			getRequest().setAttribute("errorText", prop.getText("admin.conf_editor.update_alert"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}

		return (new ForwardResolution("/components/maybeError.jsp"));
	}

	public Resolution changePriority()
	{
		String lng = PageLng.getUserLng(getRequest());
		Prop prop = Prop.getInstance(lng);
		if (isAdminLogged()==false) return new ForwardResolution(RESOLUTION_NOT_LOGGED);

		try
		{
			MenuBean mb = MenuDB.getInstance().getById(Integer.parseInt(getRequest().getParameter("menuToUpdate")));
			mb.setPriority(Integer.parseInt(getRequest().getParameter("priority")));
			MenuDB.getInstance().save(mb);
		}
		catch(Exception e)
		{
			getRequest().setAttribute("errorText", prop.getText("admin.conf_editor.update_alert"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}

		return (new ForwardResolution("/components/maybeError.jsp"));
	}

	public MealBean getMeal() {
		return meal;
	}

	public void setMeal(MealBean meal) {
		this.meal = meal;
	}

	public List<String> getAllergens() {
		return allergens;
	}

	public void setAllergens(List<String> allergens) {
		this.allergens = allergens;
	}

	public String getWeek() {
		return week;
	}

	public void setWeek(String week) {
		this.week = week;
	}

	public List<Integer> getPon() {
		return pon;
	}

	public void setPon(List<Integer> pon) {
		this.pon = pon;
	}

	public List<Integer> getUto() {
		return uto;
	}

	public void setUto(List<Integer> uto) {
		this.uto = uto;
	}

	public List<Integer> getStr() {
		return str;
	}

	public void setStr(List<Integer> str) {
		this.str = str;
	}

	public List<Integer> getStv() {
		return stv;
	}

	public void setStv(List<Integer> stv) {
		this.stv = stv;
	}

	public List<Integer> getPia() {
		return pia;
	}

	public void setPia(List<Integer> pia) {
		this.pia = pia;
	}

	public List<Integer> getSob() {
		return sob;
	}

	public void setSob(List<Integer> sob) {
		this.sob = sob;
	}

	public List<Integer> getNed() {
		return ned;
	}

	public void setNed(List<Integer> ned) {
		this.ned = ned;
	}

	public int getMenuToDelete() {
		return menuToDelete;
	}

	public void setMenuToDelete(int menuToDelete) {
		this.menuToDelete = menuToDelete;
	}
}
