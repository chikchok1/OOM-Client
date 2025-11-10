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
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.*;

public class ReservClassController {
 
    private ReservClassView view;
    private Map<String, Set<String>> reservedMap = new HashMap<>();
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
        
        // ✅ 모든 서버 통신을 serverLock으로 보호
        new Thread(() -> {
            synchronized (serverLock) {
                // ✅ ClassroomManager 서버로부터 수용인원 동기화
                if (Session.isConnected()) {
                    common.manager.ClassroomManager.getInstance().refreshFromServer(
                        Session.getOut(), Session.getIn()
                    );
                }
                
                // 1. 강의실 목록 로드
                view.loadClassrooms();
                
                // 2. 초기 데이터 로드
                String selectedRoom = view.getSelectedClassRoom();
                boolean isAvailable = checkRoomAvailabilitySync(selectedRoom);
                loadReservationDataFromServer(selectedRoom);
                
                String day = view.getSelectedDay();
                String time = view.getSelectedTime();
                int reservedCapacity = getApprovedReservedCountInternal(selectedRoom, day, time);
                
                // 3. UI 업데이트
                final int finalReservedCapacity = reservedCapacity;
                SwingUtilities.invokeLater(() -> {
                    JTable updatedTable = buildCalendarTable(selectedRoom, isAvailable);
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
                    
                    // 2. 예약 데이터 로드
                    loadReservationDataFromServer(roomName);
                    
                    // 3. 수용인원 정보 미리 가져오기
                    String day = view.getSelectedDay();
                    String time = view.getSelectedTime();
                    int reservedCapacity = getApprovedReservedCountInternal(roomName, day, time);
                    
                    // 4. UI 업데이트 (EDT에서 실행)
                    final int finalReservedCapacity = reservedCapacity;
                    SwingUtilities.invokeLater(() -> {
                        JTable updatedTable = buildCalendarTable(roomName, isAvailable);
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
     * ✅ 강의실 사용 가능 여부 확인 (동기 방식)
     * ⚠️ 임시로 항상 true 반환 (CHECK_ROOM_STATUS 응답 문제 회피)
     */
    /*
    private boolean checkRoomAvailabilitySync(String classRoom) {
        // ✅ 항상 사용 가능하다고 가정
        System.out.println("[checkRoomAvailabilitySync] " + classRoom + " - 항상 AVAILABLE 반환");
        return true;
        */
// ReservClassController.java의 checkRoomAvailabilitySync 메소드를 다음으로 교체하세요:

/**
 * 강의실 사용 가능 여부 확인 (동기 방식)
 * CHECK_ROOM_STATUS 명령을 서버에 보내고 응답을 받아 처리
 */
// ReservClassController.java의 checkRoomAvailabilitySync 최종 수정 버전

private boolean checkRoomAvailabilitySync(String classRoom) {
    try {
        // 1. 세션 연결 상태 확인
        if (!Session.isConnected()) {
            System.err.println("[checkRoomAvailabilitySync] 서버 연결 없음 - 기본값 true 반환");
            return true;
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();
        
        if (out == null || in == null) {
            System.err.println("[checkRoomAvailabilitySync] 통신 객체가 null - 기본값 true 반환");
            return true;
        }

        // 2. 버퍼에 남은 이전 메시지 비우기 (중요!)
        try {
            Socket socket = Session.getSocket();
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
        /* ⚠️ CHECK_ROOM_STATUS 응답에 문제가 있어 임시 비활성화
        try {
            if (!Session.isConnected()) {
                System.err.println("[checkRoomAvailabilitySync] 서버 연결 없음");
                return false;
            }

            PrintWriter out = Session.getOut();
            BufferedReader in = Session.getIn();

            String cleanClassRoom = classRoom.replace("호", "").trim();
            out.println("CHECK_ROOM_STATUS," + cleanClassRoom);
            out.flush();

            String response = in.readLine();
            if (response == null) {
                System.err.println("[checkRoomAvailabilitySync] 서버 응답 없음");
                return false;
            }

            return "AVAILABLE".equals(response);
        } catch (IOException e) {
            System.err.println("[checkRoomAvailabilitySync] 오류: " + e.getMessage());
            return false;
        }
}
        */
    

    class ReservationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String userName = Session.getLoggedInUserName();
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
                    "예약 불가!\n\n" +
                    "요청 인원: %d명\n" +
                    "최대 허용: %d명\n\n" +
                    "(이 강의실은 수용 인원 %d명의 50%%인 %d명까지만 예약 가능합니다)",
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
                        SwingUtilities.invokeLater(() -> 
                            view.showMessage("선택하신 강의실은 현재 사용 불가능합니다. 관리자에게 문의하세요.")
                        );
                        return;
                    }

                    String userRole = Session.getLoggedInUserRole();
                    String response = sendReservationRequestToServer(userName, selectedClassRoom, selectedDay, selectedTime, purpose, userRole, studentCount);

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
                                "예약 신청이 완료되었습니다!\n\n" +
                                "강의실: %s\n" +
                                "요일: %s\n" +
                                "시간: %s\n" +
                                "사용 인원: %d명\n\n" +
                                "조교의 승인을 기다려주세요.",
                                selectedClassRoom, selectedDay, selectedTime, studentCount
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
    protected String sendReservationRequestToServer(String name, String room, String day, String time, String purpose, String role, int studentCount) {
        String requestLine = String.join(",", "RESERVE_REQUEST", name, room, day, time, purpose, role, String.valueOf(studentCount));

        if (!Session.isConnected()) {
            System.err.println("[sendReservationRequestToServer] 서버 연결 없음");
            return "RESERVE_FAILED";
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

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
     * ✅ 서버에서 예약 데이터 로드 (serverLock 내에서만 호출)
     */
    protected void loadReservationDataFromServer(String roomName) {
        if (!Session.isConnected()) {
            System.err.println("[loadReservationDataFromServer] 서버 연결 없음");
            return;
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

        if (out == null || in == null) {
            System.err.println("[loadReservationDataFromServer] 입출력 스트림이 null");
            return;
        }

        try {
            String normalizedRoom = roomName.endsWith("호") ? roomName : roomName + "호";
            
            // ✅ 현재 강의실 데이터만 초기화
            reservedMap.put(normalizedRoom, new HashSet<>());

            System.out.println("[loadReservationDataFromServer] " + normalizedRoom + " 예약 정보 요청");

            out.println("VIEW_RESERVATION," + Session.getLoggedInUserId() + "," + roomName);
            out.flush();

            String line;
            int readCount = 0;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_OF_RESERVATION")) break;

                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    String status = parts[6].trim();
                    if (status.equals("예약됨") || status.equals("대기")) {
                        String room = parts[1].trim();
                        String day = parts[2].trim().replace("요일", "");
                        String time = parts[3].trim();
                        if (time.length() >= 3) {
                            time = time.substring(0, 3);
                        }

                        room = room.endsWith("호") ? room : room + "호";
                        reservedMap.computeIfAbsent(room, k -> new HashSet<>()).add(day + "_" + time);
                        readCount++;
                    }
                }
            }

            System.out.println("[loadReservationDataFromServer] " + normalizedRoom + " - " + readCount + "개 완료");

        } catch (IOException e) {
            System.err.println("[loadReservationDataFromServer] 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isReserved(String room, String day, String time) {
        if (!room.endsWith("호")) {
            room = room + "호";
        }

        time = time.length() >= 3 ? time.substring(0, 3) : time;
        String key = day + "_" + time;
        Set<String> reservedTimes = reservedMap.get(room);
        return reservedTimes != null && reservedTimes.contains(key);
    }

    private JTable buildCalendarTable(String room, boolean roomAvailable) {
        String[] columnNames = {"교시", "월", "화", "수", "목", "금"};
        String[] times = {"1교시", "2교시", "3교시", "4교시", "5교시", "6교시", "7교시", "8교시", "9교시"};

        DefaultTableModel model = new DefaultTableModel(times.length, columnNames.length);
        model.setColumnIdentifiers(columnNames);

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
                        String day = columnNames[column];
                        String time = times[row];
                        if (isReserved(room, day, time)) {
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
     *  서버에서 승인된 예약 인원 수 조회 (serverLock 내부용)
     */
    private int getApprovedReservedCountInternal(String room, String day, String time) {
        // ✅ 이미 serverLock 안에 있으므로 synchronized 불필요
        if (!Session.isConnected()) {
            System.err.println("[getApprovedReservedCountInternal] 서버 연결 없음");
            return 0;
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

        try {
            out.println(String.format("GET_RESERVED_COUNT,%s,%s,%s", room, day, time));
            out.flush();

            String response = in.readLine();
            if (response != null && response.startsWith("RESERVED_COUNT:")) {
                int count = Integer.parseInt(response.substring("RESERVED_COUNT:".length()));
                System.out.println(String.format("[getApprovedReservedCountInternal] %s %s %s = %d명", room, day, time, count));
                return count;
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[getApprovedReservedCountInternal] 오류: " + e.getMessage());
        }

        return 0;
    }

    /**
     * ✅ 서버에서 승인된 예약 인원 수 조회 (EDT용)
     */
    private int getApprovedReservedCount(String room, String day, String time) {
        synchronized (serverLock) {  // ✅ 서버 통신 락
            return getApprovedReservedCountInternal(room, day, time);
        }
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
     * ✅ 수용인원 정보 업데이트
     */
    private void updateCapacityPanel() {
        String room = view.getSelectedClassRoom();
        String day = view.getSelectedDay();
        String time = view.getSelectedTime();

        common.manager.ClassroomManager mgr = common.manager.ClassroomManager.getInstance();
        common.manager.ClassroomManager.Classroom c = mgr.getClassroom(room);

        if (c == null) {
            view.setCapacityInfoText("강의실 정보 없음");
            return;
        }

        int reservedCapacity = getApprovedReservedCount(room, day, time);
        int maxAllowed = c.getAllowedCapacity();
        int available = maxAllowed - reservedCapacity;

        String text = String.format(
            "수용인원:%d명 / 예약가능:%d명 (현재예약:%d명)",
            c.capacity, available, reservedCapacity
        );
        view.setCapacityInfoText(text);

        System.out.printf("[updateCapacityPanel] %s %s %s → 총:%d, 허용:%d, 예약:%d, 남음:%d%n",
                room, day, time, c.capacity, maxAllowed, reservedCapacity, available);
    }
}
