package Controller;

import View.ClientAdminView;
import common.model.User;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import Service.UserService;

public class ClientAdminController {

    private final ClientAdminView view;
    private final UserService userService;

    public ClientAdminController(ClientAdminView view) {
        this.view = view;
        this.userService = UserService.getInstance();
        this.view.addUserListSelectionListener(new UserListSelectionListener());
        this.view.addEditButtonListener(new EditButtonListener());
        this.view.addDeleteButtonListener(new DeleteButtonListener());
        loadUsers();
    }

    private void loadUsers() {
        List<User> users = userService.getAllUsers();
        if (users != null) {
            view.setUserList(users);
        } else {
            view.showMessage("사용자 목록을 불러오는데 실패했습니다.");
        }
    }

    class UserListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                User selectedUser = view.getSelectedUser();
                if (selectedUser != null) {
                    view.setIdField(selectedUser.getId());
                    view.setNameField(selectedUser.getName());
                    view.setTypeComboBox(selectedUser.getType());
                }
            }
        }
    }

    class EditButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            User selectedUser = view.getSelectedUser();
            if (selectedUser == null) {
                view.showMessage("수정할 사용자를 선택해주세요.");
                return;
            }

            String newName = view.getNameField();
            String newType = (String) view.getTypeComboBox();

            if (newName.isEmpty()) {
                view.showMessage("이름을 입력해주세요.");
                return;
            }

            selectedUser.setName(newName);
            selectedUser.setType(newType);

            if (userService.updateUser(selectedUser)) {
                view.showMessage("사용자 정보가 수정되었습니다.");
                loadUsers(); // 목록 새로고침
            } else {
                view.showMessage("사용자 정보 수정에 실패했습니다.");
            }
        }
    }

    class DeleteButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            User selectedUser = view.getSelectedUser();
            if (selectedUser == null) {
                view.showMessage("삭제할 사용자를 선택해주세요.");
                return;
            }

            int response = JOptionPane.showConfirmDialog(view,
                "정말로 " + selectedUser.getName() + " 사용자를 삭제하시겠습니까?",
                "삭제 확인", JOptionPane.YES_NO_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                if (userService.deleteUser(selectedUser.getId())) {
                    view.showMessage("사용자가 삭제되었습니다.");
                    loadUsers(); // 목록 새로고침
                } else {
                    view.showMessage("사용자 삭제에 실패했습니다.");
                }
            }
        }
    }
}
