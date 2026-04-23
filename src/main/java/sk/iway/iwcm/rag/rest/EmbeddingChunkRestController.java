package sk.iway.iwcm.rag.rest;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/settings/embedding-chunks")
//@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
@Datatable
public class EmbeddingChunkRestController extends DatatableRestControllerV2<EmbeddingChunkEntity, Long> {

    private final EmbeddingChunkRepository chunkRepository;

    @Autowired
    public EmbeddingChunkRestController(EmbeddingChunkRepository chunkRepository) {
        super(chunkRepository);
        this.chunkRepository = chunkRepository;
    }

    @Override
    public Page<EmbeddingChunkEntity> getAllItems(Pageable pageable) {
        // TODO Auto-generated method stub
        return super.getAllItems(pageable);
    }

    @Override
    public void getOptions(DatatablePageImpl<EmbeddingChunkEntity> page) {

        page.addOptions("entityType", chunkRepository.findDistinctEntityTypes(CloudToolsForCore.getDomainId()).stream().map(Enum::name).collect(Collectors.toList()));
        page.addOptions("language", Arrays.asList( Constants.getArray("languages") ));

        super.getOptions(page);
    }
}