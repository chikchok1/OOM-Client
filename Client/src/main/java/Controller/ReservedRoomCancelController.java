package Controller;

import Model.Session;
import View.Executive;
import View.ReservedRoomCancelView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;

public class ReservedRoomCancelController {

    private ReservedRoomCancelView view;

    public ReservedRoomCancelController(ReservedRoomCancelView view) {
        this.view = view;

        loadUserReservations(); // 서버로부터 예약 목록 요청
        setCancelAction();      // 취소 버튼 처리
        setBackAction();        // 이전 버튼 처리
    }

    private void loadUserReservations() {
        DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
        model.setRowCount(0); // 테이블 초기화

        //  연결 상태 확인
        if (!Session.isConnected()) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();
        String userId = Session.getLoggedInUserId();
        String role = Session.getLoggedInUserRole();

        if (out == null || in == null || userId == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않거나 로그인 정보가 없습니다.");
            return;
        }

        try {
            // 조교는 전체 예약 조회, 나머지는 본인 예약만
            if ("조교".equals(role)) {
                out.println("VIEW_ALL_RESERVATIONS");
            } else {
                out.println("VIEW_MY_RESERVATIONS," + userId);
            }
            out.flush();

            String line;
            while ((line = in.readLine()) != null && !line.startsWith("END_")) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    model.addRow(parts); // userId, time, day, room, name
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "예약 목록 수신 중 오류 발생: " + e.getMessage());
        }
    }

    //  서버에 취소 요청
    private void setCancelAction() {
        view.getCancelButton().addActionListener(e -> {
            JTable table = view.getTable();
            int selectedRow = table.getSelectedRow();

            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(view, "취소할 예약을 선택하세요.");
                return;
            }

            String userId = table.getValueAt(selectedRow, 0).toString();
            String time = table.getValueAt(selectedRow, 1).toString();
            String day = table.getValueAt(selectedRow, 2).toString();
            String room = table.getValueAt(selectedRow, 3).toString();
            String userName = table.getValueAt(selectedRow, 4).toString();

            //  연결 상태 확인
            if (!Session.isConnected()) {
                JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
                return;
            }

            PrintWriter out = Session.getOut();
            BufferedReader in = Session.getIn();

            if (out == null || in == null) {
                JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
                return;
            }

            try {
                String command = String.join(",", "CANCEL_RESERVATION", userId, time, day, room, userName);
                out.println(command);
                out.flush();

                String response = in.readLine();

                // null 체크 추가
                if (response == null) {
                    JOptionPane.showMessageDialog(view, "서버로부터 응답이 없습니다.");
                    return;
                }

                if ("CANCEL_SUCCESS".equals(response)) {
                    ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
                    JOptionPane.showMessageDialog(view, "예약이 취소되었습니다.");
                } else {
                    JOptionPane.showMessageDialog(view, "예약 취소 실패: " + response);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(view, "서버 통신 오류: " + ex.getMessage());
            }
        });
    }

    private void setBackAction() {
        view.getBackButton().addActionListener(e -> {
            view.dispose();
            Executive execView = new Executive();
            new ExecutiveController(execView);
            execView.setVisible(true);
        });
    }
}
