import WJ from "../../src/js/webjet";

export function renderPrefix(row) {
    //console.log("Render prefix, row=", row);
    return (typeof row.settings.aoColumns[row.col].renderFormatPrefix != "undefined") ? row.settings.aoColumns[row.col].renderFormatPrefix : "";
}

export function renderSuffix(row) {
    return (typeof row.settings.aoColumns[row.col].renderFormatSuffix != "undefined") ? row.settings.aoColumns[row.col].renderFormatSuffix : "";
}

export function renderTd(row, td, rowData) {

    if(td != null && (td === "" || td === row.settings.aoColumns[row.col].renderHideValue)) {
        return '<div class="datatable-column-width"></div>';
    } else {
        if(typeof row.settings.aoColumns[row.col].editor !== "undefined") {
            if (typeof row.settings.aoColumns[row.col].editor.options !== "undefined") {

                var content = "";

                if(typeof row.settings.aoColumns[row.col].editor.separator !== "undefined") {

                    td = td.toString();
                    var values = td.split(row.settings.aoColumns[row.col].editor.separator);
                    var isEmpty = true;

                    $.each(values, function(index,v){

                        if( Object.prototype.toString.call( row.settings.aoColumns[row.col].renderHideValue ) === '[object Array]' ) {

                            var isHidden = false;

                            $.each(row.settings.aoColumns[row.col].renderHideValue, function(index,hideVal){
                                if(v == hideVal){
                                    isHidden = true;
                                }
                            });

                            if(isHidden) {
                                return true;
                            }

                        } else {
                            if(v == row.settings.aoColumns[row.col].renderHideValue){
                                return true;
                            }
                        }

                        if( index > 0 && index < values.length && content !== "") {
                            content += ", ";
                        }

                        isEmpty = true;

                        $.each(row.settings.aoColumns[row.col].editor.options, function(index,option){

                            if(v == option.value) {
                                content += option.label;
                                isEmpty = false;
                            }

                        });

                        if(isEmpty){
                            content += "["+v+"]";
                        }

                    });

                } else {

                    if( Object.prototype.toString.call( td ) === '[object Array]' ) {

                        var isEmpty = true;

                        $.each(td, function(index,v){

                            if(v == row.settings.aoColumns[row.col].renderHideValue){
                                return true;
                            }

                            if( index > 0 && index < td.length && content !== "") {
                                content += ", ";
                            }

                            isEmpty = true;

                            $.each(row.settings.aoColumns[row.col].editor.options, function(index,option){

                                if(v == option.value) {
                                    content += option.label;
                                    isEmpty = false;
                                }

                            });

                            if(isEmpty){
                                content += "["+v+"]";
                            }

                        });

                    } else {

                        var isEmpty = true;

                        $.each(row.settings.aoColumns[row.col].editor.options, function(index,option){

                            //aby aj true=="true"
                            if(td == option.value || ""+td == option.value) {
                                content = option.label;
                                isEmpty = false;
                            }

                        });

                        if(isEmpty){
                            if (td === false) {
                                content += row.settings.aoColumns[row.col].editor.falseValue || WJ.translate("button.no");
                            }
                            else if (td === true) {
                                content += row.settings.aoColumns[row.col].editor.trueValue || WJ.translate("button.yes");
                            }
                            else if (typeof row.settings.aoColumns[row.col].className != "undefined" && row.settings.aoColumns[row.col].className.indexOf("allow-html")!=-1) {
                                //console.log(row.settings.aoColumns[row.col]);
                                //ak obsahuje HTML, asi sa jedna o stavove ikony
                                content = td;
                            }
                            else content = td;
                        }
                    }
                }

                if(content != ""){
                    td = content;
                }

            }
        }

        if (td == null) {
            td = "";
        }

        var template = "";

        if (typeof row.settings.aoColumns[row.col].renderFormatLinkTemplate != "undefined") {

            template = row.settings.aoColumns[row.col].renderFormatLinkTemplate;

            var data = template.split("{{");

            $.each(data, function (i, v) {
                if (v.indexOf("}}") > 0) {
                    var name = v.split("}}");
                    template = template.replace("{{" + name[0] + "}}", rowData[name[0]]);
                }
            });

            //console.log("Is link, td=", td, " row=", row);

            let linkText = WJ.escapeHtml(td);
            linkText = linkText.replaceAll("&quot;", "\"");
            linkText = linkText.replaceAll("&#x3D;", "=");
            //console.log("linkText=", linkText);

            if ("quill" === row.settings.aoColumns[row.col].editor?.type) linkText = WJ.htmlToText(td);

            return  '<div class="datatable-column-width"><a href="' + template + '">'+ renderPrefix(row) + linkText + renderSuffix(row) + '</a></div>';
        } else {
            var text = WJ.htmlToText(td);
            try {
                var className = row.settings.aoColumns[row.col].className;
                //console.log("className=", className);
                if (typeof className != "undefined" && className != null && className.indexOf("allow-html")!=-1) {
                    //console.log("allowing html, td=", td);
                    text = td;
                }
                if (typeof className != "undefined" && className != null && className.indexOf("show-html")!=-1) {
                    //console.log("allowing html, td=", td);
                    text = WJ.escapeHtml(td);
                }
            } catch (e) {}
            //console.log("class: ", row.settings.aoColumns[row.col].className);
            return '<div class="datatable-column-width">'+ renderPrefix(row) + text + renderSuffix(row) + '</div>';
        }

    }
}

export function isRangeOk(min,max,val){
    if( ( isNaN( min ) && isNaN( max ) ) ||
    ( isNaN( min ) && val <= max ) ||
    ( min <= val   && isNaN( max ) ) ||
    ( min <= val   && val <= max ) ) {
        return true;
    } else {
        return false;
    }
}

export function renderBoolean(td, type, rowData, row, isTrue, isFalse) {

    if (td === "" || td === null || typeof td == "undefined") {
        td = "";
    }

    if (td.toString().toLowerCase() === isTrue) {
        td = true;
    } else if (td.toString().toLowerCase() === isFalse) {
        td = false;
    } else {
        td = "";
    }
    //console.log("type=", type, " td=", td);

    var text = "";

    if(td === true) {
        text = WJ.translate("button.yes");
    } else if(td === false) {
        text = WJ.translate("button.no");
    } else {
        text = td;
    }

    if ( type == "filter" ) {
        return td;
    } else {
        //return renderTd(row,td);
        //pre boolean nerendrujeme hodnotu ale len true/false hodnoty
        return text;
    }
}

export function renderDate(td, type, rowData, row, dateFormat) {

    //console.log("renderDate, td=", td, " type=", type, " rowData=", rowData, " row=", row, " dateFormat=", dateFormat);

    //ak je hodnota 0 a jedna sa o date/datetime/timehm/timehms tak hodnotu povazuj za prazdny datum
    if (td == "0" && row.settings.aoColumns[row.col].className.indexOf("date") != -1 ||
        td == "0" && row.settings.aoColumns[row.col].className.indexOf("time") != -1) {
        //potrebujeme aj upravit rowData na prazdnu hodnotu kvoli DT editoru
        rowData[row.settings.aoColumns[row.col].name] = "";
        return null;
    }

    if (td === "" || td === null || typeof td === "undefined") {
        //console.log("   returning null");
        return null;
    }

    if (type == "sort" || type == "filter" ) {
        return td;
    } else {
        td = parseInt(td);
        if(!isNaN(td)) {
            //console.log("rendering date=", moment(td).format(dateFormat));
            return renderTd(row, moment(td).format(dateFormat), rowData);
        } else {
            return null;
        }
    }
}

export function renderPercentage(td, type, rowData, row, percentageFormat) {

    if (td === "" || td === null || typeof td == "undefined") {
        return "";
    }

    if (type == "sort" || type == "filter" ) {
        return parseFloat(numeral(td).format(percentageFormat).replace(" %", "").replace(",", "."));
    } else {
        return renderTd(row, numeral(td).format(percentageFormat), rowData);
    }

}

export function renderText(td, type, rowData, row) {

    if (td === "") { return "" }

    if (type == "sort" || type == "filter" ) {
        return td;
    } else {
        return renderTd(row, td, rowData);
    }

}

export function renderTextHtmlInput(td, type, rowData, row) {

    if (td === "") { return "" }

    if (type == "sort" || type == "filter" ) {
        try {
            //DT automatically escape entities, so for search we must escape it too
            if (td != null) {
                td = td.replaceAll("&", "&amp;");
                td = td.replaceAll("<", "&lt;");
                td = td.replaceAll(">", "&gt;");
                td = td.replaceAll("\"", "&quot;");
            }
            return td;
        } catch (e) {}
        return td;
    }

    return renderTd(row, td, rowData);
}

export function renderSelect(td, type, rowData, row) {

    if (td === "") { return "" }

    return renderTd(row, td, rowData);

    // if (type == "sort" || type == "filter" ) {
    //     return td;
    // } else {
    //     return renderTd(row,td);
    // }

}

export function renderCheckbox(td, type, rowData, row) {

    if (td === "") { return "" }

    return renderTd(row, td, rowData);

    // if (type == "sort" || type == "filter" ) {
    //     return td;
    // } else {
    //     return renderTd(row,td);
    // }

}

export function renderRadio(td, type, rowData, row) {

    if (td === "") { return "" }

    return renderTd(row,td);

    // if (type == "sort" || type == "filter" ) {
    //     return td;
    // } else {
    //     return renderTd(row,td);
    // }

}

export function renderNumber(td, type, rowData, row, numberFormat) {

    if (td === "") { return "" }

    if (type == "sort" || type == "filter" ) {
        return parseInt(td)
    } else {
        return renderTd(row, numeral(td).format(numberFormat), rowData);
    }

}

export function renderNumberDecimal(td, type, rowData, row, numberFormat) {

    if (td === "") { return "" }

    if (type == "sort" || type == "filter" ) {
        return parseFloat(td, 10);

    } else {
        return renderTd(row, numeral(td).format(numberFormat), rowData);
    }

}

export function renderLink(td, type, rowData, row) {

    if (td === "") { return "" }

    if (type == "sort" || type == "filter" ) {
        return td;
    } else {

        var template = "";

        if (typeof row.settings.aoColumns[row.col].renderFormatLinkTemplate != "undefined") {

            template = row.settings.aoColumns[row.col].renderFormatLinkTemplate;

            var data = template.split("{{");

            $.each(data, function (i, v) {
                if (v.indexOf("}}") > 0) {
                    var name = v.split("}}");
                    template = template.replace("{{" + name[0] + "}}", rowData[name[0]]);
                }
            });

            return  "<a href=\"" + template + "\" target=\"_blank\">" + renderTd(row, td, rowData) + "</a>";
        }

        return  "<a href=\"" + td + "\" target=\"_blank\">" + renderTd(row, td, rowData) + "</a>";

    }
}

export function renderImage(td, type, rowData, row) {

    if (td === "") { return "" }

    if (type == "sort" || type == "filter" ) {
        return td;
    } else {

        if (td!=null && (td.endsWith(".png") || td.endsWith(".gif") || td.endsWith(".jpg") || td.endsWith(".svg"))) {
            return  "<a href=\"" + WJ.escapeHtml(td) + "\" target=\"_blank\"><img src=\""+td+"\"/> " + renderTd(row, td, rowData) + "</a>";
        }

        return renderLink(td, type, rowData, row);
    }
}

export function renderMail(td, type, rowData, row) {
    if (td === "") return "";

    if (type == "sort" || type == "filter" ) {
        return td;
    } else {
        return  "<a href=\"mailto:" + td + "\" target=\"_blank\">" + renderTd(row, td, rowData) + "</a>";
    }
}

export function renderJson(td, type, rowData, row) {

    //console.log("Using DT render json, rowData=", rowData, "row=", row, "td=", td, "type=", type);
    var click = "";
    if (typeof row.settings.aoColumns[row.col].className != "undefined") click = row.settings.aoColumns[row.col].className;
    //console.log("click=", click);

    if (td === "" || td == null || td === "null") { return "" }

    //skonvertuj aj single objekt na pole
    var tdArray = Array.isArray(td) ? tdArray = td : tdArray = [td];

    //console.log(tdArray);

    //preiteruj JSON objekt a ziskaj fullPath
    var fullPath = "";
    $.each(tdArray, function( key, item ) {
        //console.log("iterating, item=", item);
        let itemFullPath = "";

        //item moze byt aj null, preto musi byt osetrene zobrazenie prazdnej cesty
        if (item != null && typeof item.fullPath != "undefined") itemFullPath = (item.domainName && item.domainName != '' && click.indexOf('alldomains')!=-1 ? item.domainName+':' : '') + item.fullPath;
        else if (item != null && typeof item.virtualPath != "undefined") itemFullPath = item.virtualPath;

        if (fullPath == "") fullPath = itemFullPath;
        else fullPath += "\n"+itemFullPath;
    });

    if (type == "sort" || type == "filter" ) {
        return fullPath;
    } else {

        //return renderLink(fullPath, type, rowData, row);
        if (click.indexOf("dt-tree-page") != -1 && tdArray.length == 1) {
            return "<a href=\"/showdoc.do?docid=" + tdArray[0].docId  + "\" target=\"_blank\">" + renderTd(row, fullPath, rowData) + "</a>";
        }

        return renderTd(row, fullPath, rowData);
    }

}

export function renderAttrs(td, type, rowData, row) {
    //render attrs to datatable view, normally it's hidden
    return renderTd(row, fullPath, rowData);
}