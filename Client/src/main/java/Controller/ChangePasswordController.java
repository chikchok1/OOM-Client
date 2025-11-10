package Controller;

import Model.Session;
import View.ChangePasswordView;
import View.Executive;
import View.RoomSelect;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.io.*;

public class ChangePasswordController {

    private final ChangePasswordView view;

    public ChangePasswordController(ChangePasswordView view) {
        this.view = view;
        view.setSaveButtonListener(e -> changePassword());
    }

    public void changePassword() {
        String currentPassword = view.getPresentPassword().trim();
        String newPassword = view.getChangePassword().trim();
        String userId = Session.getLoggedInUserId();

        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(view, "모든 필드를 입력해주세요.");
            return;
        }

        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

        if (out == null || in == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        String request = String.join(",", "CHANGE_PASSWORD", userId, currentPassword, newPassword);
        out.println(request);
        out.flush();

        try {
            String response = in.readLine();

            switch (response) {
                case "PASSWORD_CHANGED":
                    JOptionPane.showMessageDialog(view, "비밀번호가 성공적으로 변경되었습니다.");

                    if (!GraphicsEnvironment.isHeadless()) {
                        char userType = userId.charAt(0); // ✅ ID 첫 글자 기준 분기

                        switch (userType) {
                            case 'S': // 학생
                            case 'P': // 교수
                                RoomSelect roomSelect = new RoomSelect();
                                new RoomSelectController(roomSelect);
                                roomSelect.setVisible(true);
                                break;

                            case 'A': // 조교
                                Executive executive = new Executive();
                                new ExecutiveController(executive);
                                executive.setVisible(true);
                                break;

                            default:
                                JOptionPane.showMessageDialog(view, "알 수 없는 사용자 유형입니다: " + userType);
                        }

                        view.dispose();
                    }
                    break;

                case "INVALID_CURRENT_PASSWORD":
                    JOptionPane.showMessageDialog(view, "현재 비밀번호가 일치하지 않습니다.");
                    break;

                case "USER_NOT_FOUND":
                    JOptionPane.showMessageDialog(view, "사용자 정보를 찾을 수 없습니다.");
                    break;

                default:
                    JOptionPane.showMessageDialog(view, "비밀번호 변경 실패: " + response);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(view, "서버 응답 오류: " + e.getMessage());
        }
    }
}
