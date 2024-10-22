package sk.iway.iwcm.users.user_groups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;

class UserGroupsDBTest extends BaseWebjetTest {

    private UserGroupsDB userGroupsDB;

    @BeforeEach
    public void setUp() {
        userGroupsDB = UserGroupsDB.getInstance();
    }

    @Test
    void testCalculatePrice() {
        UserDetails testUser = new UserDetails();
        BigDecimal price = new BigDecimal("25.6");

        //If user if null, return full price
        assertEquals(price, userGroupsDB.calculatePrice(price, null));

        //If user is NOT in discount group, return full price
        testUser.setUserGroupsIds("");
        assertEquals(price, userGroupsDB.calculatePrice(price, testUser));

        //If user is in discount group 25%, return discounted price
        testUser.setUserGroupsIds("1334");
        assertEquals(new BigDecimal("19.2"), userGroupsDB.calculatePrice(price, testUser));

        //If user is in discount group 25% and 40%, return discounted price by 40%
        testUser.setUserGroupsIds("1334,1335");
        assertEquals(new BigDecimal("15.36"), userGroupsDB.calculatePrice(price, testUser));
    }
}
