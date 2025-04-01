package sk.iway.iwcm.components.users.userdetail;

import java.sql.Date;
import java.text.SimpleDateFormat;
import javax.annotation.processing.Generated;
import sk.iway.iwcm.users.UserDetails;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-01T15:38:04+0200",
    comments = "version: 1.6.1, compiler: Eclipse JDT (IDE) 3.42.0.v20250325-2231, environment: Java 21.0.6 (Eclipse Adoptium)"
)
public class UserDetailsEntityMapperImpl implements UserDetailsEntityMapper {

    @Override
    public UserDetails userDetailsEntityToUserDetails(UserDetailsEntity user) {
        if ( user == null ) {
            return null;
        }

        UserDetails userDetails = new UserDetails();

        userDetails.setLogin( user.getLogin() );
        if ( user.getAdmin() != null ) {
            userDetails.setAdmin( user.getAdmin() );
        }
        userDetails.setCompany( user.getCompany() );
        userDetails.setEmail( user.getEmail() );
        userDetails.setCountry( user.getCountry() );
        userDetails.setPhone( user.getPhone() );
        if ( user.getAuthorized() != null ) {
            userDetails.setAuthorized( user.getAuthorized() );
        }
        userDetails.setUserGroupsIds( user.getUserGroupsIds() );
        userDetails.setUserGroupsNames( user.getUserGroupsNames() );
        userDetails.setCity( user.getCity() );
        userDetails.setPassword( user.getPassword() );
        userDetails.setLastLogon( user.getLastLogon() );
        userDetails.setLastLogonAsDate( user.getLastLogonAsDate() );
        userDetails.setTitle( user.getTitle() );
        userDetails.setFirstName( user.getFirstName() );
        userDetails.setLastName( user.getLastName() );
        userDetails.setFieldA( user.getFieldA() );
        userDetails.setFieldB( user.getFieldB() );
        userDetails.setFieldC( user.getFieldC() );
        userDetails.setFieldD( user.getFieldD() );
        userDetails.setFieldE( user.getFieldE() );
        if ( user.getRegDate() != null ) {
            userDetails.setRegDate( map( new Date( user.getRegDate().getTime() ) ) );
        }
        if ( user.getDateOfBirth() != null ) {
            userDetails.setDateOfBirth( new SimpleDateFormat().format( user.getDateOfBirth() ) );
        }
        userDetails.setPhoto( user.getPhoto() );
        if ( user.getSexMale() != null ) {
            userDetails.setSexMale( user.getSexMale() );
        }
        userDetails.setSignature( user.getSignature() );
        if ( user.getForumRank() != null ) {
            userDetails.setForumRank( user.getForumRank() );
        }
        if ( user.getRatingRank() != null ) {
            userDetails.setRatingRank( user.getRatingRank() );
        }
        userDetails.setEditableGroups( user.getEditableGroups() );
        userDetails.setEditablePages( user.getEditablePages() );
        userDetails.setWritableFolders( user.getWritableFolders() );
        if ( user.getAllowLoginEnd() != null ) {
            userDetails.setAllowLoginEnd( new SimpleDateFormat().format( user.getAllowLoginEnd() ) );
        }
        if ( user.getAllowLoginStart() != null ) {
            userDetails.setAllowLoginStart( new SimpleDateFormat().format( user.getAllowLoginStart() ) );
        }
        userDetails.setDeliveryCity( user.getDeliveryCity() );
        userDetails.setDeliveryCompany( user.getDeliveryCompany() );
        userDetails.setDeliveryCountry( user.getDeliveryCountry() );
        userDetails.setDeliveryFirstName( user.getDeliveryFirstName() );
        userDetails.setDeliveryLastName( user.getDeliveryLastName() );
        userDetails.setDeliveryPhone( user.getDeliveryPhone() );
        userDetails.setDeliveryPsc( user.getDeliveryPsc() );
        userDetails.setFax( user.getFax() );
        userDetails.setPosition( user.getPosition() );
        if ( user.getParentId() != null ) {
            userDetails.setParentId( user.getParentId() );
        }

        return userDetails;
    }
}
