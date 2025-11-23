package Controller;

import Model.Session;
import View.ChangePasswordView;
import View.Executive;
import View.RoomSelect;

import javax.swing.*;
import java.awt.GraphicsEnvironment;

public class ChangePasswordController {

    private final ChangePasswordView view;

    public ChangePasswordController(ChangePasswordView view) {
        this.view = view;
        view.setSaveButtonListener(e -> changePassword());
    }

    public void changePassword() {
        String current = view.getPresentPassword().trim();
        String newPw = view.getChangePassword().trim();
        String userId = Session.getInstance().getLoggedInUserId();

        boolean success = ClientFacade.changePassword(userId, current, newPw);

        if (success) {
            view.dispose();
            if (!GraphicsEnvironment.isHeadless()) {
                if (userId != null && !userId.isEmpty()) {
                    char userType = userId.charAt(0);
                    switch (userType) {
                        case 'S':
                        case 'P':
                            RoomSelect roomSelect = new RoomSelect();
                            new RoomSelectController(roomSelect);
                            roomSelect.setVisible(true);
                            break;
                        case 'A':
                            Executive executive = new Executive();
                            new ExecutiveController(executive);
                            executive.setVisible(true);
                            break;
                        default:
                            JOptionPane.showMessageDialog(null, "알 수 없는 사용자 유형입니다: " + userType);
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "비밀번호 변경 실패");
        }
    }
}
