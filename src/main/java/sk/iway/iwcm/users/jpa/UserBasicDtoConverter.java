package sk.iway.iwcm.users.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import sk.iway.iwcm.users.UserBasicDto;
import sk.iway.iwcm.users.UserBasicDtoMapper;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Converter
public class UserBasicDtoConverter implements AttributeConverter<UserBasicDto, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserBasicDto user) {
        if (user != null) return user.getUserId();
        return null;
    }

    @Override
    public UserBasicDto convertToEntityAttribute(Integer userId) {
        if (userId == null || userId.intValue()<1) return null;
        UserDetails user = UsersDB.getUserCached(userId);
        if (user!=null)
        {
            UserBasicDto dto = UserBasicDtoMapper.INSTANCE.userDetailsToDto(user);
            return dto;
        }
        return null;
    }

}
