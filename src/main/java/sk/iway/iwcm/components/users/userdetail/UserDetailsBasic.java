package sk.iway.iwcm.components.users.userdetail;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@MappedSuperclass
@Getter
@Setter
public class UserDetailsBasic extends ActiveRecordRepository {

    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_users")
    @DataTableColumn(
            inputType = DataTableColumnType.ID,
            sortAfter = "FIRST"
    )
    private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "[[#{components.user.login}]]",
        hiddenEditor = true,
        sortAfter = "id",
        tab = "personalInfo"
    )
    //pre DT editor mame specialne pole editorFields.login tak, aby bolo hned za menom a priezviskom
    private String login;

    /*PERSONAL INFO - Person info*/

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.title}]]",
        tab = "personalInfo",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "useredit.personal_info")
                }
            )
        },
        visible = false,
        sortAfter = "login"
    )
    private String title;

    @Column(name = "first_name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.firstName}]]",
        tab = "personalInfo",
        sortAfter = "title"
    )
    private String firstName;

    @Column(name = "last_name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.lastName}]]",
        tab = "personalInfo",
        sortAfter = "firstName"
    )
    private String lastName;

    @Column(name = "date_of_birth")
    //deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title = "[[#{components.user.newuser.dateOfBirth}]]",
        tab = "personalInfo",
        visible = false,
        sortAfter = "lastName"
    )
    private Date dateOfBirth;

    @Column(name = "sex_male")
    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "[[#{components.user.newuser.sexMale}]]",
        tab = "personalInfo",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "reguser.male", value = "true"),
                    @DataTableColumnEditorAttr(key = "reguser.female", value = "false")
                }
            )
        },
        visible = false,
        sortAfter = "dateOfBirth"
    )
    private Boolean sexMale;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "[[#{components.user.newuser.signature}]]",
        tab = "personalInfo",
        visible = false,
        sortAfter = "sexMale"
    )
    private String signature;

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title = "[[#{components.user.newuser.photo}]]",
        tab = "personalInfo",
        visible = false,
        sortAfter = "signature"
    )
    private String photo;

    @DataTableColumn(
        inputType = DataTableColumnType.PASSWORD,
        title = "[[#{components.user.password}]]",
        tab = "personalInfo",
        hidden = true,
        className = "required",
        sortAfter = "email"
    )
    @Transient //toto nechceme citat z DB
    private String password;

    /*CONTACTS - Contacts*/

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.user.company}]]",
        tab = "contactTab",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.users.contact")
                }
            )
        },
        visible = false
    )
    private String company;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.position}]]",
        tab = "contactTab",
        visible = false
    )
    private String position;

    @Column(name = "adress")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.user.address}]]",
        tab = "contactTab",
        visible = false
    )
    private String address;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.user.city}]]",
        tab = "contactTab",
        visible = false
    )
    private String city;

    @Column(name = "PSC")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.user.zip}]]",
        tab = "contactTab",
        visible = false
    )
    private String psc;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.country}]]",
        tab = "contactTab",
        visible = false
    )
    private String country;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.user.phone}]]",
        tab = "contactTab",
        visible = false
    )
    private String phone;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.user.fax}]]",
        tab = "contactTab",
        visible = false
    )
    private String fax;

    /*CONTACTS - Delivery address*/

    @Column(name = "delivery_first_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.delivery_first_name}]]",
        tab = "contactTab",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.basket.invoice_email.delivery_address")
                }
            )
        },
        visible = false
    )
    private String deliveryFirstName;

    @Column(name = "delivery_last_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.delivery_last_name}]]",
        tab = "contactTab",
        visible = false
    )
    private String deliveryLastName;

    @Column(name = "delivery_company")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.delivery_company}]]",
        tab = "contactTab",
        visible = false
    )
    private String deliveryCompany;

    @Column(name = "delivery_adress")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.delivery_address}]]",
        tab = "contactTab",
        visible = false
    )
    private String deliveryAddress;

    @Column(name = "delivery_city")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.delivery_city}]]",
        tab = "contactTab",
        visible = false
    )
    private String deliveryCity;

    @Column(name = "delivery_psc")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.delivery_psc}]]",
        tab = "contactTab",
        visible = false
    )
    private String deliveryPsc;

    @Column(name = "delivery_country")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.delivery_country}]]",
        tab = "contactTab",
        visible = false
    )
    private String deliveryCountry;

    @Column(name = "delivery_phone")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.delivery_phone}]]",
        tab = "contactTab",
        visible = false
    )
    private String deliveryPhone;

    //Prepare and return user full name
    @JsonIgnore
    public String getFullName() {
		StringBuilder fullName = new StringBuilder("");

		if(Constants.getBoolean("fullNameIncludeTitle") && !Tools.isEmpty(title))
			fullName.append(title).append(" ");

		if(!Tools.isEmpty(firstName))
			fullName.append(firstName).append(" ");

		if(!Tools.isEmpty(lastName))
			fullName.append(lastName).append(" ");

		return fullName.toString().trim();
	}
}
