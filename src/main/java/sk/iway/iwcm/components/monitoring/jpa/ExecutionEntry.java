package sk.iway.iwcm.components.monitoring.jpa;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class ExecutionEntry implements Comparable<ExecutionEntry>, Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	@DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "")
	String whatWasExecuted;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.monitoring.component_executions")
	private Long numberOfHits = Long.valueOf(0);

	@DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.monitoring.component_cache_hits")
	private Long numberOfCacheHits = Long.valueOf(0);

	@DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.monitoring.component_execution_time")
	private Long averageExecutionTime = Long.valueOf(0);

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.monitoring.component_slowest_one")
	private Long maximumExecutionTime = Long.valueOf(0);

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.monitoring.component_fastest_one")
	private Long minimumExecutionTime = Long.MAX_VALUE;

	@DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.monitoring.component_total_time")
	private Long totalTimeOfExecutions = Long.valueOf(0);

	@DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.monitoring.average_memory_consumed")
	private Long totalMemoryConsumed = Long.valueOf(0);

	@DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.monitoring.consumption_peek")
	private Long memoryConsumptionPeek = Long.valueOf(0);

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "")
	private Long totalTimeOfCacheExecutions = Long.valueOf(0);

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "")
	private Long validMemoryMeasurements = Long.valueOf(0);

	public ExecutionEntry(){}

	public ExecutionEntry(String name) {
		this.whatWasExecuted = name;
	}

	//----------------------------GETTERS AND SETTERS-----------------------------

	public String getName()
	{
		return this.whatWasExecuted;
	}

	public long getNumberOfExecutions()
	{
		return this.numberOfHits;
	}

	public long getTotalTimeSpentOnServingThisComponent()
	{
		return totalTimeOfCacheExecutions + totalTimeOfExecutions;
	}

	public long getAverageExecutionTime()
	{
		if (this.numberOfHits == 0)
			return 0;
		return this.totalTimeOfExecutions / this.numberOfHits;
	}

	public long getAverageCacheExecutionTime()
	{
		if (this.numberOfCacheHits == 0)
			return 0;
		return this.totalTimeOfCacheExecutions / this.numberOfCacheHits;
	}


	public long getAverageMemoryConsumption()
	{
		if (validMemoryMeasurements == 0)
			return 0;
		return totalMemoryConsumed / validMemoryMeasurements;
	}


	@Override
	public int compareTo(ExecutionEntry anotherEntry)
	{
		return -Long.valueOf(getAverageExecutionTime()).
			compareTo(Long.valueOf(anotherEntry.getAverageExecutionTime()));
	}

	@Override
	public boolean equals(Object o)
   {
   	if (!(o instanceof ExecutionEntry)) return false;
   	return ((ExecutionEntry)o).getName().equals(this.getName()) && compareTo((ExecutionEntry)o)==0;
   }

	@Override
	public int hashCode(){
		return getName().hashCode() + Long.valueOf(getAverageExecutionTime()).hashCode();
	}

	@Override
	public String toString()
	{
		return whatWasExecuted + " took " + getAverageExecutionTime() + " ms on average";
	}
}
