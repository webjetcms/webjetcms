package sk.iway.iwcm.editor.approve;

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
import sk.iway.iwcm.editor.rest.GroupSchedulerDto;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoMapper;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;
import sk.iway.iwcm.editor.rest.GroupSchedulerEditorFields;
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
        private String note;
        private String approveAction;
        private long scheduleId;
    }

    @Autowired
    public ApproveGroupListener(ApproveService approveService, GroupSchedulerDtoRepository groupSchedulerDtoRepository) {
        this.approveService = approveService;
        this.groupSchedulerDtoRepository = groupSchedulerDtoRepository;
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='webpages' && event.source.subpage=='approve-group'")
    protected void setInitalApproveData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            ModelMap model = event.getSource().getModel();
            HttpServletRequest request = event.getSource().getRequest();
            Prop prop = Prop.getInstance(request);

            GroupSchedulerDto dto = validateAndGetDto(model, request, prop, false);
            if (dto == null) return;

            if("post".equalsIgnoreCase(request.getMethod())) {
                String response = approveService.approveGroupAction(dto, groupSchedulerDtoRepository, GroupsDB.getInstance());
                if(response == null) {
                    prepareResponseModel(model, null, prop.getText("approve.group.success"));
                } else {
                    prepareResponseModel(model, response, null);
                }
                return;
            }

            // ziskanie objektu a vlozenie do modelu
            Form form = new Form();
            form.setScheduleId(dto.getId());
            model.addAttribute("form", form);

            // Set groups diff
            GroupDetails originalGroup;
            // Now check, if scheduler dto is main record for group (AKA if it was create action)
			Integer count = groupSchedulerDtoRepository.countByGroupId(dto.getGroupId());
			if(count != null && count == 1) {
                originalGroup = null; // new group, no original data
            } else {
                originalGroup = GroupsDB.getInstance().getGroup( dto.getGroupId() );
            }

            String diff = ApproveService.getDiff(GroupSchedulerDtoMapper.INSTANCE.groupSchedulerDtoToGroup(dto), originalGroup, prop, false, false).toString();

            diff = Tools.unescapeHtmlEntities(diff);
            model.addAttribute("diff", diff);

            if(originalGroup == null) {
                model.addAttribute("diffLabel", prop.getText("approve.group.new_params"));
            } else {
                model.addAttribute("diffLabel", prop.getText("approve.group.changed_params"));
            }
        } catch (Exception ex) {
            Logger.error(ApproveGroupListener.class, ex);
        }
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='webpages' && event.source.subpage=='approve-del-group'")
    protected void setInitalApproveDelData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            ModelMap model = event.getSource().getModel();
            HttpServletRequest request = event.getSource().getRequest();
            Prop prop = Prop.getInstance(request);

            GroupSchedulerDto dto = validateAndGetDto(model, request, prop, true);
            if (dto == null) return;

            if("post".equalsIgnoreCase(request.getMethod())) {
                String response = approveService.approveGroupDelAction(dto, groupSchedulerDtoRepository, GroupsDB.getInstance());
                if(response == null) {
                    prepareResponseModel(model, null, prop.getText("approve.group.success"));
                } else {
                    prepareResponseModel(model, response, null);
                }
                return;
            }

            // ziskanie objektu a vlozenie do modelu
            Form form = new Form();
            form.setScheduleId(dto.getId());
            model.addAttribute("form", form);

        } catch (Exception ex) {
            Logger.error(ApproveGroupListener.class, ex);
        }
    }

    /**
     * Validate request parameters and return the GroupSchedulerDto if valid, or null if validation failed (model is populated with error).
     * @param isDelete true for delete approval, false for standard approval
     */
    private GroupSchedulerDto validateAndGetDto(ModelMap model, HttpServletRequest request, Prop prop, boolean isDelete) {
        Long scheduleId = Tools.getLongValue(request.getParameter("scheduleId"), -1L);
        if(scheduleId == -1L) {
            prepareResponseModel(model, prop.getText("approve.group.failed"), null);
            return null;
        }

        Optional<GroupSchedulerDto> dtoOpt = isDelete
            ? groupSchedulerDtoRepository.findByIdAndIsDeleteTrue(scheduleId)
            : groupSchedulerDtoRepository.findByIdAndIsDeleteFalse(scheduleId);
        if (dtoOpt.isPresent() == false) {
            prepareResponseModel(model, prop.getText("approve.group.failed"), null);
            return null;
        }

        GroupSchedulerDto dto = dtoOpt.get();

        String keyPrefix = isDelete ? "approve.group.delete." : "approve.group.";

        // First check if group needs approve
        if(dto.getAwaitingApprove() == null) {
            // It can be ok, if its already approved or disapproved
            if(dto.getApproveDate() != null && dto.getApprovedBy() != null) {
                prepareResponseModel(model, null, prop.getText(keyPrefix + "already_approved", GroupSchedulerEditorFields.getApproverName(dto.getApprovedBy())));
                return null;
            } else if(dto.getApproveDate() != null && dto.getDisapprovedBy() != null) {
                prepareResponseModel(model, null, prop.getText(keyPrefix + "already_disapproved", GroupSchedulerEditorFields.getDisapproverName(dto.getDisapprovedBy())));
                return null;
            } else {
                prepareResponseModel(model, prop.getText("approve.group.failed"), null);
                return null;
            }
        }

        return dto;
    }

    private void prepareResponseModel(ModelMap model, String errMsg, String succMsg) {
        model.addAttribute("errMsg", errMsg);
        model.addAttribute("succMsg", succMsg);
        model.addAttribute("form", null);
        model.addAttribute("diff", null);
        model.addAttribute("diffLabel", null);
    }
}
