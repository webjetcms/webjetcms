package sk.iway.iwcm.system.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Bean, ktory drzi udaje log zaznamu v pamati
 */
@Getter
@Setter
public class InMemoryLoggingEvent implements Serializable {

    @Id
    @DataTableColumn(inputType = DataTableColumnType.ID, visible = false)
    private Long id;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab="main", editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
            })
    })
    private String loggerName;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab="main", editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
    private Level level;

    @DataTableColumn(inputType = DataTableColumnType.DATETIME, tab="main", editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
            })
    })
    private Date date;

    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, tab="main", editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
            })
    })
    private String formattedMessage;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, tab="main", visible = false, editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
            })
    })
    private String mdcPropertyMap;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, tab="description", visible = false, editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "class", value = "textarea-code")
            })
    })
    private String stackTrace = "";

    private String message;

    public InMemoryLoggingEvent() {
    }

    public InMemoryLoggingEvent(ILoggingEvent eventObject) {
        this.message = eventObject.getMessage();
        this.formattedMessage = eventObject.getFormattedMessage();
        this.loggerName = eventObject.getLoggerName();
        this.level = eventObject.getLevel();
        this.mdcPropertyMap = eventObject.getMDCPropertyMap() != null ? eventObject.getMDCPropertyMap().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining("\n")) : null;
        this.date = new Date(eventObject.getTimeStamp());

        IThrowableProxy throwableProxy = eventObject.getThrowableProxy();
        if (throwableProxy != null) {
            String throwableStr = ThrowableProxyUtil.asString(throwableProxy);
            stackTrace += throwableStr;
            stackTrace += CoreConstants.LINE_SEPARATOR;
        }
    }

    public String getLevel() {
        return level != null ? level.toString() : "UNKNOWN";
    }

    public String getMessage() {
        if (Tools.isNotEmpty(message)) return message;

        if (Tools.isNotEmpty(stackTrace)) {
                int i = stackTrace.indexOf("\n");
                if (i>0) return stackTrace.substring(0, i);
        }

        return "-----";
    }

    public String getFormattedMessage() {
        if (Tools.isNotEmpty(formattedMessage)) return formattedMessage;
        return getMessage();
    }
}