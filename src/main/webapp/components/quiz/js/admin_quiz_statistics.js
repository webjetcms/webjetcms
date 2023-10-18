var datatableEditor;
$(document).ready(function() {
    setActiveSidebarLink($('.page-sidebar-menu li ul li a[href$="/components/quiz/admin_list.jsp"]'));
    var quizId = $("#quizAnswerStatistics").attr("data-quizIdValue");
    loadProperties(pageLng, quizId);

});

function setActiveSidebarLink(element) {
    element.parent().addClass("active");
    element.parent().parent().parent().addClass("open active")
    element.parent().parent().css("display", "block");
}

function loadProperties(language, quizId) {
    $.ajax({
        type: 'GET',
        url: '/rest/properties/' + language + '/components.quiz.statistics.table.info',
        success: function(data) {
            loadStatisticsTable(data, quizId);
        }
    });
}

function loadStatisticsTable(propData, quizId) {
    try {
        datatableEditor = new $.fn.dataTable.Editor({
            table: "#quizAnswerStatistics",
            fields: [{
                    label: "Question:",
                    name: "questionTitle",
                },
                {
                    label: "Right Answer Count:",
                    name: "rightAnswerCount",
                },
                {
                    label: "Wrong Answer Count:",
                    name: "wrongAnswerCount",
                },
            ],
        });
    } catch (e) {}
    var statDataTable = $("#quizAnswerStatistics").DataTable({
        ajax: {
            url: "/admin/rest/quiz/" + quizId + "/answers",
            type: "POST",
            data: function(table) {
                table.fromDate = $("#startDateId").val();
                table.toDate = $("#endDateId").val();
            },
            dataSrc: ""
        },
        columns: [
            { data: "questionTitle" },
            { data: "rightAnswerCount" },
            { data: "wrongAnswerCount" },
        ],
        searching: true,
        bInfo: true,
        bLengthChange: true,
        pageLength: 10,
        language: {
            lengthMenu: propData['components.quiz.statistics.table.info.length_menu'],
            zeroRecords: propData['components.quiz.statistics.table.info.zero_records'],
            info: propData['components.quiz.statistics.table.info.page_info'],
            infoEmpty: propData['components.quiz.statistics.table.info.empty_info'],
            infoFiltered: propData['components.quiz.statistics.table.info.filtered_info'],
            search: propData['components.quiz.statistics.table.info.search_text']
        }
    });

    $("#filterDatesButton").on("click", function() {
        statDataTable.ajax.reload();
        var dateFrom = $("#startDateId").val();
        var dateTo = $("#endDateId").val();
        var submittedRangeElement = $("#submittedQuizesRange");
        $.ajax({
            type: 'GET',
            url: '/admin/rest/quiz/' + quizId + '/answers/count?fromDate=' + dateFrom + '&toDate=' + dateTo,
            success: function(data) {
                submittedRangeElement.text(data);
            }
        });
    });

    $('#quizAnswerStatistics thead tr:eq(0) th').each(function() {
        var element = $('#quizAnswerStatistics thead tr:eq(0) th').eq($(this).index());
        element.append('<div class="table-title sortable"/>' + element.attr("data-statLabel") + '</div>');
        element.append('<div class="table-input"/><input class="form-control form-filter" type="text" placeholder="' + propData['components.quiz.statistics.table.info.vyhladat'] + '"/></div>');
    });

    statDataTable.columns().every(function(index) {
        $('#quizAnswerStatistics thead tr:eq(0) th:eq(' + index + ') div.table-input input').on('keyup change', function() {
            statDataTable.column($(this).parent().parent().index() + ':visible')
                .search(this.value)
                .draw();
        });
    });

    $('.table-input input').click(function(event) {
        event.stopPropagation();
    });
}