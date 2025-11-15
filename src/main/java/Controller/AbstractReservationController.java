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
 * 강의실 및 실습실 예약 컨트롤러의 추상 클래스
 * 공통 로직을 구현하고, 서브클래스에서 차이점만 구현하도록 함
 */
public abstract class AbstractReservationController {

    protected final Map<String, Set<String>> reservedMap = new ConcurrentHashMap<>();
    protected final Object serverLock = new Object();

    /**
     * 생성자 - 공통 초기화 로직
     */
    protected void initialize() {
        // 예약 버튼 리스너 초기화
        resetReservationButtonListener();
        addReservationListener(new ReservationListener());

        // 뒤로가기 버튼
        getBeforeButton().addActionListener(e -> {
            closeView();
            RoomSelect roomSelect = RoomSelect.getInstance();
            new RoomSelectController(roomSelect);
            roomSelect.setVisible(true);
        });

        // 방 선택 콤보박스
        getRoomComboBox().addActionListener(e -> {
            String newSelectedRoom = getSelectedRoom();
            refreshReservationAndAvailability(newSelectedRoom);
        });

        // 시간 선택 콤보박스
        getTimeComboBox().addActionListener(e -> updateCapacityPanel());

        // 날짜 선택
        getDateChooser().addPropertyChangeListener("date", evt -> {
            String newSelectedRoom = getSelectedRoom();
            refreshReservationAndAvailability(newSelectedRoom);            
        });

        // 비동기 초기화
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
                ReservationUtil.loadWeeklyReservationData(reservedMap, selectedRoom, weekStart, weekEnd);

                String dateString = selectedDate.toString();
                String day = getSelectedDay();
                String time = getSelectedTime();
                int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(selectedRoom, dateString, time);

                final int finalReservedCapacity = reservedCapacity;
                final java.time.LocalDate finalWeekStart = weekStart;
                SwingUtilities.invokeLater(() -> {
                    JTable updatedTable = ReservationUtil.buildCalendarTableWithDates(
                        reservedMap, selectedRoom, isAvailable, finalWeekStart);
                    updateCalendarTable(updatedTable);
                    updateCapacityPanelWithData(selectedRoom, day, time, finalReservedCapacity);
                });
            }
        }).start();
    }

    /**
     * 예약 정보 새로고침
     */
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

                    ReservationUtil.loadWeeklyReservationData(reservedMap, roomName, weekStart, weekEnd);

                    String dateString = selectedDate.toString();
                    String day = getSelectedDay();
                    String time = getSelectedTime();
                    int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(roomName, dateString, time);

                    final int finalReservedCapacity = reservedCapacity;
                    final java.time.LocalDate finalWeekStart = weekStart;
                    SwingUtilities.invokeLater(() -> {
                        JTable updatedTable = ReservationUtil.buildCalendarTableWithDates(
                            reservedMap, roomName, isAvailable, finalWeekStart);
                        updateCalendarTable(updatedTable);
                        updateCapacityPanelWithData(roomName, day, time, finalReservedCapacity);
                    });
                } catch (Exception e) {
                    System.err.println("[refreshReservationAndAvailability] 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 예약 리스너 - 공통 로직
     */
    class ReservationListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String selectedDateString = getSelectedDateString();
            if (selectedDateString == null) {
                showMessage("예약 날짜를 선택해주세요.");
                return;
            }

            java.time.LocalDate selectedDate = getSelectedDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate tomorrow = today.plusDays(1);
            
            if (selectedDate.isBefore(tomorrow)) {
                showMessage("최소 하루 전에 예약해야 합니다.\n" + 
                                selectedDate.toString() + "일 사용을 원하시면 " + 
                                selectedDate.minusDays(1).toString() + "일까지 예약해주세요.");
                return;
            }

            String userName = Session.getInstance().getLoggedInUserName();
            String selectedRoom = getSelectedRoom();
            String selectedDay = getSelectedDay();
            String selectedStartTime = getSelectedTime();
            String selectedEndTime = getSelectedEndTime();
            String purpose = getPurpose();
            int studentCount = getStudentCount();

            if (purpose.isEmpty()) {
                showMessage("사용 목적을 입력해주세요.");
                return;
            }

            int startHour = ReservationUtil.parseTimeToHour(selectedStartTime);
            int endHour = ReservationUtil.parseTimeToHour(selectedEndTime);
            
            System.out.println("[시간검증] 시작시간: " + selectedStartTime + " → " + startHour);
            System.out.println("[시간검증] 종료시간: " + selectedEndTime + " → " + endHour);

            if (startHour > endHour) {
                showMessage("종료 시간은 시작 시간보다 늦어야 합니다.");
                return;
            }

            int duration = endHour - startHour + 1;
            System.out.println("[시간검증] 예약 시간: " + duration + "시간 (" + startHour + "교시~" + endHour + "교시)");

            String userRole = Session.getInstance().getLoggedInUserRole();
            System.out.println("[시간검증] 사용자 역할: " + userRole);
            
            if (userRole.equals("학생") && duration > 2) {
                showMessage("학생은 최대 2시간까지만 예약 가능합니다.\n선택: " + duration + "시간");
                return;
            }
            if (!userRole.equals("학생") && duration > 3) {
                showMessage("교수/세미나/학회는 최대 3시간까지만 예약 가능합니다.\n선택: " + duration + "시간");
                return;
            }

            common.manager.ClassroomManager manager = common.manager.ClassroomManager.getInstance();
            if (!manager.checkCapacity(selectedRoom, studentCount)) {
                common.manager.ClassroomManager.Classroom classroom = manager.getClassroom(selectedRoom);
                showMessage(String.format(
                        "예약 불가!\n\n"
                        + "요청 인원: %d명\n"
                        + "최대 허용: %d명\n\n"
                        + "(이 " + getRoomTypeName() + "은(는) 수용 인원 %d명의 50%%인 %d명까지만 예약 가능합니다)",
                        studentCount,
                        classroom.getAllowedCapacity(),
                        classroom.capacity,
                        classroom.getAllowedCapacity()
                ));
                return;
            }

            new Thread(() -> {
                synchronized (serverLock) {
                    boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(selectedRoom);

                    if (!isAvailable) {
                        SwingUtilities.invokeLater(()
                                -> showMessage("선택하신 " + getRoomTypeName() + "은(는) 현재 사용 불가능합니다. 관리자에게 문의하세요.")
                        );
                        return;
                    }

                    for (int hour = startHour; hour <= endHour; hour++) {
                        String timeSlot = hour + "교시";
                        if (ReservationUtil.isReservedOnDate(reservedMap, selectedRoom, selectedDate, timeSlot)) {
                            final String conflictTime = timeSlot;
                            SwingUtilities.invokeLater(() -> 
                                showMessage(conflictTime + "는 이미 예약되어 있습니다.")
                            );
                            return;
                        }
                    }

                    boolean allSuccess = true;
                    for (int hour = startHour; hour <= endHour; hour++) {
                        String timeSlot = ReservationUtil.formatTimeSlot(hour);
                        String response = ReservationUtil.sendReservationRequestToServer(
                            userName, selectedRoom, selectedDateString, 
                            selectedDay, timeSlot, purpose, userRole, studentCount
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

                    if (allSuccess) {
                        SwingUtilities.invokeLater(() -> {
                            showMessage(String.format(
                                    "예약 신청이 완료되었습니다!\n\n"
                                    + getRoomTypeName() + ": %s\n"
                                    + "날짜: %s\n"
                                    + "요일: %s\n"
                                    + "시간: %s ~ %s\n"
                                    + "사용 인원: %d명\n\n"
                                    + "조교의 승인을 기다려주세요.",
                                    selectedRoom, selectedDateString, selectedDay, 
                                    selectedStartTime, selectedEndTime, studentCount
                            ));
                            closeView();
                            RoomSelect roomSelect = new RoomSelect();
                            new RoomSelectController(roomSelect);
                            roomSelect.setVisible(true);
                        });
                    }
                }
            }).start();
        }
    }

    /**
     * 수용 인원 패널 업데이트
     */
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

        System.out.printf("[updateCapacityPanelWithData] %s %s %s → 총:%d, 허용:%d, 예약:%d, 남음:%d%n",
                room, day, time, c.capacity, maxAllowed, reservedCapacity, available);
    }

    /**
     * 수용 인원 패널 업데이트
     */
    protected void updateCapacityPanel() {
        String room = getSelectedRoom();
        String dateString = getSelectedDateString();
        String day = getSelectedDay();
        String time = getSelectedTime();

        System.out.println("[updateCapacityPanel] 선택된 날짜: " + dateString +
                       ", 요일: " + day + ", 시간: " + time);
         
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

        System.out.printf("[updateCapacityPanel] %s %s %s → 총:%d, 허용:%d, 예약:%d, 남음:%d%n",
                room, dateString, time, c.capacity, maxAllowed, reservedCapacity, available);
    }

    // ================= 추상 메서드 - 서브클래스가 구현 =================
    
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
}
