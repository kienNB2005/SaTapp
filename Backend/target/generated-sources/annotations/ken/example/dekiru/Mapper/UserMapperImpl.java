package ken.example.dekiru.Mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import ken.example.dekiru.DTO.Request.UserCreateRequest;
import ken.example.dekiru.DTO.Request.UserUpdateRequest;
import ken.example.dekiru.DTO.Response.UserResponse;
import ken.example.dekiru.Entity.UserRole;
import ken.example.dekiru.Entity.Users;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-12T05:01:34+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public Users toUsers(UserCreateRequest userCreateRequest) {
        if ( userCreateRequest == null ) {
            return null;
        }

        Users.UsersBuilder users = Users.builder();

        users.username( userCreateRequest.getUsername() );
        users.email( userCreateRequest.getEmail() );

        return users.build();
    }

    @Override
    public UserResponse toUserResponse(Users user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.roles( userRoleListToStringList( user.getUserRoles() ) );
        userResponse.id( user.getId() );
        userResponse.username( user.getUsername() );
        userResponse.email( user.getEmail() );
        userResponse.createdAt( user.getCreatedAt() );

        return userResponse.build();
    }

    @Override
    public UserResponse toResponse(Users user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.roles( userRoleListToStringList( user.getUserRoles() ) );
        userResponse.id( user.getId() );
        userResponse.username( user.getUsername() );
        userResponse.email( user.getEmail() );
        userResponse.createdAt( user.getCreatedAt() );

        return userResponse.build();
    }

    @Override
    public void updateUser(Users user, UserUpdateRequest req) {
        if ( req == null ) {
            return;
        }

        if ( req.getEmail() != null ) {
            user.setEmail( req.getEmail() );
        }
    }

    protected List<String> userRoleListToStringList(List<UserRole> list) {
        if ( list == null ) {
            return null;
        }

        List<String> list1 = new ArrayList<String>( list.size() );
        for ( UserRole userRole : list ) {
            list1.add( map( userRole ) );
        }

        return list1;
    }
}
