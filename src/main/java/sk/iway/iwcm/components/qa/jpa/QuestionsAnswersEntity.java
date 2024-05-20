package sk.iway.iwcm.components.qa.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;

@Entity
@Table(name = "questions_answers")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_QA_UPDATE)
public class QuestionsAnswersEntity implements Serializable{

	private static final long serialVersionUID = -1805144846544468397L;

    @Id
    @Column(name = "qa_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_questions_answers")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID", tab="basic")
    private Long id;

	@Column(name = "question")
	@NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="[[#{qa.form.question}]]",
		editor = {
            @DataTableColumnEditor(type = "quill")
        },
		tab = "answer"
    )
	@javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String question;


	//Autocomplete field
	@Column(name = "group_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{qa.roots}]]",
		tab = "basic",
		editor = {
			@DataTableColumnEditor(attr = {
				@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/qa/autocomplete"),
				@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
			})
		}
    )
	private String groupName;

	@Column(name = "category_name")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{qa.form.categoryName}]]",
		tab = "basic"
    )
	private String categoryName;

	@Column(name = "from_name")
	@NotBlank
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{qa.form.fromName}]]",
		tab = "basic"
    )
	private String fromName;

	@Column(name = "from_company")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{components.qa.ask.your_company}]]",
		visible = false,
		tab = "basic"
    )
	private String fromCompany;

	@Column(name = "from_phone")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{components.qa.ask.your_phone}]]",
		visible = false,
		tab = "basic"
    )
	private String fromPhone;

	@Column(name = "from_email")
	@NotBlank
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{qa.form.fromEmail}]]",
		visible = false,
		tab = "basic"
    )
	private String fromEmail;



	@Column(name = "to_name")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{qa.form.toName}]]",
		visible = false,
		tab = "answer"
    )
	private String toName;

	@Column(name = "to_email")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{qa.form.toEmail}]]",
		visible = false,
		tab = "answer"
    )
	private String toEmail;

	@Column(name = "answer_to_email")
	@DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title="[[#{qa.form.answer}]]",
		tab = "answer"
    )
	@javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
	private String answerToEmail;

	@Column(name = "allow_publish_on_web")
	@DataTableColumn(
		title = "components.qa.allowPublishOnWeb",
        inputType = DataTableColumnType.BOOLEAN,
		visible = false,
		tab = "answer"
    )
	private Boolean allowPublishOnWeb;

	@Column(name = "publish_on_web")
	@DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="[[#{qa.form.publishOnWeb}]]",
		visible = false,
		tab = "answer"
    )
	private Boolean publishOnWeb;

    @Column(name = "answer")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title="[[#{qa.form.answerWeb}]]",
		tab = "answer"
    )
	@javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String answer;

	@Column(name = "question_date")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="[[#{qa.date}]]",
		hiddenEditor = true
    )
	private Date questionDate;

	//Column hold boolean information if answer for question is already add
	@Transient
	@DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="[[#{components.qa.answer.exist}]]",
		hiddenEditor = true
    )
	private Boolean answerCheck;

	@Column(name = "sort_priority")
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
		title="[[#{qa.form.sortPriority}]]",
		visible = false,
		tab = "basic"
    )
	private Integer sortPriority;


	/*Hidden unused fields*/

	@Column(name = "answer_date")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
		hidden = true,
		hiddenEditor = true
    )
	private Date answerDate;

	@Column(name = "hash")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		hidden = true,
		hiddenEditor = true
    )
	private String hash;

	@Column(name = "field_a")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.qa.field_a",
		visible = false,
		tab = "fields"
    )
	private String fieldA;

	@Column(name = "field_b")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.qa.field_b",
		visible = false,
		tab = "fields"
    )
	private String fieldB;

	@Column(name = "field_c")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.qa.field_c",
		visible = false,
		tab = "fields"
    )
	private String fieldC;

	@Column(name = "field_d")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.qa.field_d",
		visible = false,
		tab = "fields"
    )
	private String fieldD;

	@Column(name = "domain_id")
	@DataTableColumn(
        inputType = DataTableColumnType.HIDDEN,
		tab="basic"
    )
	private Integer domainId;

	@Transient
    @DataTableColumnNested
	private transient QuestionsAnswersEditorFields editorFields = null;

	public Boolean getAnswerCheck() {
		return isAnswerCheck();
	}

	public Boolean isAnswerCheck() {
		if (Tools.isNotEmpty(getAnswer()) || Tools.isNotEmpty(getAnswerToEmail()) || Boolean.TRUE.equals(publishOnWeb) || getAnswerDate() != null) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
}

