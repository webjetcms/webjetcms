package sk.iway.iwcm.editor.rest;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.doc.GroupDetails;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupSchedulerDtoMapper {

    GroupSchedulerDtoMapper INSTANCE = Mappers.getMapper(GroupSchedulerDtoMapper.class);

    @Mapping(source = "schedulerId", target = "id")
    @Mapping(source = "groupId", target = "groupId")
    @Mapping(source = "forceTheUseOfGroupTemplate", target = "forceGroupTemplate")
    @Mapping(source = "newPageDocIdTemplate", target = "newPageDocidTemplate")
    @Mapping(source = "hiddenInAdmin", target = "hiddenInAdmin", qualifiedByName = "booleanToInteger")
    GroupSchedulerDto groupToGroupSchedulerDto(GroupDetails group);
    List<GroupSchedulerDto> groupDetailsListToGroupSchedulerDtos(List<GroupDetails> groupDetailsList);

    @Mapping(source = "id", target = "schedulerId")
    @Mapping(source = "groupId", target = "groupId")
    @Mapping(source = "forceGroupTemplate", target = "forceTheUseOfGroupTemplate")
    @Mapping(source = "newPageDocidTemplate", target = "newPageDocIdTemplate")
    @Mapping(source = "hiddenInAdmin", target = "hiddenInAdmin", qualifiedByName = "integerToBoolean")
    GroupDetails groupSchedulerDtoToGroup(GroupSchedulerDto dto);
    List<GroupDetails> groupSchedulerDtosToGroupDetailsList(List<GroupSchedulerDto> dtos);

    @Named("booleanToInteger")
    default Integer booleanToInteger(boolean value) {
        return value ? 1 : 0;
    }

    @Named("integerToBoolean")
    default boolean integerToBoolean(Integer value) {
        return value != null && value == 1;
    }
}
