package sk.iway.iwcm.components.reservation;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;

/**
 * ReservationManager.java - vykonava pracu s databazou, posiela e-mail,
 * generuje hash pre ReservationAjaxAction.java
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2008
 * @author $Author: jeeff $
 * @version $Revision: 1.9 $
 * @created Date: 20.12.2008 14:55:51
 * @modified $Date: 2010/01/20 08:41:45 $
 */
public class ReservationManager
{
	protected ReservationManager() {
		//utility class
	}

	private static ReservationBean fillReservationBean(ResultSet rs)
	{
		ReservationBean rb = new ReservationBean();
		try
		{
			rb.setReservationId(rs.getInt("reservation_id"));
			rb.setReservationObjectId(rs.getInt("reservation_object_id"));
			long datum = DB.getDbTimestamp(rs, "date_from");
			rb.setDateFrom(datum > 0 ? new Date(datum) : null);
			datum = DB.getDbTimestamp(rs, "date_to");
			rb.setDateTo(datum > 0 ? new Date(datum) : null);
			rb.setName(DB.getDbString(rs, "name"));
			rb.setSurname(DB.getDbString(rs, "surname"));
			rb.setEmail(DB.getDbString(rs, "email"));
			rb.setPurpose(DB.getDbString(rs, "purpose"));
			rb.setAccepted(rs.getBoolean("accepted"));
			rb.setHashValue(DB.getDbString(rs, "hash_value"));
			rb.setPhoneNumber(DB.getDbString(rs, "phone_number"));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return rb;
	}

	private static ReservationObjectBean fillReservationObjectBean(ResultSet rs)
	{
		Prop prop = Prop.getInstance();
		ReservationObjectBean rob = new ReservationObjectBean();
		try
		{
			if (rs.getInt("reservation_object_id") == 0)
			{
				return null;
			}
			rob.setReservationObjectId(rs.getInt("reservation_object_id"));
			rob.setName(rs.getString("name"));
			if (rs.getBoolean("must_accepted"))
				rob.setMustAcceptedString(prop.getText("components.reservation.admin_object_list.yes"));
			else
				rob.setMustAcceptedString(prop.getText("components.reservation.admin_object_list.no"));
			rob.setEmailAccepter(rs.getString("email_accepter"));
			rob.setMaxReservations(rs.getInt("max_reservations"));
			if (rob.getMaxReservations() < 1)
				rob.setMaxReservations(1);
			rob.setCancelTimeBefor(rs.getInt("cancel_time_befor"));
			String timeFrom = DB.getDbString(rs, "reservation_time_from");
			rob.setReservationTimeFrom(Tools.isEmpty(timeFrom) ? "0:00" : timeFrom);
			String timeTo = DB.getDbString(rs, "reservation_time_to");
			rob.setReservationTimeTo(Tools.isEmpty(timeTo) ? "23:59" : timeTo);
			rob.setPriceForDay(rs.getDouble("price_for_day"));
			rob.setPriceForHour(rs.getDouble("price_for_hour"));
			rob.setPriceForDayString(rs.getDouble("price_for_day"));
			rob.setPriceForHourString(rs.getDouble("price_for_hour"));
			rob.setReservationForAllDay(rs.getBoolean("reservation_for_all_day"));
			rob.setPhotoLink(DB.getDbString(rs, "photo_link"));
			rob.setDescription(DB.getDbString(rs, "description"));
			rob.setTimeUnit(DB.getDbString(rs, "time_unit"));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return rob;
	}

	/**
	 * Vrati vsetky rezervacne objekty z tabulky reservation_object
	 *
	 * @return List naplneny rezervacnymi objektami, ktore je mozne si
	 *         rezervovat - z tabulky reservation_object
	 */
	public static List<ReservationObjectBean> getReservationObjects()
	{
		List<ReservationObjectBean> reservationObjects = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM reservation_object WHERE " + CloudToolsForCore.getDomainIdSqlWhere(false)
						+ " ORDER BY name");
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationObjects.add(fillReservationObjectBean(rs));
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservationObjects;
	}

	/**
	 * Vrati vsetky rezervacne objekty z tabulky reservation_object
	 *
	 * @param email
	 *           , podla ktoreho chceme vyfiltrovat objekty
	 *
	 * @return List naplneny rezervacnymi objektami, ktore je mozne si
	 *         rezervovat - z tabulky reservation_object vyfiltrovane podla
	 *         parametra email
	 */
	public static List<ReservationObjectBean> getReservationObjects(String email)
	{
		List<ReservationObjectBean> reservationObjects = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder sql = new StringBuilder("SELECT * FROM reservation_object WHERE reservation_object_id > 0"
					+ CloudToolsForCore.getDomainIdSqlWhere(true));
		try
		{
			db_conn = DBPool.getConnection();
			if (Tools.isNotEmpty(email))
				sql.append(" AND email_accepter = ?");
			sql.append(" ORDER BY name");
			Logger.debug(ReservationManager.class, "sql=" + sql.toString());
			ps = db_conn.prepareStatement(sql.toString());
			if (Tools.isNotEmpty(email))
			{
				int psCounter = 1;
				ps.setString(psCounter++, email);
			}
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationObjects.add(fillReservationObjectBean(rs));
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservationObjects;
	}

	/**
	 * Vrati vsetky rezervacne objekty z tabulky reservation_object - ci su to
	 * celodenne rezervacie
	 *
	 * @param reservationForAllDay
	 *           - ci su to celodenne rezervacie
	 *
	 * @return List
	 */
	public static List<ReservationObjectBean> getReservationObjectsForAllDay(boolean reservationForAllDay)
	{
		List<ReservationObjectBean> reservationObjects = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder sql = new StringBuilder("SELECT * FROM reservation_object WHERE reservation_object_id > 0"
					+ CloudToolsForCore.getDomainIdSqlWhere(true));
		try
		{
			db_conn = DBPool.getConnection();
			sql.append(" AND reservation_for_all_day = ?");
			sql.append(" ORDER BY name");
			Logger.debug(ReservationManager.class, "sql=" + sql.toString());
			ps = db_conn.prepareStatement(sql.toString());
			int psCounter = 1;
			ps.setBoolean(psCounter++, reservationForAllDay);
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationObjects.add(fillReservationObjectBean(rs));
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservationObjects;
	}

	/**
	 * Funkcia, ktora vrati zoznam vsetkych emailovych adries schvalovatelov
	 * rezervacii
	 */
	public static List<String> getAccepterEmails()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<String> emails = new ArrayList<>();
		String sql = "SELECT DISTINCT email_accepter FROM reservation_object WHERE " + CloudToolsForCore.getDomainIdSqlWhere(false);
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next())
			{
				String email = DB.getDbString(rs, "email_accepter");
				if (Tools.isNotEmpty(email))
					emails.add(email);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return (emails);
	}

	/**
	 * Vrati (naplni) rezervacny objekt so zadanym identifikatorom
	 *
	 * @param reservationObjectId
	 *           - identifikator rezervacneho objektu, ktory chceme ziskat
	 * @return bean s properties rezervacneho objektu s identifikatorom
	 *         reservationObjectId
	 */
	public static ReservationObjectBean getReservationObjectById(int reservationObjectId)
	{
		ReservationObjectBean reservationObject = new ReservationObjectBean();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM reservation_object WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, reservationObjectId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationObject = fillReservationObjectBean(rs);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservationObject;
	}

	public static ReservationObjectBean getReservationObjectForAllDayById(int reservationObjectId, boolean reservationForAllDay)
	{
		ReservationObjectBean reservationObject = new ReservationObjectBean();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			StringBuilder sql = new StringBuilder("SELECT * FROM reservation_object WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			if (reservationForAllDay)
			{
				sql.append(" AND reservation_object_id in (SELECT reservation_object_id FROM reservation_object WHERE reservation_for_all_day = ?");
				sql.append(CloudToolsForCore.getDomainIdSqlWhere(true) + ")");
			}
			ps = db_conn.prepareStatement(sql.toString());
			ps.setInt(1, reservationObjectId);
			ps.setBoolean(2, reservationForAllDay);
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationObject = fillReservationObjectBean(rs);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservationObject;
	}

	/**
	 * Vyfiltruje a vrati rezervacie z tabulky reservation podla zadanych
	 * parametrov
	 *
	 * @param filterDateFrom
	 *           od ktoreho datumu sa maju vyselektovat rezervacie
	 * @param filterDateTo
	 *           do ktoreho datumu sa maju vyselektovat rezervacie
	 * @param filterObjectId
	 *           identifikator rezervacneho objektu, podla ktoreho sa maju
	 *           rezervacie vyfiltrovat
	 * @return List naplneny rezervaciami, ktore splnaju podmienky udane
	 *         vstupnymi parametrami
	 */
	public static List<ReservationBean> getReservations(Date filterDateFrom, Date filterDateTo, int filterObjectId)
	{
		List<ReservationBean> reservations = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sqlItemDateTo = "";
		String sqlItemObject = "";
		if (filterDateTo != null)
			sqlItemDateTo = "AND date_to <= ?";
		if (filterObjectId != 0)
			sqlItemObject = "AND reservation_object_id = ?";
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT * FROM reservation WHERE date_to >= ?" + CloudToolsForCore.getDomainIdSqlWhere(true) + sqlItemDateTo + " "
						+ sqlItemObject + " ORDER BY date_from ASC";
			ps = db_conn.prepareStatement(sql);
			int psIndex = 1;
			if (filterDateFrom == null)
			{
				// nastavuje od zaciatku dnesneho dna
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				ps.setTimestamp(psIndex++, new Timestamp(cal.getTimeInMillis()));
			}
			else
				ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestamp(Tools.formatDate(filterDateFrom.getTime()), "00:00:00")));
			if (filterDateTo != null)
				ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestamp(Tools.formatDate(filterDateTo.getTime()), "23:59:59")));
			if (filterObjectId != 0)
				ps.setInt(psIndex++, filterObjectId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				ReservationBean reservation = fillReservationBean(rs);
				reservations.add(reservation);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservations;
	}

	public static List<ReservationBean> getAllReservationsForAllDay(Date filterDateFrom, Date filterDateTo,
				boolean reservationForAllDay)
	{
		List<ReservationBean> reservations = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sqlItemDateTo = "";
		StringBuilder sqlItemObject = new StringBuilder("");
		if (filterDateTo != null)
		{
			sqlItemDateTo = "AND date_to <= ?";
		}
		if (reservationForAllDay)
		{
			sqlItemObject.append("AND reservation_object_id in (SELECT reservation_object_id FROM reservation_object WHERE reservation_for_all_day = ?");
			sqlItemObject.append(CloudToolsForCore.getDomainIdSqlWhere(true) + ")");
		}
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT * FROM reservation WHERE date_to >= ?" + CloudToolsForCore.getDomainIdSqlWhere(true) + sqlItemDateTo + " "
						+ sqlItemObject.toString() + " ORDER BY date_from ASC";
			ps = db_conn.prepareStatement(sql);
			int psIndex = 1;
			if (filterDateFrom == null)
			{
				// nastavuje od zaciatku dnesneho dna
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				ps.setTimestamp(psIndex++, new Timestamp(cal.getTimeInMillis()));
			}
			else
				ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestamp(Tools.formatDate(filterDateFrom.getTime()), "00:00:00")));
			if (filterDateTo != null)
			{
				ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestamp(Tools.formatDate(filterDateTo.getTime()), "23:59:59")));
			}
			if (reservationForAllDay)
			{
				ps.setBoolean(psIndex++, reservationForAllDay);
			}
			rs = ps.executeQuery();
			while (rs.next())
			{
				ReservationBean reservation = fillReservationBean(rs);
				reservations.add(reservation);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservations;
	}

	public static List<ReservationBean> getAllReservationsForAllDayObjectId(Date filterDateFrom, Date filterDateTo,
				boolean reservationForAllDay, String reservationObjectIdString)
	{
		List<ReservationBean> reservations = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sqlItemDateTo = "";
		StringBuilder sqlItemObject = new StringBuilder("");
		String sqlItemObjectId = "";
		if (filterDateTo != null)
		{
			//musi byt date_from aby nam naslo tie co zacinaju pred koncovym datumom
			sqlItemDateTo = "AND date_from <= ?";
		}
		int reservationObjectId = Integer.valueOf(reservationObjectIdString);
		if (reservationForAllDay)
		{
			sqlItemObject.append("AND reservation_object_id in (SELECT reservation_object_id FROM reservation_object WHERE reservation_for_all_day = ?");
			sqlItemObject.append(CloudToolsForCore.getDomainIdSqlWhere(true) + ")");
		}
		if (reservationObjectId != 0)
		{
			sqlItemObjectId = "AND reservation_object_id = ?";
		}
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT * FROM reservation WHERE date_to >= ?" + CloudToolsForCore.getDomainIdSqlWhere(true) + sqlItemDateTo + " "
						+ sqlItemObject.toString() + " " + sqlItemObjectId + " ORDER BY date_from ASC";
			ps = db_conn.prepareStatement(sql);
			int psIndex = 1;
			if (filterDateFrom == null)
			{
				// nastavuje od zaciatku dnesneho dna
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				ps.setTimestamp(psIndex++, new Timestamp(cal.getTimeInMillis()));
			}
			else
				ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestamp(Tools.formatDate(filterDateFrom.getTime()), "00:00:00")));
			if (filterDateTo != null)
			{
				ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestamp(Tools.formatDate(filterDateTo.getTime()), "23:59:59")));
			}
			if (reservationForAllDay)
			{
				ps.setBoolean(psIndex++, reservationForAllDay);
			}
			if (reservationObjectId != 0)
			{
				ps.setInt(psIndex++, reservationObjectId);
			}
			rs = ps.executeQuery();
			while (rs.next())
			{
				ReservationBean reservation = fillReservationBean(rs);
				reservations.add(reservation);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservations;
	}

	/**
	 * Vygeneruje md5 hash zo zadaneho Stringu
	 *
	 * @param code
	 *           - Retazec, ktory chceme zahashovat
	 * @return vrati zakodovany md5 retazec z <b>code</b>
	 */
	private static String generateHash(String code)
	{
		String hashword = null;
		try
		{
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(code.getBytes());
			BigInteger hash = new BigInteger(1, md5.digest());
			hashword = hash.toString(16);
		}
		catch (NoSuchAlgorithmException nsae)
		{
			sk.iway.iwcm.Logger.error(nsae);
		}
		return hashword;
	}

	/**
	 * Ulozi rezervaciu do tabulky reservation
	 *
	 * @param reservation
	 *           - objekt, v ktorom su ulozene jednotlive vlastnosti danej
	 *           rezervacie
	 * @param serverName
	 *           - nazov serveru, aby sme vedeli vyskladat spravny odkaz do mejlu
	 * @param prop
	 *           - properties, ktore sa pouzije na pisanie emailu
	 * @return 0 - ak ulozenie do databazy prebehlo v poriadku, 1 - ak pouzivatel
	 *         nezadal objekt
	 * @throws ParseException
	 */
	public static int addReservation(ReservationBean reservation, String serverName, Prop prop, String lng) throws ParseException
	{
		int returnValue = 0; // uspesne pridanie rezervacie
		String dateFrom = "";
		String dateTo = "";
		if (reservation.getReservationObjectId() == 0)
			return (1); // je potrebne vybrat objekt
		boolean mustReservationAccepted = ReservationManager.mustReservationObjectAccepted(reservation.getReservationObjectId());
		if (mustReservationAccepted)
			reservation.setHashValue(ReservationManager.generateHash(reservation.getName() + reservation.getDateFrom()));
		else
			reservation.setHashValue("");

		//toto sa deje iba pri novej rezervacii, pri existujucej to meni admin
		if(reservation.getReservationId()<1)
		{
			if (!mustReservationAccepted)
				reservation.setAccepted(true);
			else
				reservation.setAccepted(false);
		}
		// ak musi byt rezervacia schvalena, nastav, ze to este nie je schvalene
		// ak nemusi byt schvalena, nastav true, cize uz je to akoze schvalene
		boolean accept = false;
		if (reservation.isAccepted())
			accept = true;
		// formatovanie datumov do stringu kvoli rozdeleniu casu a datumu na dve
		// polozky
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		dateFrom = formatter.format(reservation.getDateFrom());
		dateTo = formatter.format(reservation.getDateTo());
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Date from = sdf.parse(dateFrom + " " + reservation.getStartTime());
		Date to = sdf.parse(dateTo + " " + reservation.getFinishTime());
		Connection db_conn = null;
		PreparedStatement ps = null;

		String sql = "INSERT INTO reservation (reservation_object_id, date_from, date_to, name, surname, email, "
					+ "purpose, accepted, hash_value, phone_number, domain_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		if(reservation.getReservationId()>0)
			sql = "UPDATE reservation SET reservation_object_id=?, date_from=?, date_to=?, name=?, surname=?, email=?, "
						+ "purpose=?, accepted=?, hash_value=?, phone_number=?, domain_id=? WHERE reservation_id=?";

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			int psCounter = 1;
			ps.setInt(psCounter++, reservation.getReservationObjectId());
			ps.setTimestamp(psCounter++, new Timestamp(from.getTime()));
			ps.setTimestamp(psCounter++, new Timestamp(to.getTime()));
			ps.setString(psCounter++, reservation.getName());
			ps.setString(psCounter++, reservation.getSurname());
			ps.setString(psCounter++, reservation.getEmail());
			ps.setString(psCounter++, reservation.getPurpose());
			ps.setBoolean(psCounter++, accept);
			ps.setString(psCounter++, reservation.getHashValue());
			ps.setString(psCounter++, reservation.getPhoneNumber());
			ps.setInt(psCounter++, CloudToolsForCore.getDomainId());
			//ak je to update nastavime co updateujeme
			if(reservation.getReservationId()>0)
				ps.setInt(psCounter++, reservation.getReservationId());
			ps.executeUpdate();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			returnValue = 2;
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		if (mustReservationAccepted && reservation.getReservationId()<1)
			ReservationManager.sendAcceptationEmail(reservation, serverName, prop, lng);
		return returnValue;
	}

	private static String notNull(String s) {
		if (s == null) return "";
		return s;
	}

	/**
	 * Posle akceptacny e-mail na adresu schvalovatela, hned po submitnuti
	 * rezervacie
	 *
	 * @param reservation
	 *           - objekt, v ktorom su ulozene jednotlive vlastnosti danej
	 *           rezervacie
	 * @param serverName
	 *           - nazov serveru, aby sme vedeli vyskladat spravny odkaz do mejlu
	 * @param prop
	 *           - properties, ktore sa pouzije na pisanie emailu
	 */
	private static void sendAcceptationEmail(ReservationBean reservation, String serverName, Prop prop, String lng)
	{
		int reservationId = -1;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn
						.prepareStatement("SELECT MAX(reservation_id) AS reservation_id FROM reservation WHERE reservation_object_id = ? AND"
									+ " name = ? AND surname = ? AND email = ? " + CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservation.getReservationObjectId());
			ps.setString(psCounter++, reservation.getName());
			ps.setString(psCounter++, reservation.getSurname());
			ps.setString(psCounter++, reservation.getEmail());
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationId = rs.getInt("reservation_id");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		String recipientEmail = ReservationManager.getEmailAccepter(reservation.getReservationObjectId());
		String senderName = notNull(reservation.getName()) + " " + notNull(reservation.getSurname());
		String senderEmail = notNull(reservation.getEmail());
		String reservationObject = getReservationObjectName(reservation.getReservationObjectId());
		String dateTimeFrom = Tools.formatDate(reservation.getDateFrom()) + " " + reservation.getStartTime();
		String dateTimeTo = Tools.formatDate(reservation.getDateTo()) + " " + reservation.getFinishTime();
		String subject = prop.getText("components.reservation.mail.title") + senderName + " - " + reservationObject;
		String phoneNumber = notNull(reservation.getPhoneNumber());
		String message = prop.getText("components.reservation.mail.greeting") + ",<br /><br />" + senderName
					+ " (" + prop.getText("user.phone") + " )" + phoneNumber + " "
					+ prop.getText("components.reservation.mail.next") + " <b>" + reservationObject + "</b> "
					+ prop.getText("components.reservation.mail.next2") + dateTimeFrom + " "
					+ prop.getText("components.reservation.mail.next3") + dateTimeTo + " "
					+ prop.getText("components.reservation.mail.next4") + "<br/> " + reservation.getPurpose() + " <br /><br />"
					+ prop.getText("components.reservation.mail.next5") + "<a href=\"/sk/iway/"
					+ "iwcm/components/reservation/ReservationAjax.action?accept&reservation.reservationId=" + reservationId
					+ "&reservation.hashValue=" + reservation.getHashValue() + "&language="+lng+"\">"
					+ prop.getText("components.reservation.mail.accept") + "</a>";
		SendMail.send(senderName, senderEmail, recipientEmail, null, null, null, subject, message, "http://" + serverName, null);
	}

	/** JVY - zatial iba rozpracovane - POKUS
	 * Posle e-mail po schvaleni rezervacie schvalovatelom na adresu hosta, hned po schvaleni
	 * rezervacie
	 *
	 * @param reservation
	 *           - objekt, v ktorom su ulozene jednotlive vlastnosti danej
	 *           rezervacie
	 * @param serverName
	 *           - nazov serveru, aby sme vedeli vyskladat spravny odkaz do mejlu
	 * @param prop
	 *           - properties, ktore sa pouzije na pisanie emailu
	 */
	/*private static void sendConfirmationEmail(ReservationBean reservation, String serverName, Prop prop, String lng)
	{
		int reservationId = -1;
		SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn
						.prepareStatement("SELECT reservation_id FROM reservation WHERE reservation_object_id = ? AND date_from = ? AND date_to = ? AND"
									+ " name = ? AND surname = ? AND email = ? AND purpose = ? AND phone_number = ?" + CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservation.getReservationObjectId());
			ps.setString(psCounter++, (sqlDateFormat.format(reservation.getDateFrom()) + " " + reservation.getStartTime()));
			ps.setString(psCounter++, (sqlDateFormat.format(reservation.getDateTo()) + " " + reservation.getFinishTime()));
			ps.setString(psCounter++, reservation.getName());
			ps.setString(psCounter++, reservation.getSurname());
			ps.setString(psCounter++, reservation.getEmail());
			ps.setString(psCounter++, reservation.getPurpose());
			ps.setString(psCounter++, reservation.getPhoneNumber());
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationId = rs.getInt("reservation_id");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		if (reservationId > 0) {
			String recipientEmail = reservation.getEmail();
			String visitorName = reservation.getName() + " " + reservation.getSurname();
			String senderName = "Penzion Cucoriedka: " + serverName; // JVY - prerobit!!
			String senderEmail = ReservationManager.getEmailAccepter(reservation.getReservationObjectId());
			String reservationObject = getReservationObjectName(reservation.getReservationObjectId());
			String dateTimeFrom = reservation.getStartTime() + " " + Tools.formatDate(reservation.getDateFrom());
			String dateTimeTo = reservation.getFinishTime() + " " + Tools.formatDate(reservation.getDateFrom());
			String subject = prop.getText("components.reservation.mail.title") + senderName + " - " + reservationObject;
			//String phoneNumber = reservation.getPhoneNumber();


					// Dobry den pan/pani {meno hosta},
					// Vasa rezervacia objektu {izba} v {nazov hotela} v datume od {od} do {do} bola potvrdena schvalovatelom.
					// Cena za pobyt je: {cena}
					// Prosim dokoncite svoju objednavku kliknutim <a href="{reservationManager.getLink?}">SEM</a> a naslednym zaplatenim pobytu.
					// Dakujeme! Tesime sa na Vas!
					// {Nazov hotela}
			String message = prop.getText("components.reservation.mail.greeting") + visitorName + ",<br /><br />"
						+ "Vasa rezervacia objektu" + " <b>" + reservationObject + "</b> "
						+ " v datume od: " + dateTimeFrom + " "
						+ " do: " + dateTimeTo + " "
						+ "bola potvrdena schvalovatelom."+"<br>"
						+ "Cena za pobyt je: " + "{cena}" + "<br>"
						+ "Dovod navstevy: " + "<br/> " + reservation.getPurpose() + " <br /><br />"
						+ "Prosim dokoncite svoju objednavku kliknutim " + "<a href='" + "{reservationManager.getLink?}"+"'>"
						+ "SEM" + "</a>" + " a naslednym zaplatenim pobytu."
						+ "V pripade akychkolvek otazok alebo pre zmenu udajov nas prosim kontaktujte na mail:"
						+ senderEmail + "<br><br>"
						+ "Dakujeme a tesime sa na vas!";

			SendMail.send(senderName, senderEmail, recipientEmail, senderEmail, null, null, subject, message, "http://" + serverName, null);
		}
	}*/



	/**
	 * Vrati e-mailovu adresu schvalovatela na zaklade id rezervacneho objektu
	 *
	 * @param reservationObjectId
	 *           - identifikacne cislo rezervacneho objektu
	 * @return e-mail schvalovatela daneho objektu, ak rezervacny objekt
	 *         nepotrebuje schvalenie vrati null
	 */
	private static String getEmailAccepter(int reservationObjectId)
	{
		String emailAccepter = "";
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT email_accepter FROM reservation_object WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationObjectId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				emailAccepter = rs.getString("email_accepter");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return emailAccepter;
	}

	/**
	 * Zisti, ci musi byt rezervacia daneho rezervacneho objektu schvalena
	 *
	 * @param reservationObjectId
	 *           id rezervacneho objektu, u ktoreho zistujeme potrebu schvalenia
	 * @return true ak musi byt schvaleny, false ak nepotrebuje schvalenie
	 */
	public static boolean mustReservationObjectAccepted(int reservationObjectId)
	{
		boolean mustAccepted = true;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT must_accepted FROM reservation_object WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationObjectId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				mustAccepted = rs.getBoolean("must_accepted");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return mustAccepted;
	}

	/**
	 * Vymaze rezervaciu z tabulky reservation
	 *
	 * @param reservationId
	 *           - identifikacne cislo rezervacie, ktoru chceme vymazat
	 * @param passwd
	 *           - heslo na vymazanie rezervacie, zadava sa pri pridavani objektu
	 * @param currUserEmail
	 *           - email prihlaseneho uzivatela
	 * @return 0 ak vymazanie z databazy prebehlo v poriadku, 1 ak pouzivatel
	 *         zadal nespravne heslo, 2 - ina SQL chyba
	 */
	public static int deleteReservation(int reservationId, String passwd, String currUserEmail)
	{
		int returnValue = 0; // rezervacia bola vymazana
		String resEmail = getReservationEmailById(reservationId); // nekontrolujem
																						// heslo ak
																						// som
																						// vytvoril
																						// rezervaciu
		if (!ReservationManager.isDeletedWithoutPass(reservationId) && !resEmail.equals(currUserEmail))
		{
			if (!ReservationManager.getReservationObjectPasswd(ReservationManager.getReservationObjectId(reservationId)).equals(
						ReservationManager.generateHash(passwd))) // overuje heslo
				return 1; // nespravne heslo, chyba
		}
		// skontrolujem ci nieje neskoro uz vymazat rezervaciu
		int hoursBefor = ReservationManager.getReservationObject(ReservationManager.getReservationObjectId(reservationId))
					.getCancelTimeBefor();
		if (hoursBefor > 0)
		{
			Calendar nowBefore = Calendar.getInstance();
			ReservationBean reservation = ReservationManager.getReservationById(reservationId);
			if (reservation == null) return 2;
			nowBefore.setTimeInMillis(reservation.getDateFrom().getTime());
			nowBefore.add(Calendar.HOUR_OF_DAY, -hoursBefor);
			Logger.debug(ReservationManager.class, Tools.formatDateTime(nowBefore.getTimeInMillis()));
			if (Tools.getNow() > nowBefore.getTimeInMillis())
				return 3;
		}
		Connection db_conn = null;
		PreparedStatement ps = null;
		int delRows = 0;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM reservation WHERE reservation_id = ?" + CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationId);
			delRows = ps.executeUpdate();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (SQLException e)
		{
			returnValue = 2;
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		if (delRows != 0)
			return 0; // delete sa podaril
		return returnValue;
	}

	/**
	 * Ulozi alebo zmeni vlastnosti rezervacneho objektu do tabulky
	 * reservation_object
	 *
	 * @param reservationObject
	 *           - objekt, v ktorom su ulozene jednotlive vlastnosti daneho
	 *           rezervacneho objektu
	 * @param reservationObjectId
	 *           - identifikator objektu, ktoreho vlastnosti sa maju zmenit
	 * @return 0 - ak ulozenie do databazy prebehlo v poriadku, 1 - ak pouzivatel
	 *         nezadal email schvalovatela, hoci zadal, ze schvalenie je
	 *         potrebne, 2 - ak nazov objektu uz existuje, 3 - ina SQL chyba, 4 -
	 *         nezhoduju sa hesla
	 */
	public static int addEditReservationObject(ReservationObjectBean reservationObject, int reservationObjectId)
	{
		int returnValue = 0; // vsetko OK, uspesne vlozenie
		String sql = "";
		if (!reservationObject.isChangePass())
		{
			reservationObject.setPasswd(null); // ak prehliadac automaticky vlozi
															// heslo
			reservationObject.setPasswdRepeat(null);
		}
		if (reservationObject.getMaxReservations() < 1)
			reservationObject.setMaxReservations(1);
		if (reservationObject.getCancelTimeBefor() < 1)
			reservationObject.setCancelTimeBefor(0);
		if (reservationObject.getPasswd() != null)
		{
			if (reservationObject.getPasswdRepeat() == null)
				return 4;
			if (!reservationObject.getPasswdRepeat().equals(reservationObject.getPasswd()))
				return 4;
		}
		if (reservationObject.isMustAccepted() && reservationObject.getEmailAccepter() == null)
			return 1; // musi sa zadat email schvalovatela, ak je to potrebne
		int saveRows = 0;
		Connection db_conn = null;
		PreparedStatement ps = null;
		if (reservationObjectId > 0 && reservationObject.isChangePass()) // pri
																								// zmene,
																								// chce
																								// zmenit
																								// aj
																								// heslo
			sql = "UPDATE reservation_object SET name = ?, must_accepted = ?, email_accepter = ?, passwd = ?, max_reservations = ?, cancel_time_befor = ?, reservation_time_from = ?, reservation_time_to = ?, price_for_day = ?, price_for_hour = ?, reservation_for_all_day = ?, photo_link = ?, description = ?, time_unit=?, domain_id = ? WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true);
		if (reservationObjectId > 0 && !reservationObject.isChangePass()) // pri
																								// zmene
																								// nechce
																								// zmenit
																								// heslo
			sql = "UPDATE reservation_object SET name = ?, must_accepted = ?, email_accepter = ?, max_reservations = ?, cancel_time_befor = ?, reservation_time_from = ?, reservation_time_to = ? , price_for_day = ?, price_for_hour = ?, reservation_for_all_day = ?, photo_link = ?, description = ?, time_unit=?, domain_id = ? WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true);
		if (reservationObjectId <= 0)
			sql = "INSERT INTO reservation_object (name, must_accepted, email_accepter, passwd, max_reservations, cancel_time_befor, reservation_time_from, reservation_time_to, price_for_day, price_for_hour, reservation_for_all_day, photo_link, description, time_unit, domain_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try
		{
			Logger.debug(ReservationManager.class, sql);
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			int psCounter = 1;
			ps.setString(psCounter++, reservationObject.getName());
			ps.setBoolean(psCounter++, reservationObject.isMustAccepted());
			ps.setString(psCounter++, (reservationObject.getEmailAccepter() == null) ? "" : reservationObject.getEmailAccepter());
			if (reservationObjectId <= 0)
				ps.setString(
							psCounter++,
							(reservationObject.getPasswd() == null)
										? ""
										: ReservationManager.generateHash(reservationObject.getPasswd()));
			if (reservationObjectId > 0 && reservationObject.isChangePass())
				ps.setString(
							psCounter++,
							(reservationObject.getPasswd() == null)
										? ""
										: ReservationManager.generateHash(reservationObject.getPasswd()));
			ps.setInt(psCounter++, reservationObject.getMaxReservations());
			ps.setInt(psCounter++, reservationObject.getCancelTimeBefor());
			ps.setString(psCounter++, reservationObject.getReservationTimeFrom());
			ps.setString(psCounter++, reservationObject.getReservationTimeTo());
			if (reservationObject.getReservationForAllDay())
			{
				ps.setDouble(psCounter++, reservationObject.getPriceForDay());
				ps.setDouble(psCounter++, 0);// price_for_hour
			}
			else
			{
				ps.setDouble(psCounter++, 0);// price_for_day
				ps.setDouble(psCounter++, reservationObject.getPriceForHour());
			}
			ps.setBoolean(psCounter++, reservationObject.getReservationForAllDay());
			ps.setString(psCounter++, reservationObject.getPhotoLink());
			ps.setString(psCounter++, reservationObject.getDescription());
			ps.setString(psCounter++, reservationObject.getTimeUnit());
			ps.setInt(psCounter++, CloudToolsForCore.getDomainId());
			if (reservationObjectId > 0)
			{
				ps.setInt(psCounter++, reservationObjectId);
			}
			saveRows = ps.executeUpdate();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		/*
		 * catch (MySQLIntegrityConstraintViolationException eUnique) {
		 * returnValue = 2; // rovnake meno }
		 */
		catch (SQLException e)
		{
			returnValue = 3;
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		if (saveRows > 0)
			return (0); // insert sa podaril
		else
			return (returnValue);
	}

	/**
	 * Vymaze rezervacny objekt z tabulky reservation_object
	 *
	 * @param reservationObjectId
	 *           - identifikacne cislo rezervacneho objektu, ktory chceme vymazat
	 * @return 0 ak vymazanie z databazy prebehlo v poriadku, 1 ak je objekt
	 *         rezervovany, 2 - ina SQL chyba
	 */
	public static int deleteReservationObject(int reservationObjectId)
	{
		int returnValue = 0; // uspech
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM reservation_object WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationObjectId);
			ps.executeUpdate();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (SQLException e)
		{
			returnValue = 2; // nepodarilo sa vymazat
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return returnValue;
	}

	/**
	 * Vrati heslo rezervacneho objektu, ktore je potrebne zadat na vymazanie
	 * jeho rezervacie
	 *
	 * @param reservationObjectId
	 *           - identifikacne cislo rezervacneho objektu
	 * @return heslo zahashovane md5 z tabulky reservation_object
	 */
	public static String getReservationObjectPasswd(int reservationObjectId)
	{
		String passwd = "";
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT passwd FROM reservation_object WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationObjectId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				passwd = rs.getString("passwd");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return passwd;
	}

	/**
	 * Vrati identifikator rezervacneho objektu podla identifikatora, na ktory je
	 * vystavena rezervacia
	 *
	 * @param reservationId
	 *           - identifikacne cislo rezervacie
	 * @return identifikator rezervacneho objektu, na ktory je vystavena
	 *         rezervacia
	 */
	public static int getReservationObjectId(int reservationId)
	{
		int reservationObjectId = 0;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT reservation_object_id FROM reservation WHERE reservation_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationObjectId = rs.getInt("reservation_object_id");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservationObjectId;
	}

	/**
	 * Naplni a vrati Bean rezervacny objekt podla jeho identifikatora
	 *
	 * @param reservationObjectId
	 *           - identifikacne cislo rezervacneho objektu
	 * @return naplneny reservationObjectBean
	 */
	public static ReservationObjectBean getReservationObject(int reservationObjectId)
	{
		ReservationObjectBean reservationObject = new ReservationObjectBean();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM reservation_object WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationObjectId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationObject = fillReservationObjectBean(rs);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservationObject;
	}

	/**
	 * Vrati nazov rezervacneho objektu na zaklade jeho id
	 *
	 * @param reservationObjectId
	 *           - identifikacne cislo rezervacneho objektu
	 * @return nazov rezervacneho objektu
	 */
	public static String getReservationObjectName(int reservationObjectId)
	{
		String reservationObjectName = "";
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT name FROM reservation_object WHERE reservation_object_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationObjectId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				reservationObjectName = rs.getString("name");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return reservationObjectName;
	}

	/**
	 * Vrati hash danej rezervacie na zaklade id rezervacneho objektu, kvoli
	 * spekulacnym schvalovaniam
	 *
	 * @param reservationId
	 *           - identifikacne cislo rezervacie
	 * @return unikatnu hashovu hodnotu rezervacie z tabulky reservation
	 */
	public static String getHashValue(int reservationId)
	{
		String hashValue = "";
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT hash_value FROM reservation WHERE reservation_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				hashValue = rs.getString("hash_value");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return hashValue;
	}

	/**
	 * Akceptacia rezervacie odkazom z mejlu
	 *
	 * @param reservationId
	 *           - identifikacne cislo rezervacie, ktoru chceme schvalit
	 * @param hash
	 *           - kod, ktory je potrebne zadat, aby sa predislo spekulativnym
	 *           schvalovaniam
	 * @return true ak sa schvalenie rezervacie podarilo, inak false
	 */
	public static boolean acceptReservation(int reservationId, String hash)
	{
		if (!hash.equals(ReservationManager.getHashValue(reservationId)))
			return false;
		int saveRows = 0;
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE reservation SET accepted = "+DB.getBooleanSql(true)+" WHERE reservation_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			int psCounter = 1;
			ps.setInt(psCounter++, reservationId);
			saveRows = ps.executeUpdate();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		if (saveRows != 0)
			return true; // update sa podaril
		return false;
	}

	/**
	 * Zisti, ci na vymazanie danej rezervacie je potrebne heslo
	 *
	 * @param reservationId
	 *           - identifikacne cislo rezervacie, o ktorej chceme zistit, ci na
	 *           jej vymazanie je potrebne heslo
	 * @return true ak je mozne tuto rezervaciu vymazat bez hesla, inak false
	 */
	public static boolean isDeletedWithoutPass(int reservationId)
	{
		if (Tools.isEmpty(ReservationManager.getReservationObjectPasswd(ReservationManager.getReservationObjectId(reservationId))))
			return true;
		return false;
	}

	/**
	 * Zisti, ci rezervacia nie je v konflikte s inou, uz ulozenou rezervaciou.
	 * Konflikt znamena ze sa rezervacie prekryvaju v case bez ohladu na to ci su
	 * schvalene alebo nie, jedna sa o rezervacie an ten isty objekt samozrejme
	 *
	 * @param reservation
	 * @return
	 * @throws ParseException
	 */
	@SuppressWarnings("rawtypes")
	public static int isConflict(ReservationBean reservation) throws ParseException
	{
		String from = "";
		String to = "";
		Timestamp dateFrom = null;
		Timestamp dateTo = null;
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm");
		from = formatter.format(reservation.getDateFrom());
		to = formatter.format(reservation.getDateTo());
		dateFrom = new Timestamp(sdf.parse(from + " " + reservation.getStartTime()).getTime());
		dateTo = new Timestamp(sdf.parse(to + " " + reservation.getFinishTime()).getTime());
		List conflictReservations = new SimpleQuery()
					.forList("Select * from reservation where "
								+ CloudToolsForCore.getDomainIdSqlWhere(false)
								+ " AND reservation_object_id = ? AND ("
								+ " (date_from > ? AND date_from < ?) OR (date_to > ? AND date_to < ?) OR (date_from = ? AND date_to = ?)"
								+ " OR (? > date_from AND ? < date_to) OR (? > date_from AND ? < date_to) ) AND reservation_id != ?",
								reservation.getReservationObjectId(), dateFrom, dateTo, dateFrom, dateTo, dateFrom, dateTo, dateTo,
								dateTo, dateFrom, dateFrom, reservation.getReservationId());
		return conflictReservations.size();
	}

	/**
	 * vrati email uzivatela, ktory si rezervoval
	 *
	 * @param reservationId
	 * @return
	 */
	public static String getReservationEmailById(int reservationId)
	{
		String result = "";
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT email FROM reservation WHERE reservation_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, reservationId);
			rs = ps.executeQuery();
			if (rs.next())
				result = rs.getString("email");
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return result;
	}

	/**
	 * vrati rezervaciu
	 *
	 * @param reservationId
	 * @return
	 */
	public static ReservationBean getReservationById(int reservationId)
	{
		ReservationBean result = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM reservation WHERE reservation_id = ?"
						+ CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, reservationId);
			rs = ps.executeQuery();
			if (rs.next())
				result = fillReservationBean(rs);
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return result;
	}

	public static int getReservationObjectId(ReservationObjectBean bean)
	{
		int result = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT reservation_object_id FROM reservation_object WHERE name=? AND description=? ORDER BY reservation_object_id DESC");
			ps.setString(1, bean.getName());
			ps.setString(2, bean.getDescription());
			rs = ps.executeQuery();
			if (rs.next())
			{
				result = rs.getInt("reservation_object_id");
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return result;
	}

	public static void sendApprovalStatusChangedEmail(ReservationBean reservation, boolean approved, HttpServletRequest request)
	{
		if(reservation==null || reservation.getReservationId()<0 || Tools.isEmpty(reservation.getEmail()))
			return;
		ReservationObjectBean reservationObject = ReservationManager.getReservationObjectById(reservation.getReservationObjectId());
		if(reservationObject==null || Tools.isEmpty(reservationObject.getName()))
			return;

		Prop prop = Prop.getInstance(PageLng.getUserLng(request));

		String emailUrl = "/components/reservation/email_reservation.jsp";
		String canceledString = "";
		if(approved==false)
			canceledString = "&cancel=true";

		//data emailu
		String downloadUrl = Tools.getBaseHrefLoopback(request) + emailUrl + "?reservationId=" + reservation.getReservationId() + "&hash="+reservation.getHashValue() + canceledString;
		String data = Tools.downloadUrl(downloadUrl);

		String senderName = reservationObject.getName();
		String fromEmail = "no-reply@"+Tools.getBaseHref(request).replace("https://", "").replace("http://", "").replace("www.", "");
		String toEmail = reservation.getEmail();
		String subject = prop.getText("components.reservation.approved");
		if(approved==false)
			subject = prop.getText("components.reservation.canceled");

		if (Tools.isNotEmpty(data) && toEmail.indexOf('@')!=-1)
		{
			SendMail.send(senderName, fromEmail, toEmail, subject, data, request);
		}
	}
}
