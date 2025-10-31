package sk.iway.iwcm.components.forms;

import java.util.Date;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;

import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;
import org.eclipse.persistence.annotations.Converters;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserDetailsConverter;

@MappedSuperclass
@Converters(value = {
    @Converter(name = "UserDetailsConverter", converterClass = UserDetailsConverter.class)
})
@Setter
@Getter
public class FormsEntityBasic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_forms")
    private Long id;

    @Column(name = "form_name")
    private String formName;

    @Lob
    private String data;

    @Lob
    private String files;

    @Column(name = "create_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    @Lob
    private String html;

    @Column(name = "user_id")
    @Convert("UserDetailsConverter")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) //toto nepotrebujeme deserializovat pri post requeste
    private UserDetails userDetails;

    @Lob
    private String note;

    @Column(name = "doc_id")
    private int docId;

    @Column(name = "last_export_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastExportDate;

    @Column(name = "domain_id")
    private int domainId;

    @Column(name = "double_optin_confirmation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date doubleOptinConfirmationDate;

    @Column(name = "double_optin_hash")
    private String doubleOptinHash;

    @Transient
    private Map<String, String> columnNamesAndValues;

    @Transient
    private int count;
}
