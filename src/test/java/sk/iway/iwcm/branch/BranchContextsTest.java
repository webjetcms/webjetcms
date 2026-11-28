package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;

/** Verifies nested technical LIVE scopes and request cleanup on a reused thread. */
class BranchContextsTest {
    private MockedStatic<BranchDomains> domains;
    @BeforeEach void domainIds() {
        domains = mockStatic(BranchDomains.class);
        domains.when(() -> BranchDomains.id("a.example.sk")).thenReturn(1);
        domains.when(() -> BranchDomains.id("b.example.sk")).thenReturn(2);
    }
    @AfterEach void closeDomainIds() { domains.close(); }

    @Test
    void restoresBranchAfterTechnicalLiveAndCleansUpAfterFailure() {
        RequestBean bean = new RequestBean();
        MockHttpServletRequest request = new MockHttpServletRequest();
        BranchContext branch = new BranchContext(42, "example.sk", 7, BranchContext.Mode.ADMIN);
        try (var requests = mockStatic(SetCharacterEncodingFilter.class)) {
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(bean);
            assertEquals(BranchContext.Mode.LIVE, BranchContexts.current().mode());
            assertThrows(IllegalArgumentException.class, () -> {
                try (var outer = BranchContexts.open(request, branch)) {
                    assertEquals(branch, BranchContexts.current(request));
                    assertEquals(branch, BranchContexts.current());
                    try (var live = BranchContexts.live(request, "example.sk", 7)) {
                        assertFalse(BranchContexts.current().isBranch());
                        assertEquals(0, BranchContexts.current(request).branchId());
                    }
                    assertEquals(branch, BranchContexts.current());
                    throw new IllegalArgumentException("Downstream failure");
                }
            });
            assertNull(request.getAttribute(BranchContexts.ATTRIBUTE));
            assertNull(RequestBean.getAttribute(BranchContexts.ATTRIBUTE));
            assertFalse(BranchContexts.current(new MockHttpServletRequest()).isBranch());
        }
    }

    @Test
    void rejectsChangedBranchAndChangedDomainEvenForLive() {
        BranchContext live = BranchContext.live("a.example.sk", 7);
        assertDoesNotThrow(() -> BranchContexts.requireSameSelection(live, 0, "1"));
        assertThrows(IllegalStateException.class, () -> BranchContexts.requireSameSelection(live, 0, "2"));
        assertThrows(IllegalStateException.class, () -> BranchContexts.requireSameSelection(live, 12, "1"));
        assertThrows(IllegalStateException.class, () -> BranchContexts.requireSameSelection(live, 0, null));
    }

    @Test
    void rejectsInvalidModeIdentityAndHasLiveDefaultWithoutRequestBean() {
        assertThrows(IllegalArgumentException.class, () -> new BranchContext(1, "a.sk", 7, BranchContext.Mode.LIVE));
        assertThrows(IllegalArgumentException.class, () -> new BranchContext(0, "a.sk", 7, BranchContext.Mode.ADMIN));
        assertThrows(IllegalArgumentException.class, () -> new BranchContext(1, "", 7, BranchContext.Mode.PREVIEW));
        assertThrows(IllegalArgumentException.class, () -> new BranchContext(1, "a.sk", 0, BranchContext.Mode.PREVIEW));
        try (var requests = mockStatic(SetCharacterEncodingFilter.class)) {
            assertEquals(BranchContext.live("", 0), BranchContexts.current());
        }
    }
}
