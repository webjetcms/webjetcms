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
    @Mapping(target = "dateOfBirth", source = "dateOfBirth", qualifiedByName = "dateToString")
    @Mapping(target = "allowLoginStart", source = "allowLoginStart", qualifiedByName = "dateToString")
    @Mapping(target = "allowLoginEnd", source = "allowLoginEnd", qualifiedByName = "dateToString")
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "lastLogonAsDate", ignore = true) //avoid accidental overwrite if not needed
    UserDetails userDetailsEntityToUserDetails(UserDetailsEntity user);


    @Named("dateToLong")
    default long dateToLong(Date value) {
        if (value == null) return 0L;
        return value.getTime();
    }

    @Named("dateToString")
    default String dateToString(Date value) {
        if (value == null) return null;
        // java.sql.Date#toString returns ISO format yyyy-MM-dd
        return value.toString();
    }
}
