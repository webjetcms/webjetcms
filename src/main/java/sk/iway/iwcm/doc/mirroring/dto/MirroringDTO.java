package sk.iway.iwcm.doc.mirroring.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.doc.mirroring.jpa.MirroringEditorFields;
import sk.iway.iwcm.doc.mirroring.rest.MirroringService;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.json.IdFullPath;

@Getter
@Setter
public class MirroringDTO {

    @DataTableColumn(inputType = DataTableColumnType.ID, title = "components.mirroring.sync_id")
    private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "dt-style-text-nowrap show-html",
        hiddenEditor = true
    )
    private String fieldA;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "dt-style-text-nowrap show-html",
        hiddenEditor = true
    )
    private String fieldB;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "dt-style-text-nowrap show-html",
        hiddenEditor = true
    )
    private String fieldC;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "dt-style-text-nowrap show-html",
        hiddenEditor = true
    )
    private String fieldD;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "dt-style-text-nowrap show-html",
        hiddenEditor = true
    )
    private String fieldE;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "dt-style-text-nowrap show-html",
        hiddenEditor = true
    )
    private String fieldF;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "dt-style-text-nowrap show-html",
        hiddenEditor = true
    )
    private String fieldG;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "dt-style-text-nowrap show-html",
        hiddenEditor = true
    )
    private String fieldH;

    private Boolean multipleErr;
    private Boolean nestingWarn;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.mirroring.ignore_problems"
    )
    private Boolean ignoreProblems;

    @Transient
    @DataTableColumnNested
	private transient MirroringEditorFields editorFields = null;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    private IdFullPath selectorA;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    private IdFullPath selectorB;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    private IdFullPath selectorC;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    private IdFullPath selectorD;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    private IdFullPath selectorE;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    private IdFullPath selectorF;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    private IdFullPath selectorG;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    private IdFullPath selectorH;

    public void setField(char field, String value) {
        switch (field) {
            case 'A': this.fieldA = value; break;
            case 'B': this.fieldB = value; break;
            case 'C': this.fieldC = value; break;
            case 'D': this.fieldD = value; break;
            case 'E': this.fieldE = value; break;
            case 'F': this.fieldF = value; break;
            case 'G': this.fieldG = value; break;
            case 'H': this.fieldH = value; break;
            default:
               break;
        }
    }

    public String getField(char field, boolean toLowerCase) {
        String value = switch (field) {
            case 'A' -> this.fieldA;
            case 'B' -> this.fieldB;
            case 'C' -> this.fieldC;
            case 'D' -> this.fieldD;
            case 'E' -> this.fieldE;
            case 'F' -> this.fieldF;
            case 'G' -> this.fieldG;
            case 'H' -> this.fieldH;
            default -> "";
        };

        if(value == null) value = "";

        return toLowerCase == true ? value.toLowerCase() : value;
    }

    public void setSelectField(char field, int id, String fullPath) {
        IdFullPath value = new IdFullPath();
        value.setId(id);
        value.setFullPath(fullPath);

        switch (field) {
            case 'A': this.selectorA = value; break;
            case 'B': this.selectorB = value; break;
            case 'C': this.selectorC = value; break;
            case 'D': this.selectorD = value; break;
            case 'E': this.selectorE = value; break;
            case 'F': this.selectorF = value; break;
            case 'G': this.selectorG = value; break;
            case 'H': this.selectorH = value; break;
            default:
            break;
        }
    }

    public IdFullPath getSelectField(char field) {
        switch (field) {
            case 'A': return this.selectorA;
            case 'B': return this.selectorB;
            case 'C': return this.selectorC;
            case 'D': return this.selectorD;
            case 'E': return this.selectorE;
            case 'F': return this.selectorF;
            case 'G': return this.selectorG;
            case 'H': return this.selectorH;
            default: return null;
        }
    }

    public boolean hasSelectedDuplicates() {
        Set<Integer> seenIds = new HashSet<>();
        return getSelectedAsList(false).stream().anyMatch(e -> !seenIds.add(e));
    }

    public List<Integer> getSelectedAsList(boolean removeDuplicate) {
        List<Integer> selected = new ArrayList<>();

        for(Character alphabet = 'A'; alphabet <= MirroringService.LAST_ALPHABET; alphabet++) {
            IdFullPath field = getSelectField(alphabet);
            if(field != null && field.getId() != null) {
                if(removeDuplicate == false || selected.contains(field.getId()) != true) selected.add(field.getId());
            }
        }

        return selected;
    }

    public Set<Integer> getSelectedAsSet() {
        return new HashSet<>( getSelectedAsList(false) );
    }
}