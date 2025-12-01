package sk.iway.iwcm.components.users.userdetail;

import java.sql.Date;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.users.UserDetails;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserDetailsEntityMapper {
    UserDetailsEntityMapper INSTANCE = Mappers.getMapper(UserDetailsEntityMapper.class);

    //Map regDate Date->long and long->Date
    @Mapping(target = "regDate", source = "regDate", qualifiedByName = "dateToLong")
    @Mapping(target = "lastLogonAsDate", ignore = true) //avoid accidental overwrite if not needed
    UserDetails userDetailsEntityToUserDetails(UserDetailsEntity user);

    @Mapping(target = "regDate", source = "regDate", qualifiedByName = "longToDate")
    UserDetailsEntity userDetailsToUserDetailsEntity(UserDetails user);

    @Named("dateToLong")
    default long dateToLong(Date value) {
        if (value == null) return 0L;
        return value.getTime();
    }

    @Named("longToDate")
    default Date longToDate(long value) {
        if (value <= 0L) return null;
        return new Date(value);
    }
}
