package Controller;

import Model.Session;
import View.LoginForm;
import View.MembershipView;
import common.model.MembershipModel;
import javax.swing.JFrame;
import java.io.IOException;

public class LoginController {

    private LoginForm view;
    private MembershipView membershipView;
    private NetworkFacade networkFacade; // Facade 인스턴스

    public LoginController(LoginForm view) {
        this.view = view;
        this.networkFacade = new NetworkFacade(); // Facade 초기화

        this.view.addLoginListener(e -> handleLogin());
        this.view.addJoinListener(e -> openMembership());
    }

    public void handleLogin() {
        String id = view.getUserId();
        String password = view.getPassword();

        if (id.isEmpty() || password.isEmpty()) {
            view.showMessage("아이디와 비밀번호를 모두 입력하세요.");
            return;
        }

        try {
            // [Facade 사용] 복잡한 소켓 연결 및 로그인 프로토콜 처리를 위임
            String response = networkFacade.attemptLogin(id, password);

            if (response == null) {
                view.showMessage("서버로부터 응답이 없습니다.");
                return;
            }

            String[] parts = response.split(",");
            String status = parts[0];

            switch (status) {
                case "SERVER_BUSY":
                    view.showMessage("접속 인원 초과. 나중에 다시 시도해주세요.");
                    break;
                case "ALREADY_LOGGED_IN":
                    view.showMessage("이미 로그인된 사용자입니다.");
                    break;
                case "SUCCESS":
                    String userName = parts.length > 1 ? parts[1] : "이름없음";
                    
                    // [Singleton 사용] 세션 정보 설정
                    Session session = Session.getInstance();
                    session.setLoggedInUserId(id);
                    session.setLoggedInUserName(userName);
                    
                    String role = switch (id.charAt(0)) {
                        case 'S' -> "학생";
                        case 'P' -> "교수";
                        case 'A' -> "조교";
                        default -> "알 수 없음";
                    };
                    session.setLoggedInUserRole(role);

                    view.showMessage("로그인 성공!");
                    view.dispose();

                    // [Factory 사용] 역할에 맞는 메인 화면 생성
                    JFrame mainView = ViewFactory.createMainView(id.charAt(0));
                    mainView.setVisible(true);
                    break;
                default:
                    view.showMessage("로그인 실패: 아이디 또는 비밀번호 불일치");
            }
        } catch (IOException e) {
            view.showMessage("서버 통신 오류: " + e.getMessage());
        }
    }

    private void openMembership() {
        if (membershipView == null || !membershipView.isVisible()) {
            membershipView = new MembershipView();
            MembershipModel membershipModel = new MembershipModel();
            new MembershipController(membershipView, membershipModel, view);
            view.setVisible(false);
            membershipView.setVisible(true);
        }
    }
}