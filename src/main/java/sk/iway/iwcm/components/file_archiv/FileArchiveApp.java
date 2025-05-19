package sk.iway.iwcm.components.file_archiv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.ui.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.spring.NullAwareBeanUtils;

@WebjetComponent("sk.iway.iwcm.components.file_archiv.FileArchiveApp")
@WebjetAppStore(
    nameKey = "components.file_archiv.name",
    descKey="components.file_archiv.desc",
    customHtml = "/apps/file-archive/mvc/editor-component.html",
    imagePath = "ti ti-archive", galleryImages = "/apps/file-archive/mvc/", commonSettings = true)
@Getter
@Setter
public class FileArchiveApp extends WebjetComponentAbstract {

    private static final String PRIORITY  = "priority";
    private static final String DATE_INSERT = "dateInsert";
    private static final String VIRTUAL_FILE_NAME = "virtualFileName";

    @Getter
    private class FileArchivatorDTO extends FileArchivatorBean {

        private String downloadPath;
        private String validityFormatted;
        private List<FileArchivatorDTO> archivFiles;
        private List<FileArchivatorDTO> patterns;

        private FileArchivatorDTO(FileArchivatorBean file, Prop prop) {

            //copy all properties from FileArchivatorBean
            NullAwareBeanUtils.copyProperties(file, this);

            //set validityFormatted depending of validity dates
            if (file.getValidFrom() != null && file.getValidTo() != null) {
                this.validityFormatted = prop.getText("components.file_archiv.validFromTo", Tools.formatDateTime(file.getValidFrom()), Tools.formatDateTime(file.getValidTo()));
            } else if (file.getValidFrom() != null) {
                this.validityFormatted = prop.getText("components.file_archiv.validFrom", Tools.formatDateTime(file.getValidFrom()));
            } else if (file.getValidTo() != null) {
                this.validityFormatted = prop.getText("components.file_archiv.validTo", Tools.formatDateTime(file.getValidTo()));
            } else {
                this.validityFormatted = prop.getText("components.file_archiv.validNone");
            }

            String virtualPath = file.getVirtualPath();
            if(virtualPath.startsWith(FileArchivSupportMethodsService.SEPARATOR) == false) virtualPath = FileArchivSupportMethodsService.SEPARATOR + virtualPath;
            this.downloadPath = virtualPath + "?v=" + file.getDateInsert().getTime();

            if(Tools.isTrue(archiv)) {
                List<FileArchivatorBean> patternFiles = getHistoryFiles(file.getId());
                this.archivFiles = patternFiles.stream()
                    .map(f -> new FileArchivatorDTO(f, prop))
                    .toList();
            }
            if(Tools.isEmpty(archivFiles)) this.archivFiles = null;

            if(Tools.isTrue(showPatterns)) {
                List<FileArchivatorBean> patternFiles = getMainPatterns(file.getVirtualPath());
                this.patterns = patternFiles.stream()
                    .map(f -> new FileArchivatorDTO(f, prop))
                    .toList();
            }
            if(Tools.isEmpty(patterns)) this.patterns = null;
        }

        private List<FileArchivatorBean> getHistoryFiles(Long fileArchiveId) {
            //Only files, that are allready uploaded
            List<FileArchivatorBean> historyFiles = FileArchivatorDB.getByReferenceId(fileArchiveId)
                .stream()
                .filter(f -> ( Integer.valueOf(-1).equals(f.getUploaded()) && f.getShowFile() ) )
                .toList();

            return historyFiles
                .stream()
                .sorted((f1, f2) -> {
                    String sortColumn = getSortColumn(order);
                    if (PRIORITY.equals(sortColumn)) {
                        return Tools.isTrue(asc) ? Integer.compare(f1.getPriority(), f2.getPriority()) : Integer.compare(f2.getPriority(), f1.getPriority());
                    } else if (DATE_INSERT.equals(sortColumn)) {
                        return Tools.isTrue(asc) ? f1.getDateInsert().compareTo(f2.getDateInsert()) : f2.getDateInsert().compareTo(f1.getDateInsert());
                    } else if (VIRTUAL_FILE_NAME.equals(sortColumn)) {
                        return Tools.isTrue(asc) ? f1.getVirtualFileName().compareToIgnoreCase(f2.getVirtualFileName()) : f2.getVirtualFileName().compareToIgnoreCase(f1.getVirtualFileName());
                    }
                    return 0;
                }).toList();
        }

        private List<FileArchivatorBean> getMainPatterns(String virtualPath) {
            Specification<FileArchivatorBean> spec = (root, query, builder) -> {
                final List<Predicate> predicates = new ArrayList<>();
                //Pattern of file
                predicates.add(builder.equal(root.get("referenceToMain"), virtualPath));
                addDefaultPredicates(predicates, root, query, builder, ascPatterns, orderPatterns);
                return builder.and(predicates.toArray(new Predicate[predicates.size()]));
            };
            return repository.findAll(spec);
        }
    }

    @JsonIgnore
    private FileArchiveRepository repository;
    private static final String VIEW_PATH = "/apps/file-archive/mvc/file-archive"; //NOSONAR

    @Autowired
    public FileArchiveApp(FileArchiveRepository fileArchiveRepository) {
        this.repository = fileArchiveRepository;
        String fileDir = Constants.getString("fileArchivDefaultDirPath");
        if(Tools.isNotEmpty(fileDir) && FileArchivSupportMethodsService.SEPARATOR.equals(fileDir) == false && fileDir.startsWith(FileArchivSupportMethodsService.SEPARATOR) == false) fileDir = FileArchivSupportMethodsService.SEPARATOR + fileDir;
        this.dir = fileDir;
    }

    //
    @DataTableColumn(inputType = DataTableColumnType.JSON, tab = "basic", className = "dt-tree-dir-simple", title="components.file_archiv.directory", hidden = true, editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-root", value = "constant:fileArchivDefaultDirPath"),
                @DataTableColumnEditorAttr(key = "data-dt-field-skipFolders", value = "fileArchivInsertLaterDirPath"),
            }
        )
    })
    private String dir;

    @DataTableColumn( inputType = DataTableColumnType.CHECKBOX, tab = "basic", title="components.file_archiv.files_with_sub_files")
    private Boolean subDirsInclude;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.file_archiv.kod_produktu")
    private String productCode;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.file_archiv.product", tab = "basic",
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/file-archive/autocomplete-product"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_dir"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
	private String product;

	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.bazar.category", tab = "basic",
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/file-archive/autocomplete-category"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_dir"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
	private String category;


    @DataTableColumn( inputType = DataTableColumnType.CHECKBOX, tab = "basic", title="components.file_archiv.show_only_selected_files")
    private Boolean showOnlySelected = false;

    @DataTableColumn( inputType = DataTableColumnType.TEXTAREA, tab = "basic", title="components.file_archiv.show_only_selected_files")
    private String globalIds;


    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "advanced", title="components.file_archiv.sort_main_files_by",
        editor = {
            @DataTableColumnEditor(
                    options = {
                        @DataTableColumnEditorAttr(key = "components.news.ORDER_PRIORITY", value = PRIORITY),
                        @DataTableColumnEditorAttr(key = "components.file_archiv.time_order", value = "time"),
                        @DataTableColumnEditorAttr(key = "components.file_archiv.order.virtual_file_name", value = "virtual_file_name")
                    }
            )
        }
    )
	private String orderMain;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title="components.file_archiv.ascending_sort_main_file")
    private Boolean ascMain = true;

    @DataTableColumn( inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title="components.file_archiv.entry_pre_opened")
    private Boolean open = false;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title="components.file_archiv.show_archiv",
        editor = { @DataTableColumnEditor( attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before") } ) }
    )
    private Boolean archiv;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "advanced", title="components.file_archiv.show_entry_by",
        editor = {
            @DataTableColumnEditor(
                    options = {
                        @DataTableColumnEditorAttr(key = "components.news.ORDER_PRIORITY", value = PRIORITY),
                        @DataTableColumnEditorAttr(key = "components.file_archiv.time_order", value = "time"),
                        @DataTableColumnEditorAttr(key = "components.file_archiv.order.virtual_file_name", value = "virtual_file_name")
                    }
            )
        }
    )
    private String order;

    @DataTableColumn( inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title="components.file_archiv.ascending_sort_archiv_files")
    private Boolean asc = true;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title="components.file_archiv.show_patterns",
        editor = { @DataTableColumnEditor( attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before") } ) }
    )
    private Boolean showPatterns;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "advanced", title="components.file_archiv.sort_pattern_files_by",
        editor = {
            @DataTableColumnEditor(
                    options = {
                        @DataTableColumnEditorAttr(key = "components.news.ORDER_PRIORITY", value = PRIORITY),
                        @DataTableColumnEditorAttr(key = "components.file_archiv.time_order", value = "time"),
                        @DataTableColumnEditorAttr(key = "components.file_archiv.order.virtual_file_name", value = "virtual_file_name")
                    }
            )
        }
    )
    private String orderPatterns;

    @DataTableColumn( inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title="components.file_archiv.ascending_sort_pattern_files")
    private Boolean ascPatterns = true;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "filesToShow",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/file-archive?globalIds={globalIds}&selectedFilesApp=true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.file_archiv.FileArchivatorBean"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,duplicate,import,export,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "1,desc")
            }
        )
    })
    private List<FileArchivatorBean> filesToSelect;

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(FileArchiveApp.class, "Init of FileArchiveApp app");
    }

    private void addDefaultPredicates(List<Predicate> predicates, Root<FileArchivatorBean> root, CriteriaQuery<?> query, CriteriaBuilder builder, boolean asc, String type) {
        //Only files that are allowed to be showed
        predicates.add(builder.isTrue(root.get("showFile")));
        //Only main files
        predicates.add(builder.equal(root.get("referenceId"), -1));
        //Only uploaded files
        predicates.add(builder.equal(root.get("uploaded"), -1));
        //Domain id
        predicates.add(builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()));
        //Order
        if(Tools.isTrue(asc)) query.orderBy(builder.asc(root.get( getSortColumn(type) )));
        else query.orderBy(builder.desc(root.get( getSortColumn(type) )));
    }

    private List<FileArchivatorBean> getMainFilesData() {
        Specification<FileArchivatorBean> spec = (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            //FilePath
            if(Tools.isTrue(subDirsInclude)) predicates.add(builder.like(root.get("filePath"), dir + "%"));
            else predicates.add(builder.equal(root.get("filePath"), dir));

            //product code is plain text - to lower
            if(Tools.isNotEmpty(productCode)) predicates.add(builder.equal(builder.lower(root.get("productCode")), productCode.toLowerCase()));

            //This are autocompletes, no need to lower
            if(Tools.isNotEmpty(product)) predicates.add(builder.equal(root.get("product"), product));
            if(Tools.isNotEmpty(category)) predicates.add(builder.equal(root.get("category"), category));

            addDefaultPredicates(predicates, root, query, builder, ascMain, orderMain);

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };

        return repository.findAll(spec);
    }

    private String getSortColumn(String value) {
        if(Tools.isEmpty(value)) return DATE_INSERT;

        if(PRIORITY.equalsIgnoreCase(value)) return PRIORITY;
        if("time".equalsIgnoreCase(value)) return DATE_INSERT;
        if("virtual_file_name".equalsIgnoreCase(value)) return VIRTUAL_FILE_NAME;

        return DATE_INSERT;
    }

    @DefaultHandler
	public String view(Model model, HttpServletRequest request)
	{
        //String DB data are dumb, so we must remove prefix slash
        dir = FileArchivSupportMethodsService.normalizeToOldPath(dir);

        List<FileArchivatorBean> datas = new ArrayList<>();
        if(Tools.isTrue(showOnlySelected)) {
            //If user by mistake put comma or space, replace it with +
            globalIds = globalIds.replace(",", "+");
            globalIds = globalIds.replace(" ", "+");
            //Do it using set globalIds
            int[] globalIdsArr = Tools.getTokensInt(globalIds, "+, ");
            if(globalIdsArr.length == 0) globalIdsArr = new int[] { -1 };
            datas = repository.findAllMainFilesUploadedNotPatternIdsIn(
                Arrays.stream(globalIdsArr).boxed().toList(),
                CloudToolsForCore.getDomainId(),
                Pageable.unpaged()
            );
        } else {
            //Get data by selected params
            datas = getMainFilesData();
        }

        Prop prop = Prop.getInstance(request);

        List<FileArchivatorDTO> mainFiles = new ArrayList<>();
        for(FileArchivatorBean file : datas) {
            //Skip pattern files
            if(Tools.isNotEmpty(file.getReferenceToMain())) continue;
            mainFiles.add( new FileArchivatorDTO( file, prop ) );
        }

        model.addAttribute("mainFiles", mainFiles);
        model.addAttribute("showExpanded", Tools.isTrue(open));
        model.addAttribute("showHistoryFiles", Tools.isTrue(archiv));
        model.addAttribute("showPatterns", Tools.isTrue(showPatterns));
        model.addAttribute("tools", Tools.class);

        return VIEW_PATH;
    }
}