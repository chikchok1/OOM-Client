package Controller;

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
     * 초기화 - 공통 로직
     */
    protected void initialize() {
        setupEventListeners();
        loadInitialData();
    }

    /**
     * 이벤트 리스너 설정 - 공통 로직
     */
    private void setupEventListeners() {
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

    /**
     * 초기 데이터 로드 - 공통 로직
     */
    private void loadInitialData() {
        new Thread(() -> {
            synchronized (serverLock) {
                if (Session.getInstance().isConnected()) {
                    common.manager.ClassroomManager.getInstance().refreshFromServer(
                            Session.getInstance().getOut(), Session.getInstance().getIn()
                    );
                }

                List<String> rooms = loadRoomList();

                if (rooms.isEmpty()) {
                    SwingUtilities.invokeLater(() ->
                            showMessage(getRoomTypeName() + " 목록을 불러올 수 없습니다.\nClassrooms.txt 파일을 확인해주세요.")
                    );
                }

                setRoomList(rooms);

                String selectedRoom = getSelectedRoom();
                boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(selectedRoom);

                java.time.LocalDate selectedDate = getSelectedDate();
                if (selectedDate == null) {
                    selectedDate = java.time.LocalDate.now().plusDays(1);
                }
                java.time.LocalDate weekStart = ReservationUtil.getWeekStart(selectedDate);
                java.time.LocalDate weekEnd = weekStart.plusDays(6);
                ReservationUtil.loadWeeklyReservationData(reservedMap, statusMap, selectedRoom, weekStart, weekEnd);

                String dateString = selectedDate.toString();
                String day = getSelectedDay();
                String time = getSelectedTime();
                int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(selectedRoom, dateString, time);

                final int finalReservedCapacity = reservedCapacity;
                final java.time.LocalDate finalWeekStart = weekStart;
                SwingUtilities.invokeLater(() -> {
                    JTable updatedTable = ReservationUtil.buildCalendarTableWithDates(
                            reservedMap, statusMap, selectedRoom, isAvailable, finalWeekStart);
                    updateCalendarTable(updatedTable);
                    updateCapacityPanelWithData(selectedRoom, day, time, finalReservedCapacity);
                });
            }
        }).start();
    }

    // ============================================================
    // 템플릿 메서드: 예약 프로세스의 알고리즘 골격
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
     */
    private boolean validateDate(ReservationData data) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate tomorrow = today.plusDays(1);

        if (data.selectedDate.isBefore(tomorrow)) {
            showMessage("최소 하루 전에 예약해야 합니다.\n" +
                    data.selectedDate.toString() + "일 사용을 원하시면 " +
                    data.selectedDate.minusDays(1).toString() + "일까지 예약해주세요.");
            return false;
        }

        return true;
    }

    /**
     * 4단계: 시간 검증 (역할별 규칙이 다름 - Hook 메서드)
     * 서브클래스에서 오버라이드 가능하지만, 기본 구현 제공
     */
    protected boolean validateReservationTime(ReservationData data) {
        int startHour = ReservationUtil.parseTimeToHour(data.startTime);
        int endHour = ReservationUtil.parseTimeToHour(data.endTime);

        System.out.println("[시간검증] 시작시간: " + data.startTime + " → " + startHour);
        System.out.println("[시간검증] 종료시간: " + data.endTime + " → " + endHour);

        if (startHour > endHour) {
            showMessage("종료 시간은 시작 시간보다 늦어야 합니다.");
            return false;
        }

        int duration = endHour - startHour + 1;
        System.out.println("[시간검증] 예약 시간: " + duration + "시간 (" + startHour + "교시~" + endHour + "교시)");
        System.out.println("[시간검증] 사용자 역할: " + data.userRole);

        // 역할별 시간 제한 (Hook 메서드 호출)
        return checkRoleBasedTimeLimit(data.userRole, duration);
    }

    /**
     * Hook 메서드: 역할별 시간 제한 체크
     * 서브클래스에서 강의실/실습실별 다른 규칙을 적용할 수 있음
     */
    protected boolean checkRoleBasedTimeLimit(String role, int duration) {
        if (role.equals("학생") && duration > 2) {
            showMessage("학생은 최대 2시간까지만 예약 가능합니다.\n선택: " + duration + "시간");
            return false;
        }
        if (!role.equals("학생") && duration > 3) {
            showMessage("교수는 최대 3시간까지만 예약 가능합니다.\n선택: " + duration + "시간");
            return false;
        }
        return true;
    }

    /**
     * 5단계: 수용 인원 검증 (공통이지만 메시지가 다름 - Hook 메서드)
     */
    protected boolean validateCapacity(ReservationData data) {
        common.manager.ClassroomManager manager = common.manager.ClassroomManager.getInstance();
        
        if (!manager.checkCapacity(data.room, data.studentCount)) {
            common.manager.ClassroomManager.Classroom classroom = manager.getClassroom(data.room);
            
            // Hook 메서드 호출 - 서브클래스에서 메시지 커스터마이징 가능
            showCapacityErrorMessage(classroom, data.studentCount);
            return false;
        }

        return true;
    }

    /**
     * Hook 메서드: 수용 인원 초과 메시지
     * 서브클래스에서 강의실/실습실별 다른 메시지 표시 가능
     */
    protected void showCapacityErrorMessage(common.manager.ClassroomManager.Classroom classroom, int requestedCount) {
        showMessage(String.format(
                "예약 불가!\n\n"
                        + "요청 인원: %d명\n"
                        + "최대 허용: %d명\n\n"
                        + "(이 %s은(는) 수용 인원 %d명의 50%%인 %d명까지만 예약 가능합니다)",
                requestedCount,
                classroom.getAllowedCapacity(),
                getRoomTypeName(),
                classroom.capacity,
                classroom.getAllowedCapacity()
        ));
    }

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

    // ============================================================
    // 기존 메서드들
    // ============================================================

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

    protected void updateCapacityPanelWithData(String room, String day, String time, int reservedCapacity) {
        common.manager.ClassroomManager mgr = common.manager.ClassroomManager.getInstance();
        common.manager.ClassroomManager.Classroom c = mgr.getClassroom(room);

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

        common.manager.ClassroomManager mgr = common.manager.ClassroomManager.getInstance();
        common.manager.ClassroomManager.Classroom c = mgr.getClassroom(room);

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