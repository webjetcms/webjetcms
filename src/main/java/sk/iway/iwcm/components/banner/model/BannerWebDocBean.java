package sk.iway.iwcm.components.banner.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

@Entity
@Table(name="banner_doc")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_BANNER_CREATE)
public class BannerWebDocBean extends ActiveRecordRepository implements Serializable {

	@JsonIgnore
    private static final long serialVersionUID = -1L;

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_banner_gr")
	@Column(name="id")
	private Long id;

    @Column(name="doc_id")
	private Integer docId;

    @ManyToOne
	@JsonBackReference(value="bannerBeanDoc")
	@JoinColumn(name="banner_id")
	private BannerBean bannerBeanDoc;

    public BannerWebDocBean() {}

	public BannerWebDocBean(String path) {
        DocDetails doc = DocDB.getInstance().getCreateDoc(path);
        if (doc != null) docId = doc.getDocId();
    }

    @Override
	public void setId(Long id)
	{
		this.id = id;
	}

	@Override
	public Long getId()
	{
		return id;
	}

    public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}

	@JsonIgnore
	public BannerBean getBanner() {
		return bannerBeanDoc;
	}

	public void setBanner(BannerBean bannerBeanDoc) {
		this.bannerBeanDoc = bannerBeanDoc;
	}

	@JsonProperty(access = Access.READ_ONLY)
    public String getFullPath() {
		DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
		if (doc != null) {
			return doc.getFullPath();
		}
		return "";
	}

	@JsonIgnore
	public String toString() {
		return "BannerDocBean id="+getId()+" path="+getFullPath();
	}
}
