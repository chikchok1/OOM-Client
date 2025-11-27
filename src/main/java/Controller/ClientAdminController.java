package Controller;

import View.ClientAdmin;
import View.Executive;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ClientAdminController {

    private final ClientAdmin view;

    public ClientAdminController(ClientAdmin view) {
        this.view = view;

        loadUsersFromServer(); // 서버에서 사용자 목록 로드 (via facade)

        view.getJButton2().addActionListener(e -> deleteSelectedUser());
        view.getJButton1().addActionListener(e -> updateSelectedUser());
        view.getJButton3().addActionListener(e -> goBackToExecutive());
    }

    protected void loadUsersFromServer() {
        DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
        // Delegate to facade (which will run in background)
        ClientFacade.loadUsers(model);
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

        // Delegate deletion to facade which will call loadUsersFromServer on success
        ClientFacade.deleteUser(userId, view, () -> loadUsersFromServer());
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

        ClientFacade.updateUser(userId, newName, newPw, view, row, model);
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
