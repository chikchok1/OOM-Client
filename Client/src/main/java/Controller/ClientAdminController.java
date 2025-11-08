package Controller;

import View.ClientAdmin;
import View.Executive;
import Model.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;

public class ClientAdminController {

    private final ClientAdmin view;

    public ClientAdminController(ClientAdmin view) {
        this.view = view;

        loadUsersFromServer(); // 서버에서 사용자 목록 로드

        view.getJButton2().addActionListener(e -> deleteSelectedUser());
        view.getJButton1().addActionListener(e -> updateSelectedUser());
        view.getJButton3().addActionListener(e -> goBackToExecutive());
    }

    protected void loadUsersFromServer() {
        DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
        model.setRowCount(0);  // 기존 데이터 초기화

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

        if (out == null || in == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        try {
            out.println("GET_ALL_USERS");
            out.flush();

            String line;
            while ((line = in.readLine()) != null && !line.equals("END_OF_USERS")) {
                String[] tokens = line.split(",");
                if (tokens.length == 3) {
                    model.addRow(tokens);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "서버에서 사용자 목록을 불러오는 중 오류 발생: " + e.getMessage());
        }
    }

    private void deleteSelectedUser() {
        JTable table = view.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(view, "삭제할 사용자를 선택하세요.");
            return;
        }

        String userId = (String) model.getValueAt(row, 1);

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();
        if (out == null || in == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        try {
            out.println("DELETE_USER," + userId);
            out.flush();

            String response = in.readLine();
            if (response == null) {
                JOptionPane.showMessageDialog(view, "서버 응답 없음");
                return;
            }

            if ("DELETE_SUCCESS".equals(response)) {
                JOptionPane.showMessageDialog(view, "삭제 성공");
                loadUsersFromServer();  // 전체 다시 로딩
            } else {
                JOptionPane.showMessageDialog(view, "삭제 실패: " + response);
            }

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view, "삭제 중 오류 발생: " + e.getMessage());
        }
    }

    private void updateSelectedUser() {
        JTable table = view.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(view, "수정할 사용자를 선택하세요.");
            return;
        }

        String userId = (String) model.getValueAt(row, 1);
        String oldName = (String) model.getValueAt(row, 0);
        String oldPw = (String) model.getValueAt(row, 2);

        String newName = JOptionPane.showInputDialog(view, "새 이름:", oldName);
        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        String newPw = JOptionPane.showInputDialog(view, "새 비밀번호:", oldPw);
        if (newPw == null || newPw.trim().isEmpty()) {
            return;
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();
        if (out == null || in == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        try {
            out.println("UPDATE_USER," + userId + "," + newName + "," + newPw);
            out.flush();

            String response = in.readLine();
            if ("UPDATE_SUCCESS".equals(response)) {
                model.setValueAt(newName, row, 0);
                model.setValueAt(newPw, row, 2);
                JOptionPane.showMessageDialog(view, "수정 성공");
            } else {
                JOptionPane.showMessageDialog(view, "수정 실패: " + response);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "수정 요청 중 오류 발생: " + e.getMessage());
        }
    }

    private void goBackToExecutive() {
        view.dispose();
        Executive executive = view.getExecutive();
        if (executive != null) {
            executive.setVisible(true);
        } else {
            System.err.println("Executive 인스턴스가 null입니다.");
        }
    }
}
