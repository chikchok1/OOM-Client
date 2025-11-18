package Controller;

import Model.Session;
import View.ChangePasswordView;
import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.io.IOException;

public class ChangePasswordController {

    private final ChangePasswordView view;
    private final NetworkFacade networkFacade; // 네트워크 전담 객체

    public ChangePasswordController(ChangePasswordView view) {
        this.view = view;
        this.networkFacade = new NetworkFacade(); // Facade 초기화
        view.setSaveButtonListener(e -> changePassword());
    }

    public void changePassword() {
        String currentPassword = view.getPresentPassword().trim();
        String newPassword = view.getChangePassword().trim();
        String userId = Session.getLoggedInUserId(); // 호환성 계층을 통해 안전하게 호출

        // 1. 입력값 검증
        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(view, "모든 필드를 입력해주세요.");
            return;
        }

        try {
            // 퍼사드 패턴 (Facade Pattern)
            // 복잡한 스트림 처리와 프로토콜 문자열 조립을 Facade 뒤로 숨김
            String response = networkFacade.changePassword(userId, currentPassword, newPassword);

            if (response == null) {
                JOptionPane.showMessageDialog(view, "서버 응답이 없습니다.");
                return;
            }

            switch (response) {
                case "PASSWORD_CHANGED":
                    JOptionPane.showMessageDialog(view, "비밀번호가 성공적으로 변경되었습니다.");

                    if (!GraphicsEnvironment.isHeadless()) {
                        char userType = userId.charAt(0);
                        // 팩토리 메서드 패턴 (Factory Method Pattern)
                        // 역할(S/P/A)에 따라 다음 화면을 생성하는 복잡한 switch문을 제거하고 위임
                        try {
                            JFrame nextView = ViewFactory.createMainView(userType);
                            nextView.setVisible(true);
                        } catch (IllegalArgumentException e) {
                            JOptionPane.showMessageDialog(view, "화면 전환 오류: " + e.getMessage());
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
            JOptionPane.showMessageDialog(view, "서버 통신 오류: " + e.getMessage());
        }
    }
}