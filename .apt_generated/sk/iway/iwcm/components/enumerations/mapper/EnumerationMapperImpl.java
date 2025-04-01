package sk.iway.iwcm.components.enumerations.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import sk.iway.iwcm.components.enumerations.dto.EnumerationDataDto;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-01T15:38:05+0200",
    comments = "version: 1.6.1, compiler: Eclipse JDT (IDE) 3.42.0.v20250325-2231, environment: Java 21.0.6 (Eclipse Adoptium)"
)
public class EnumerationMapperImpl implements EnumerationMapper {

    @Override
    public EnumerationDataDto toEnumerationDataDto(EnumerationDataBean enumerationDataBean) {
        if ( enumerationDataBean == null ) {
            return null;
        }

        EnumerationDataDto enumerationDataDto = new EnumerationDataDto();

        if ( enumerationDataBean.getId() != null ) {
            enumerationDataDto.setId( enumerationDataBean.getId().intValue() );
        }
        enumerationDataDto.setString1( enumerationDataBean.getString1() );
        enumerationDataDto.setString2( enumerationDataBean.getString2() );
        enumerationDataDto.setString3( enumerationDataBean.getString3() );
        enumerationDataDto.setDecimal1( enumerationDataBean.getDecimal1() );
        enumerationDataDto.setDecimal2( enumerationDataBean.getDecimal2() );
        enumerationDataDto.setDecimal3( enumerationDataBean.getDecimal3() );

        return enumerationDataDto;
    }

    @Override
    public List<EnumerationDataDto> toEnumerationDataDtos(List<EnumerationDataBean> enumerationDataBeans) {
        if ( enumerationDataBeans == null ) {
            return null;
        }

        List<EnumerationDataDto> list = new ArrayList<EnumerationDataDto>( enumerationDataBeans.size() );
        for ( EnumerationDataBean enumerationDataBean : enumerationDataBeans ) {
            list.add( toEnumerationDataDto( enumerationDataBean ) );
        }

        return list;
    }

    @Override
    public EnumerationDataBean toEnumerationDataBean(EnumerationDataDto enumerationDataDto) {
        if ( enumerationDataDto == null ) {
            return null;
        }

        EnumerationDataBean enumerationDataBean = new EnumerationDataBean();

        enumerationDataBean.setDecimal1( enumerationDataDto.getDecimal1() );
        enumerationDataBean.setDecimal2( enumerationDataDto.getDecimal2() );
        enumerationDataBean.setDecimal3( enumerationDataDto.getDecimal3() );
        enumerationDataBean.setString1( enumerationDataDto.getString1() );
        enumerationDataBean.setString2( enumerationDataDto.getString2() );
        enumerationDataBean.setString3( enumerationDataDto.getString3() );
        enumerationDataBean.setId( (long) enumerationDataDto.getId() );

        return enumerationDataBean;
    }

    @Override
    public List<EnumerationDataBean> toEnumerationDataBeans(List<EnumerationDataDto> enumerationDataDtos) {
        if ( enumerationDataDtos == null ) {
            return null;
        }

        List<EnumerationDataBean> list = new ArrayList<EnumerationDataBean>( enumerationDataDtos.size() );
        for ( EnumerationDataDto enumerationDataDto : enumerationDataDtos ) {
            list.add( toEnumerationDataBean( enumerationDataDto ) );
        }

        return list;
    }
}
