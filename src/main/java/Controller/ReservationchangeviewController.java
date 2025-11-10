package Controller;

import View.Reservationchangeview;
import View.RoomSelect;
import Model.Session;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.io.*;

public class ReservationchangeviewController {

    private Reservationchangeview view;

    public ReservationchangeviewController(Reservationchangeview view) {
        this.view = view;

        this.view.setChangeButtonActionListener(e -> handleReservationChange());
        this.view.setBackButtonActionListener(e -> handleBack());
        this.view.setCancelButtonActionListener(e -> handleReservationCancel());

        this.view.getReservationTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = view.getReservationTable().getSelectedRow();
                    if (selectedRow != -1) {
                        String selectedId = (String) view.getReservationTable().getValueAt(selectedRow, 0);
                        view.setReservationId(selectedId);
                    }
                }
            }
        });

        loadUserReservations();
    }

    private void handleBack() {
        view.dispose();
        RoomSelect roomSelect = RoomSelect.getInstance();
        new RoomSelectController(roomSelect);
        roomSelect.setVisible(true);
    }

    private void handleReservationChange() {
        int selectedRow = view.getReservationTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(view, "변경할 예약을 선택하세요.");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) view.getReservationTable().getModel();
        String reservationId = (String) model.getValueAt(selectedRow, 0);
        String originalTime = (String) model.getValueAt(selectedRow, 1);
        String originalDay = (String) model.getValueAt(selectedRow, 2);
        String originalRoom = (String) model.getValueAt(selectedRow, 3);
        String userName = (String) model.getValueAt(selectedRow, 4);

        String newDay = view.getSelectedDay();
        String newTime = view.getSelectedTime();
        String selectedRoom = view.getSelectedRoom();
        String changeNumber = view.getChangeNumber();
        String userId = Session.getInstance().getLoggedInUserId();

        if (newDay.equals("선택") || newTime.equals("선택") || selectedRoom == null) {
            JOptionPane.showMessageDialog(view, "모든 항목을 입력하세요.");
            return;
        }
        
        // 변경 인원 검증
        if (changeNumber.isEmpty()) {
            JOptionPane.showMessageDialog(view, "변경 인원을 입력하세요.");
            return;
        }
        
        try {
            int number = Integer.parseInt(changeNumber);
            if (number <= 0) {
                JOptionPane.showMessageDialog(view, "변경 인원은 1명 이상이어야 합니다.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(view, "변경 인원은 숫자로 입력해야 합니다.");
            return;
        }

        if (userId == null || userName == null || userId.isEmpty() || userName.isEmpty()) {
            JOptionPane.showMessageDialog(view, "로그인 정보가 없습니다. 먼저 로그인해주세요.");
            return;
        }

        // 중복 예약 확인
        if (isConflictExists(selectedRoom, newDay, newTime)) {
            JOptionPane.showMessageDialog(view, "이미 해당 시간에 예약된 강의실입니다.");
            return;
        }

        // 서버에 변경 요청 전송
        try {
            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            // 변경 인원 포함하여 전송
            String command = String.join(",", "CHANGE_RESERVATION",
                    userId, originalTime, originalDay, originalRoom,
                    newTime, newDay, selectedRoom, userName, changeNumber);
            out.println(command);
            out.flush();
            
            System.out.println("[DEBUG] 변경 요청 전송: " + command);

            String response = in.readLine();
            switch (response) {
                case "CHANGE_SUCCESS":
                    JOptionPane.showMessageDialog(view, "변경 요청이 저장되었습니다.");
    ((DefaultTableModel) view.getReservationTable().getModel()).removeRow(selectedRow);
                    break;
                case "CHANGE_DUPLICATE_REQUEST":
                    JOptionPane.showMessageDialog(view, "해당 시간/강의실에 이미 변경 요청이 존재합니다.");
                    break;
                default:
                    JOptionPane.showMessageDialog(view, "변경 실패: " + response);
                    break;
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "서버 통신 오류: " + e.getMessage());
        }
    }

    private void handleReservationCancel() {
        int selectedRow = view.getReservationTable().getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(view, "취소할 예약을 선택하세요.");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) view.getReservationTable().getModel();
        String userId = (String) model.getValueAt(selectedRow, 0);
        String time = (String) model.getValueAt(selectedRow, 1);
        String day = (String) model.getValueAt(selectedRow, 2);
        String room = (String) model.getValueAt(selectedRow, 3);
        String name = (String) model.getValueAt(selectedRow, 4);

        try {
            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            String command = String.join(",", "CANCEL_RESERVATION", userId, time, day, room, name);
            out.println(command);
            out.flush();

            String response = in.readLine();
            if ("CANCEL_SUCCESS".equals(response)) {
                model.removeRow(selectedRow);
                JOptionPane.showMessageDialog(view, "예약이 취소되었습니다.");
            } else {
                JOptionPane.showMessageDialog(view, "예약 취소 실패: " + response);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "서버 통신 오류: " + e.getMessage());
        }
    }

    private void loadUserReservations() {
        String userId = Session.getInstance().getLoggedInUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) view.getReservationTable().getModel();
        model.setRowCount(0);

        try {
            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            out.println("VIEW_MY_RESERVATIONS," + userId);
            out.flush();

            String line;
            while ((line = in.readLine()) != null && !line.equals("END_OF_MY_RESERVATIONS")) {
                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    model.addRow(new String[]{parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]});
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "예약 불러오기 오류: " + e.getMessage());
        }
    }

    private boolean isConflictExists(String room, String day, String time) {
        try {
            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            String command = String.join(",", "CHECK_ROOM_TIME", room, day, time);
            out.println(command);
            out.flush();

            String response = in.readLine();
            return "CONFLICT".equals(response);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "중복 확인 중 오류 발생: " + e.getMessage());
            return true;
        }
    }
}