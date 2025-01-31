package sk.iway.iwcm.components.perex_groups;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.doc.PerexGroupBean;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PerexGroupBeanToPerexGroupsEntityMapper {

    PerexGroupBeanToPerexGroupsEntityMapper INSTANCE = Mappers.getMapper(PerexGroupBeanToPerexGroupsEntityMapper.class);

    @Mapping(source = "perexGroupId", target = "id", qualifiedByName = "intToLong")
    @Mapping(source = "relatedPages", target = "relatedPages", qualifiedByName = "tokensToString")
    PerexGroupsEntity perexGroupBeanToPerexGroupsEntity(PerexGroupBean perexGroupBean);
    List<PerexGroupsEntity> perexGroupBeanListToPerexGroupsEntityList(List<PerexGroupBean> perexGroupBeanList);

    @Named("intToLong")
    default Long intToLong(int id) {
        return Long.valueOf(id);
    }

    @Named("tokensToString")
    default String tokensToString(String[] relatedPages) {
        if(relatedPages.length < 1) return "";
        return String.join(",", relatedPages);
    }

    @Mapping(source = "id", target = "perexGroupId", qualifiedByName = "longToInt")
    PerexGroupBean perexGroupsEntityToPerexGroupBean (PerexGroupsEntity perexGroupsEntity);
    List<PerexGroupBean> perexGroupsEntityListToPerexGroupBeanList (List<PerexGroupsEntity> perexGroupsEntityList);

    @Named("longToInt")
    default int longToInt(Long id) {
        return id.intValue();
    }
}
