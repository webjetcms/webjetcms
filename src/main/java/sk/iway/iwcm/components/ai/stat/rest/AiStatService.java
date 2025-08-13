package sk.iway.iwcm.components.ai.stat.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.stat.dto.DaysUsageDTO;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;
import sk.iway.iwcm.utils.Pair;

@Service
public class AiStatService {

    public static final void addRecord(String assiatntName, Integer usedTokens, AiStatRepository statRepo) {
        if(statRepo == null) throw new IllegalStateException("AiStatRepository is not provided");
        if(Tools.isEmpty(assiatntName) == true) throw new IllegalStateException("Assitant name is nor provided");

        AiStatEntity newStatRecord = new AiStatEntity();
        newStatRecord.setAssistantName(assiatntName);
        newStatRecord.setUsedTokens(usedTokens);
        newStatRecord.setCreated(new Date());
        newStatRecord.setDomainId(CloudToolsForCore.getDomainId());
        statRepo.save(newStatRecord);
    }

    public static final List<LabelValueInteger> getPieChartDataMostUsed(Date[] dateRangeArr, AiStatRepository statRepo) {
        if(statRepo == null) throw new IllegalStateException("AiStatRepository is not provided");
        List<AiStatEntity> entities = statRepo.findAllByCreatedBetweenAndDomainId(dateRangeArr[0], dateRangeArr[1], CloudToolsForCore.getDomainId());

        Map<String, Integer> mappedValues = new HashMap<>();
        entities.stream()
            .map(AiStatEntity::getAssistantName)
            .forEach(name -> mappedValues.put(name, mappedValues.getOrDefault(name, 0) + 1));

        return mapToList(mappedValues);
    }

    public static final List<LabelValueInteger> getPieChartDataMostTokens(Date[] dateRangeArr, AiStatRepository statRepo) {
        if(statRepo == null) throw new IllegalStateException("AiStatRepository is not provided");
        List<AiStatEntity> entities = statRepo.findAllByCreatedBetweenAndDomainId(dateRangeArr[0], dateRangeArr[1], CloudToolsForCore.getDomainId());

        Map<String, Integer> mappedValues = new HashMap<>();
        entities.stream()
            .forEach(entity -> mappedValues.merge(
                entity.getAssistantName(),
                entity.getUsedTokens() != null ? entity.getUsedTokens() : 0,
                Integer::sum
            ));

        return mapToList(mappedValues);
    }

    private static final List<LabelValueInteger> mapToList(Map<String, Integer> mappedValues) {
        List<LabelValueInteger> data = new ArrayList<>();
        mappedValues.forEach((key, value) -> {
            data.add(new LabelValueInteger(key, value));
        });
        return data;
    }

    public static final Map<String, List<DaysUsageDTO>> getLineChartData(Date[] dateRangeArr, AiStatRepository statRepo) {
        if(statRepo == null) throw new IllegalStateException("AiStatRepository is not provided");
        List<AiStatEntity> entities = statRepo.findAllByCreatedBetweenAndDomainId(dateRangeArr[0], dateRangeArr[1], CloudToolsForCore.getDomainId());

        Date from = DateTools.setTimePart(dateRangeArr[0], 0, 0, 0, 0);
        Date to = DateTools.setTimePart(dateRangeArr[1], 0, 0, 0, 0);

        Map<String, Pair<Integer, Integer>> dateCombinations = new HashMap<>();
        Set<String> assitantNames = new HashSet<>();
        for(AiStatEntity entity : entities) {
            Date date = DateTools.setTimePart(entity.getCreated(), 0, 0, 0, 0);
            String id = entity.getAssistantName() + "_" + date.getTime();

            //Save assistant name
            assitantNames.add(entity.getAssistantName());

            Pair<Integer, Integer> value = dateCombinations.get(id);
            if(value == null) dateCombinations.put(id, new Pair<>(1, entity.getUsedTokens()));
            else dateCombinations.put(id, new Pair<>(value.getFirst() + 1, value.getSecond() + entity.getUsedTokens()));
        }

        //
        Map<String, List<DaysUsageDTO>> data = new HashMap<>();
        for(String assitantName : assitantNames) {
            //Prepare list
            List<DaysUsageDTO> dayData = new ArrayList<>();

            for(Date date = from; !date.after(to); date = DateTools.addDays(date, 1)) {
                Pair<Integer, Integer> dayValue = dateCombinations.get(assitantName + "_" + date.getTime());
                if(dayValue == null) dayData.add(new DaysUsageDTO(date, 0, 0));
                else dayData.add(new DaysUsageDTO(date, dayValue.getFirst(), dayValue.getSecond()));
            }

            data.put(assitantName, dayData);
        }

        return data;
    }
}
