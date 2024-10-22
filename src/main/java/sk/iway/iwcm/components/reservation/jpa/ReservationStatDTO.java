package sk.iway.iwcm.components.reservation.jpa;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class ReservationStatDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="reguser.fullname"
    )
	private String userName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="reservation.reservation_object.selected_object"
    )
	private String reservationObjectName;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="reservation.reservation_stat.numberOfReservations"
    )
    private Integer numberOfReservations;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="reservation.reservation_stat.totalPrice"
    )
    private BigDecimal totalPrice;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="reservation.reservation_stat.numberOfReservedDays"
    )
    private Integer numberOfReservedDays;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="reservation.reservation_stat.averageIntervalInDays"
    )
    private BigDecimal averageIntervalInDays;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="reservation.reservation_stat.totalReservedHours"
    )
	private BigDecimal totalReservedHours;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="reservation.reservation_stat.averageTimePerDay"
    )
    private BigDecimal averageTimePerDay;
}
