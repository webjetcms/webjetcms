package sk.iway.iwcm.components.forms.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.forms.FormsService;

@Service
public class FormsArchiveServiceImpl extends FormsService<FormsArchiveRepository, FormsArchiveEntity> {

    //private final FormsRepository formsRepository;

    @Autowired
    public FormsArchiveServiceImpl(FormsArchiveRepository formsHistoryRepository) {
        super(formsHistoryRepository);
        //this.formsRepository = formsRepository;
    }
}