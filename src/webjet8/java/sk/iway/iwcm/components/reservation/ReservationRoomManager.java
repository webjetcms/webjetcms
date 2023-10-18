package sk.iway.iwcm.components.reservation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 * ReservationRoomManager.java
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2014
 * @author $Author: jeeff mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 6.11.2014 18:20:23
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class ReservationRoomManager
{
	private int posun = 2;

	/**
	 * Zoznam obsadenosti: datum rezervacie, obsadenost/max obsadenost (
	 * occupancy/max reservation), objekt rezervacie, pozicia dna a pozicia
	 * objektu rezervacie
	 *
	 * @param data
	 * @param datesBetween
	 * @param reservationObjectList
	 * @return
	 * @throws ParseException
	 */
	public String[][] getArrayFromHashPovodne(Map<String, Integer> data, List<Date> datesBetween,
				List<ReservationObjectBean> reservationObjectList) throws ParseException
	{
		String[][] str = null;
		Object[] keys = data.keySet().toArray();
		Object[] values = data.values().toArray();
		str = new String[keys.length][values.length];
		for (int i = 0; i < keys.length; i++)
		{
			String key = (String) keys[i];
			String[] keyArray = key.split("-");
			// reservationDatum-reservationObjectId-maxReservations
			String datumReservation = keyArray[0];
			String reservationObjectId = keyArray[1];
			String maxReservations = keyArray[2];
			Integer occupancy = (Integer) values[i];
			DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
			String datumReservationBezMena = datumReservation.substring(datumReservation.indexOf(".") + 1);
			Date datumReservationDate = dateFormat.parse(datumReservationBezMena);
			int dayPosition = getDatePosition(datesBetween, datumReservationDate);
			int roomPosition = getRoomPosition(reservationObjectList, Integer.valueOf(reservationObjectId));
			str[i][0] = datumReservation;
			str[i][1] = occupancy + "/" + maxReservations;
			str[i][2] = reservationObjectId;
			str[i][3] = String.valueOf(dayPosition);
			str[i][4] = String.valueOf(roomPosition);
		}
		return str;
	}

	/**
	 * Zoznam obsadenosti: datum rezervacie, obsadenost/max obsadenost (
	 * occupancy/max reservation), objekt rezervacie, pozicia dna, pozicia
	 * objektu rezervacie, pozicia mena objektu rezervacie
	 *
	 * @param data
	 * @return
	 * @throws ParseException
	 */
	public String[][] getOccupancyArrayFromHash(Map<String, Integer> data, List<Date> datesBetween,
				List<ReservationObjectBean> reservationObjectList) throws ParseException
	{
		Object[] keys = data.keySet().toArray();
		Object[] values = data.values().toArray();
		int numberOfRow = reservationObjectList.size() + reservationObjectList.size() + 1;
		if (1 == numberOfRow)
		{
			numberOfRow = 0;
		}
		int dates = datesBetween.size();
		String[][] str = new String[numberOfRow][dates];
		for (int k = 1, j = 0; k < numberOfRow; k += 2, j++)
		{
			int room = getMaxReservationPosition(reservationObjectList, j);
			ReservationObjectBean reservationObject = reservationObjectList.get(j);
			String name = reservationObject.getName();
			for (int i = 0; i < datesBetween.size(); i++)
			{
				str[k - 1][0] = name;
				str[k][i] = "0/" + room;
			}
		}
		for (int i = 0; i < keys.length; i++)
		{
			String key = (String) keys[i];
			String[] keyArray = key.split("-");
			// datumReservation-reservationObjectId-maxReservations
			String datumReservation = keyArray[0];
			String reservationObjectId = keyArray[1];
			String maxReservations = keyArray[2];
			Integer occupancy = (Integer) values[i];
			DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
			String datumReservationBezMena = datumReservation.substring(datumReservation.indexOf(".") + 1);
			Date datumReservationDate = dateFormat.parse(datumReservationBezMena);
			int dayPosition = getDatePosition(datesBetween, datumReservationDate);
			int roomPosition = getRoomPosition(reservationObjectList, Integer.valueOf(reservationObjectId));
			str[roomPosition][dayPosition] = occupancy + "/" + maxReservations;
		}
		return str;
	}

	/**
	 * Pozicia datumu v tabulke rezervacii
	 *
	 * @param datesBetween
	 * @param datumReservation
	 * @return
	 */
	private int getDatePosition(List<Date> datesBetween, Date datumReservation)
	{
		int ret = 0;
		for (int i = 0; i < datesBetween.size(); i++)
		{
			Date actualDate = datesBetween.get(i);
			if (actualDate.equals(datumReservation))
			{
				ret = i;
				break;
			}
		}
		return ret;
	}

	/**
	 * Pozicia izby ( jej obsadenosti ) v tabulke rezervacii
	 *
	 * @param reservationObjectList
	 * @param reservationObjectId
	 * @return
	 */
	public int getRoomPosition(List<ReservationObjectBean> reservationObjectList, int reservationObjectId)
	{
		int ret = 0;
		for (int i = 0, k = 1; i < reservationObjectList.size(); i++, k += 2)
		{
			ReservationObjectBean reservationObject = reservationObjectList.get(i);
			int resObjId = reservationObject.getReservationObjectId();
			if (reservationObjectId == resObjId)
			{
				ret = k;
				break;
			}
		}
		return ret;
	}

	/**
	 * Pozicia max reservation v tabulke rezervacii
	 *
	 * @param reservationObjectList
	 * @param reservationObjectId
	 * @return
	 */
	public int getMaxReservationPosition(List<ReservationObjectBean> reservationObjectList, int reservationObjectOrder)
	{
		int ret = 0;
		for (int i = 0; i < reservationObjectList.size(); i++)
		{
			if (reservationObjectOrder == i)
			{
				ReservationObjectBean reservation = reservationObjectList.get(i);
				ret = reservation.getMaxReservations();
				break;
			}
		}
		return ret;
	}

	/**
	 * Naplni zoznam dni do tabulky
	 *
	 * @param data
	 * @return
	 */
	public String[][] getDaysArrayFromList(List<String> data)
	{
		String[][] str = null;
		Object[] values = data.toArray();
		str = new String[values.length][7];
		for (int i = 0; i < values.length; i++)
		{
			String value = (String) values[i];
			String[] keyArray = value.split("\\.");
			// nazovDna.den.mesiac.rok
			String denNazov = keyArray[0];
			String den = keyArray[1];
			String mesiac = keyArray[2];
			String rok = keyArray[3];
			String mesiacNazovIn = keyArray[4];
			str[i][0] = denNazov;
			str[i][1] = den;
			str[i][2] = mesiac;
			str[i][3] = rok;
			str[i][4] = mesiacNazovIn;
			str[i][5] = value;
			str[i][6] = "" + i;
		}
		return str;
	}

	/**
	 * List obsadenosti pre vsetky dni medzi datumami Zatial iba pre jeden
	 * rezervacny objekt
	 *
	 * @param occupancyMap
	 * @param datumBetweenStringListForDay
	 * @return
	 */
	public List<String> getListOccupancyWithDatum(Map<String, Integer> occupancyMap,
				List<String> datumBetweenStringListForDay, String rezobjid)
	{
		List<String> retOccupancyList = new ArrayList<>();
		// iba obsadene dni
		List<String> occupancyList = getListOccupancyForOccupedDays(occupancyMap);
		String emptyOccupancy = "";
		String del = "-";
		ReservationObjectBean resObj = ReservationManager.getReservationObject(Integer.valueOf(rezobjid));
		int maxReservations = resObj.getMaxReservations();
		String occupancyRatioEmpty = "0/" + maxReservations;
		emptyOccupancy = rezobjid + del + occupancyRatioEmpty;
		// najskor kazdy den z rozsahu naplnim '0/maxReservation'
		for (int i = 0; i < datumBetweenStringListForDay.size(); i++)
		{
			String day = datumBetweenStringListForDay.get(i);
			retOccupancyList.add(i, day + del + emptyOccupancy);
		}
		// neskor ich prechadzam a ak nejaky den ma nenulovu obsadenost prepisem
		// ho
		for (int k = 0; k < datumBetweenStringListForDay.size(); k++)
		{
			String dayFull = datumBetweenStringListForDay.get(k);
			String[] dayArray = dayFull.split("-");
			String day = dayArray[0];
			for (int m = 0; m < occupancyList.size(); m++)
			{
				String occupancy2 = occupancyList.get(m);
				String[] occupancySplits2 = occupancy2.split(del);
				String occupancyDay2 = occupancySplits2[0];
				String occupancyObjectId = occupancySplits2[1];
				String occupancy = occupancySplits2[2];
				if (day.equals(occupancyDay2))
				{
					String ocup = dayFull + del + occupancyObjectId + del + occupancy;
					retOccupancyList.set(k, ocup);
				}
			}
		}
		return retOccupancyList;
	}

	/**
	 * List obsadenosti, iba pre dni, ked su izby obsadene aspon jednym
	 * zakaznikom
	 *
	 * <pre>
	 * Format:
	 * datumReservation-reservationObjectId-occupancy/maxReservations
	 *
	 * Št.06.11.2014.okt-42-Studio-1/4
	 * Št.17.11.2014.okt-42-Studio-3/4
	 * </pre>
	 *
	 * @param occupancyMap
	 * @return
	 */
	private List<String> getListOccupancyForOccupedDays(Map<String, Integer> occupancyMap)
	{
		Iterator<String> keySetIterator = occupancyMap.keySet().iterator();
		List<String> retList = new ArrayList<>();
		while (keySetIterator.hasNext())
		{
			String keyPovod = keySetIterator.next();
			String value = String.valueOf(occupancyMap.get(keyPovod));
			String maxReservations = keyPovod.substring(keyPovod.lastIndexOf("-") + 1);
			String key = keyPovod.substring(0, keyPovod.lastIndexOf("-") + 1);
			key = key + value;
			retList.add(key + "/" + maxReservations);
		}
		return retList;
	}

	public String[][] getArrayOccupancyWithDatum(Map<String, Integer> occupancyMap, List<String> datumBetweenStringListForDay,
				String rezobjid)
	{
		List<String> occupancyWithDatum = getListOccupancyWithDatum(occupancyMap, datumBetweenStringListForDay, rezobjid);
		// datumReservation-positionColumn/positionRow-reservationObjectId-occupancy/maxReservations
		String[][] occupancy = null;
		Object[] values = occupancyWithDatum.toArray();
		occupancy = new String[12][8];
		// skratene nazvy dni v tyzdni
		for (int i = 0; i < values.length; i++)
		{
			// Po.03.11.2014.nov-1/1-43-0/4
			// datum
			String value = (String) values[i];
			String[] keyArray = value.split("-");
			String datumFull = keyArray[0];
			String[] datumArray = datumFull.split("\\.");
			String den = datumArray[1];

			// position
			String position = keyArray[1];
			String[] positionArray = position.split("\\/");
			String positionColumn = positionArray[0];
			int column = Integer.parseInt(positionColumn);
			String positionRow = positionArray[1];

			int row = Integer.valueOf(positionRow) + posun;
			// occupancy
			String occup = keyArray[3];
			int percentageOccupancy = getOccupancyPercentage(occup);
			String occupancyTitle = getOccupancyTitle(occup);
			String out = den + "-" + occupancyTitle + "-" + percentageOccupancy + "-" + datumArray[1]+"."+datumArray[2]+"."+datumArray[3];
			Logger.debug(ReservationRoomManager.class, "" + row + "/" + column + ":" + out);
			occupancy[row][column] = out;
		}

		//odstran prazdne riadky
		List<String[]> occupancyList = new ArrayList<>();
		for (int i=0; i<occupancy.length; i++)
		{
			boolean isNotEmpty = false;
			//System.out.print(i+": ");
			for (int j=0; j<occupancy[i].length; j++)
			{
				//System.out.print(occupancy[i][j]+";");
				if (Tools.isNotEmpty(occupancy[i][j])) isNotEmpty = true;
			}
			//System.out.println("");

			if (isNotEmpty)
			{
				occupancyList.add(occupancy[i]);
			}
		}

		occupancy = new String[occupancyList.size()][8];
		for (int i=0; i<occupancyList.size(); i++)
		{
			for (int j=0; j<8; j++)
			{
				String[] row = occupancyList.get(i);
				occupancy[i][j] = row[j];
			}
		}

		return occupancy;
	}

	private int getOccupancyPercentage(String ratioOccupancy)
	{
		int ret = 100;
		String[] ratioArray = ratioOccupancy.split("\\/");
		String occupiedRooms = ratioArray[0];
		String numberAllRooms = ratioArray[1];
		double occupiedRoomsInt = Double.parseDouble(occupiedRooms);
		double numberAllRoomsInt = Double.parseDouble(numberAllRooms);
		double ratio = occupiedRoomsInt / numberAllRoomsInt;
		int percentage = (int) (ratio * 30d);
		int obsadene = percentage;
		// vsade ratam obsadene izby a zobrazujem volne
		ret = 30 - obsadene;
		return ret;
	}

	private String getOccupancyTitle(String ratioOccupancy)
	{
		String ret = "";
		String[] ratioArray = ratioOccupancy.split("\\/");
		String occupiedRooms = ratioArray[0];
		String numberAllRooms = ratioArray[1];
		int occupiedRoomsInt = Integer.parseInt(occupiedRooms);
		int numberAllRoomsInt = Integer.parseInt(numberAllRooms);
		// vsade ratam obsadene izby a zobrazujem volne
		int freeRooms = numberAllRoomsInt - occupiedRoomsInt;
		ret = freeRooms + " z " + numberAllRooms;
		return ret;
	}
}
