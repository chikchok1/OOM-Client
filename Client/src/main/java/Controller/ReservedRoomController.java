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

// [4] 이전 버튼 클릭 시
view.getBeforeButton().addActionListener(e -> {
    view.dispose();  // 현재 창 닫기
    String userId = Session.getLoggedInUserId();

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
    
    /*

    private void loadReservedRooms(String selectedRoom) {
    JTable table = view.getTable();

    // 테이블 초기화
    for (int row = 0; row < table.getRowCount(); row++) {
        for (int col = 1; col < table.getColumnCount(); col++) {
            table.setValueAt("", row, col);
        }
    }

    String userId = Session.getLoggedInUserId();
    boolean isPrivileged = userId.startsWith("P") || userId.startsWith("A");

    PrintWriter out = Session.getOut();
    BufferedReader in = Session.getIn();

    if (out == null || in == null) {
        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
        return;
    }

    // 서버에 요청 전송
    String request = String.format("VIEW_RESERVATION,%s,%s", userId, selectedRoom);
    out.println(request);
    out.flush();

    try {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("END_OF_RESERVATION")) break; // 서버에서 예약 끝 표시

            String[] tokens = line.split(",");
            if (tokens.length < 7) continue;

            String name = tokens[0].trim();         // 예약자 이름
            String room = tokens[1].trim();         // 강의실
            String day = tokens[2].trim();          // 요일
            String period = tokens[3].trim();       // 교시
            String status = tokens[6].trim();       // 상태 (예: 예약됨)

            
            if (!room.equals(selectedRoom)) continue; // 선택한 강의실만 표시

            int col = getDayColumn(day);
            int row = getPeriodRow(period);

            if (col != -1 && row != -1) {
                String display = isPrivileged ? name : "예약됨";
                String current = (String) table.getValueAt(row, col);
                if (current == null || current.isEmpty()) {
                    table.setValueAt(display, row, col);
                } else if (!current.contains(display)) {
                    table.setValueAt(current + ", " + display, row, col);
                }
            }
        }

    } catch (IOException e) {
        JOptionPane.showMessageDialog(view, "서버 응답 처리 중 오류: " + e.getMessage());
    }
}
*/
    
    private void loadReservedRooms(String selectedRoom) {
    JTable table = view.getTable();

    // 테이블 초기화
    for (int row = 0; row < table.getRowCount(); row++) {
        for (int col = 1; col < table.getColumnCount(); col++) {
            table.setValueAt("", row, col);
        }
    }

    String userId = Session.getLoggedInUserId();
    boolean isPrivileged = userId.startsWith("P") || userId.startsWith("A");

    PrintWriter out = Session.getOut();
    BufferedReader in = Session.getIn();

    if (out == null || in == null) {
        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
        return;
    }

    // 서버에 요청 전송
    String request = String.format("VIEW_RESERVATION,%s,%s", userId, selectedRoom);
    out.println(request);
    out.flush();

    try {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("END_OF_RESERVATION")) break;

            String[] tokens = line.split(",");
            if (tokens.length < 7) continue;

            String name = tokens[0].trim();     // 예약자 이름
            String room = tokens[1].trim();     // 강의실/실습실
            String day = tokens[2].trim();      // 요일
            String period = tokens[3].trim();   // 교시
            String status = tokens[6].trim();   // 상태

            if (!room.equals(selectedRoom)) continue;

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
                    if (name.equals(Session.getLoggedInUserName())) {
                        if (current == null || current.isEmpty()) {
                            table.setValueAt("예약됨", row, col);
                        } else if (!current.contains("예약됨")) {
                            table.setValueAt(current + ", 예약됨", row, col);
                        }
                    }
                }
            }
        }
    } catch (IOException e) {
        JOptionPane.showMessageDialog(view, "서버 응답 처리 중 오류: " + e.getMessage());
    }
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
