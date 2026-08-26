package sk.iway.iwcm.components.file_archiv;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class FileArchivatorDBTest
{
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "virtual_file_name DESC", "name,uploaded", "COUNT(*)", "fa.virtual_file_name"})
	void getDistinctListByPropertyRejectsInvalidSqlIdentifiers(String column)
	{
		assertTrue(FileArchivatorDB.getDistinctListByProperty(column).isEmpty());
	}
}
