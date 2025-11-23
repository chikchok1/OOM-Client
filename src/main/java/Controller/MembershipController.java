package Controller;

import common.model.MembershipModel;
import View.LoginForm;
import View.MembershipView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import common.utils.ConfigLoader;

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

                if (name.isEmpty() || studentId.isEmpty() || password.isEmpty()) {
                    view.showMessage("모든 필드를 입력해주세요.");
                    return;
                }

                if (!isValidId(studentId)) {
                    view.showMessage("아이디는 대문자 S/P/A + 숫자 3개로 구성되어야 합니다.\n예: S123");
                    return;
                }

                if (!isValidPassword(password)) {
                    view.showMessage("비밀번호는 최소 4자리에서 최대 8자리여야 합니다.");
                    return;
                }

                String serverIp = ConfigLoader.getProperty("server.ip");
                int serverPort = Integer.parseInt(ConfigLoader.getProperty("server.port"));

                try (
                    Socket socket = new Socket(serverIp, serverPort);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                ) {
                    out.println("REGISTER," + name + "," + studentId + "," + password);
                    System.out.println("REGISTER 요청 보냄");

                    String response = in.readLine();
                    System.out.println("서버 응답 수신: " + response);

                    if ("SUCCESS".equals(response)) {
                        view.showMessage("회원가입이 완료되었습니다.");
                        view.disposeView();
                        loginForm.setVisible(true);
                    } else if ("DUPLICATE".equals(response)) {
                        view.showMessage("이미 존재하는 학번입니다. 다른 학번을 사용해주세요.");
                    } else {
                        view.showMessage("회원가입 실패: " + response);
                    }
                } catch (IOException ex) {
                    view.showMessage("서버와 연결할 수 없습니다: " + ex.getMessage());
                }
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
