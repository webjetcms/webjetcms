package sk.iway.iwcm.components.users.userdetail;

import java.sql.Date;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.users.UserDetails;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserDetailsEntityMapper {
    UserDetailsEntityMapper INSTANCE = Mappers.getMapper(UserDetailsEntityMapper.class);

    UserDetails userDetailsEntityToUserDetails(UserDetailsEntity user);

    default long map(Date value) {
        if (value == null) return 0;
        return value.getTime();
    }
}
