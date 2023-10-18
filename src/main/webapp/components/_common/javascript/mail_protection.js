//stara verzia, nove je v page_functions.js.jsp
function decodeEmailImpl(encodeEmail)
{
	var email = encodeEmail;
	if (email.indexOf("~") > -1)
		email = email.replace(/~/gi, '@');
	if(email.indexOf("!") > -1)
		email = email.replace(/!/gi, '.');
	
	// cele to pre istotu otoc
	var newEmail = new Array(email.length);
	for (i = 0; i < email.length; i++)
	{
		newEmail[i] = email.charAt(email.length - i - 1);
	}
	email = newEmail.join("");
	return email;
}
function decodeEmail(encodeEmail)
{	
	window.location.href="mailto:"+decodeEmailImpl(encodeEmail);
}
function writeEmailToPage(encodeEmail)
{
	document.write(decodeEmailImpl(encodeEmail));
}