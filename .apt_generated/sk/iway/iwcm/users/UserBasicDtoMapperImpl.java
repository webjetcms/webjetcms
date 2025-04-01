package sk.iway.iwcm.users;

import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-01T15:38:04+0200",
    comments = "version: 1.6.1, compiler: Eclipse JDT (IDE) 3.42.0.v20250325-2231, environment: Java 21.0.6 (Eclipse Adoptium)"
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
