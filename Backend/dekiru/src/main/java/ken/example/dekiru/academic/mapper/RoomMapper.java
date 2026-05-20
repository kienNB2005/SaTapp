package ken.example.dekiru.academic.mapper;

import ken.example.dekiru.academic.dto.RoomResponse;
import ken.example.dekiru.academic.entity.Room;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    
    RoomResponse toRoomResponse(Room room);
    
    List<RoomResponse> toRoomResponseList(List<Room> rooms);
    
    Room toRoom(RoomResponse response);
}

