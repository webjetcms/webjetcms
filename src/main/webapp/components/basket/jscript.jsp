<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
< 0 )
	  {
	    num = Math.abs( num );
	    isNegative = true;
	  }
	  cents = Math.floor( ( num * 100 + 0.5 ) % 100 );
	  num = Math.floor( ( num * 100 + 0.5 ) / 100 ).toString();
	  if ( cents < 10 )
	  {
	    cents = "0" + cents;
	  }
	  for ( i = 0; i < Math.floor( ( num.length - ( 1 + i ) ) / 3 ); i++)
	  {
	    num = num.substring( 0 ,num.length - ( 4 * i + 3 ) ) + ' ' + num.substring( num.length - ( 4 * i + 3 ) );
	  }

	  var result = num + ',' + cents;
	  if ( isNegative )
	  {
	    result = "-" + result;
	  }
	  return result;
	}
	

function formatCurrency( num )
{
  return formatNumber(num) + " Eur";
}

function fixNumber(number)
{
	if ("" == number) return 0;
	return number.replace(',', '.');
}


function calculatePriceWithVat(price, vat)
{
	price = fixNumber(price);
	vat = fixNumber(vat);
	if (vat > 100 || vat < 0) vat = 80;
	return (vat / 100.0 + 1.0) * price;
}

function calculatePriceWithoutVat(price, vat)
{
	price = fixNumber(price);
	vat = fixNumber(vat);
	if (vat > 100 || vat < 0) vat = 80;
	return price / (vat / 100.0 + 1.0);
}

function calculateWithVat(formName)
{
	var priceWithVat = calculatePriceWithVat(document.forms[formName].fieldK.value, document.forms[formName].fieldL.value);
	document.forms[formName].priceWithVat.value = formatNumber(priceWithVat);
}

function calculateWithoutVat(formName)
{
	var priceWithoutVat = calculatePriceWithoutVat(document.forms[formName].priceWithVat.value, document.forms[formName].fieldL.value);
	document.forms[formName].fieldK.value = formatNumber(priceWithoutVat);
}