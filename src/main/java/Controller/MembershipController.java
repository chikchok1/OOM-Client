package Controller;

import common.model.MembershipModel;
import View.LoginForm;
import View.MembershipView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MembershipController {
    private MembershipView view;
    private MembershipModel model;
    private LoginForm loginForm;

    public MembershipController(MembershipView view, MembershipModel model, LoginForm loginForm) {
        this.view = view;
        this.model = model;
        this.loginForm = loginForm;

        this.view.setCustomActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = view.getName();
                String studentId = view.getStudentId();
                String password = view.getPassword();
                ClientFacade.register(name, studentId, password, view, loginForm);
            }
        });
    }

    boolean isValidId(String userId) {
        return userId.matches("[SPA][0-9]{3}");
    }

    boolean isValidPassword(String password) {
        return password.length() >= 4 && password.length() <= 8;
    }
}
