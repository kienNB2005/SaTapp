package ken.example.dekiru.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record SuggestedSlotDto(
    LocalDate sessionDate,
    Byte periodStart,
    Byte periodEnd,
    String dayOfWeek,
    List<DropdownOption> availableRooms
) {}
