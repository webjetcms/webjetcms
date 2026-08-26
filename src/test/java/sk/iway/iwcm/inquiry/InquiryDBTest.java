package sk.iway.iwcm.inquiry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InquiryDBTest
{
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"answer_id DESC", "(SELECT password FROM users)", "ia.answer_id--"})
	void resolveOrderByUsesDefaultForInvalidValues(String orderBy)
	{
		assertEquals("ia.answer_id", InquiryDB.resolveOrderBy(orderBy));
	}

	@Test
	void resolveOrderByPrefixesUnqualifiedColumn()
	{
		assertEquals("ia.answer_clicks", InquiryDB.resolveOrderBy("answer_clicks"));
	}

	@Test
	void resolveOrderByPreservesQualifiedColumn()
	{
		assertEquals("i.question_id", InquiryDB.resolveOrderBy("i.question_id"));
	}
}
