package sk.iway.iwcm.components.reservation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;

/**
 *  ReservationObjectTimesDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2016
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 20.1.2016 15:32:56
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ReservationObjectTimesDB extends JpaDB<ReservationObjectTimesBean>
{
	private static ReservationObjectTimesDB instance = new ReservationObjectTimesDB();

	public ReservationObjectTimesDB()
	{
		super(ReservationObjectTimesBean.class);
	}

	public static ReservationObjectTimesDB getInstance()
	{
		return instance;
	}

	public List<ReservationObjectTimesBean> getByReservationObjectId(int id)
	{
		List<ReservationObjectTimesBean> result = super.findBy(filterEquals("object_id", id),filterEquals("domain_id", CloudToolsForCore.getDomainId()));
		Collections.sort(result, new Comparator<ReservationObjectTimesBean>(){
		   @Override
		   public int compare(final ReservationObjectTimesBean lhs,ReservationObjectTimesBean rhs) {
		   	if(lhs.getDen()<rhs.getDen())
		   		return -1;
		   	else
		   		return 1;
		     }
		 });

		return result;
	}
}
