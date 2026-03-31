package sk.iway.iwcm.editor.rest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.service.ApproveService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class ApproveGroupListener {

    private final ApproveService approveService;
    private final GroupSchedulerDtoRepository groupSchedulerDtoRepository;

    // crate inner class form
    @Component
    @Getter
    @Setter
    public static class Form {
        private String message;
        private String approveAction;
        private long scheduleId;
    }

    @Autowired
    public ApproveGroupListener(ApproveService approveService, GroupSchedulerDtoRepository groupSchedulerDtoRepository) {
        this.approveService = approveService;
        this.groupSchedulerDtoRepository = groupSchedulerDtoRepository;
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='webpages' && event.source.subpage=='approve-group'")
    protected void setInitalData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            ModelMap model = event.getSource().getModel();
            HttpServletRequest request = event.getSource().getRequest();
            Prop prop = Prop.getInstance(request);

            Long scheduleId = Tools.getLongValue(request.getParameter("scheduleId"), -1L);
            if(scheduleId == -1L) {
                prepareResponseModel(model, prop.getText("approve.group.failed"), null);
                return;
            }

            Optional<GroupSchedulerDto> dtoOpt = groupSchedulerDtoRepository.findByIdAndAwaitingApproveIsNotNull(scheduleId);
		    if (dtoOpt.isPresent() == false) {
                prepareResponseModel(model, prop.getText("approve.group.failed"), null);
                return;
            }

            if("post".equalsIgnoreCase(request.getMethod())) {
                // Its post action
                String response = approveService.approveGroupAction(dtoOpt.get(), groupSchedulerDtoRepository, GroupsDB.getInstance());
                if(response == null) {
                    prepareResponseModel(model, null, prop.getText("approve.group.success"));
                } else {
                    prepareResponseModel(model, response, null);
                }
                return;
            }

            // ziskanie objektu a vlozenie do modelu
            Form form = new Form();
            form.setScheduleId(scheduleId);
            model.addAttribute("form", form);

            // Set groups diff
            GroupDetails originalGroup = GroupsDB.getInstance().getGroup( dtoOpt.get().getGroupId() );

            String diff = ApproveService.getDiff(GroupSchedulerDtoMapper.INSTANCE.groupSchedulerDtoToGroup(dtoOpt.get()), originalGroup, prop, false, false).toString();
            diff = Tools.unescapeHtmlEntities(diff);
            model.addAttribute("diff", diff);

            if(originalGroup == null) {
                model.addAttribute("diffLabel", prop.getText("approve.group.new_params"));
            } else {
                model.addAttribute("diffLabel", prop.getText("approve.group.changed_params"));
            }
        } catch (Exception ex) {
            Logger.error(WebPagesListener.class, ex);
        }
    }

    private void prepareResponseModel(ModelMap model, String errMsg, String succMsg) {
        model.addAttribute("errMsg", errMsg);
        model.addAttribute("succMsg", succMsg);
        model.addAttribute("form", null);
        model.addAttribute("diff", null);
        model.addAttribute("diffLabel", null);
    }
}
