package sk.iway.iwcm.components.inquiry.jpa;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

@Entity
@Table(name = "inquiry_answers")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INQUIRY)
public class InquiryAnswerEntity extends ActiveRecordRepository implements Serializable {

    @Id
    @Column(name = "answer_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_inquiry_answers")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    @Accessors(chain = false)
    private Long id;

    @Column(name = "question_id")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        hidden = true,
        editor = @DataTableColumnEditor(
            attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
        )
    )
    @jakarta.validation.constraints.NotNull
    private Integer questionId;

    @Column(name = "answer_text")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "qa.answer"
    )
    @NotEmpty
    @Size(max = 255)
    private String answerText;

    @Column(name = "image_path")
    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        className = "image",
        title = "editor.perex.image",
        renderFormat = "dt-format-image"
    )
    @Size(max = 255)
    private String imagePath;

    @Column(name = "url")
    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title = "inquiry.url"
    )
    @Size(max = 255)
    private String url;

    @Column(name = "answer_clicks")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        className = "hideOnCreate disabled",
        title = "inquiry.click_number"
    )
    private Integer answerClicks;

    @Column(name = "domain_id")
    private Integer domainId;
}
