package sk.iway.iwcm.doc.attributes.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

@Entity
@Table(name = "doc_atr")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_DOC_ATTRIBUTES)
public class DocAtrEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_doc_atr")
    private Long id;

    @Column(name = "doc_id")
    private Integer docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference(value="atrDef")
    @JoinColumn(name = "atr_id")
    private DocAtrDefEntity atrDef;

    @Column(name = "value_string")
    private String valueString;

    @Column(name = "value_int")
    private Double valueNumber;

    @Column(name = "value_bool")
    private Boolean valueBoolean;
}
