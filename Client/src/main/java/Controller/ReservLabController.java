package Controller;

import Model.Session;
import View.ReservLabView;
import View.RoomSelect;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

public class ReservLabController {
    private ReservLabView view;
    private Map<String, Set<String>> reservedMap = new HashMap<>();
private final Object loadLock = new Object();

    public ReservLabController(ReservLabView view) {
        this.view = view;
        this.view.resetReservationButtonListener();
        this.view.addReservationListener(new ReservationListener());

        this.view.getBeforeButton().addActionListener(e -> {
            view.dispose();
            RoomSelect roomSelect = RoomSelect.getInstance();
            new RoomSelectController(roomSelect);
            roomSelect.setVisible(true);
        });

        String initialRoom = view.getSelectedClassRoom();
        refreshReservationData(initialRoom);
        view.updateCalendarTable(buildCalendarTable(initialRoom));

        this.view.getLabComboBox().addActionListener(e -> {
            String selectedRoom = view.getSelectedClassRoom();
            refreshReservationData(selectedRoom);
            view.updateCalendarTable(buildCalendarTable(selectedRoom));
        });

        // ✅ 콤보박스 변경 시마다 수용인원 갱신
        this.view.getLabComboBox().addActionListener(e -> updateCapacityPanel());
        this.view.getDayComboBox().addActionListener(e -> updateCapacityPanel());
        this.view.getTimeComboBox().addActionListener(e -> updateCapacityPanel());

        // ✅ 초기 표시 (화면 열릴 때 한 번 갱신)
        updateCapacityPanel();
    }

      // ✅ 강의실과 동일한 구조로 예약/인원 동시 갱신
    protected void refreshReservationAndAvailability(String roomName) {
        reservedMap.clear(); // 캐시 초기화
        refreshReservationData(roomName);
        view.updateCalendarTable(buildCalendarTable(roomName));
        updateCapacityPanel();
        System.out.println("[ReservLabController] " + roomName + " 예약 및 인원 데이터 새로고침 완료");
    }
    
 protected void refreshReservationData(String roomName) {
        reservedMap.clear();
        loadReservationDataFromServer(roomName);
    }

   protected void checkRoomAvailability(String room, Consumer<Boolean> callback) {
    new Thread(() -> {
        boolean available = false;
        try {
            // 연결 상태 확인 
            if (!Session.isConnected()) {
                System.err.println("[ReservLabController] 서버 연결이 유효하지 않음");
                SwingUtilities.invokeLater(() -> callback.accept(false));
                return;
            }

            PrintWriter out = Session.getOut();
            BufferedReader in = Session.getIn();

            String cleanRoom = room.replace("호", "").trim();
            out.println("CHECK_ROOM_STATUS," + cleanRoom);
            out.flush();

            String response = in.readLine();
            // null 체크 
            if (response == null) {
                System.err.println("[ReservLabController] 서버 응답 없음");
                available = false;
            } else {
                available = "AVAILABLE".equals(response);
            }
        } catch (IOException e) {
            System.err.println("[ReservLabController] 서버 통신 오류: " + e.getMessage());
        }

        boolean finalAvailable = available;
        SwingUtilities.invokeLater(() -> callback.accept(finalAvailable));
    }).start();
}

   protected void loadReservationDataFromServer(String roomName) {
    synchronized (loadLock) { // ✅ 동기화 블록 시작

        // 연결 상태 확인
        if (!Session.isConnected()) {
            System.err.println("[ReservLabController] 서버 연결이 유효하지 않음");
            return;
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

        if (out == null || in == null) {
            System.err.println("[ReservLabController] 세션 입출력 스트림이 null입니다.");
            return;
        }

        try {
            String normalizedRoom = roomName.endsWith("호") ? roomName : roomName + "호";
            reservedMap.put(normalizedRoom, new HashSet<>());

            System.out.println("[ReservLabController] " + normalizedRoom + " 예약 정보 요청 시작");

            out.println("VIEW_RESERVATION," + Session.getLoggedInUserId() + "," + roomName);
            out.flush();

            String line;
            int count = 0;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_OF_RESERVATION")) break;

                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    String status = parts[6].trim();
                    if (status.equals("예약됨") || status.equals("대기")) {
                        String room = parts[1].trim();
                        String day = parts[2].trim().replace("요일", "");
                        String time = parts[3].trim().substring(0, 3);
                        room = room.endsWith("호") ? room : room + "호";
                        reservedMap.computeIfAbsent(room, k -> new HashSet<>()).add(day + "_" + time);
                        count++;
                    }
                }
            }

            System.out.println("[ReservLabController] " + normalizedRoom + " - " + count + "개의 예약 정보 수신 완료");

        } catch (IOException e) {
            System.err.println("[ReservLabController] 예약 데이터 수신 오류: " + e.getMessage());
        }
    } //  synchronized 종료
}
    private boolean isReserved(String room, String day, String time) {
        if (!room.endsWith("호")) room += "호";
        String key = day + "_" + (time.length() >= 3 ? time.substring(0, 3) : time);
        Set<String> reservedTimes = reservedMap.get(room);
        return reservedTimes != null && reservedTimes.contains(key);
    }

   protected String sendReservationRequestToServer(String name, String room, String day, String time, String purpose, String role) {
    // ✅ 학생 수 추가
    int studentCount = view.getStudentCount();
    String requestLine = String.join(",", "RESERVE_REQUEST", name, room, day, time, purpose, role, String.valueOf(studentCount));

    //  연결 상태 확인 
    if (!Session.isConnected()) {
        System.err.println("[ReservLabController] 서버 연결이 없습니다.");
        return "RESERVE_FAILED";
    }

    PrintWriter out = Session.getOut();
    BufferedReader in = Session.getIn();

    try {
        out.println(requestLine);
        out.flush();
        
        String response = in.readLine();
        //  null 체크 
        if (response == null) {
            System.err.println("[ReservLabController] 서버 응답 없음");
            return "RESERVE_FAILED";
        }
        
        System.out.println("[ReservLabController] 서버 응답: " + response);
        return response;
    } catch (IOException e) {
        System.err.println("[ReservLabController] 예약 요청 전송 중 오류: " + e.getMessage());
        return "RESERVE_FAILED";
    }
}

    class ReservationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String userName = Session.getLoggedInUserName();
            String selectedRoom = view.getSelectedClassRoom();
            String selectedDay = view.getSelectedDay();
            String selectedTime = view.getSelectedTime();
            String purpose = view.getPurpose();
            int studentCount = view.getStudentCount();

            if (purpose.isEmpty()) {
                view.showMessage("사용 목적을 입력해주세요.");
                return;
            }

            // ✅ 수용 인원 체크 추가
            common.manager.ClassroomManager manager = common.manager.ClassroomManager.getInstance();
            if (!manager.checkCapacity(selectedRoom, studentCount)) {
                common.manager.ClassroomManager.Classroom classroom = manager.getClassroom(selectedRoom);
                view.showMessage(String.format(
                    "예약 불가!\n\n" +
                    "요청 인원: %d명\n" +
                    "최대 허용: %d명\n\n" +
                    "(이 실습실은 수용 인원 %d명의 50%%인 %d명까지만 예약 가능합니다)",
                    studentCount,
                    classroom.getAllowedCapacity(),
                    classroom.capacity,
                    classroom.getAllowedCapacity()
                ));
                return;
            }

            checkRoomAvailability(selectedRoom, isAvailable -> {
                if (!isAvailable) {
                    view.showMessage("해당 실습실은 현재 사용 불가능합니다.");
                    return;
                }

                String userRole = Session.getLoggedInUserRole();
                String response = sendReservationRequestToServer(userName, selectedRoom, selectedDay, selectedTime, purpose, userRole);

                // ✅ CAPACITY_EXCEEDED 응답 처리 추가
                if (response.startsWith("CAPACITY_EXCEEDED")) {
                    String[] parts = response.split(":");
                    if (parts.length > 1) {
                        view.showMessage("수용 인원 초과!\n최대 " + parts[1] + "명까지만 예약 가능합니다.");
                    } else {
                        view.showMessage("수용 인원을 초과하여 예약할 수 없습니다.");
                    }
                    return;
                }

                switch (response) {
                    case "RESERVE_SUCCESS":
                        view.showMessage(String.format(
                            "예약 신청이 완료되었습니다!\n\n" +
                            "실습실: %s\n" +
                            "요일: %s\n" +
                            "시간: %s\n" +
                            "사용 인원: %d명\n\n" +
                            "조교의 승인을 기다려주세요.",
                            selectedRoom, selectedDay, selectedTime, studentCount
                        ));
                        view.closeView();
                        RoomSelect rs = new RoomSelect();
                        new RoomSelectController(rs);
                        rs.setVisible(true);
                        break;
                    case "RESERVE_CONFLICT":
                        view.showMessage("이미 해당 시간에 예약이 존재합니다.");
                        break;
                    case "RESERVE_FAILED":
                    default:
                        view.showMessage("예약 중 오류가 발생했습니다.");
                        break;
                }
            });
        }
    }

    // ✅ 서버에 수용 인원 체크 요청
    protected String checkCapacityFromServer(String room, int studentCount) {
        if (!Session.isConnected()) {
            System.err.println("[checkCapacityFromServer] 서버 연결 없음");
            return "ERROR";
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

        try {
            out.println(String.format("CHECK_CAPACITY,%s,%d", room, studentCount));
            out.flush();

            String response = in.readLine();
            if (response == null) {
                System.err.println("[checkCapacityFromServer] 서버 응답 없음");
                return "ERROR";
            }

            System.out.println(String.format("[서버응답] 수용인원 체크: %s", response));
            return response;

        } catch (IOException e) {
            System.err.println("[checkCapacityFromServer] 오류: " + e.getMessage());
            return "ERROR";
        }
    }

    // ✅ 서버에서 승인된 예약 인원 수를 조회
    private int getApprovedReservedCount(String room, String day, String time) {
        if (!Session.isConnected()) {
            System.err.println("[getApprovedReservedCount] 서버 연결 없음");
            return 0;
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

        try {
            // 서버에 예약 인원 수 요청
            out.println(String.format("GET_RESERVED_COUNT,%s,%s,%s", room, day, time));
            out.flush();

            String response = in.readLine();
            if (response != null && response.startsWith("RESERVED_COUNT:")) {
                int count = Integer.parseInt(response.substring("RESERVED_COUNT:".length()));
                System.out.println(String.format("[서버응답] %s %s %s 예약인원: %d명", room, day, time, count));
                return count;
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[getApprovedReservedCount] 오류: " + e.getMessage());
        }

        return 0;
    }

    // ✅ 현재 선택된 (실습실, 요일, 교시)에 따른 수용가능인원 실시간 표시
    private void updateCapacityPanel() {
        String room = view.getSelectedClassRoom();
        String day = view.getSelectedDay();
        String time = view.getSelectedTime();

        common.manager.ClassroomManager mgr = common.manager.ClassroomManager.getInstance();
        common.manager.ClassroomManager.Classroom c = mgr.getClassroom(room);

        if (c == null) {
            view.setCapacityInfoText("실습실 정보 없음");
            return;
        }

        int reservedCapacity = getApprovedReservedCount(room, day, time);
        //  최대 허용 인원(50%)에서 현재 예약 인원을 뺀 값
        int maxAllowed = c.getAllowedCapacity(); // 30명 -> 15명
        int available = maxAllowed - reservedCapacity;

        String text = String.format(
            "수용인원:%d명 / 예약가능:%d명 (현재예약:%d명)",
            c.capacity, available, reservedCapacity
        );
        view.setCapacityInfoText(text);

        System.out.printf("[수용인원체크] %s %s %s → 총:%d명, 허용:%d명, 예약:%d명, 남음:%d명%n",
                room, day, time, c.capacity, c.getAllowedCapacity(), reservedCapacity, available);
    }

    public JTable buildCalendarTable(String room) {
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
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (column == 0) {
                    cell.setBackground(Color.LIGHT_GRAY);
                    cell.setHorizontalAlignment(JLabel.CENTER);
                    cell.setText(value != null ? value.toString() : "");
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

                return cell;
            }
        });

        return table;
    }
}
