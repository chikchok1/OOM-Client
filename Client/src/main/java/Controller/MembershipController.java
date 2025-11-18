package Controller;

import common.model.MembershipModel;
import View.LoginForm;
import View.MembershipView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class MembershipController {
    private MembershipView view;
    private MembershipModel model;
    private LoginForm loginForm;
    private NetworkFacade networkFacade;

    public MembershipController(MembershipView view, MembershipModel model, LoginForm loginForm) {
        this.view = view;
        this.model = model;
        this.loginForm = loginForm;
        this.networkFacade = new NetworkFacade();

        this.view.setCustomActionListener(e -> {
            String name = view.getName();
            String studentId = view.getStudentId();
            String password = view.getPassword();

            if (!validateInput(name, studentId, password)) return;

            try {
                // [Facade 사용] 소켓 생성/해제 로직이 사라져 코드가 매우 간결해짐
                String response = networkFacade.register(name, studentId, password);

                if ("SUCCESS".equals(response)) {
                    view.showMessage("회원가입 완료.");
                    view.disposeView();
                    loginForm.setVisible(true);
                } else if ("DUPLICATE".equals(response)) {
                    view.showMessage("이미 존재하는 학번입니다.");
                } else {
                    view.showMessage("가입 실패: " + response);
                }
            } catch (IOException ex) {
                view.showMessage("서버 연결 오류: " + ex.getMessage());
            }
        });
    }
    
    // 유효성 검사 분리
    private boolean validateInput(String name, String id, String pw) {
        if (name.isEmpty() || id.isEmpty() || pw.isEmpty()) {
            view.showMessage("모든 필드를 입력해주세요.");
            return false;
        }
        if (!id.matches("[SPA][0-9]{3}")) {
            view.showMessage("아이디 형식이 올바르지 않습니다. \n 아이디는 대문자 S/P/A + 숫자 3개로 구성되어야 합니다.\n 예: S123");
            return false;
        }
        if (pw.length() < 4 || pw.length() > 8) {
            view.showMessage("비밀번호는 4~8자여야 합니다.");
            return false;
        }
        return true;
    }
}