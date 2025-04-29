# List of reservations

Application **List of reservations** allows you to create/edit/delete bookings, as well as import them from an Excel file and export them to an Excel/CSV file (or even print them immediately when exporting).

The list also contains 3 buttons for approving/rejecting/resetting a reservation, see. section [Approval of reservations](#approval-of-reservations). In the left menu you can also go to [Reservation statistics](../reservations-stat/README.md).

![](reservation-datatable.png)

The booking editor contains 4 tabs, the functions of which are described in the following description.

## Basic

Card **Basic** as the name suggests, contains basic information about the reservation. The list with the selection of the reservation object to which the reservation relates is important.

| Hourly bookings | Day bookings |
| :-------------------------------------: | :-------------------------------------: |
| ![](reservation-editor_basic_tab_1.png) | ![](reservation-editor_basic_tab_2.png) |

The composition of the card will vary depending on whether the selected booking object is all-day or not. For more information see [List of reservation objects](../reservation-objects/README.md).

**All day reservations**

In this mode, the object is reserved for the whole day. The best example is booking a hotel room. You need to enter the date range of the reservation and the arrival/departure time.

!>**Warning:** if the reservation object can only be booked for a whole day, the last day in the date range is not counted because the customer leaves on that day. Therefore, the booking range for full-day bookings must be a minimum of 2 days.

**Hourly bookings**

There is one important thing to remember with this type of booking. If you choose days from 01.01.2022 to 03.01.2022 between 08:00 and 16:00, it does not mean that the reservation starts on 01.01.2022 at 08:00 and lasts until 03.01.2022 at 16:00 and everything in that range is booked. But it is not. With these chosen values, it means in practice that you book this **you book the reservation object from 08:00 to 16:00 for each day separately**. The reason is simple, this way you can book a property at a specific time for multiple days without having to book the entire interval. If the booking interval for a given object is set from 05:00 to 20:00, only the specified time interval is booked for individual days and the rest of the interval is not booked, even if the reservation lasts for example 3 days.

At the bottom of the tab, you can see an overview with the booking intervals for each day of the week. These times are set according to the currently selected booking object. This is a help when creating/editing a reservation if you don't remember exactly when a particular reservation object can be booked for a particular day of the week. This overview is displayed **for hourly bookings only**.

![](reservation-editor_basic_tab_4.png)

Field **Notice** displays information about the validity of the booking being created. That is, whether or not a reservation for the selected object can be created in the given range. More information about the possible states in this field is explained in the section [Validation of reservations](#validation-of-reservations). If the reservation is valid, the border of the text box will turn green and if the reservation is not valid, it will turn red.

**Booking price** displays the current price of the booking being created. The price depends on the selected reservation object, the reservation interval and the special prices set for the reservation object.

!>**Warning:** the user's discount is then automatically applied to this booking price. This percentage discount is set for specific [user groups](../../../../admin/users/user-groups.md). If a user belongs to multiple **user groups** that have a set percentage discount, the largest of these will be used. If the discount has a value of `0%`, the booking amount does not change. If the discount has a value of `100%`, booking is free.

!>**Warning:** the current prices and discounts apply, i.e. the price that was calculated when the booking was made. This means that if you have a reservation scheduled for, for example, a month from now and the price of the reservation object goes up in that time or the user's discount changes, the price of the reservation **will not change**. **However** If you modify your booking, the current prices and discounts will be applied, which may change the original price, which you will not be able to get back.

The booking status and the price of the reservation are updated whenever the date, time or reservation object is changed.

![](reservation-editor_basic_tab_3.png)

## Personal data

In the charts **Personal data** the reservation requestor's details can be set. Most of the data is automatically set from the logged-in person's profile, but these values can also be changed or not entered at all.

![](reservation-editor_personalInfo_tab.png)

## Special price

Card **Special price** contains a nested table with information about the special price of the reservation object for a specific period. The entries in the table are set according to the currently selected reservation object from the tab [Basic](../reservations/README.md#Basic) and can only be exported, but cannot be added, edited or deleted (they are for information purposes only).

![](reservation-editor_specialPrice_tab.png)

## Approval

Card **Approval** is used to change the reservation status. This tab is only displayed under specific conditions.

Conditions to view the card:
- editing a record, the tab only appears when editing a booking record
- need for approval, the reservation object that the reservation is trying to reserve must have the parameter **Approval is required** and must have the email of the approver entered
- approver, the card can only be shown to the person who has permission to approve the booking. This means that if the email of the logged-in user matches the email of the approver entered in the booking system and the previous points have been met, the card will be displayed to the user

Once displayed, the tab contains 3 buttons to change the booking status. These actions will be better explained in the section **Approval of reservations**.

![](reservation-editor_acceptation_tab.png)

## Validation of reservations

Reservation validation logic is an important part of the application **List of reservations**, which checks whether the reservation being created or modified complies with all the rules and conditions. Validation is automatically triggered when you try to save a new reservation or modify an existing reservation. If the reservation is valid (meets all the necessary requirements) the create/edit action will be successful, otherwise an error will occur and the user will be notified with either a specific or a general error message.

The reservation will not be saved/edited until it meets all validation rules.

### Date range

The from date must be less than the to date (if the dates are the same, only one day is reserved).

!>**Warning:** in the case of **all-day bookings**, the range shall be **at least 2 days** as the last day of the range is the day of departure and does not count towards the booking as such.

### Time range

Validation of the time range varies according to the type of reservation.

**All day reservations**

For all-day bookings, the time component is not validated as **Arrival time** / **Departure time** they are not on the same day and it's up to your decision.

!>**Warning:** we strongly recommend that **Arrival time** was greater than **Departure time** otherwise the room will not be bookable for new customers on the day of the old customer's departure.

**Hourly bookings**

**Time from** must be sharply less than **Time to** because you must book a minimum of 1 minute. At the same time, it is checked whether this time range is greater than or at least equal to the specified "Minimum reservation length (in minutes)" value set for the reservation object.

### Scope in the past

You cannot book the day(s) in the past. If you are trying to make a reservation for today then the time slot cannot be in the past.

!>**Warning:** at your own risk, you have the option to save the reservation even if the selected range is in the past. Just in the tab **Basic** choose the option **Allow creation in the past**.

### Valid booking range

This check shall be carried out only for **hourly reservations**. Checks whether the specified time range is within the object's reservation interval. This check is performed for each booked day separately. For example, if you are trying to book a property between 08:00-09:00 for the next 3 days, and even one of these days has a different booking interval, the booking is not valid. Of course, special booking intervals for individual days of the week are also taken into account, for more information see [times by day](../reservation-objects/README.md#times-by-day).

### Maximum number of bookings at the same time

In this case, it is checked whether after adding a reservation, multiple reservations will not be crossed at the same time as allowed by the parameter **Maximum number of bookings at the same time** of the object. Reservations are considered overlapping if their time intervals overlap in at least one minute (intervals do not overlap if the beginning of one is the end of the other in the same minute).

**All day reservations**

For these bookings we have recommended that **Arrival time** was sharply greater than the time of departure. For example, if the check-in time is always 14:00 and the check-out time is always 10:00, then after the old customer leaves at 10:00 there is still time to clean (or otherwise prepare) the room for the new customer who checks in at 14:00. Such bookings do not overlap and it is possible to book a room on the day another customer leaves. In reality, it looks like the bookings intersect, but this is not the case (since the old customer leaves before the new one arrives).

**Hourly bookings**

For example, if you have 4 reservations, and their time intervals are `08:00-11:00 / 08:00-09:00 / 09:00-10:00 / 10:00-11:00` so we see that the reservation at the time of `08:00-11:00` intersects with 3 different reserves, but never more than 2 at the same time, because the other 3 reserves do not intersect with each other.

The check takes all bookings over the property on the same day and tests whether this limit will be exceeded after adding our booking. This is tested for each day separately and if even one day exceeds the maximum number of bookings at the same time, the booking will be marked as invalid.

!>**Warning:** intersection control uses **only approved** reservations above the booking object.

!>**Warning:** you have the option to overbook at your own risk, even in the event of overbooking where the maximum number of bookings is exceeded. Simply tab **Basic** choose the option **Allow capacity overruns**.

## Approval of reservations

Changing the reservation status is possible either by using the editor and more precisely in the tab [Approval](../reservations/README.md#Approval), which is only displayed under certain specific conditions, or by using the buttons to change the booking status.

As in the card **Approval** also the buttons offer 3 different states namely :
- ![](button-approve.png ":no-zoom"), **Approval** Reservations (Reservations approved)
- ![](button-reject.png ":no-zoom"), **Dismiss** Reservations (Bookings rejected)
- ![](button-reset.png ":no-zoom"), **Reset status** Reservations (Reservations pending approval)

### Approval required

A reservation needs to be approved if the reservation object has an approver set and the person creating/editing the reservation is not the approver. The person logged in is NOT the approver if their email does not match the set approver email in the booking object.

In this case, a booking approval request will be sent to the approver's email. This request contains basic information about the booking as well as a direct link to the booking.

### Automatic approval

You do not need to approve a reservation if the reservation object does not have an approver set, or if the reservation object has an approver set and the approver is the person who is currently logged in. The logged-in person IS the approver if their email matches the set approver email in the booking object.

### Making a reservation

When we try to create a reservation, validation will run in the background. If successful, the reservation is saved and its status is set according to the situation:
- if the booking does NOT NEED to be approved, the booking is automatically approved and given the status **The reservation has been approved**.
- if the reservation MUST be approved, it will be saved automatically with the status **Reservation pending approval** and a request for approval is sent to the approver's email. The approver can decide whether to approve, reject or make no change to the booking. However, if the status of the reservation changes, a message is sent to the reservation requestor's email informing them of the status change (this requestor email was automatically pre-set when the reservation was created in the [Personal data](../reservations/README.md#personal-data)).

### Modifying the reservation

When we try to modify the reservation, validation will run in the background. If successful, the reservation is saved and its status is set according to the situation:
- if the booking does NOT NEED to be approved, the modified booking will be saved and automatically approved, giving it the status **The reservation has been approved**.
- if the reservation MUST be approved, its previous status is reset to **Reservation pending approval** (regardless of whether it was approved or rejected) and again a message is sent to the approver's email asking for approval. The approver can again decide whether to approve, reject or not make any changes to the booking. However, if the status of the booking changes, a message is again sent to the booking requestor's email informing them of the change in status.

### Reservation status change

As mentioned above, you can try to change the reservation status by using the card or buttons. In the case of a card, this is handled in such a way that only the person who has the right to do so can see it. In the case of buttons, it is treated so that everyone can see them.

Each time you try to change your reservation, you will see a confirmation of the action.

![](approve_sure.png)

![](reject_sure.png)

![](reset_sure.png)

If you decide to cancel, nothing will happen. If you decide to run the action anyway, our right over this booking will be checked in the background. If it is the case that we do not have the right, an error message will be displayed and the reservation status will not change.

![](approve_no_right.png)

If we have the right to change, it will depend on what state you are trying to set up:

**BOOKING APPROVAL**, no matter what the original state of the reservation is, a validation is triggered in the background which decides what happens to the reservation

- if the validation is successful, the reservation is approved, a confirmation message is displayed and an email is sent to the applicant
- if the validation was unsuccessful, the reservation is automatically rejected, an error message with the reason for rejection is displayed and an email is sent to the applicant

![](approved.png)

**REFUSAL OF RESERVATION**, no matter the original booking status, the booking will simply be rejected, a confirmation message will be returned and an email will be sent to the requester.

![](rejected.png)

**RESETTING THE RESERVATION STATUS**, no matter the original reservation status, the reservation status will simply be reset, a confirmation message will be returned and a message will be sent to the requester.

![](reset.png)

The emails sent to the applicant when the status of a reservation is changed contain basic information about the reservation, its new status and also the name of the approver who changed the status of this reservation.

## Deleting reservations

Deleting reservations depends on the reservation object over which the reservation was created. The reservation object can have a password set that changes the deletion process - the password was set in the tab [Advanced](../reservation-objects/README.md#Advanced).

If the password is NOT set, reservations over this reservation object will be deleted.

If a password IS set, you will be prompted to enter that password.

- If you are deleting multiple reservations over the same object (which has a password), you will only be prompted to enter this password once (you don't have to enter it twice for each reservation separately).
- If you are deleting multiple reservations that are over multiple objects with a password set, you may be prompted for multiple passwords (again, no duplicates).

The password request displayed will inform you for which reservation object you need to enter the password. If you enter multiple passwords and choose not to enter one or more of them (cancel the password entry action), the overall action of deleting the reservation will not be affected. An unentered password is automatically considered bad and therefore will not delete reservations and objects with this password.

![](set_password.png)

Once you have entered all the necessary passwords, you will be prompted to delete your reservations. You can use the displayed window to abort the whole action or to confirm the decision.

![](delete_dialog.png)

If one or more passwords were incorrect, a message will be returned for each reservation that could not be deleted because of this. The message contains information about which reservation object password was not entered correctly and which reservation could not be deleted because of this.

![](delete_error.png)
