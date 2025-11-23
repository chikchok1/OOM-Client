package Controller;

import common.model.MembershipModel;
import View.LoginForm;
import View.MembershipView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import Service.UserService;

public class MembershipController {
    private final MembershipView view;
    private final UserService userService;

    public MembershipController(MembershipView view) {
        this.view = view;
        this.userService = UserService.getInstance();
        this.view.addRegisterButtonListener(new RegisterButtonListener());
    }

    class RegisterButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String id = view.getId();
            String password = view.getPassword();
            String name = view.getNameField();
            String type = view.getType();

            if (id.isEmpty() || password.isEmpty() || name.isEmpty()) {
                view.showMessage("모든 필드를 입력해주세요.");
                return;
            }

            boolean isSuccess = userService.register(id, password, name, type);

            if (isSuccess) {
                view.showMessage("회원가입 성공");
                view.dispose();
            } else {
                view.showMessage("회원가입 실패: 이미 존재하는 ID일 수 있습니다.");
            }
        }
    }
}
