package sk.iway.iwcm.helpers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UserDetails;

/**
 *  BeanDiffJUnit.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 29.03.2010 16:48:33
 *@modified     $Date: 2009/12/11 15:42:33 $
 */
public class BeanDiffJUnit extends BaseWebjetTest
{
	@Test
	public void singlePropertyDifference()
	{
		GroupDetails original = new GroupDetails();
		original.setGroupName("Original");
		GroupDetails changed = new GroupDetails(original);
		changed.setGroupName("changed");
		Map<String, PropertyDiff> changes = new BeanDiff().whitelist("groupName", "groupId", "defaultDocId").setOriginal(original).setNew(changed).diff();
		assertTrue(changes.size() == 1);
		assertTrue(changes.containsKey("groupName"));
	}

	@Test
	public void blackListTest()
	{
		GroupDetails original = new GroupDetails();
		original.setGroupName("Original");
		GroupDetails changed = new GroupDetails(original);
		changed.setGroupName("changed");
		BeanDiff diff = new BeanDiff();
		Map<String, PropertyDiff> changesWhitelist = diff.whitelist("groupName").setOriginal(original).setNew(changed).diff();
		Map<String, PropertyDiff> changesBlacklist = diff.blacklist("groupName").setOriginal(original).setNew(changed).diff();
		assertTrue(changesWhitelist.size() == 1);
		assertTrue(changesBlacklist.size() == 0);
	}

	@Test
	public void nullPropertyTest()
	{
		UserDetails original = new UserDetails();
		original.setLogin("Original");
		UserDetails changed = new UserDetails();
		changed.setLogin(null);
		BeanDiff diff = new BeanDiff();
		Map<String, PropertyDiff> changes = diff.whitelist("login").setOriginal(original).setNew(changed).diff();
		assertTrue(changes.size() == 1);
		assertTrue(changes.get("login").valueAfter.equals(""));
	}

	@Test
	public void diffPrinter()
	{
		GroupDetails original = new GroupDetails();
		original.setGroupName("Original");
		GroupDetails changed = new GroupDetails(original);
		changed.setGroupName("changed");
		BeanDiff diff = new BeanDiff().whitelist("groupName", "groupId", "defaultDocId").setOriginal(original).setNew(changed);
		String html = new BeanDiffPrinter(diff).toHtml();
		assertTrue(html.contains("Original -> changed"));
	}

}