package sk.iway.iwcm.components.qa.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.Password;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.qa.jpa.QuestionsAnswersEditorFields;
import sk.iway.iwcm.components.qa.jpa.QuestionsAnswersEntity;
import sk.iway.iwcm.components.qa.jpa.QuestionsAnswersRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UsersDB;

/**
 * Aplikacia Otazky a odpovede #53913
 */
@RestController
@RequestMapping("/admin/rest/qa")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuQa')")
@Datatable
public class QuestionsAnswersRestController extends DatatableRestControllerV2<QuestionsAnswersEntity, Long> {

    private final QuestionsAnswersRepository questionsAnswersRepository;
    private final QuestionsAnswersService questionsAnswersService;

    @Autowired
    public QuestionsAnswersRestController(QuestionsAnswersRepository questionsAnswersRepository,
                                          HttpServletRequest request,
                                          QuestionsAnswersService questionsAnswersService) {
        super(questionsAnswersRepository);
        this.questionsAnswersRepository = questionsAnswersRepository;
        this.questionsAnswersService = questionsAnswersService;
    }

    @Override
    public QuestionsAnswersEntity getOneItem(long id) {

        if(id != -1) {
            //ziskaj udaje volanim super.getOneItem aby sa korektne vykonalo porovnanie domain_id stlpca
            return super.getOneItem(id);
        }

        //novy zaznam, nastav hodnoty aktualne prihlaseneho usera
        QuestionsAnswersEntity entity = new QuestionsAnswersEntity();

        Identity user = UsersDB.getCurrentUser(getRequest());

        //For new entity we fill User column using Identity of actual loged user
        entity.setFromName(user.getFullName());
        entity.setFromEmail(user.getEmail());

        entity.setToName(user.getFullName());
        entity.setToEmail(user.getEmail());

        return entity;
    }

    @Override
    public void beforeSave(QuestionsAnswersEntity entity) {
        //For new entity set question and answer date
        if (entity.getQuestionDate()==null) entity.setQuestionDate(new Date());

        if (Tools.isEmpty(entity.getHash())) entity.setHash(Password.generatePassword(16));
        if (entity.getSortPriority()==null) entity.setSortPriority(questionsAnswersService.getNewPriority(entity.getGroupName()));

        //ak sa vytvara otazka v admine nema toto nastavene, takze nastavime na true
        if (entity.getAllowPublishOnWeb()==null) entity.setAllowPublishOnWeb(Boolean.TRUE);

        if(Tools.isNotEmpty(entity.getAnswerToEmail()) || Tools.isNotEmpty(entity.getAnswer())) {
            entity.setAnswerDate(new Date());
        }
    }

    @Override
    public void afterSave(QuestionsAnswersEntity original, QuestionsAnswersEntity entity) {
        //Send email (text of message is saved in answerToEmail), treba original, pretoze entita nema ulozene answerToEmail (ktore nie je v DB)
        if(!questionsAnswersService.sendAnswerEmail(original, getRequest())) {
            throwError("qa.result.fail");
        }
    }

    @Override
    public QuestionsAnswersEntity processFromEntity(QuestionsAnswersEntity entity, ProcessItemAction action) {

        //nastav volne polia
        QuestionsAnswersEditorFields ef = new QuestionsAnswersEditorFields();
        ef.setFieldsDefinition(ef.getFields(entity, "components.qa", 'D'));

        entity.setEditorFields(ef);

        //oznacenie este neodpovedaneho zaznamu cervenou farbou
        if (Boolean.FALSE.equals(entity.isAnswerCheck())) ef.setRowClass("is-not-answered");

        return entity;
    }

    /**
     * Vrati zoznam uz existujucich skupin
     * @param term
     * @return
     */
    @GetMapping("/autocomplete")
    public List<String> getAutocomplete(@RequestParam String term) {

        List<String> ac = new ArrayList<>();

        //Get all where group name is like %term%, and distict because its autocomplete list and we dont want duplicity
        List<QuestionsAnswersEntity> groupNamesPage =  questionsAnswersRepository.findAllByGroupNameLikeAndDomainId("%" + term + "%", CloudToolsForCore.getDomainId());

        //Loop gained entities and add group name to autcomplete list "ac"
        for(QuestionsAnswersEntity entity : groupNamesPage) {
            if (ac.contains(entity.getGroupName())==false) ac.add(entity.getGroupName());
        }

        return ac;
    }
}
