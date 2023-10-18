package sk.iway.iwcm.components.basket;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

/**
 *  BasketInvoicePaymentDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff blade $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.4.2010 16:10:51
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BasketInvoicePaymentDB
{
	/**
	 * naplni BasketInvoicePayment
	 * @param rs
	 * @return
	 */
	private static BasketInvoicePayment fillBasketInvoicePayment(ResultSet rs)
	{
		BasketInvoicePayment result = new BasketInvoicePayment();
		try
		{
			result.setPaymentId(rs.getInt("payment_id"));
			result.setInvoiceId(rs.getInt("invoice_id"));
			result.setCreateDate(new Date(DB.getDbTimestamp(rs, "create_date")));
			result.setPayedPrice(rs.getBigDecimal("payed_price"));
			result.setPaymentMethod(DB.getDbString(rs, "payment_method"));
			long closedDate = DB.getDbTimestamp(rs, "closed_date");
			if(closedDate == 0)
				result.setClosedDate(null);
			else
				result.setClosedDate(new Date(closedDate));
			result.setConfirmed((Boolean)rs.getObject("confirmed"));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return result;
	}

	/**
	 * vracia vyslednu sumu zaplatenu ciastkovymi platbami na zaklade invoiceId
	 * @param invoiceId
	 * @return
	 */
	public static BigDecimal getPaymentsSum(int invoiceId)
	{
		BigDecimal result = null;
		try
		{
			List<?> prices = DB.queryForList("SELECT payed_price FROM basket_invoice_payments WHERE invoice_id = ? AND closed_date IS NOT NULL AND confirmed=?", Integer.valueOf(invoiceId), Boolean.TRUE);
			if(prices != null)
			{
				result = BigDecimal.ZERO;
				for(Object price : prices)
				{
					if(price instanceof Number && price != null)
						result = result.add((BigDecimal)price);
				}
			}
		}
		catch (Exception e)
		{
			result = BigDecimal.ZERO;
			sk.iway.iwcm.Logger.error(e);
		}

		return result;
	}


	/**
	 * ziska zaznam na zaklade payment_id z DB
	 * @param paymentId
	 * @return
	 */
	public static BasketInvoicePayment getBasketInvoicePaymentById(int paymentId)
	{
		BasketInvoicePayment result = null;
		try
		{
			Connection db_conn = DBPool.getConnection();
			try
			{
				PreparedStatement ps = db_conn.prepareStatement("SELECT * FROM basket_invoice_payments WHERE payment_id = ?");
				try
				{
					ps.setInt(1, paymentId);
					ResultSet rs = ps.executeQuery();
					try
					{
						if (rs.next())
						{
							result = fillBasketInvoicePayment(rs);
						}
					}
					finally { rs.close(); }
				}
				finally { ps.close(); }
			}
			finally { db_conn.close(); }
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return result;
	}

	/**
	 * vlozi, aktualizuje zaznam BasketInvoicePayment, pri aktualizacii aktualizuje len closedDate a confirmed (pouziva sa pri sparovani platieb)
	 * @param payment
	 * @return
	 */
	public static BasketInvoicePayment insertUpdateBasketInvoicePayment(BasketInvoicePayment payment)
	{
		BasketInvoicePayment invoicePayment = new BasketInvoicePayment();
		try
		{
			if(payment != null && payment.getPaymentId() > 0)
			{
				DB.execute("UPDATE basket_invoice_payments SET closed_date=?, confirmed=? WHERE payment_id=?",
							   new Timestamp(payment.getClosedDate().getTime()), payment.getConfirmed(), Integer.valueOf(payment.getPaymentId()));

				invoicePayment = getBasketInvoicePaymentById(payment.getPaymentId());

				Logger.println(BasketInvoicePayment.class, "Aktualizujem platbu > id= "+invoicePayment.getPaymentId()+"; invoiceId= "+invoicePayment.getInvoiceId()+"; price= "+invoicePayment.getPayedPrice()+"; method= "+invoicePayment.getPaymentMethod()+"; closedDate= "+payment.getClosedDate()+"; confirmed: "+payment.getConfirmed());
			}
			else if(payment != null)
			{
				//skontrolujem ci platba nepresahuje celkovu platbu
				BasketInvoiceBean invoice = InvoiceDB.getInvoiceById(payment.getInvoiceId());

				BigDecimal totalPriceVat = new BigDecimal(invoice.getTotalPriceVat());
				totalPriceVat = totalPriceVat.setScale(2, RoundingMode.HALF_UP);

				BigDecimal payedPrice = payment.getPayedPrice().add(getPaymentsSum(payment.getInvoiceId()));
				payedPrice = payedPrice.setScale(2, RoundingMode.HALF_DOWN);

				//ak bola zaplatena suma minus total cena rozdielna o viac ako 1 (pri zaokruhlovani moze byt taky rozdiel) tak to preskocme, asi enjaka duplicita platby
				if(payedPrice.subtract(totalPriceVat).doubleValue() > 1)
					return null;
				//---

				DB.execute("INSERT INTO basket_invoice_payments(invoice_id, create_date, payed_price, payment_method, closed_date, confirmed) VALUES (?,?,?,?,?,?)",
							   Integer.valueOf(payment.getInvoiceId()), new Timestamp(payment.getCreateDate().getTime()), payment.getPayedPrice(), payment.getPaymentMethod(), (payment.getClosedDate() == null ? null : new Timestamp(payment.getClosedDate().getTime())), payment.getConfirmed());

				int paymentId = DB.queryForInt("SELECT max(payment_id) AS id FROM basket_invoice_payments WHERE invoice_id=? AND payment_method=?", Integer.valueOf(payment.getInvoiceId()), payment.getPaymentMethod());

				invoicePayment = getBasketInvoicePaymentById(paymentId);

				Logger.println(BasketInvoicePayment.class, "Ukladam platbu > id= "+invoicePayment.getPaymentId()+"; invoiceId= "+invoicePayment.getInvoiceId()+"; price= "+invoicePayment.getPayedPrice()+"; method= "+invoicePayment.getPaymentMethod());
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return invoicePayment;
	}

	/**
	 * vymaze zaznam
	 * @param paymentId
	 * @return
	 */
	public static boolean deleteBasketInvoicePayment(int paymentId)
	{
		boolean ok = true;
		try
		{
			DB.execute("DELETE FROM basket_invoice_payments WHERE payment_id = ?", Integer.valueOf(paymentId));
			Logger.println(BasketInvoicePayment.class, "Mazem platbu > id= " + paymentId);
		}
		catch (Exception e)
		{
			ok = false;
			sk.iway.iwcm.Logger.error(e);
		}

		return ok;
	}

	/**
	 * ziska zaznamy pre objednavku (invoiceId)
	 * @param invoiceId
	 * @param typ -> true/false - vrati uspesne/neuspesne platby, null - vrati vsetky
	 * @return
	 */
	public static List<BasketInvoicePayment> getBasketInvoicePaymentByInvoiceId(int invoiceId, Boolean typ)
	{
		List<BasketInvoicePayment> result = new ArrayList<BasketInvoicePayment>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			String typSql = "";
			if(typ != null)
				typSql = " AND confirmed = ?";
			ps = db_conn.prepareStatement("SELECT * FROM basket_invoice_payments WHERE invoice_id = ?"+typSql);
			ps.setInt(1, Integer.valueOf(invoiceId));
			if(typ != null)
				ps.setBoolean(2, typ);
			rs = ps.executeQuery();
			while (rs.next())
				result.add(fillBasketInvoicePayment(rs));
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

		return result;
	}
}
