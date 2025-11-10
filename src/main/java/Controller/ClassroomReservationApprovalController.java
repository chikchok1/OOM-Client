package Controller;

import Model.Session;
import View.ClassroomReservationApproval;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;

public class ClassroomReservationApprovalController {

    private ClassroomReservationApproval view;

    public ClassroomReservationApprovalController(ClassroomReservationApproval view) {
        this.view = view;

        loadAllRequests();
        setApproveButtonAction();
        setRejectButtonAction();
        addTableClickListener();
    }

    private void loadAllRequests() {
        DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
        model.setRowCount(0);

        //  연결 상태 확인
        if (!Session.getInstance().isConnected()) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();

        if (out == null || in == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }
        
        String userId = Session.getInstance().getLoggedInUserId();

        out.println("GET_RESERVATION_REQUESTS");
        out.flush();

        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_OF_REQUESTS")) {
                    break;
                }

                // 서버가 보낸 형식: id,time,day,room,name,studentCount
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    model.addRow(parts);
                } else if (parts.length == 5) {
                    // 구 버전 호환성: 학생 수가 없으면 "1" 추가
                    String[] partsWithCount = new String[6];
                    System.arraycopy(parts, 0, partsWithCount, 0, 5);
                    partsWithCount[5] = "1";
                    model.addRow(partsWithCount);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "서버 응답 오류: " + e.getMessage());
        }
    }

    private void addTableClickListener() {
        view.getTable().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int selectedRow = view.getTable().getSelectedRow();
                if (selectedRow != -1) {
                    String reservationId = view.getTable().getValueAt(selectedRow, 0).toString();
                    view.setReservationId(reservationId);
                }
            }
        });
    }

    public void setApproveButtonAction() {
        view.getApproveButton().addActionListener(e -> {
            int selectedRow = view.getTable().getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(view, "승인할 행을 선택하세요.");
                return;
            }

            DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
            String id = ((String) model.getValueAt(selectedRow, 0)).trim();
            String time = ((String) model.getValueAt(selectedRow, 1)).trim();
            String day = ((String) model.getValueAt(selectedRow, 2)).trim();
            String room = ((String) model.getValueAt(selectedRow, 3)).trim();
            String name = ((String) model.getValueAt(selectedRow, 4)).trim();

            //  연결 상태 확인
            if (!Session.getInstance().isConnected()) {
                JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
                return;
            }

            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            out.println(String.format("APPROVE_RESERVATION,%s,%s,%s,%s,%s", id, time, day, room, name));
            out.flush();

            try {
                String response = in.readLine();
                // null 체크 추가
                if (response == null) {
                    JOptionPane.showMessageDialog(view, "서버로부터 응답이 없습니다.");
                    return;
                }

                if ("APPROVE_SUCCESS".equals(response)) {
                    model.removeRow(selectedRow);
                    JOptionPane.showMessageDialog(view, "예약 승인 완료되었습니다.");
                } else {
                    JOptionPane.showMessageDialog(view, "승인 실패: " + response);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(view, "서버 응답 오류: " + ex.getMessage());
            }
        });
    }

    public void setRejectButtonAction() {
        view.getRejectButton().addActionListener(e -> {
            int selectedRow = view.getTable().getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(view, "거절할 행을 선택하세요.");
                return;
            }

            DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
            String id = ((String) model.getValueAt(selectedRow, 0)).trim();
            String time = ((String) model.getValueAt(selectedRow, 1)).trim();
            String day = ((String) model.getValueAt(selectedRow, 2)).trim();
            String room = ((String) model.getValueAt(selectedRow, 3)).trim();
            String name = ((String) model.getValueAt(selectedRow, 4)).trim();

            //  연결 상태 확인
            if (!Session.getInstance().isConnected()) {
                JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
                return;
            }

            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            out.println(String.format("REJECT_RESERVATION,%s,%s,%s,%s,%s", id, time, day, room, name));
            out.flush();

            try {
                String response = in.readLine();
                //  null 체크 추가
                if (response == null) {
                    JOptionPane.showMessageDialog(view, "서버로부터 응답이 없습니다.");
                    return;
                }

                if ("REJECT_SUCCESS".equals(response)) {
                    model.removeRow(selectedRow);
                    JOptionPane.showMessageDialog(view, "거절 처리 완료되었습니다.");
                } else {
                    JOptionPane.showMessageDialog(view, "거절 실패: " + response);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(view, "서버 응답 오류: " + ex.getMessage());
            }
        });
    }
}
