package sk.iway.iwcm.ebanking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *  MoneyJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.11.2010 13:51:09
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MoneyJUnit
{
	@Test
	public void shouldReturnFormattedFloat()
	{
		Money oneAndHalf = Money.fromDouble(1.50, Currency.getInstance("EUR"));
		assertThat(oneAndHalf.getDouble(), is(1.50));
		assertThat(oneAndHalf.getAmount(), is(150L));
	}

	@Test
	public void shouldAcceptCents()
	{
		Money oneAndHalf = Money.fromEuroCents(150);
		assertThat(oneAndHalf.getDouble(), is(1.50));
	}

	@Test
	public void shouldBeAbleToAddTwoMoney()
	{
		Money twoEuro = Money.fromEuroCents(200);
		Money threeEuro = Money.fromEuroCents(300);
		assertThat(twoEuro.plus(threeEuro), is(Money.fromEuroCents(500)));
	}

	@Test
	public void shouldBeAbleToSubtractTwoMoney()
	{
		Money twoEuro = Money.fromEuroCents(200);
		Money threeEuro = Money.fromEuroCents(300);
		assertThat(threeEuro.minus(twoEuro), is(Money.fromEuroCents(100)));
	}

	@Test
	public void cantAddTwoDifferentCurrencies()
	{
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			Money.fromEuroCents(100).plus(Money.fromCents(100, Currency.getInstance("USD")));
		});
	}

	@Test
	public void shouldRoundValues()
	{
		Money twoAndABit = Money.fromEuroCents(153);
		RoundingStrategy toFiveCentsLower = new RoundingStrategy(){
			@Override
			public Money round(Money original){
				long originalAmount = original.getAmount();
				long rounded = (originalAmount / 5) * 5;
				return Money.fromCents(rounded, original.getCurrency());
			}
		};
		Money rounded = twoAndABit.round(toFiveCentsLower);
		assertThat(rounded.getDouble(), is(1.5));
	}

	@Test
	public void shouldSplitCorrectly()
	{
		Money oneEuro = Money.fromEuroCents(100);
		List<Money> split = oneEuro.splitIntoParts(3);
		Money together = split.get(0).plus(split.get(1)).plus(split.get(2));
		assertThat(together, is(oneEuro));
	}

	@Test
	public void shouldSplitNegativeValuesAsWell()
	{
		Money debtOneEuro = Money.fromEuroCents(-100);
		List<Money> split = debtOneEuro.splitIntoParts(3);
		Money together = split.get(0).plus(split.get(1)).plus(split.get(2));
		assertThat(together, is(debtOneEuro));
	}

	@Test
	public void shouldSplitZeroAmounts()
	{
		Money veryLittle = Money.fromEuroCents(1);
		List<Money> split = veryLittle.splitIntoParts(3);
		Money together = split.get(0).plus(split.get(1)).plus(split.get(2));
		assertThat(together, is(veryLittle));
	}

	@Test
	public void shouldRound()
	{
		Money finnishEuroAndTwoCents = Money.fromEuroCents(102);
		Money rounded = finnishEuroAndTwoCents.round(RoundingStrategies.toFiveCentsUpwards());
		assertThat(rounded, is(Money.fromEuroCents(105)));

		Money czechKorunaAndSomething = Money.fromCents(156, Currency.getInstance("CZK"));
		Money roundedCzech = czechKorunaAndSomething.round(RoundingStrategies.downwardsTo(100));
		assertThat(roundedCzech, is(Money.fromCents(100, Currency.getInstance("CZK"))));
	}

	/**
	 * Following code converted a wrong value
	 * <code>
	 * 	Double asDouble = Double.valueOf("35.05");
	 * 	long asLong = Double.valueOf(asDouble*100.0).longValue(); //3504
	 * </code>
	 *
	 */
	@Test
	public void shouldRoundBeforeConverting()
	{
		Double asDouble = Double.valueOf("35.05");
		Money converted = Money.fromEuroDouble(asDouble);
		assertThat(converted, is(Money.fromEuroCents(3505)));
	}
}