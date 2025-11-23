package Controller;

import Manager.ClientClassroomManager;
import Model.Session;
import Util.ReservationUtil;
import View.RoomSelect;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

/**
 * 템플릿 메서드 패턴 적용
 * 예약 프로세스의 알고리즘 골격을 정의
 */
public abstract class AbstractReservationController {

    protected final Map<String, Set<String>> reservedMap = new ConcurrentHashMap<>();
    protected final Map<String, Map<String, String>> statusMap = new ConcurrentHashMap<>();
    protected final Object serverLock = new Object();

    /**
     *  Strategy 패턴 적용: 기본 초기화 메서드
     * StandardReservationInitStrategy를 기본 전략으로 사용
     */
    protected void initialize() {
        initialize(new StandardReservationInitStrategy());
    }
    
    /**
     *  Strategy 패턴: 전략을 주입받아 초기화
     * @param strategy 초기화 전략
     */
    protected void initialize(InitializationStrategy strategy) {
        strategy.initialize(this);
    }

    /**
     * 이벤트 리스너 설정 - 공통 로직
     * Strategy 패턴 적용: package-private으로 변경하여 전략 클래스에서 접근 가능
     */
    void setupEventListeners() {
        resetReservationButtonListener();
        addReservationListener(new ReservationListener());

        getBeforeButton().addActionListener(e -> {
            closeView();
            RoomSelect roomSelect = RoomSelect.getInstance();
            new RoomSelectController(roomSelect);
            roomSelect.setVisible(true);
        });

        getRoomComboBox().addActionListener(e -> {
            String newSelectedRoom = getSelectedRoom();
            refreshReservationAndAvailability(newSelectedRoom);
        });

        getTimeComboBox().addActionListener(e -> updateCapacityPanel());

        getDateChooser().addPropertyChangeListener("date", evt -> {
            String newSelectedRoom = getSelectedRoom();
            refreshReservationAndAvailability(newSelectedRoom);
        });
    }

    // ============================================================
    // 템플릿 메서드: 예약 프로세스의 알고리즘
    // ============================================================

    /**
     * 템플릿 메서드 - 예약 프로세스의 전체 흐름 정의
     * final로 선언하여 서브클래스가 오버라이드 할 수 없도록 함
     */
    protected final void processReservation(ActionEvent e) {
        // 1단계: 입력 데이터 수집
        ReservationData data = collectReservationData();
        
        // 2단계: 기본 검증 (공통)
        if (!validateBasicInput(data)) {
            return;
        }
        
        // 3단계: 날짜 검증 (공통)
        if (!validateDate(data)) {
            return;
        }
        
        // 4단계: 시간 검증 (역할별 다름 - Hook 메서드)
        if (!validateReservationTime(data)) {
            return;
        }
        
        // 5단계: 수용 인원 검증 (공통이지만 메시지가 다름 - Hook 메서드)
        if (!validateCapacity(data)) {
            return;
        }
        
        // 6단계: 추가 검증 (서브클래스에서 필요시 오버라이드 - Hook 메서드)
        if (!performAdditionalValidation(data)) {
            return;
        }
        
        // 7단계: 서버에 예약 요청 (공통)
        submitReservation(data);
    }

    /**
     * 1단계: 입력 데이터 수집 (공통)
     */
    private ReservationData collectReservationData() {
        return new ReservationData(
                getSelectedDateString(),
                getSelectedDate(),
                Session.getInstance().getLoggedInUserName(),
                getSelectedRoom(),
                getSelectedDay(),
                getSelectedTime(),
                getSelectedEndTime(),
                getPurpose(),
                getStudentCount(),
                Session.getInstance().getLoggedInUserRole()
        );
    }

    /**
     * 2단계: 기본 입력 검증 (공통)
     */
    private boolean validateBasicInput(ReservationData data) {
        if (data.dateString == null) {
            showMessage("예약 날짜를 선택해주세요.");
            return false;
        }

        if (data.purpose.isEmpty()) {
            showMessage("사용 목적을 입력해주세요.");
            return false;
        }

        return true;
    }

    /**
     * 3단계: 날짜 검증 (공통)
     *  ReservationControllerUtil 사용
     */
    private boolean validateDate(ReservationData data) {
        ReservationControllerUtil.ValidationResult result = 
            ReservationControllerUtil.validateReservationDate(data.selectedDate);
        
        if (!result.isValid) {
            showMessage(result.errorMessage);
            return false;
        }
        
        return true;
    }

    /**
     * 4단계: 시간 검증 (역할별 규칙이 다름 - Hook 메서드)
     * ReservationControllerUtil 사용
     */
    protected boolean validateReservationTime(ReservationData data) {
        System.out.println("[시간검증] 시작시간: " + data.startTime);
        System.out.println("[시간검증] 종료시간: " + data.endTime);
        System.out.println("[시간검증] 사용자 역할: " + data.userRole);
        
        ReservationControllerUtil.ValidationResult result = 
            ReservationControllerUtil.validateTimeRange(data.startTime, data.endTime, data.userRole);
        
        if (!result.isValid) {
            showMessage(result.errorMessage);
            return false;
        }
        
        return true;
    }

    // checkRoleBasedTimeLimit() 메서드는 ReservationControllerUtil로 이동

    /**
     * 5단계: 수용 인원 검증 (공통이지만 메시지가 다름 - Hook 메서드)
     *  ReservationControllerUtil 사용
     */
    protected boolean validateCapacity(ReservationData data) {
        ReservationControllerUtil.ValidationResult result = 
            ReservationControllerUtil.validateCapacity(data.room, data.studentCount);
        
        if (!result.isValid) {
            showMessage(result.errorMessage);
            return false;
        }
        
        return true;
    }

    // showCapacityErrorMessage() 메서드는 ReservationControllerUtil로 통합

    /**
     * 6단계: 추가 검증 (Hook 메서드)
     * 서브클래스에서 필요시 추가 검증 로직 구현
     * 기본 구현은 true 반환
     */
    protected boolean performAdditionalValidation(ReservationData data) {
        // 기본적으로 추가 검증 없음
        // 서브클래스에서 필요시 오버라이드
        return true;
    }

    /**
     * 7단계: 서버에 예약 요청 (공통)
     */
    private void submitReservation(ReservationData data) {
        new Thread(() -> {
            synchronized (serverLock) {
                // 방 가용성 체크
                boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(data.room);

                if (!isAvailable) {
                    SwingUtilities.invokeLater(() ->
                            showMessage("선택하신 " + getRoomTypeName() + "은(는) 현재 사용 불가능합니다. 관리자에게 문의하세요.")
                    );
                    return;
                }

                // 시간대별 예약 충돌 체크
                int startHour = ReservationUtil.parseTimeToHour(data.startTime);
                int endHour = ReservationUtil.parseTimeToHour(data.endTime);

                for (int hour = startHour; hour <= endHour; hour++) {
                    String timeSlot = hour + "교시";
                    if (ReservationUtil.isReservedOnDate(reservedMap, data.room, data.selectedDate, timeSlot)) {
                        final String conflictTime = timeSlot;
                        SwingUtilities.invokeLater(() ->
                                showMessage(conflictTime + "는 이미 예약되어 있습니다.")
                        );
                        return;
                    }
                }

                // 예약 요청 전송
                boolean allSuccess = true;
                for (int hour = startHour; hour <= endHour; hour++) {
                    String timeSlot = ReservationUtil.formatTimeSlot(hour);
                    String response = ReservationUtil.sendReservationRequestToServer(
                            data.userName, data.room, data.dateString,
                            data.day, timeSlot, data.purpose, data.userRole, data.studentCount
                    );

                    if (!"RESERVE_SUCCESS".equals(response)) {
                        allSuccess = false;
                        final String failedTime = timeSlot;
                        SwingUtilities.invokeLater(() ->
                                showMessage(failedTime + " 예약에 실패했습니다: " + response)
                        );
                        break;
                    }
                }

                // 예약 완료 처리
                if (allSuccess) {
                    SwingUtilities.invokeLater(() -> {
                        showReservationSuccessMessage(data);
                        closeView();
                        RoomSelect roomSelect = new RoomSelect();
                        new RoomSelectController(roomSelect);
                        roomSelect.setVisible(true);
                    });
                }
            }
        }).start();
    }

    /**
     * Hook 메서드: 예약 성공 메시지
     * 서브클래스에서 커스터마이징 가능
     */
    protected void showReservationSuccessMessage(ReservationData data) {
        showMessage(String.format(
                "예약 신청이 완료되었습니다!\n\n"
                        + "%s: %s\n"
                        + "날짜: %s\n"
                        + "요일: %s\n"
                        + "시간: %s ~ %s\n"
                        + "사용 인원: %d명\n\n"
                        + "조교의 승인을 기다려주세요.",
                getRoomTypeName(), data.room, data.dateString, data.day,
                data.startTime, data.endTime, data.studentCount
        ));
    }

    /**
     * 예약 리스너 - 템플릿 메서드 호출
     */
    class ReservationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 템플릿 메서드 호출
            processReservation(e);
        }
    }

    protected void refreshReservationAndAvailability(String roomName) {
        new Thread(() -> {
            synchronized (serverLock) {
                try {
                    boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(roomName);

                    java.time.LocalDate selectedDate = getSelectedDate();
                    if (selectedDate == null) {
                        selectedDate = java.time.LocalDate.now().plusDays(1);
                    }
                    java.time.LocalDate weekStart = ReservationUtil.getWeekStart(selectedDate);
                    java.time.LocalDate weekEnd = weekStart.plusDays(6);

                    ReservationUtil.loadWeeklyReservationData(reservedMap, statusMap, roomName, weekStart, weekEnd);

                    String dateString = selectedDate.toString();
                    String day = getSelectedDay();
                    String time = getSelectedTime();
                    int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(roomName, dateString, time);

                    final int finalReservedCapacity = reservedCapacity;
                    final java.time.LocalDate finalWeekStart = weekStart;
                    SwingUtilities.invokeLater(() -> {
                        JTable updatedTable = ReservationUtil.buildCalendarTableWithDates(
                                reservedMap, statusMap, roomName, isAvailable, finalWeekStart);
                        updateCalendarTable(updatedTable);
                        updateCapacityPanelWithData(roomName, day, time, finalReservedCapacity);
                    });
                } catch (Exception ex) {
                    System.err.println("[refreshReservationAndAvailability] 오류: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    public void updateCapacityPanelWithData(String room, String day, String time, int reservedCapacity) {
        ClientClassroomManager mgr = ClientClassroomManager.getInstance();
        common.dto.ClassroomDTO c = mgr.getClassroom(room);

        if (c == null) {
            setCapacityInfoText(getRoomTypeName() + " 정보 없음");
            return;
        }

        int maxAllowed = c.getAllowedCapacity();
        int available = maxAllowed - reservedCapacity;

        String text = String.format(
                "수용인원:%d명 / 예약가능:%d명 (현재예약:%d명)",
                c.capacity, available, reservedCapacity
        );
        setCapacityInfoText(text);
    }

    protected void updateCapacityPanel() {
        String room = getSelectedRoom();
        String dateString = getSelectedDateString();
        String day = getSelectedDay();
        String time = getSelectedTime();

        ClientClassroomManager mgr = ClientClassroomManager.getInstance();
        common.dto.ClassroomDTO c = mgr.getClassroom(room);

        if (c == null) {
            setCapacityInfoText(getRoomTypeName() + " 정보 없음");
            return;
        }

        if (dateString == null) {
            setCapacityInfoText("날짜를 선택해주세요");
            return;
        }

        int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(room, dateString, time);
        int maxAllowed = c.getAllowedCapacity();
        int available = maxAllowed - reservedCapacity;

        String text = String.format(
                "수용인원:%d명 / 예약가능:%d명 (현재예약:%d명)",
                c.capacity, available, reservedCapacity
        );
        setCapacityInfoText(text);
    }

    // ============================================================
    // 추상 메서드 - 서브클래스가 구현
    // ============================================================

    protected abstract String getRoomTypeName();
    protected abstract String getSelectedRoom();
    protected abstract List<String> loadRoomList();
    protected abstract void setRoomList(List<String> rooms);
    protected abstract String getSelectedDateString();
    protected abstract java.time.LocalDate getSelectedDate();
    protected abstract String getSelectedDay();
    protected abstract String getSelectedTime();
    protected abstract String getSelectedEndTime();
    protected abstract String getPurpose();
    protected abstract int getStudentCount();
    protected abstract JButton getBeforeButton();
    protected abstract JComboBox<String> getRoomComboBox();
    protected abstract JComboBox<String> getTimeComboBox();
    protected abstract com.toedter.calendar.JDateChooser getDateChooser();
    protected abstract void resetReservationButtonListener();
    protected abstract void addReservationListener(ActionListener listener);
    protected abstract void showMessage(String message);
    protected abstract void closeView();
    protected abstract void updateCalendarTable(JTable table);
    protected abstract void setCapacityInfoText(String text);

    /**
     * 예약 데이터를 담는 내부 클래스
     */
    protected static class ReservationData {
        public final String dateString;
        public final java.time.LocalDate selectedDate;
        public final String userName;
        public final String room;
        public final String day;
        public final String startTime;
        public final String endTime;
        public final String purpose;
        public final int studentCount;
        public final String userRole;

        public ReservationData(String dateString, java.time.LocalDate selectedDate,
                               String userName, String room, String day,
                               String startTime, String endTime, String purpose,
                               int studentCount, String userRole) {
            this.dateString = dateString;
            this.selectedDate = selectedDate;
            this.userName = userName;
            this.room = room;
            this.day = day;
            this.startTime = startTime;
            this.endTime = endTime;
            this.purpose = purpose;
            this.studentCount = studentCount;
            this.userRole = userRole;
        }
    }
}
