package sk.iway.iwcm.components.perex_groups;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.drew.lang.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

@Entity
@Table(name = "perex_group_doc")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_PEREX_GROUP_UPDATE)
public class PerexGroupDocEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_perex_group_doc")
    private Long id;

    @Column(name = "doc_id")
    @NotNull
    private Long docId;

    @Column(name = "perex_group_id")
    private Long perexGroupId;
}
