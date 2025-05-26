package sk.iway.iwcm.components.inquiry.jpa;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;

@Entity
@Table(name = "inquiry")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INQUIRY)
public class InquiryEntity extends ActiveRecordRepository implements Serializable {

    @Id
    @Column(name = "question_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_inquiry")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    @Accessors(chain = false)
    private Long id;

    @Column(name = "question_text")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        className = "dt-row-edit",
        renderFormatLinkTemplate = "javascript:;",
        title = "inquiry.question2",
        tab = "basic"
    )
    @NotEmpty
    @Size(max = 255)
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String questionText;

    @Column(name = "question_active")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "components.inquirysimple.active",
        hidden = true,
        tab = "basic"
    )
    private Boolean questionActive;

    @Column(name = "multiple")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "inquiry.answer.multiple",
        hidden = true,
        tab = "basic"
    )
    private Boolean multiple;

    @Column(name = "question_group")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "inquiry.group",
        tab = "advanced",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/inquiry/autocomplete"),
                    @DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    @Size(max = 255)
    private String questionGroup;

    @Column(name = "answer_text_ok")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "inquiry.answer_text_ok",
        hidden = true,
        tab = "advanced"
    )
    private String answerTextOk;

    @Column(name = "answer_text_fail")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "inquiry.answer_text_fail",
        hidden = true,
        tab = "advanced"
    )
    private String answerTextFail;

    @Column(name = "hours")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "inquiry.hours_number",
        hidden = true,
        tab = "advanced"
    )
    private Integer hours;

    @Column(name = "date_from")
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title = "inquiry.valid_since",
        tab = "advanced"
    )
    private Date dateFrom;

    @Column(name = "date_to")
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title = "inquiry.valid_till",
        tab = "advanced"
    )
    private Date dateTo;

    @Column(name = "total_clicks")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "inquiry.click_number",
        className = "hideOnCreate disabled"
    )
    private Integer totalClicks;

    @Column(name = "domain_id")
    private Integer domainId;

    //Bind editor fields
    @Transient
    @DataTableColumnNested
	private transient InquiryEditorFields editorFields = null;
}
