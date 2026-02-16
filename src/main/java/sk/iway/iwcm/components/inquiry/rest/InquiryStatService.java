package sk.iway.iwcm.components.inquiry.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.inquiry.jpa.InquiryAnswerEntity;
import sk.iway.iwcm.components.inquiry.jpa.InquiryAnswerRepository;
import sk.iway.iwcm.components.inquiry.jpa.InquiryUsersVoteEntity;
import sk.iway.iwcm.components.inquiry.jpa.InquiryUsersVoteRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;

public class InquiryStatService {

    private InquiryStatService() {}

    public static final String QUESTION_ID = "questionId";
    public static final String DAY_DATE = "dayDate";
    public static final String USER_ID = "userId";
    public static final String ANSWER_ID = "answerId";
    private static final String ANSWER_TEXT = "answerText";

    @Getter
    @Setter
    public static class LineChartData {
        Integer value;
        Date dayDate;
        public LineChartData(Integer value, Date dayDate) {
            this.value = value;
            this.dayDate = dayDate;
        }
        public void incrementValue() { this.value++; }
    }

    public static void getTableData(Map<String, String> params, List<Predicate> predicates, Root<InquiryUsersVoteEntity> root, CriteriaBuilder builder) {
        long questionId = -1L;
        long userId = -3L;
        long answerId = -1L;
        String stringRange = null;
        boolean isNestedTab = false;

        //Get params from map
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if(entry.getKey().equalsIgnoreCase(QUESTION_ID)) {
                questionId = Tools.getLongValue(entry.getValue(), -1L);
            } else if(entry.getKey().equalsIgnoreCase(DAY_DATE)) {
                stringRange = entry.getValue();
            } else if(entry.getKey().equalsIgnoreCase(USER_ID)) {
                userId = Tools.getLongValue(entry.getValue(), -3L);
            } else if(entry.getKey().equalsIgnoreCase(ANSWER_ID)) {
                answerId = Tools.getLongValue(entry.getValue(), -1L);
            } else if(entry.getKey().equalsIgnoreCase("nestedTab")) {
                isNestedTab = Tools.getBooleanValue(entry.getValue(), false);
            }
        }

        if(!isNestedTab) {
            // Remove all params
            params.remove(QUESTION_ID);
            params.remove(DAY_DATE);
            params.remove(USER_ID);
            params.remove(ANSWER_ID);
            params.remove("nestedTab");

            // Clear predicates
            predicates.clear();

            //Set our own predicates
            predicates.add(builder.equal(root.get(QUESTION_ID), questionId));

            Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
            predicates.add(builder.between(root.get(DAY_DATE), dateRangeArr[0], dateRangeArr[1]));

            if(answerId > -1) {
                predicates.add(builder.equal(root.get(ANSWER_ID), answerId));
            }

            if(userId == -3) {
                //Do nothing - all users
            } else if(userId == -2) {
                //Only logged users
                predicates.add( builder.greaterThanOrEqualTo(root.get(USER_ID), 0) );
            } else if(userId == -1) {
                //Only not logged users
                predicates.add( builder.equal(root.get(USER_ID), -1) );
            } else {
                //Specific user
                predicates.add(builder.equal(root.get(USER_ID), userId));
            }
        }
    }

    public static Map<Long, String> getPrepareUsersNames(long questionId, InquiryUsersVoteRepository iur, UserDetailsRepository udr) {
        Map<Long, String> usersFullNameMap = new HashMap<>();

        // Prepare list of uniqe user ID's
        List<Long> usersIds = iur.findAllDistinctUserIds(questionId, CloudToolsForCore.getDomainId());

        if(usersIds == null || usersIds.isEmpty()) return usersFullNameMap;

        for(UserDetailsEntity user : udr.findAllByIdIn(usersIds)) {
            usersFullNameMap.putIfAbsent(user.getId(), user.getFullName());
        }

        return usersFullNameMap;
    }

    public static Map<Long, String> getPreparedAnswers(long questionId, InquiryAnswerRepository iar) {
        Map<Long, String> answersMap = new HashMap<>();
        List<InquiryAnswerEntity> answers =  iar.findAllByQuestionIdAndDomainId((int)questionId, CloudToolsForCore.getDomainId());
        for(InquiryAnswerEntity answer : answers) {
            answersMap.putIfAbsent(answer.getId(), answer.getAnswerText());
        }
        return answersMap;
    }

    public static DatatablePageImpl<InquiryUsersVoteEntity> getPreparedPage(long questionId, Page<InquiryUsersVoteEntity> oldPage, InquiryAnswerRepository iar, InquiryUsersVoteRepository iur, UserDetailsRepository udr) {

        if(questionId < 1) return new DatatablePageImpl<>( new ArrayList<>() );

        Map<Long, String> answersMap = getPreparedAnswers(questionId, iar);

        Map<Long, String> usersFullNameMap = getPrepareUsersNames(questionId, iur, udr);

        DatatablePageImpl<InquiryUsersVoteEntity> page = new DatatablePageImpl<>( oldPage.getContent() );

        for(InquiryUsersVoteEntity vote : page.getContent()) {
            vote.setAnswerText( answersMap.get(vote.getAnswerId()) );
            vote.setUserFullName( usersFullNameMap.get(vote.getUserId()) );
        }

        page.addOptions(ANSWER_TEXT, answersMap);
        page.addOptions("userFullName", usersFullNameMap);

        return page;
    }

    public static List<LabelValueInteger> getPieChartData(DatatablePageImpl<InquiryUsersVoteEntity> page) {
        // Count answers
        Map<Long, Integer> countOfAnswers = new HashMap<>();
        for (InquiryUsersVoteEntity vote : page.getContent()) {
            //Integer::sum gives VSCode java warning even when merge has Null checks
            countOfAnswers.merge(vote.getAnswerId(), 1, (oldValue, newValue) -> (oldValue == null ? 0 : oldValue) + (newValue == null ? 0 : newValue));
        }

        //Prepare chart data
        List<LabelValueInteger> chartData = new ArrayList<>();

        if(page.getOptions() == null) return chartData;

        List<OptionDto> options = page.getOptions().get(ANSWER_TEXT);
        if(options != null) {
            countOfAnswers.forEach((key, value) -> {
                for (OptionDto option : options) {
                    if (option.getValue().equals(String.valueOf(key))) {
                        chartData.add(new LabelValueInteger(option.getLabel(), value));
                        break;
                    }
                }
            });
        }

        return chartData;
    }

    public static Map<String, List<LineChartData>> getLineChartData(DatatablePageImpl<InquiryUsersVoteEntity> page, String dayDate) {
        Map<String, List<LineChartData>> chartData = new HashMap<>();
        Date[] dateRange = StatService.processDateRangeString(dayDate);

        if(page.getOptions() == null) return chartData;

        List<OptionDto> options = page.getOptions().get(ANSWER_TEXT);
        if(options != null) {
            for(OptionDto question : options) {
                // Empty serie for requested date range
                List<LineChartData> series = getEmptySeries(dateRange[0], dateRange[1]);

                //Loop through all votes, found those for this question and increment the value
                for(InquiryUsersVoteEntity vote : page.getContent()) {
                    if(vote.getAnswerId().equals(Long.valueOf(question.getValue()))) {
                        long index = DateTools.getDaysBetween(dateRange[0], vote.getDayDate());
                        series.get((int)index).incrementValue();
                    }
                }

                //Set filled serie to chartData
                chartData.put(question.getLabel(), series);
            }
        }
        return chartData;
    }

    public static List<LineChartData> getEmptySeries(Date from, Date to) {
        List<Date> dates = DateTools.getDatesBetweenInclude(from, to, 12);
        List<LineChartData> series = new ArrayList<>();
        for (Date date : dates) {
            series.add(new LineChartData(0, date));
        }
        return series;
    }

    public static void saveInquiryUserVote(InquiryUsersVoteEntity vote) {
        InquiryUsersVoteRepository iuvr = Tools.getSpringBean("inquiryUsersVoteRepository", InquiryUsersVoteRepository.class);

        if(vote == null) return;

        if(vote.getDomainId() == null)
            vote.setDomainId(CloudToolsForCore.getDomainId());

        iuvr.save(vote);
    }
}