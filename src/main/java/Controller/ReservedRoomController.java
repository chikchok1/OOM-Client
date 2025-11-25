package Controller;

import common.model.ReservedRoomModel;
import Model.Session;
import View.ReservedRoomView;
import View.RoomSelect;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JOptionPane;
import javax.swing.JTable;

public class ReservedRoomController {

    private ReservedRoomView view;
    private ReservedRoomModel model;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ReservedRoomController(ReservedRoomView view) {
        this.view = view;
        this.model = new ReservedRoomModel();
        addListeners();
    }

    private void addListeners() {
        // [1] "확인" 버튼 클릭 시
        view.getCheckButton().addActionListener(e -> {
            String selectedRoom = view.getSelectedRoom();
            if (!"선택".equals(selectedRoom)) {
                loadReservedRooms(selectedRoom);
            }
        });

        // [2] 강의실 선택 콤보박스
        view.getClassComboBox().addActionListener(e -> {
            if (view.isUpdating()) {
                return;
            }
            view.setUpdating(true);
            view.resetLabSelection(); // 실습실 초기화
            String selectedRoom = view.getSelectedRoom();
            if (!"선택".equals(selectedRoom)) {
                loadReservedRooms(selectedRoom);
            }
            view.setUpdating(false);
        });

        // [3] 실습실 선택 콤보박스
        view.getLabComboBox().addActionListener(e -> {
            if (view.isUpdating()) {
                return;
            }
            view.setUpdating(true);
            view.resetClassSelection(); // 강의실 초기화
            String selectedRoom = view.getSelectedRoom();
            if (!"선택".equals(selectedRoom)) {
                loadReservedRooms(selectedRoom);
            }
            view.setUpdating(false);
        });
        
        // ✅ [4] 날짜 선택기 리스너 추가
        view.getDateChooser().addPropertyChangeListener("date", evt -> {
            String selectedRoom = view.getSelectedRoom();
            if (!"선택".equals(selectedRoom)) {
                System.out.println("[날짜 변경] 선택된 날짜: " + view.getSelectedDateString());
                loadReservedRooms(selectedRoom);
            }
        });

// [4] 이전 버튼 클릭 시
view.getBeforeButton().addActionListener(e -> {
    view.dispose();  // 현재 창 닫기
    String userId = Session.getInstance().getLoggedInUserId();

    if (userId != null && userId.startsWith("A")) {
        // 조교일 경우 기존 Executive 인스턴스 재사용
        if (view.getExecutive() != null) {
            view.getExecutive().setVisible(true);
        } else {
            System.err.println("[오류] Executive 인스턴스가 null입니다.");
        }
    } else {
        // 학생 또는 교수는 RoomSelect로 이동
        RoomSelect roomSelect = RoomSelect.getInstance();  // 싱글톤 인스턴스
        new RoomSelectController(roomSelect);
        roomSelect.setVisible(true);
    }
});

    }
    private void loadReservedRooms(String selectedRoom) {
    JTable table = view.getTable();

    // 테이블 초기화
    for (int row = 0; row < table.getRowCount(); row++) {
        for (int col = 1; col < table.getColumnCount(); col++) {
            table.setValueAt("", row, col);
        }
    }

    // ✅ 선택된 날짜의 주간 계산
    java.time.LocalDate selectedDate = view.getSelectedDate();
    if (selectedDate == null) {
        selectedDate = java.time.LocalDate.now().plusDays(1);
    }
    
    // 주의 시작일 (월요일)
    java.time.LocalDate weekStart = getWeekStart(selectedDate);
    java.time.LocalDate weekEnd = weekStart.plusDays(6);
    
    System.out.println("[예약 로드] 주간 범위: " + weekStart + " ~ " + weekEnd);

    String userId = Session.getInstance().getLoggedInUserId();
    boolean isPrivileged = userId.startsWith("P") || userId.startsWith("A");

    PrintWriter out = Session.getInstance().getOut();
    BufferedReader in = Session.getInstance().getIn();

    if (out == null || in == null) {
        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
        return;
    }

    // 서버에 요청 전송
    // ✅ 주간 범위를 서버에 전달
    String request = String.format("VIEW_RESERVATION,%s,%s,%s,%s", 
        userId, selectedRoom, weekStart.toString(), weekEnd.toString());
    out.println(request);
    out.flush();
    
    System.out.println("[서버 요청] " + request);

    try {
        String line;
        int totalCount = 0;
        int filteredCount = 0;
        
        while ((line = in.readLine()) != null) {
            if (line.equals("END_OF_RESERVATION")) break;
            
            totalCount++;

            String[] tokens = line.split(",");
            if (tokens.length < 9) {
                System.err.println("[필드 부족] " + line);
                continue;  // ✅ 9개 필드로 수정
            }

            String name = tokens[0].trim();     // 예약자 이름
            String room = tokens[1].trim();     // 강의실/실습실
            String dateStr = tokens[2].trim();  // ✅ 날짜 (yyyy-MM-dd)
            String day = tokens[3].trim();      // 요일
            String period = tokens[4].trim();   // 교시
            String status = tokens[7].trim();   // 상태

            if (!room.equals(selectedRoom)) {
                System.out.println("[방 불일치] " + room + " != " + selectedRoom);
                continue;
            }
            
            // ✅ 서버에서 이미 날짜 필터링됨 - 추가 필터링 불필요
            filteredCount++;

            int col = getDayColumn(day);
            int row = getPeriodRow(period);

            if (col != -1 && row != -1) {
                String current = (String) table.getValueAt(row, col);

                if (isPrivileged) {
                    // 교수/조교는 예약자 이름 표시
                    if (current == null || current.isEmpty()) {
                        table.setValueAt(name, row, col);
                    } else if (!current.contains(name)) {
                        table.setValueAt(current + ", " + name, row, col);
                    }
                } else {
                    // 학생은 본인 예약만 "예약됨"으로 표시
                    if (name.equals(Session.getInstance().getLoggedInUserName())) {
                        if (current == null || current.isEmpty()) {
                            table.setValueAt("예약됨", row, col);
                        } else if (!current.contains("예약됨")) {
                            table.setValueAt(current + ", 예약됨", row, col);
                        }
                    }
                }
            }
        }
        
        System.out.println(String.format("[예약 필터링 완료] 전체: %d건, 표시: %d건", 
            totalCount, filteredCount));
        
    } catch (IOException e) {
        JOptionPane.showMessageDialog(view, "서버 응답 처리 중 오류: " + e.getMessage());
    }
}

/**
 * ✅ 주의 시작일 (월요일) 계산
 */
private java.time.LocalDate getWeekStart(java.time.LocalDate date) {
    java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
    int daysToSubtract = dayOfWeek.getValue() - 1; // 월요일=1
    return date.minusDays(daysToSubtract);
}

    private int getDayColumn(String day) {
        return switch (day) {
            case "월요일" ->
                1;
            case "화요일" ->
                2;
            case "수요일" ->
                3;
            case "목요일" ->
                4;
            case "금요일" ->
                5;
            default ->
                -1;
        };
    }

    private int getPeriodRow(String period) {
        return switch (period) {
            case "1교시(09:00~10:00)" ->
                0;
            case "2교시(10:00~11:00)" ->
                1;
            case "3교시(11:00~12:00)" ->
                2;
            case "4교시(12:00~13:00)" ->
                3;
            case "5교시(13:00~14:00)" ->
                4;
            case "6교시(14:00~15:00)" ->
                5;
            case "7교시(15:00~16:00)" ->
                6;
            case "8교시(16:00~17:00)" ->
                7;
            case "9교시(17:00~18:00)" ->
                8;
            default ->
                -1;
        };
    }
}
