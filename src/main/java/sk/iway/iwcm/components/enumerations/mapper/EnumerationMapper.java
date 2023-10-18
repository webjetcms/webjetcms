package sk.iway.iwcm.components.enumerations.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.components.enumerations.dto.EnumerationDataDto;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;

/**
 * Date: 09.04.2018
 * Time: 14:59
 * Project: webjet8
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2018
 *
 * @author mpijak
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EnumerationMapper {
    EnumerationMapper INSTANCE = Mappers.getMapper( EnumerationMapper.class );

    EnumerationDataDto toEnumerationDataDto(EnumerationDataBean enumerationDataBean);
    List<EnumerationDataDto> toEnumerationDataDtos(List<EnumerationDataBean> enumerationDataBeans);

    EnumerationDataBean toEnumerationDataBean(EnumerationDataDto enumerationDataDto);
    List<EnumerationDataBean> toEnumerationDataBeans(List<EnumerationDataDto> enumerationDataDtos);
}
