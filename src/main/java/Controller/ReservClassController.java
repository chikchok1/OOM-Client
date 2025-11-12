package Controller;

import Model.Session;
import View.ReservClassView;
import View.RoomSelect;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.table.*;

public class ReservClassController {

    private ReservClassView view;
    // ✅ Thread-safe: ConcurrentHashMap 사용
    private final Map<String, Set<String>> reservedMap = new ConcurrentHashMap<>();
    // ✅ 서버 통신을 위한 단일 락 객체
    private final Object serverLock = new Object();

    public ReservClassController(ReservClassView view) {
        this.view = view;
        this.view.resetReservationButtonListener();
        this.view.addReservationListener(new ReservationListener());

        this.view.getBeforeButton().addActionListener(e -> {
            view.dispose();
            RoomSelect roomSelect = RoomSelect.getInstance();
            new RoomSelectController(roomSelect);
            roomSelect.setVisible(true);
        });

        // ✅ 먼저 이벤트 리스너 등록
        this.view.getClassComboBox().addActionListener(e -> {
            String newSelectedRoom = view.getSelectedClassRoom();
            refreshReservationAndAvailability(newSelectedRoom);
        });

        this.view.getDayComboBox().addActionListener(e -> updateCapacityPanel());
        this.view.getTimeComboBox().addActionListener(e -> updateCapacityPanel());

        //  날짜 변경 시 수용인원 갱신 (이 코드 추가!)
        this.view.getDateChooser().addPropertyChangeListener("date", evt -> {
String newSelectedRoom = view.getSelectedClassRoom();
    refreshReservationAndAvailability(newSelectedRoom);            
//updateCapacityPanel();
        });

        //  모든 서버 통신을 serverLock으로 보호
        new Thread(() -> {
            synchronized (serverLock) {
                //  ClassroomManager 서버로부터 수용인원 동기화
                if (Session.getInstance().isConnected()) {
                    common.manager.ClassroomManager.getInstance().refreshFromServer(
                            Session.getInstance().getOut(), Session.getInstance().getIn()
                    );
                }

                // 1. 강의실 목록 로드
                view.loadClassrooms();

                // 2. 초기 데이터 로드
                String selectedRoom = view.getSelectedClassRoom();
                boolean isAvailable = checkRoomAvailabilitySync(selectedRoom);
                
                // ✅ 주간 예약 데이터 로드
                java.time.LocalDate selectedDate = view.getSelectedDate();
                if (selectedDate == null) {
                    selectedDate = java.time.LocalDate.now().plusDays(1);
                }
                java.time.LocalDate weekStart = getWeekStart(selectedDate);
                java.time.LocalDate weekEnd = weekStart.plusDays(6);
                loadWeeklyReservationData(selectedRoom, weekStart, weekEnd);

                String dateString = selectedDate.toString();
                String day = view.getSelectedDay();
                String time = view.getSelectedTime();
                int reservedCapacity = getApprovedReservedCountForDate(selectedRoom, dateString, time);

                // 3. UI 업데이트
                final int finalReservedCapacity = reservedCapacity;
                final java.time.LocalDate finalWeekStart = weekStart;
                SwingUtilities.invokeLater(() -> {
                    JTable updatedTable = buildCalendarTableWithDates(selectedRoom, isAvailable, finalWeekStart);
                    view.updateCalendarTable(updatedTable);
                    updateCapacityPanelWithData(selectedRoom, day, time, finalReservedCapacity);
                });
            }
        }).start();
    }

    /**
     * ✅ 강의실 예약 데이터 및 상태를 갱신 (단일 쓰레드에서 순차 실행)
     */
    protected void refreshReservationAndAvailability(String roomName) {
        new Thread(() -> {
            synchronized (serverLock) {  // ✅ 모든 서버 통신을 순차적으로 실행
                try {
                    // 1. 강의실 상태 확인
                    boolean isAvailable = checkRoomAvailabilitySync(roomName);

                    // 2. 선택한 날짜의 주간 시작일/종료일 계산
                    java.time.LocalDate selectedDate = view.getSelectedDate();
                    if (selectedDate == null) {
                        selectedDate = java.time.LocalDate.now().plusDays(1);
                    }
                    java.time.LocalDate weekStart = getWeekStart(selectedDate);
                    java.time.LocalDate weekEnd = weekStart.plusDays(6);

                    // 3. 해당 주의 예약 데이터만 로드
                    loadWeeklyReservationData(roomName, weekStart, weekEnd);

                    // 4. 수용인원 정보 미리 가져오기
                    String dateString = selectedDate.toString();
                    String day = view.getSelectedDay();
                    String time = view.getSelectedTime();
                    int reservedCapacity = getApprovedReservedCountForDate(roomName, dateString, time);

                    // 5. UI 업데이트 (EDT에서 실행)
                    final int finalReservedCapacity = reservedCapacity;
                    final java.time.LocalDate finalWeekStart = weekStart;
                    SwingUtilities.invokeLater(() -> {
                        JTable updatedTable = buildCalendarTableWithDates(roomName, isAvailable, finalWeekStart);
                        view.updateCalendarTable(updatedTable);
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
     * ✅ 주의 시작일 계산 (월요일 기준)
     */
    private java.time.LocalDate getWeekStart(java.time.LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        int daysToSubtract = dayOfWeek.getValue() - 1; // 월요일=1
        return date.minusDays(daysToSubtract);
    }

    private boolean checkRoomAvailabilitySync(String classRoom) {
        try {
            // 1. 세션 연결 상태 확인
            if (!Session.getInstance().isConnected()) {
                System.err.println("[checkRoomAvailabilitySync] 서버 연결 없음 - 기본값 true 반환");
                return true;
            }

            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            if (out == null || in == null) {
                System.err.println("[checkRoomAvailabilitySync] 통신 객체가 null - 기본값 true 반환");
                return true;
            }

            // 2. 버퍼에 남은 이전 메시지 비우기 (중요!)
            try {
                Socket socket = Session.getInstance().getSocket();
                if (socket != null) {
                    socket.setSoTimeout(100); // 100ms 타임아웃
                    while (in.ready()) {
                        String discarded = in.readLine();
                        System.out.println("[checkRoomAvailabilitySync] 버퍼에서 버린 메시지: " + discarded);
                    }
                    socket.setSoTimeout(5000); // 다시 5초로 설정
                }
            } catch (Exception e) {
                // 버퍼 비우기 실패는 무시
            }

            // 3. 강의실 이름 정제
            String cleanClassRoom = classRoom.replace(" ", "").trim();
            if (!cleanClassRoom.endsWith("호")) {
                cleanClassRoom = cleanClassRoom + "호";
            }

            // 4. 서버에 명령 전송
            String command = "CHECK_ROOM_STATUS," + cleanClassRoom;
            out.println(command);
            out.flush();
            System.out.println("[checkRoomAvailabilitySync] 서버 전송: " + command);

            // 5. 응답 대기
            String response = null;
            try {
                response = in.readLine();
                System.out.println("[checkRoomAvailabilitySync] 서버 응답: " + response);
            } catch (java.net.SocketTimeoutException e) {
                System.err.println("[checkRoomAvailabilitySync] 타임아웃 - 기본값 true 반환");
                return true;
            }

            if (response == null || response.isEmpty()) {
                System.err.println("[checkRoomAvailabilitySync] 응답 없음 - 기본값 true 반환");
                return true;
            }

            // 6. SUCCESS 응답 처리 (로그인 잔류 응답인 경우)
            if ("SUCCESS".equals(response)) {
                System.out.println("[checkRoomAvailabilitySync] SUCCESS는 잔류 응답 - 재시도");
                // 한 번 더 시도
                out.println(command);
                out.flush();
                response = in.readLine();
                System.out.println("[checkRoomAvailabilitySync] 재시도 응답: " + response);
            }

            // 7. 최종 응답 처리
            switch (response) {
                case "AVAILABLE":
                    System.out.println("[checkRoomAvailabilitySync] " + cleanClassRoom + " - 사용 가능");
                    return true;

                case "UNAVAILABLE":
                    System.out.println("[checkRoomAvailabilitySync] " + cleanClassRoom + " - 사용 불가!");
                    return false;

                default:
                    System.err.println("[checkRoomAvailabilitySync] 알 수 없는 응답: " + response);
                    return true;
            }

        } catch (IOException e) {
            System.err.println("[checkRoomAvailabilitySync] IOException: " + e.getMessage());
            return true;
        } catch (Exception e) {
            System.err.println("[checkRoomAvailabilitySync] 예외: " + e.getMessage());
            return true;
        }
    }

    class ReservationListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // ✅ 날짜 선택 검증
            String selectedDateString = view.getSelectedDateString();
            if (selectedDateString == null) {
                view.showMessage("예약 날짜를 선택해주세요.");
                return;
            }

            // ✅ 최소 하루 전 예약 검증
            java.time.LocalDate selectedDate = view.getSelectedDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate tomorrow = today.plusDays(1);
            
            if (selectedDate.isBefore(tomorrow)) {
                view.showMessage("최소 하루 전에 예약해야 합니다.\n" + 
                                selectedDate.toString() + "일 사용을 원하시면 " + 
                                selectedDate.minusDays(1).toString() + "일까지 예약해주세요.");
                return;
            }

            String userName = Session.getInstance().getLoggedInUserName();
            String selectedClassRoom = view.getSelectedClassRoom();
            String selectedDay = view.getSelectedDay();
            String selectedTime = view.getSelectedTime();
            String purpose = view.getPurpose();
            int studentCount = view.getStudentCount();

            if (purpose.isEmpty()) {
                view.showMessage("사용 목적을 입력해주세요.");
                return;
            }

            // ✅ 수용 인원 체크
            common.manager.ClassroomManager manager = common.manager.ClassroomManager.getInstance();
            if (!manager.checkCapacity(selectedClassRoom, studentCount)) {
                common.manager.ClassroomManager.Classroom classroom = manager.getClassroom(selectedClassRoom);
                view.showMessage(String.format(
                        "예약 불가!\n\n"
                        + "요청 인원: %d명\n"
                        + "최대 허용: %d명\n\n"
                        + "(이 강의실은 수용 인원 %d명의 50%%인 %d명까지만 예약 가능합니다)",
                        studentCount,
                        classroom.getAllowedCapacity(),
                        classroom.capacity,
                        classroom.getAllowedCapacity()
                ));
                return;
            }

            // ✅ 비동기로 서버 통신
            new Thread(() -> {
                synchronized (serverLock) {
                    boolean isAvailable = checkRoomAvailabilitySync(selectedClassRoom);

                    if (!isAvailable) {
                        SwingUtilities.invokeLater(()
                                -> view.showMessage("선택하신 강의실은 현재 사용 불가능합니다. 관리자에게 문의하세요.")
                        );
                        return;
                    }

                    String userRole = Session.getInstance().getLoggedInUserRole();
                    // ✅ 날짜 정보 포함하여 전송
                    String response = sendReservationRequestToServer(userName, selectedClassRoom, selectedDateString, selectedDay, selectedTime, purpose, userRole, studentCount);

                    SwingUtilities.invokeLater(() -> {
                        if (response.startsWith("CAPACITY_EXCEEDED")) {
                            String[] parts = response.split(":");
                            if (parts.length > 1) {
                                view.showMessage("수용 인원 초과!\n최대 " + parts[1] + "명까지만 예약 가능합니다.");
                            } else {
                                view.showMessage("수용 인원을 초과하여 예약할 수 없습니다.");
                            }
                        } else if ("RESERVE_SUCCESS".equals(response)) {
                            view.showMessage(String.format(
                                    "예약 신청이 완료되었습니다!\n\n"
                                    + "강의실: %s\n"
                                    + "날짜: %s\n"
                                    + "요일: %s\n"
                                    + "시간: %s\n"
                                    + "사용 인원: %d명\n\n"
                                    + "조교의 승인을 기다려주세요.",
                                    selectedClassRoom, selectedDateString, selectedDay, selectedTime, studentCount
                            ));
                            view.closeView();
                            RoomSelect roomSelect = new RoomSelect();
                            new RoomSelectController(roomSelect);
                            roomSelect.setVisible(true);
                        } else if ("RESERVE_CONFLICT".equals(response)) {
                            view.showMessage("해당 시간에 이미 예약이 존재합니다.");
                        } else {
                            view.showMessage("예약 중 오류가 발생했습니다.");
                        }
                    });
                }
            }).start();
        }
    }

    /**
     * ✅ 예약 요청 전송 (serverLock 내에서만 호출)
     */
    protected String sendReservationRequestToServer(String name, String room, String dateString, String day, String time, String purpose, String role, int studentCount) {
        // ✅ 날짜 정보 포함
        String requestLine = String.join(",", "RESERVE_REQUEST", name, room, dateString, day, time, purpose, role, String.valueOf(studentCount));

        if (!Session.getInstance().isConnected()) {
            System.err.println("[sendReservationRequestToServer] 서버 연결 없음");
            return "RESERVE_FAILED";
        }

        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();

        try {
            out.println(requestLine);
            out.flush();

            String response = in.readLine();
            if (response == null) {
                System.err.println("[sendReservationRequestToServer] 서버 응답 없음");
                return "RESERVE_FAILED";
            }

            System.out.println("[sendReservationRequestToServer] 서버 응답: " + response);
            return response;
        } catch (IOException e) {
            System.err.println("[sendReservationRequestToServer] 오류: " + e.getMessage());
            return "RESERVE_FAILED";
        }
    }

    /**
     *  주간 예약 데이터 로드 (날짜 기반)
     */
    protected void loadWeeklyReservationData(String roomName, java.time.LocalDate weekStart, java.time.LocalDate weekEnd) {
        if (!Session.getInstance().isConnected()) {
            System.err.println("[loadWeeklyReservationData] 서버 연결 없음");
            return;
        }

        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();

        if (out == null || in == null) {
            System.err.println("[loadWeeklyReservationData] 입출력 스트림이 null");
            return;
        }

        try {
            String normalizedRoom = roomName.endsWith("호") ? roomName : roomName + "호";
            // ✅ Thread-safe Set으로 초기화
            reservedMap.put(normalizedRoom, ConcurrentHashMap.newKeySet());

            System.out.printf("[loadWeeklyReservationData] %s %s ~ %s 예약 정보 요청%n", 
                normalizedRoom, weekStart.toString(), weekEnd.toString());

            // 주간 예약 조회 요청
            out.println(String.format("VIEW_WEEKLY_RESERVATION,%s,%s,%s", 
                roomName, weekStart.toString(), weekEnd.toString()));
            out.flush();

            String line;
            int readCount = 0;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_OF_RESERVATION")) {
                    break;
                }

                String[] parts = line.split(",");
                // ✅ 9개 필드로 수정 (name,room,date,day,time,purpose,role,status,studentCount)
                if (parts.length >= 9) {
                    String status = parts[7].trim();
                    if (status.equals("예약됨") || status.equals("대기")) {
                        String room = parts[1].trim();
                        String dateString = parts[2].trim();  // 날짜
                        String day = parts[3].trim().replace("요일", "");  // 요일
                        String time = parts[4].trim();
                        if (time.length() >= 3) {
                            time = time.substring(0, 3);
                        }

                        room = room.endsWith("호") ? room : room + "호";
                        // ✅ Thread-safe: ConcurrentHashMap.newKeySet() 사용
                        reservedMap.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet())
                            .add(dateString + "_" + day + "_" + time);
                        readCount++;
                    }
                }
            }

            System.out.printf("[loadWeeklyReservationData] %s - %d개 완료%n", normalizedRoom, readCount);

        } catch (IOException e) {
            System.err.println("[loadWeeklyReservationData] 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }



    /**
     * ✅ 날짜 기반 예약 여부 확인
     */
    private boolean isReservedOnDate(String room, java.time.LocalDate date, String time) {
        if (!room.endsWith("호")) {
            room = room + "호";
        }

        time = time.length() >= 3 ? time.substring(0, 3) : time;
        
        // 요일 계산
        String[] dayNames = {"월", "화", "수", "목", "금", "토", "일"};
        int dayOfWeek = date.getDayOfWeek().getValue();
        String day = dayNames[dayOfWeek - 1];
        
        String key = date.toString() + "_" + day + "_" + time;
        Set<String> reservedTimes = reservedMap.get(room);
        
        boolean result = reservedTimes != null && reservedTimes.contains(key);
        if (result) {
            System.out.printf("[isReservedOnDate] %s %s %s = 예약됨%n", room, date, time);
        }
        return result;
    }



    /**
     * ✅ 날짜가 포함된 캘린더 테이블 생성
     */
    private JTable buildCalendarTableWithDates(String room, boolean roomAvailable, java.time.LocalDate weekStart) {
        String[] columnNames = {"교시", "월", "화", "수", "목", "금"};
        String[] times = {"1교시", "2교시", "3교시", "4교시", "5교시", "6교시", "7교시", "8교시", "9교시"};

        // 각 요일의 날짜 계산
        java.time.LocalDate[] weekDates = new java.time.LocalDate[5];
        for (int i = 0; i < 5; i++) {
            weekDates[i] = weekStart.plusDays(i);
        }

        DefaultTableModel model = new DefaultTableModel(times.length, columnNames.length);
        
        // 헤더에 날짜 포함
        String[] headerWithDates = new String[columnNames.length];
        headerWithDates[0] = "교시";
        for (int i = 1; i < columnNames.length; i++) {
            java.time.LocalDate date = weekDates[i - 1];
            headerWithDates[i] = String.format("%s\n%02d/%02d", 
                columnNames[i], date.getMonthValue(), date.getDayOfMonth());
        }
        model.setColumnIdentifiers(headerWithDates);

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(Color.GRAY);

        TableColumn firstColumn = table.getColumnModel().getColumn(0);
        firstColumn.setPreferredWidth(60);
        firstColumn.setMaxWidth(60);
        firstColumn.setMinWidth(60);

        for (int i = 0; i < times.length; i++) {
            model.setValueAt(times[i], i, 0);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (column == 0) {
                    cell.setBackground(Color.LIGHT_GRAY);
                    cell.setHorizontalAlignment(JLabel.CENTER);
                    cell.setText(value != null ? value.toString() : "");
                } else {
                    if (!roomAvailable) {
                        cell.setBackground(Color.DARK_GRAY);
                        cell.setText("X");
                        cell.setForeground(Color.WHITE);
                    } else {
                        java.time.LocalDate date = weekDates[column - 1];
                        String time = times[row];
                        if (isReservedOnDate(room, date, time)) {
                            cell.setBackground(Color.RED);
                            cell.setText("");
                        } else {
                            cell.setBackground(Color.WHITE);
                            cell.setText("");
                        }
                    }
                }
                return cell;
            }
        });

        return table;
    }



    /**
     * ✅ 날짜 기반 승인된 예약 인원 수 조회
     */
    private int getApprovedReservedCountForDate(String room, String dateString, String time) {
        if (!Session.getInstance().isConnected()) {
            System.err.println("[getApprovedReservedCountForDate] 서버 연결 없음");
            return 0;
        }

        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();

        try {
            out.println(String.format("GET_RESERVED_COUNT_BY_DATE,%s,%s,%s", room, dateString, time));
            out.flush();

            String response = in.readLine();
            if (response != null && response.startsWith("RESERVED_COUNT:")) {
                int count = Integer.parseInt(response.substring("RESERVED_COUNT:".length()));
                System.out.println(String.format("[getApprovedReservedCountForDate] %s %s %s = %d명", room, dateString, time, count));
                return count;
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[getApprovedReservedCountForDate] 오류: " + e.getMessage());
        }

        return 0;
    }





    /**
     * ✅ 수용인원 정보 업데이트 (데이터 전달 방식)
     */
    private void updateCapacityPanelWithData(String room, String day, String time, int reservedCapacity) {
        common.manager.ClassroomManager mgr = common.manager.ClassroomManager.getInstance();
        common.manager.ClassroomManager.Classroom c = mgr.getClassroom(room);

        if (c == null) {
            view.setCapacityInfoText("강의실 정보 없음");
            return;
        }

        int maxAllowed = c.getAllowedCapacity();
        int available = maxAllowed - reservedCapacity;

        String text = String.format(
                "수용인원:%d명 / 예약가능:%d명 (현재예약:%d명)",
                c.capacity, available, reservedCapacity
        );
        view.setCapacityInfoText(text);

        System.out.printf("[updateCapacityPanelWithData] %s %s %s → 총:%d, 허용:%d, 예약:%d, 남음:%d%n",
                room, day, time, c.capacity, maxAllowed, reservedCapacity, available);
    }

    /**
     * ✅ 수용인원 정보 업데이트 (날짜 기반)
     */
    private void updateCapacityPanel() {
        String room = view.getSelectedClassRoom();
        String dateString = view.getSelectedDateString();
        String day = view.getSelectedDay();
        String time = view.getSelectedTime();

        System.out.println("[updateCapacityPanel] 선택된 날짜: " + dateString +
                       ", 요일: " + day + ", 시간: " + time);
         
        common.manager.ClassroomManager mgr = common.manager.ClassroomManager.getInstance();
        common.manager.ClassroomManager.Classroom c = mgr.getClassroom(room);

        if (c == null) {
            view.setCapacityInfoText("강의실 정보 없음");
            return;
        }

        // 날짜가 선택되지 않은 경우
        if (dateString == null) {
            view.setCapacityInfoText("날짜를 선택해주세요");
            return;
        }

        // 서버에서 해당 날짜의 예약 인원 조회
        int reservedCapacity = getApprovedReservedCountForDate(room, dateString, time);
        int maxAllowed = c.getAllowedCapacity();
        int available = maxAllowed - reservedCapacity;

        String text = String.format(
                "수용인원:%d명 / 예약가능:%d명 (현재예약:%d명)",
                c.capacity, available, reservedCapacity
        );
        view.setCapacityInfoText(text);

        System.out.printf("[updateCapacityPanel] %s %s %s → 총:%d, 허용:%d, 예약:%d, 남음:%d%n",
                room, dateString, time, c.capacity, maxAllowed, reservedCapacity, available);
    }
}
