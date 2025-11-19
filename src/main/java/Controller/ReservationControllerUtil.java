package Controller;

import Manager.ClientClassroomManager;
import Util.ReservationUtil;

/**
 * 예약 관련 공통 유틸리티 메서드 모음
 * AbstractReservationController와 서브클래스에서 공통으로 사용
 */
public class ReservationControllerUtil {
    
    /**
     * 시간 문자열을 교시로 변환
     * 예: "1교시(09:00~10:00)" -> 1
     */
    public static int parseTimeToHour(String timeSlot) {
        return ReservationUtil.parseTimeToHour(timeSlot);
    }
    
    /**
     * 교시를 시간 문자열로 변환
     * 예: 1 -> "1교시(09:00~10:00)"
     */
    public static String formatTimeSlot(int hour) {
        return ReservationUtil.formatTimeSlot(hour);
    }
    
    /**
     * 예약 시간 범위 검증
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param role 사용자 역할
     * @return ValidationResult (isValid, errorMessage)
     */
    public static ValidationResult validateTimeRange(String startTime, String endTime, String role) {
        int startHour = parseTimeToHour(startTime);
        int endHour = parseTimeToHour(endTime);
        
        if (startHour > endHour) {
            return new ValidationResult(false, "종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        
        int duration = endHour - startHour + 1;
        
        if (role.equals("학생") && duration > 2) {
            return new ValidationResult(false, 
                String.format("학생은 최대 2시간까지만 예약 가능합니다.\n선택: %d시간", duration));
        }
        
        if (!role.equals("학생") && duration > 3) {
            return new ValidationResult(false, 
                String.format("교수/세미나/학회는 최대 3시간까지만 예약 가능합니다.\n선택: %d시간", duration));
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * 수용 인원 검증
     * @param roomName 강의실/실습실 이름
     * @param requestedCount 요청 인원
     * @return ValidationResult (isValid, errorMessage)
     */
    public static ValidationResult validateCapacity(String roomName, int requestedCount) {
        ClientClassroomManager manager = ClientClassroomManager.getInstance();
        common.dto.ClassroomDTO classroom = manager.getClassroom(roomName);
        
        if (classroom == null) {
            return new ValidationResult(false, "강의실/실습실 정보를 찾을 수 없습니다.");
        }
        
        if (!manager.checkCapacity(roomName, requestedCount)) {
            String message = String.format(
                "예약 불가!\n\n"
                + "요청 인원: %d명\n"
                + "최대 허용: %d명\n\n"
                + "(이 강의실/실습실은 수용 인원 %d명의 50%%인 %d명까지만 예약 가능합니다)",
                requestedCount,
                classroom.getAllowedCapacity(),
                classroom.capacity,
                classroom.getAllowedCapacity()
            );
            return new ValidationResult(false, message);
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * 날짜 검증 (최소 하루 전 예약)
     * @param selectedDate 선택된 날짜
     * @return ValidationResult (isValid, errorMessage)
     */
    public static ValidationResult validateReservationDate(java.time.LocalDate selectedDate) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate tomorrow = today.plusDays(1);
        
        if (selectedDate.isBefore(tomorrow)) {
            String message = String.format(
                "최소 하루 전에 예약해야 합니다.\n%s일 사용을 원하시면 %s일까지 예약해주세요.",
                selectedDate.toString(),
                selectedDate.minusDays(1).toString()
            );
            return new ValidationResult(false, message);
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * 검증 결과를 담는 클래스
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }
}
