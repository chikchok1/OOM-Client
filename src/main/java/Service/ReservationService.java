package Service;

import Model.Session;
import Util.MessageDispatcher;
import Util.ReservationUtil;
import java.io.PrintWriter;
import java.util.*;

/**
 * 예약 서비스
 * 예약 관련 비즈니스 로직과 서버 통신을 담당
 */
public class ReservationService {
    
    private static final int SERVER_RESPONSE_TIMEOUT_SECONDS = 30;
    
    /**
     * 승인된 예약 목록 조회
     * @return 예약 정보 리스트
     */
    public List<ReservationDTO> getApprovedReservations() {
        if (!validateServerConnection()) {
            throw new IllegalStateException("서버에 연결되어 있지 않습니다.");
        }
        
        String userId = Session.getInstance().getLoggedInUserId();
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("사용자 로그인 정보를 찾을 수 없습니다.");
        }
        
        return fetchApprovedReservationsFromServer();
    }
    
    /**
     * 예약 변경
     */
    public ChangeReservationResponse changeReservation(ChangeReservationRequest request) {
        if (!validateServerConnection()) {
            return new ChangeReservationResponse(false, "서버 연결 없음", null);
        }
        
        // 강의실 가용성 확인
        boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(request.getNewRoom());
        if (!isAvailable) {
            return new ChangeReservationResponse(false, "선택하신 강의실은 현재 사용 불가능합니다.", null);
        }
        
        // 시간대 중복 확인
        String conflictTime = checkTimeSlotConflicts(request);
        if (conflictTime != null) {
            return new ChangeReservationResponse(false, 
                conflictTime + "는 이미 예약되어 있습니다.", null);
        }
        
        // 서버에 변경 요청
        return sendChangeRequestToServer(request);
    }
    
    /**
     * 예약 취소
     */
    public CancelReservationResponse cancelReservation(ReservationDTO reservation) {
        if (!validateServerConnection()) {
            return new CancelReservationResponse(false, "서버 연결 없음");
        }
        
        return sendCancelRequestToServer(reservation);
    }
    
    // ==================== Private Methods ====================
    
    private boolean validateServerConnection() {
        return Session.getInstance().isConnected();
    }
    
    private List<ReservationDTO> fetchApprovedReservationsFromServer() {
        List<ReservationDTO> reservations = new ArrayList<>();
        
        try {
            PrintWriter out = Session.getInstance().getOut();
            MessageDispatcher dispatcher = MessageDispatcher.getInstance();
            
            if (out == null || dispatcher == null) {
                return reservations;
            }
            
            out.println("VIEW_APPROVED_RESERVATIONS");
            out.flush();
            
            int attempts = 0;
            int maxAttempts = 100;
            
            while (attempts < maxAttempts) {
                attempts++;
                String line = dispatcher.waitForResponse(SERVER_RESPONSE_TIMEOUT_SECONDS);
                
                if (line == null || line.equals("END_OF_APPROVED_RESERVATIONS")) {
                    break;
                }
                
                if (line.equals("VIEW_APPROVED_RESERVATIONS_SUCCESS")) {
                    continue;
                }
                
                if (line.startsWith("CLASS,") || line.startsWith("LAB,")) {
                    ReservationDTO dto = parseReservationLine(line);
                    if (dto != null) {
                        reservations.add(dto);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[ReservationService] 예약 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reservations;
    }
    
    private ReservationDTO parseReservationLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 11) {
            return null;
        }
        
        try {
            return new ReservationDTO(
                parts[0].trim(),  // fileType
                parts[10].trim(), // userId
                parts[1].trim(),  // name
                parts[2].trim(),  // room
                parts[3].trim(),  // date
                parts[4].trim(),  // day
                parts[5].trim(),  // time
                parts[6].trim(),  // purpose
                parts[7].trim(),  // role
                Integer.parseInt(parts[9].trim()) // studentCount
            );
        } catch (Exception e) {
            System.err.println("[ReservationService] 파싱 오류: " + line);
            return null;
        }
    }
    
    private String checkTimeSlotConflicts(ChangeReservationRequest request) {
        int startHour = ReservationUtil.parseTimeToHour(request.getStartTime());
        int endHour = ReservationUtil.parseTimeToHour(request.getEndTime());
        
        for (int hour = startHour; hour <= endHour; hour++) {
            String timeSlot = ReservationUtil.formatTimeSlot(hour);
            if (ReservationUtil.isReservedOnDate(
                    request.getReservedMap(), 
                    request.getNewRoom(), 
                    request.getSelectedDate(), 
                    timeSlot)) {
                return timeSlot;
            }
        }
        return null;
    }
    
    private ChangeReservationResponse sendChangeRequestToServer(ChangeReservationRequest request) {
        try {
            PrintWriter out = Session.getInstance().getOut();
            MessageDispatcher dispatcher = MessageDispatcher.getInstance();
            
            if (out == null || dispatcher == null) {
                return new ChangeReservationResponse(false, "서버 스트림 없음", null);
            }
            
            String command = buildChangeCommand(request);
            out.println(command);
            out.flush();
            
            String response = dispatcher.waitForResponse(SERVER_RESPONSE_TIMEOUT_SECONDS);
            
            if (response == null || response.isEmpty()) {
                return new ChangeReservationResponse(false, "서버 응답 없음", null);
            }
            
            if ("CHANGE_SUCCESS".equals(response)) {
                return new ChangeReservationResponse(true, "예약 변경 성공", null);
            } else if (response.startsWith("CHANGE_FAILED_CONFLICT:")) {
                String conflictTime = response.substring("CHANGE_FAILED_CONFLICT:".length());
                return new ChangeReservationResponse(false, 
                    conflictTime + "는 이미 다른 예약이 있습니다.", conflictTime);
            } else if ("CHANGE_FAILED_NOT_FOUND".equals(response)) {
                return new ChangeReservationResponse(false, "기존 예약을 찾을 수 없습니다.", null);
            } else {
                return new ChangeReservationResponse(false, response, null);
            }
            
        } catch (Exception e) {
            System.err.println("[ReservationService] 변경 요청 실패: " + e.getMessage());
            return new ChangeReservationResponse(false, "오류 발생: " + e.getMessage(), null);
        }
    }
    
    private String buildChangeCommand(ChangeReservationRequest request) {
        ReservationDTO original = request.getOriginal();
        String sessionUserName = Session.getInstance().getLoggedInUserName();
        String sessionUserRole = Session.getInstance().getLoggedInUserRole();
        
        String finalName = (sessionUserName != null && !sessionUserName.isEmpty())
                ? sessionUserName : original.getName();
        String finalRole = (request.getRole() != null && !request.getRole().isEmpty())
                ? request.getRole() : (sessionUserRole != null ? sessionUserRole : original.getRole());
        
        String oldFileType = original.getFileType();
        String newFileType = determineFileType(request.getNewRoom());
        
        String normOldRoom = ReservationUtil.normalizeRoomName(original.getRoom());
        String normNewRoom = ReservationUtil.normalizeRoomName(request.getNewRoom());
        
        int startHour = ReservationUtil.parseTimeToHour(request.getStartTime());
        int endHour = ReservationUtil.parseTimeToHour(request.getEndTime());
        
        StringBuilder sb = new StringBuilder();
        sb.append("CHANGE_RESERVATION_FULL,");
        sb.append(oldFileType).append(",");
        sb.append(newFileType).append(",");
        sb.append(original.getUserId()).append(",");
        sb.append(finalName).append(",");
        sb.append(normOldRoom).append(",");
        sb.append(original.getDate()).append(",");
        sb.append(original.getDay()).append(",");
        sb.append(original.getTime()).append(",");
        
        for (int hour = startHour; hour <= endHour; hour++) {
            String timeSlot = ReservationUtil.formatTimeSlot(hour);
            sb.append(normNewRoom).append("|")
                    .append(request.getDateString()).append("|")
                    .append(request.getDay()).append("|")
                    .append(timeSlot).append("|")
                    .append(request.getPurpose()).append("|")
                    .append(finalRole).append("|")
                    .append(request.getStudentCount()).append(";");
        }
        
        return sb.toString();
    }
    
    private String determineFileType(String room) {
        Manager.ClientClassroomManager mgr = Manager.ClientClassroomManager.getInstance();
        common.dto.ClassroomDTO classroom = mgr.getClassroom(room);
        return (classroom != null && "LAB".equals(classroom.type)) ? "LAB" : "CLASS";
    }
    
    private CancelReservationResponse sendCancelRequestToServer(ReservationDTO reservation) {
        try {
            PrintWriter out = Session.getInstance().getOut();
            MessageDispatcher dispatcher = MessageDispatcher.getInstance();
            
            if (out == null || dispatcher == null) {
                return new CancelReservationResponse(false, "서버 연결이 끊어졌습니다.");
            }
            
            String requesterId = Session.getInstance().getLoggedInUserId();
            String command = String.format("CANCEL_RESERVATION,%s,%s,%s,%s,%s,%s,%s",
                    requesterId,
                    reservation.getUserId(),
                    reservation.getDay(),
                    reservation.getDate(),
                    reservation.getTime(),
                    reservation.getRoom(),
                    reservation.getName()
            );
            
            out.println(command);
            out.flush();
            
            String response = dispatcher.waitForResponse(SERVER_RESPONSE_TIMEOUT_SECONDS);
            
            if (response == null) {
                return new CancelReservationResponse(false, "서버 응답 타임아웃");
            }
            
            if ("CANCEL_SUCCESS".equals(response)) {
                return new CancelReservationResponse(true, "예약 취소 성공");
            } else if ("CANCEL_FAILED_NOT_FOUND".equals(response)) {
                return new CancelReservationResponse(false, 
                    "해당 예약을 찾을 수 없습니다.\n이미 취소되었을 수 있습니다.");
            } else {
                return new CancelReservationResponse(false, response);
            }
            
        } catch (Exception e) {
            System.err.println("[ReservationService] 취소 요청 실패: " + e.getMessage());
            return new CancelReservationResponse(false, "오류 발생: " + e.getMessage());
        }
    }
    
    // ==================== DTO Classes ====================
    
    /**
     * 예약 정보 DTO
     */
    public static class ReservationDTO {
        private final String fileType;
        private final String userId;
        private final String name;
        private final String room;
        private final String date;
        private final String day;
        private final String time;
        private final String purpose;
        private final String role;
        private final int studentCount;
        
        public ReservationDTO(String fileType, String userId, String name, String room,
                String date, String day, String time, String purpose, String role, int studentCount) {
            this.fileType = fileType;
            this.userId = userId;
            this.name = name;
            this.room = room;
            this.date = date;
            this.day = day;
            this.time = time;
            this.purpose = purpose;
            this.role = role;
            this.studentCount = studentCount;
        }
        
        // Getters
        public String getFileType() { return fileType; }
        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getRoom() { return room; }
        public String getDate() { return date; }
        public String getDay() { return day; }
        public String getTime() { return time; }
        public String getPurpose() { return purpose; }
        public String getRole() { return role; }
        public int getStudentCount() { return studentCount; }
    }
    
    /**
     * 예약 변경 요청 DTO
     */
    public static class ChangeReservationRequest {
        private final ReservationDTO original;
        private final java.time.LocalDate selectedDate;
        private final String dateString;
        private final String newRoom;
        private final String day;
        private final String startTime;
        private final String endTime;
        private final String purpose;
        private final String role;
        private final int studentCount;
        private final Map<String, Set<String>> reservedMap;
        
        public ChangeReservationRequest(ReservationDTO original, java.time.LocalDate selectedDate,
                String dateString, String newRoom, String day, String startTime, String endTime,
                String purpose, String role, int studentCount, Map<String, Set<String>> reservedMap) {
            this.original = original;
            this.selectedDate = selectedDate;
            this.dateString = dateString;
            this.newRoom = newRoom;
            this.day = day;
            this.startTime = startTime;
            this.endTime = endTime;
            this.purpose = purpose;
            this.role = role;
            this.studentCount = studentCount;
            this.reservedMap = reservedMap;
        }
        
        // Getters
        public ReservationDTO getOriginal() { return original; }
        public java.time.LocalDate getSelectedDate() { return selectedDate; }
        public String getDateString() { return dateString; }
        public String getNewRoom() { return newRoom; }
        public String getDay() { return day; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getPurpose() { return purpose; }
        public String getRole() { return role; }
        public int getStudentCount() { return studentCount; }
        public Map<String, Set<String>> getReservedMap() { return reservedMap; }
    }
    
    /**
     * 예약 변경 응답 DTO
     */
    public static class ChangeReservationResponse {
        private final boolean success;
        private final String message;
        private final String conflictTime;
        
        public ChangeReservationResponse(boolean success, String message, String conflictTime) {
            this.success = success;
            this.message = message;
            this.conflictTime = conflictTime;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getConflictTime() { return conflictTime; }
    }
    
    /**
     * 예약 취소 응답 DTO
     */
    public static class CancelReservationResponse {
        private final boolean success;
        private final String message;
        
        public CancelReservationResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}