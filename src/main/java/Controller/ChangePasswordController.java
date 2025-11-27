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
        // Validate required fields early to avoid unnecessary server calls and
        // to match test expectations (show message on empty fields).
        if (current.isEmpty() || newPw.isEmpty()) {
            JOptionPane.showMessageDialog(null, "모든 필드를 입력해주세요.");
            return;
        }
        String userId = Session.getInstance().getLoggedInUserId();
        String response = Controller.ClientFacade.changePasswordRequest(userId, current, newPw);

        if (response == null) {
            JOptionPane.showMessageDialog(null, "서버 응답 없음(타임아웃)");
            return;
        }

        switch (response) {
            case "PASSWORD_CHANGED":
                // 뷰는 즉시 닫아 테스트에서 검증 가능하도록 동기적으로 처리
                try { view.dispose(); } catch (Throwable ignored) {}

                // 성공: 모달 차단 없이 비모달 자동 닫힘 알림 표시 및 다음 화면 오픈은 UI 스레드에서 처리
                SwingUtilities.invokeLater(() -> {
                    try {
                        javax.swing.JOptionPane pane = new javax.swing.JOptionPane("비밀번호가 성공적으로 변경되었습니다.", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        javax.swing.JDialog dialog = pane.createDialog((java.awt.Frame) null, "비밀번호 변경 완료");
                        dialog.setModal(false);
                        dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
                        dialog.setVisible(true);

                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                            try { javax.swing.SwingUtilities.invokeLater(dialog::dispose); } catch (Throwable ignored) {}
                        }).start();
                    } catch (Throwable t) {
                        System.err.println("[ChangePasswordController] 성공 알림 표시 중 오류: " + t.getMessage());
                    }

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
                });
                break;
            case "INVALID_CURRENT_PASSWORD":
                JOptionPane.showMessageDialog(null, "현재 비밀번호가 일치하지 않습니다.");
                break;
            case "USER_NOT_FOUND":
                JOptionPane.showMessageDialog(null, "사용자 정보를 찾을 수 없습니다.");
                break;
            default:
                JOptionPane.showMessageDialog(null, "비밀번호 변경 실패: " + response);
        }
    }
}
