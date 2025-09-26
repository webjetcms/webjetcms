package sk.iway.iwcm.components.ai.stat.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.stat.dto.DaysUsageDTO;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.SpecSearch;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.utils.Pair;

/**
 * Service for AI statistics - handles data processing for stats section/graphs
 */
@Service
public class AiStatService {

    private static final int PIE_CHART_TOP_N = 5;

    public static final void addRecord(Long assistantId, Integer usedTokens, AiStatRepository statRepo, HttpServletRequest request) {
        if(statRepo == null) throw new IllegalStateException("AiStatRepository is not provided");
        if(assistantId == null) throw new IllegalStateException("Assitant is not specified");

        AiStatEntity newStatRecord = new AiStatEntity();
        newStatRecord.setAssistantId(assistantId);
        newStatRecord.setUsedTokens(usedTokens);
        newStatRecord.setCreated(new Date());

        Identity currentUser = UsersDB.getCurrentUser(request);
        newStatRecord.setUserId(currentUser != null ? currentUser.getUserId() : -1L);
        newStatRecord.setDomainId(CloudToolsForCore.getDomainId());
        statRepo.save(newStatRecord);
    }

    public static final List<LabelValueInteger> getPieChartDataMostUsed(String created, String provider, String action, String groupName, AiStatRepository statRepo, AssistantDefinitionRepository assistantsRepo, Prop prop) {
        if(statRepo == null) throw new IllegalStateException("AiStatRepository is not provided");
        List<AiStatEntity> entities = statRepo.findAll( getSpecification(action, provider, groupName, StatService.processDateRangeString(created), null, null) );

        Map<Long, Integer> mappedValues = new HashMap<>();
        entities.stream()
            .map(AiStatEntity::getAssistantId)
            .forEach(assistantId -> mappedValues.put(assistantId, mappedValues.getOrDefault(assistantId, 0) + 1));


        return doShit(mappedValues, assistantsRepo, prop);
    }

    public static final List<LabelValueInteger> getPieChartDataMostTokens(String created, String provider, String action, String groupName, AiStatRepository statRepo, AssistantDefinitionRepository assistantsRepo, Prop prop) {
        if(statRepo == null) throw new IllegalStateException("AiStatRepository is not provided");
        List<AiStatEntity> entities = statRepo.findAll( getSpecification(action, provider, groupName, StatService.processDateRangeString(created), null, null) );

        Map<Long, Integer> mappedValues = new HashMap<>();
        entities.stream()
            .forEach(entity -> mappedValues.merge(
                entity.getAssistantId(),
                entity.getUsedTokens() != null ? entity.getUsedTokens() : 0,
                Integer::sum
            ));

        return doShit(mappedValues, assistantsRepo, prop);
    }

    public static final Map<String, List<DaysUsageDTO>> getLineChartData(String created, String provider, String action, String groupName, AiStatRepository statRepo, AssistantDefinitionRepository assistantsRepo) {
        if(statRepo == null) throw new IllegalStateException("AiStatRepository is not provided");
        Date[] dateRangeArr = StatService.processDateRangeString(created);
        List<AiStatEntity> entities = statRepo.findAll( getSpecification(action, provider, groupName, dateRangeArr, null, null) );

        Date from = DateTools.setTimePart(dateRangeArr[0], 0, 0, 0, 0);
        Date to = DateTools.setTimePart(dateRangeArr[1], 0, 0, 0, 0);

        Map<Long, String> assistantsNames = getAssistantsNames(assistantsRepo);

        Map<String, Pair<Integer, Integer>> dateCombinations = new HashMap<>();
        Set<String> assitantNames = new HashSet<>();
        for(AiStatEntity entity : entities) {
            String assistantName = assistantsNames.getOrDefault(entity.getAssistantId(), "KOKOS");

            Date date = DateTools.setTimePart(entity.getCreated(), 0, 0, 0, 0);
            String id = assistantName + "_" + date.getTime();

            //Save assistant name
            assitantNames.add(assistantName);

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

    private static final List<LabelValueInteger> doShit(Map<Long, Integer> mappedValues, AssistantDefinitionRepository assistantsRepo, Prop prop) {
        // Sort entries by value descending
        List<Map.Entry<Long, Integer>> sortedEntries = new ArrayList<>(mappedValues.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        // Prepare new map for top 5 + "Others"
        Map<Long, Integer> topMap = new LinkedHashMap<>();
        int count = 0;
        int othersSum = 0;

        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            if (count < PIE_CHART_TOP_N) {
                topMap.put(entry.getKey(), entry.getValue());
            } else {
                othersSum += entry.getValue();
            }
            count++;
        }

        // Use key -1L for "Others"
        if (othersSum > 0) {
            topMap.put(-1L, othersSum);
        }

        // Now topMap contains top 5 and "Others" as 6th value
        return mapToList(topMap, assistantsRepo, prop);
    }

    private static final List<LabelValueInteger> mapToList(Map<Long, Integer> mappedValues, AssistantDefinitionRepository assistantsRepo, Prop prop) {
        Map<Long, String> assistantsNames = getAssistantsNames(assistantsRepo);

        List<LabelValueInteger> data = new ArrayList<>();
        mappedValues.forEach((key, value) -> {
            data.add(new LabelValueInteger(assistantsNames.getOrDefault(key, prop.getText("components.stat.other")), value));
        });
        return data;
    }

    private static final Map<Long, String> getAssistantsNames(AssistantDefinitionRepository assistantsRepo) {
        Map<Long, String> assistantsNames = new TreeMap<>();
        for(AssistantDefinitionEntity assistant : AiAssistantsService.getAssistantsFromDB(assistantsRepo)) {
            assistantsNames.put(assistant.getId(), assistant.getName() + " (" + assistant.getProvider() + ")");
        }

        return assistantsNames
        ;
    }

    public static Specification<AiStatEntity> getSpecification(Map<String, String> params, Pageable pageable) {
        String action = Tools.isEmpty(params.get("searchAssistantAction")) == true ? null : params.get("searchAssistantAction");
        String provider = Tools.isEmpty(params.get("searchAssistantProvider")) == true ? null : params.get("searchAssistantProvider");
        String groupName = Tools.isEmpty(params.get("searchAssistantGroupName")) == true ? null : params.get("searchAssistantGroupName");
        Date[] dateCreated = StatService.processDateRangeString( params.get("searchCreated") );
        String userName = Tools.isEmpty(params.get("searchUserName")) == true ? null : params.get("searchUserName");


        return getSpecification(action, provider, groupName, dateCreated, userName, pageable);
    }

    private static Specification<AiStatEntity> getSpecification(String action, String provider, String groupName, Date[] dateCreated, String userName, Pageable pageable) {
        return (root, query, cb) -> {
            var sub = query.subquery(Long.class);
            var ade = sub.from(AssistantDefinitionEntity.class);

            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(ade.get("id"), root.get("assistantId")));

            if (Tools.isEmpty(action) == false) {
                predicates.add(cb.equal(ade.get("action"), action));
            }

            if (Tools.isEmpty(provider) == false) {
                predicates.add(cb.equal(ade.get("provider"), provider));
            }

            if (Tools.isEmpty(groupName) == false) {
                predicates.add(cb.equal(ade.get("groupName"), groupName));
            }

            //Date created is ALLWAYS set at least to default 30 days
            if (dateCreated != null && dateCreated.length == 2 && dateCreated[0] != null && dateCreated[1] != null) {
                // Filter by AiStatEntity.created (root), not AssistantDefinitionEntity.created
                predicates.add(cb.between(root.get("created"), dateCreated[0], dateCreated[1]));
            }

            //Serach by user name
            if (Tools.isEmpty(userName) == false) {
                SpecSearch<AiStatEntity> specSearch = new SpecSearch<>();
                specSearch.addSpecSearchUserFullName(userName, "userId", predicates, root, cb);
            }

            sub.select(ade.get("id")).where(predicates.toArray(new Predicate[0]));

            //Apply sorting from pageable
            if (pageable != null && pageable.getSort() != null && pageable.getSort().isSorted()) {
                List<javax.persistence.criteria.Order> orders = new java.util.ArrayList<>();
                pageable.getSort().forEach(o -> {
                    try {
                        if (o.isAscending()) {
                            orders.add(cb.asc(root.get(o.getProperty())));
                        } else {
                            orders.add(cb.desc(root.get(o.getProperty())));
                        }
                    } catch (IllegalArgumentException ex) {
                        //Ignore invalid sort property
                    }
                });
                if (orders.isEmpty() == false) {
                    query.orderBy(orders);
                }
            }

            return cb.exists(sub);
        };
    }

    public static List<AiStatEntity> fillStatEntities(List<AiStatEntity> entities, AssistantDefinitionRepository repo) {
        if(repo == null) throw new IllegalStateException("AssistantDefinitionRepository is not provided");

        //Get assistants from cache/db and put into map
        Map<Long, AssistantDefinitionEntity> assistantsMap = new HashMap<>();
        for(AssistantDefinitionEntity assistant : AiAssistantsService.getAssistantsFromDB(repo)) {
            assistantsMap.put(assistant.getId(), assistant);
        }

        Map<Integer, String> cachedUsersNames = new TreeMap<>();

        for(AiStatEntity entity : entities) {
            AssistantDefinitionEntity assistant = assistantsMap.getOrDefault(entity.getAssistantId(), null);
            if(assistant == null) {
                entity.setAssistantAction("-");
                entity.setAssistantGroupName("-");
                entity.setAssistantName("-");
                entity.setAssistantProvider("-");
                entity.setUserName("-");
            } else {
                entity.setAssistantAction( assistant.getAction() );
                entity.setAssistantGroupName( assistant.getGroupName() );
                entity.setAssistantName( assistant.getName() );
                entity.setAssistantProvider( assistant.getProvider() );

                int userId = (entity.getUserId() == null) ? -1 : entity.getUserId().intValue();
                String userName = cachedUsersNames.get(userId);
                if(userName == null) {
                    UserDetails user = UsersDB.getUserCached(userId);
                    userName = (user == null) ? "UNKNOWN" : user.getFullName();
                    cachedUsersNames.put(userId, userName);
                }

                entity.setUserName(userName);
            }
        }

        return entities;
    }
}
