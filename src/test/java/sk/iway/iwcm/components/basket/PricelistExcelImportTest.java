package sk.iway.iwcm.components.basket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.DB;

class PricelistExcelImportTest
{
	@Test
	void fieldPropertyNameIsMappedToDatabaseColumn()
	{
		assertEquals("field_k", PricelistExcelImport.formatFieldName("fieldK"));
		assertEquals("field_k", PricelistExcelImport.formatFieldName("FIELDK"));
		assertEquals("field_e", PricelistExcelImport.formatFieldName("fieldLongName"));
		assertEquals("custom_price", PricelistExcelImport.formatFieldName("custom_price"));
	}

	@Test
	void sqlSyntaxIsNotSilentlyRemovedDuringFormatting()
	{
		String formattedFieldName = PricelistExcelImport.formatFieldName("fieldK DESC");

		assertEquals("fieldK DESC", formattedFieldName);
		assertFalse(DB.isValidSqlIdentifier(formattedFieldName, false));
	}
}
