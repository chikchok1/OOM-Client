package Controller;

import Model.Session;
import Service.UserService;
import View.ChangePasswordView;
import java.awt.event.ActionEvent;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.io.*;

public class ChangePasswordController {

    private final ChangePasswordView view;
    private final UserService userService;

    public ChangePasswordController(ChangePasswordView view) {
        this.view = view;
        this.userService = UserService.getInstance();
        this.view.addChangeButtonListener(new ChangeButtonListener());
    }

    class ChangeButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String id = Session.getInstance().getCurrentUser().getId();
            String oldPassword = view.getOldPassword();
            String newPassword = view.getNewPassword();
            String confirmPassword = view.getConfirmPassword();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                view.showMessage("모든 필드를 입력해주세요.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                view.showMessage("새 비밀번호가 일치하지 않습니다.");
                return;
            }

            String status = userService.changePassword(id, oldPassword, newPassword);

            switch (status) {
                case "success":
                    view.showMessage("비밀번호가 성공적으로 변경되었습니다.");
                    view.dispose();
                    break;
                case "fail":
                    view.showMessage("현재 비밀번호가 일치하지 않습니다.");
                    break;
                default:
                    view.showMessage("비밀번호 변경 중 오류가 발생했습니다.");
                    break;
            }
        }
    }
}
