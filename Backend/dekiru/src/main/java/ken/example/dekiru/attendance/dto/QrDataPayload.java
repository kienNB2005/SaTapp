package ken.example.dekiru.attendance.dto;

public record QrDataPayload(Long sessionId, String qrToken, String type) {}

