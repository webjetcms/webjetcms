package sk.iway.iwcm.components.proxy.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "proxy")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_PROXY_CREATE)
public class ProxyBean extends ActiveRecordRepository {

    @Id
    @Column(name = "proxy_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_proxy")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

    /*TAB BASIC*/
    @Column(name = "name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.proxy.name",
        tab = "basic"
    )
    @Size(max = 255)
    @NotBlank
    private String name;

    @Column(name = "local_url")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.proxy.localUrl",
        tab = "basic"
    )
    @NotBlank
	private String localUrl;

    @Column(name = "remote_server")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.remoteServer",
        tab = "basic"
    )
    @Size(max = 255)
    @NotBlank
	private String remoteServer;

    @Column(name = "remote_url")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.remoteUrl",
        tab = "basic"
    )
    @Size(max = 255)
    @NotBlank
	private String remoteUrl;

    @Column(name = "remote_port")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.proxy.remotePort",
        tab = "basic"
    )
	private int remotePort;

    @Column(name = "encoding")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.encoding",
        tab = "basic",
        visible = false
    )
    @Size(max = 16)
    @NotBlank
    private String encoding;

    @Column(name = "proxy_method")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.proxy.proxyMethod",
        tab = "basic",
        visible = false,
        editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "ProxyByHttpClient4", value = "ProxyByHttpClient4"),
					@DataTableColumnEditorAttr(key = "ProxyBySocket", value = "ProxyBySocket")
				}
			)
		}
    )
    private String proxyMethod;

    @Column(name = "include_ext")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.includeExt",
        tab = "basic",
        visible = false
    )
    @Size(max = 255)
    private String includeExt;

    @Column(name = "crop_start")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.proxy.cropStart",
        tab = "basic",
        visible = false
    )
    @Size(max = 255)
	private String cropStart;

    @Column(name = "keep_crop_start")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.proxy.keepCropStart",
        tab = "basic",
        visible = false
    )
    private boolean keepCropStart;

    @Column(name = "crop_end")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.proxy.cropEnd",
        tab = "basic",
        visible = false
    )
    @Size(max = 255)
	private String cropEnd;

    @Column(name = "keep_crop_end")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.proxy.keepCropEnd",
        tab = "basic",
        visible = false
    )
    private boolean keepCropEnd;


    /*TAB SECURITY*/
    @Column(name = "auth_method")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.proxy.authMethod",
        tab = "security",
        visible = false,
        editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "", value = ""),
					@DataTableColumnEditorAttr(key = "NTLM", value = "ntlm"),
					@DataTableColumnEditorAttr(key = "Basic", value = "basic")
				},
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.proxy.securityLogin")
                }
			)
		}
    )
    @Size(max = 16)
	private String authMethod;

    @Column(name = "auth_username")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.authUsername",
        tab = "security",
        visible = false
    )
    @Size(max = 64)
	private String authUsername;

    @Column(name = "auth_password")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.authPassword",
        tab = "security",
        visible = false
    )
    @Size(max = 64)
	private String authPassword;

    @Column(name = "auth_host")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.authHost",
        tab = "security",
        visible = false
    )
    @Size(max = 64)
	private String authHost;

    @Column(name = "auth_domain")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.authDomain",
        tab = "security",
        visible = false
    )
    @Size(max = 64)
	private String authDomain;

    @Column(name = "allowed_methods")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.proxy.allowedMethods",
        tab = "security",
        visible = false
    )
    @Size(max = 64)
	private String allowedMethods;

	@Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

	public int getProxyId() {
		if(id == null) return 0;
		return id.intValue();
	}

	public void setProxyId(int proxyId) {
		this.id = Long.valueOf(proxyId);
	}
}
