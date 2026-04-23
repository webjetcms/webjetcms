package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sourceforge.stripes.mock.MockServletContext;
import sk.iway.iwcm.database.SimpleQuery;

/**
 *  PKeyGeneratorJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.7.2010 16:30:30
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PKeyGeneratorJUnit
{
	private static final String TEST_TABLE = "_test";


	@BeforeEach
	public void setup()
	{
		Constants.setServletContext(new MockServletContext(""));
		Constants.setInt("pkeyGenOffset", 0);
		new SimpleQuery().execute("CREATE TABLE _test (_test_id BIGINT PRIMARY KEY);");
		new SimpleQuery().execute("INSERT INTO _test VALUES(?) ", Integer.MAX_VALUE - 2);
		//value now = MAX_VALUE - 1
		PkeyGenerator.getNextValue(TEST_TABLE);
	}

	@AfterEach
	public void tearDown()
	{
		new SimpleQuery().execute("DELETE FROM pkey_generator WHERE table_name = ?", TEST_TABLE);
		new SimpleQuery().execute("DROP TABLE _test");
	}

	@Test
	public void getInt()
	{
		int value = PkeyGenerator.getNextValue(TEST_TABLE);
		assertTrue(value == Integer.MAX_VALUE);
	}

	@Test
	public void getLong()
	{
		long value = PkeyGenerator.getNextValueAsLong(TEST_TABLE);
		//value now == MAX_VALUE
		value = PkeyGenerator.getNextValueAsLong(TEST_TABLE);
		//value now = MAX_VALUE + 1
		assertTrue(value == (long)(Integer.MAX_VALUE) + 1);
	}

	/**
	 * Test concurrent allocate calls - simulates multiple Galera nodes calling allocate simultaneously.
	 * Each thread gets its own PkeyBean pointing to the same DB row.
	 * Verifies all allocated ranges are non-overlapping.
	 */
	@Test
	public void testConcurrentAllocate() throws Exception
	{
		String concurrentTable = "_test_conc";

		int threadCount = 5;

		//create test table and pkey_generator entry
		new SimpleQuery().execute("CREATE TABLE _test_conc (_test_conc_id BIGINT PRIMARY KEY)");
		new SimpleQuery().execute("INSERT INTO pkey_generator (name, value, table_name, table_pkey_name) VALUES (?, 100, ?, ?)",
			concurrentTable, concurrentTable, concurrentTable + "_id");

		try
		{
			int pkeyGenIncrement = Constants.getInt("pkeyGenIncrement");
			int pkeyGenBlockSize = Constants.getInt("pkeyGenBlockSize");
			long blockIncrement = (long)pkeyGenBlockSize * pkeyGenIncrement;

			PkeyGenerator pkGen = PkeyGenerator.getInstance(true);

			//create separate PkeyBeans for each thread - simulating separate app server instances
			PkeyBean[] beans = new PkeyBean[threadCount];
			for (int i = 0; i < threadCount; i++)
			{
				beans[i] = new PkeyBean();
				beans[i].setName(concurrentTable);
				beans[i].setTableName(concurrentTable);
				beans[i].setTablePkeyName(concurrentTable + "_id");
				beans[i].setValue(0);
				//set maxValue > 0 to skip the initial getMaxValue lookup from data table
				beans[i].setMaxValue(1);
			}

			CountDownLatch startLatch = new CountDownLatch(1);
			List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
			long[][] ranges = new long[threadCount][2];

			ExecutorService executor = Executors.newFixedThreadPool(threadCount);
			try
			{
				for (int t = 0; t < threadCount; t++)
				{
					final int idx = t;
					executor.submit(() -> {
						try
						{
							//all threads wait here until released simultaneously
							startLatch.await();
							pkGen.allocate(beans[idx]);
							ranges[idx][0] = beans[idx].getValue();
							ranges[idx][1] = beans[idx].getMaxValue();
						}
						catch (Exception e)
						{
							errors.add(e);
						}
					});
				}

				//release all threads at once
				startLatch.countDown();
				executor.shutdown();
				if (executor.awaitTermination(30, TimeUnit.SECONDS) == false)
				{
					executor.shutdownNow();
					throw new AssertionError("Threads did not finish within 30 seconds, interrupted hanging workers");
				}
			}
			catch (AssertionError ae) { throw ae; }
			catch (Exception e) { throw new AssertionError("Unexpected exception during concurrent execution", e); }
			finally { executor.shutdownNow(); }
			assertTrue(errors.isEmpty(), "No exceptions expected, but got: " + errors);

			//verify each thread got a valid range
			for (int i = 0; i < threadCount; i++)
			{
				assertTrue(ranges[i][1] > 0, "Thread " + i + " maxValue should be > 0, got: " + ranges[i][1]);
				assertTrue(ranges[i][0] > 0, "Thread " + i + " value should be > 0, got: " + ranges[i][0]);
				assertEquals(blockIncrement, ranges[i][1] - ranges[i][0],
					"Thread " + i + " range size should equal blockIncrement (" + blockIncrement + ")");
			}

			//verify no two threads got overlapping ranges
			for (int i = 0; i < threadCount; i++)
			{
				for (int j = i + 1; j < threadCount; j++)
				{
					boolean noOverlap = ranges[i][1] <= ranges[j][0] || ranges[j][1] <= ranges[i][0];
					assertTrue(noOverlap,
						"Ranges must not overlap: thread " + i + " [" + ranges[i][0] + "," + ranges[i][1] + ") " +
						"vs thread " + j + " [" + ranges[j][0] + "," + ranges[j][1] + ")");
				}
			}

			//verify all maxValues are unique - each thread must have allocated a distinct block
			Set<Long> maxValues = new HashSet<>();
			for (int i = 0; i < threadCount; i++)
			{
				assertTrue(maxValues.add(ranges[i][1]),
					"Duplicate maxValue detected: " + ranges[i][1] + " (thread " + i + ")");
			}

			//verify DB value was incremented correctly: initial(100) + 5 * blockIncrement
			long expectedDbValue = 100 + (long)threadCount * blockIncrement;
			long actualDbValue = new SimpleQuery().forLong("SELECT value FROM pkey_generator WHERE name=?", concurrentTable);
			assertEquals(expectedDbValue, actualDbValue,
				"DB value should reflect all allocations: expected " + expectedDbValue + " but got " + actualDbValue);

			Logger.debug(PkeyGenerator.class, "Concurrent allocate test passed, ranges:");
			for (int i = 0; i < threadCount; i++)
			{
				Logger.debug(PkeyGenerator.class, "  Thread " + i + ": [" + ranges[i][0] + ", " + ranges[i][1] + ")");
			}
		}
		finally
		{
			new SimpleQuery().execute("DELETE FROM pkey_generator WHERE name=?", concurrentTable);
			new SimpleQuery().execute("DROP TABLE _test_conc");
		}
	}

	/**
	 * Test concurrent getNextValueAsLong calls - verifies no duplicate values
	 * are ever returned even under heavy concurrent load.
	 */
	@Test
	public void testConcurrentGetNextValue() throws Exception
	{
		String concurrentTable = "_test_conc2";
		int threadCount = 5;
		int valuesPerThread = 100;

		new SimpleQuery().execute("CREATE TABLE _test_conc2 (_test_conc2_id BIGINT PRIMARY KEY)");

		try
		{
			//trigger initial key creation
			PkeyGenerator.getNextValue(concurrentTable);

			CountDownLatch startLatch = new CountDownLatch(1);
			List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
			List<Long> allValues = Collections.synchronizedList(new ArrayList<>());

			ExecutorService executor = Executors.newFixedThreadPool(threadCount);
			try
			{
				for (int t = 0; t < threadCount; t++)
				{
					executor.submit(() -> {
						try
						{
							startLatch.await();
							for (int i = 0; i < valuesPerThread; i++)
							{
								long val = PkeyGenerator.getNextValueAsLong(concurrentTable);
								allValues.add(val);
							}
						}
						catch (Exception e)
						{
							errors.add(e);
						}
					});
				}

				startLatch.countDown();
				executor.shutdown();
				if (executor.awaitTermination(60, TimeUnit.SECONDS) == false)
				{
					executor.shutdownNow();
					throw new AssertionError("Threads did not finish within 60 seconds, interrupted hanging workers");
				}
			}
			catch (AssertionError ae) { throw ae; }
			catch (Exception e) { throw new AssertionError("Unexpected exception during concurrent execution", e); }
			finally { executor.shutdownNow(); }
			assertTrue(errors.isEmpty(), "No exceptions expected, but got: " + errors);

			int expectedTotal = threadCount * valuesPerThread;
			assertEquals(expectedTotal, allValues.size(),
				"Should have " + expectedTotal + " values total");

			//verify all values are unique - the critical check
			Set<Long> uniqueValues = new HashSet<>(allValues);
			assertEquals(expectedTotal, uniqueValues.size(),
				"All " + expectedTotal + " values must be unique, but only " + uniqueValues.size() + " were unique");

			//verify values are all positive
			for (Long val : allValues)
			{
				assertTrue(val > 0, "All values must be positive, got: " + val);
			}

			//verify values are monotonically arranged (sorted should be contiguous with increment step)
			List<Long> sorted = new ArrayList<>(allValues);
			Collections.sort(sorted);
			int pkeyGenIncrement = Constants.getInt("pkeyGenIncrement");
			for (int i = 1; i < sorted.size(); i++)
			{
				long diff = sorted.get(i) - sorted.get(i - 1);
				assertEquals(pkeyGenIncrement, diff,
					"Values should increase by pkeyGenIncrement (" + pkeyGenIncrement + "), " +
					"but diff between " + sorted.get(i-1) + " and " + sorted.get(i) + " is " + diff);
			}
		}
		finally
		{
			new SimpleQuery().execute("DELETE FROM pkey_generator WHERE name=?", concurrentTable);
			new SimpleQuery().execute("DROP TABLE _test_conc2");
		}
	}
}