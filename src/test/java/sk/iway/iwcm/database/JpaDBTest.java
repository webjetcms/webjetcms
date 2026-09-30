package sk.iway.iwcm.database;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;

class JpaDBTest
{
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "virtualFileName DESC", "COUNT(id)", "id,uploaded"})
	void getValuesRejectsInvalidPropertyBeforeCreatingQuery(String property)
	{
		JpaDB<FileArchivatorBean> database = new JpaDB<>(FileArchivatorBean.class);

		assertThrows(IllegalArgumentException.class, () -> database.getValues(property));
	}
}
