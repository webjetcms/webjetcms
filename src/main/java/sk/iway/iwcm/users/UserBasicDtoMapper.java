package sk.iway.iwcm.users;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserBasicDtoMapper {
    UserBasicDtoMapper INSTANCE = Mappers.getMapper(UserBasicDtoMapper.class);

    UserBasicDto userDetailsToDto(UserDetails user);
}
