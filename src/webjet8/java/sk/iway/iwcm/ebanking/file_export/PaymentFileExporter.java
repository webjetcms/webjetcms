package sk.iway.iwcm.ebanking.file_export;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmOutputStream;

/**
 *  PaymentFileExporter.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 25.8.2009 16:06:00
 *@modified     $Date: 2010/01/20 11:14:26 $
 */
public abstract class PaymentFileExporter
{
	private BigDecimal maximumCap = new BigDecimal(Long.MAX_VALUE);

	private List<Payment> payments;

	private IwcmFile exportFile;

	private StringBuilder content;

	private Payment sourceAccount;

	public IwcmFile createExportFile(List<Payment> payments) throws InsufficientFundsException
	{
		this.payments = new ArrayList<Payment>(payments);
		throwExceptionIfCapIsReached();
		exportFile = createEmptyFileForExport();
		createFileContent();
		writeContentToExportFile();
		return exportFile;
	}

	private void throwExceptionIfCapIsReached() throws InsufficientFundsException
	{
		BigDecimal counter = BigDecimal.ZERO;
		for (Payment payment : payments)
			counter = counter.add(payment.getAmount());

		if (counter.compareTo(maximumCap) > 0)
			throw new InsufficientFundsException();
	}

	private IwcmFile createEmptyFileForExport()
	{
		String exceptionMessage = null;
		try
		{
			String filePath = createFilePath();
			String directory = filePath.substring(0, filePath.lastIndexOf('/') + 1);
			String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
			IwcmFile file = new IwcmFile(directory, fileName);
			file.mkdirs();
			file.createNewFile();
			return file;
		}
		catch (IOException e)
		{
			sk.iway.iwcm.Logger.error(e);
			exceptionMessage = e.getMessage();
		}
		throw new IllegalStateException(exceptionMessage);
	}

	private void createFileContent()
	{
		content = new StringBuilder();

		content.append(createHeader());
		for (Payment payment : this.payments)
			content.append(createRowFrom(payment));
		content.append(createFooter());
	}

	protected abstract String createRowFrom(Payment payment);

	protected String createHeader()
	{
		return "";
	}

	protected String createFooter()
	{
		return "";
	}

	private void writeContentToExportFile()
	{
		String exceptionMessage = null;
		IwcmOutputStream output = null;
		try
		{
			output = new IwcmOutputStream(exportFile);
			output.write(content.toString().getBytes());
			output.close();
			output = null;
		}
		catch (IOException e) {
			exceptionMessage = e.getMessage();
			try
			{
				if (output != null)
					output.close();
			}
			catch (IOException e1){}
		}
		if (exceptionMessage != null)
			throw new IllegalStateException(exceptionMessage);
	}

	protected String createFilePath()
	{
		return new StringBuilder(Tools.getRealPath("/files/protected/")).
			append('/').
			append(getExporterIdentifierForFileName()).
			append("_on_").
			append(new SimpleDateFormat("yyyy-MM-dd").format(new Date())).
			append('.').
			append(getFileExtension()).toString();
	}


	protected abstract String getFileExtension();

	protected abstract String getExporterIdentifierForFileName();

	public void setMaximumCap(BigDecimal maximumCap)
	{
		this.maximumCap = maximumCap;
	}

	public void setSourceAccount(Payment sourceAccount)
	{
		this.sourceAccount = sourceAccount;
	}

	protected Payment getSourceAccount()
	{
		return sourceAccount;
	}
}