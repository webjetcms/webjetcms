package sk.iway.iwcm.users;

import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-03-25T15:55:09+0100",
    comments = "version: 1.6.1, compiler: Eclipse JDT (IDE) 3.41.0.z20250213-2037, environment: Java 21.0.6 (Eclipse Adoptium)"
)
public class UserBasicDtoMapperImpl implements UserBasicDtoMapper {

    @Override
    public UserBasicDto userDetailsToDto(UserDetails user) {
        if ( user == null ) {
            return null;
        }

        UserBasicDto userBasicDto = new UserBasicDto();

        userBasicDto.setEmail( user.getEmail() );
        userBasicDto.setFirstName( user.getFirstName() );
        userBasicDto.setFullName( user.getFullName() );
        userBasicDto.setLastName( user.getLastName() );
        userBasicDto.setLogin( user.getLogin() );
        userBasicDto.setPhoto( user.getPhoto() );
        userBasicDto.setUserId( user.getUserId() );

        return userBasicDto;
    }

    @Override
    public UserDetails dtoToUserDetails(UserBasicDto user) {
        if ( user == null ) {
            return null;
        }

        UserDetails userDetails = new UserDetails();

        userDetails.setUserId( user.getUserId() );
        userDetails.setLogin( user.getLogin() );
        userDetails.setEmail( user.getEmail() );
        userDetails.setFirstName( user.getFirstName() );
        userDetails.setLastName( user.getLastName() );
        userDetails.setPhoto( user.getPhoto() );

        return userDetails;
    }
}
