package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.test.BaseWebjetTest;

/** The shared-table isolation prerequisite must not vary by request domain. */
class BranchFlagScopeTest extends BaseWebjetTest
{
    /** Domain overrides remain functional for ordinary values but cannot weaken this global flag. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void branchFlagIgnoresDomainAlias(boolean enabled)
    {
        boolean originalAliases = Constants.isConstantsAliasSearch();
        Constants.setConstantsAliasSearch(false);
        boolean originalFlag = Constants.getBoolean("branchesEnabled");
        String originalAliasFlag = Constants.getString("p01-autotest-branchesEnabled");
        String originalProbe = Constants.getString("p01-autotest-p01ScopeProbe");
        RequestBean request = new RequestBean();
        request.setDomain("p01.example.invalid");
        try (var requests = mockStatic(SetCharacterEncodingFilter.class);
            var domains = mockStatic(MultiDomainFilter.class))
        {
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(request);
            domains.when(() -> MultiDomainFilter.getDomainAlias("p01.example.invalid")).thenReturn("p01-autotest");
            Constants.setBoolean("branchesEnabled", enabled);
            Constants.setBoolean("p01-autotest-branchesEnabled", !enabled);
            Constants.setBoolean("p01-autotest-p01ScopeProbe", !enabled);
            Constants.setConstantsAliasSearch(true);
            assertEquals(enabled, Constants.getBoolean("branchesEnabled"));
            assertEquals(!enabled, Constants.getBoolean("p01ScopeProbe"));
        }
        finally
        {
            Constants.setConstantsAliasSearch(false);
            Constants.setBoolean("branchesEnabled", originalFlag);
            Constants.setString("p01-autotest-branchesEnabled", originalAliasFlag);
            Constants.setString("p01-autotest-p01ScopeProbe", originalProbe);
            Constants.setConstantsAliasSearch(originalAliases);
        }
    }
}
