package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;

/** Real database comparison and publication guards for a newly created empty working version. */
@EnabledIfEnvironmentVariable(named="WEBJET_P06_DB",matches="true")
class BranchMergeEmptyTest extends BranchJpaTest {
    @Autowired BranchRepository repository;

    /** An empty package must not validate a placeholder branch belonging to another domain. */
    @Test void emptyPackageReportsOnlyMissingChanges() throws Exception {
        Map<String,String> configuration=new LinkedHashMap<>();
        for(String key:List.of("enableStaticFilesExternalDir","branchesEnabled","branchStorageRoot","multiDomainEnabled","fulltextExecuteApps","fulltextDataAscMethod"))
            configuration.put(key,Constants.getString(key));
        BranchEntity branch=null;
        int domainId=0;
        try {
            Constants.setBoolean("enableStaticFilesExternalDir",true);
            Constants.setBoolean("branchesEnabled",true);
            
            Constants.setBoolean("multiDomainEnabled",true);
            Constants.setBoolean("fulltextExecuteApps",false);
            Constants.setString("fulltextDataAscMethod","");
            Files.createDirectories(Path.of("/tmp/webjet-p06-empty-private"));
            Constants.setString("branchStorageRoot","/tmp/webjet-p06-empty-private");
            String domain="p06-empty.example";
            domainId=BranchTestDomains.ensure(domain);
            assertTrue(repository.findFirstByIdAndDomainId(1L,domainId).isEmpty());
            Identity user=new Identity();user.setUserId(1);user.setAdmin(true);user.setEditableGroups(Integer.toString(domainId));
            RequestBean bean=new RequestBean();bean.setUser(user);bean.setDomain(domain);
            SetCharacterEncodingFilter.setCurrentRequestBean(bean);
            branch=createBranch(domainId,"autotest empty working version","Empty package regression",user.getUserId());
            assertEquals(domainId,repository.findById(branch.getId()).orElseThrow().getDomainId());

            BranchMergeService service=new BranchMergeService();
            var report=service.detail(branch.getId(),domain,user);
            assertTrue(report.items().isEmpty());
            assertEquals(List.of("Balík neobsahuje žiadne zmeny."),report.errors());
            assertFalse(report.ready());
            assertThrows(ResponseStatusException.class,()->service.claim(report.id(),domain,user,report.revision(),report.preparationHash(),false));
            assertThrows(ResponseStatusException.class,()->service.schedule(report.id(),domain,user,report.revision(),report.preparationHash(),System.currentTimeMillis()+3600000));
        } finally {
            if(branch!=null) repository.deleteById(branch.getId());
            if(domainId>0) try(var connection=DBPool.getConnection();var statement=connection.prepareStatement("DELETE FROM `groups` WHERE group_id=? AND domain_name='p06-empty.example'")) {
                statement.setInt(1,domainId);statement.executeUpdate();
                sk.iway.iwcm.doc.GroupsDB.getInstance(true);
            }
            configuration.forEach(Constants::setString);
            SetCharacterEncodingFilter.setCurrentRequestBean(null);
        }
    }
}
