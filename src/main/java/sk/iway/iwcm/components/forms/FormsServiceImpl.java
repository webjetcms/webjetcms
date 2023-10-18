package sk.iway.iwcm.components.forms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FormsServiceImpl extends FormsService<FormsRepository, FormsEntity> {

    //private final FormsRepository formsRepository;

    @Autowired
    public FormsServiceImpl(FormsRepository formsRepository) {
        super(formsRepository);
        //this.formsRepository = formsRepository;
    }
}
