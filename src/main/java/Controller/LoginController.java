package Controller;

import common.model.MembershipModel;
import Model.Session;
import Service.UserService;
import View.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JOptionPane;
import common.utils.ConfigLoader;

public class LoginController {

    private final LoginView view;
    private final UserService userService;

    public LoginController(LoginView view) {
        this.view = view;
        this.userService = UserService.getInstance();
        this.view.addLoginButtonListener(new LoginButtonListener());
    }

    class LoginButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String id = view.getId();
            String password = view.getPassword();

            // 빈 값 체크
            if (id.isEmpty() || password.isEmpty()) {
                view.showMessage("아이디와 비밀번호를 모두 입력하세요.");
                return;
            }

            try {
                // 서버 연결
                String serverIp = ConfigLoader.getProperty("server.ip");
                int serverPort = Integer.parseInt(ConfigLoader.getProperty("server.port"));
                Socket socket = new Socket(serverIp, serverPort);
                
                // ⚠️ 중요: 소켓 타임아웃 설정 (30초)
                socket.setSoTimeout(30000);
                System.out.println("[LoginController] 소켓 타임아웃 설정: 30초");
                
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 로그인 요청 전송
                out.println("LOGIN," + id + "," + password);
                String response = in.readLine();

                if (response == null) {
                    view.showMessage("서버로부터 응답이 없습니다.");
                    closeConnection(socket, in, out);
                    return;
                }

                // 서버 응답 처리
                switch (response.split(",")[0]) {
                    case "SERVER_BUSY":
                        view.showMessage("현재 접속 인원이 초과되었습니다. 나중에 다시 시도해주세요.");
                        closeConnection(socket, in, out);
                        break;

                    case "ALREADY_LOGGED_IN":
                        view.showMessage("이미 로그인된 사용자입니다. 다른 사용자 계정으로 로그인하거나 나중에 다시 시도하세요.");
                        closeConnection(socket, in, out);
                        break;

                    case "SUCCESS":
                        String userName = response.split(",").length > 1 ? response.split(",")[1] : "이름없음";
                        
                        // 세션 저장
                        Session.getInstance().setLoggedInUserId(id);
                        Session.getInstance().setLoggedInUserName(userName);
                        Session.getInstance().setSocket(socket);
                        Session.getInstance().setIn(in);
                        Session.getInstance().setOut(out);
                        
                        // ✅ INIT 메시지 전송 (서버 스레드 블로킹 방지용)
                        out.println("INIT");
                        out.flush();
            
                        //  사용자 역할 설정 (S: 학생, P: 교수, A: 조교)
                        String role = switch (id.charAt(0)) {
                            case 'S' -> "학생";
                            case 'P' -> "교수";
                            case 'A' -> "조교";
                            default  -> "알 수 없음";
                        };
                        Session.getInstance().setLoggedInUserRole(role);
                        
                        // 로그인 성공 메시지 및 화면 전환
                        view.showMessage("로그인 성공!");
                        view.dispose();
                        openUserMainView(id.charAt(0));
                        break;

                    case "FAIL":
                    default:
                        view.showMessage("로그인 실패: 아이디 또는 비밀번호가 틀렸습니다.");
                        closeConnection(socket, in, out);
                        break;
                }

            } catch (IOException ex) {
                view.showMessage("서버와 연결할 수 없습니다: " + ex.getMessage());
            }
        }
    }

    private void openUserMainView(char userType) {
        switch (userType) {
            case 'S': // 학생
            case 'P': // 교수
                // 이미 열려 있는 RoomSelect 닫기
                for (java.awt.Window window : java.awt.Window.getWindows()) {
                    if (window instanceof RoomSelect) {
                        window.dispose();
                    }
                }

                RoomSelect roomSelect = new RoomSelect();
                new RoomSelectController(roomSelect);

                // 윈도우 종료시 로그아웃 처리
                roomSelect.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        logoutAndCloseSocket();
                    }
                });

                roomSelect.setVisible(true);
                break;

            case 'A': // 조교
                Executive executive = new Executive();
                new ExecutiveController(executive);

                executive.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        logoutAndCloseSocket();
                    }
                });

                executive.setVisible(true);
                break;

            default:
                System.out.println("알 수 없는 사용자 유형입니다: " + userType);
                break;
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

    private void logoutAndCloseSocket() {
        try {
            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();
            Socket socket = Session.getInstance().getSocket();
            String userId = Session.getInstance().getLoggedInUserId();
            
            if (out != null) {
                out.println("EXIT");
                out.flush();
                System.out.println("EXIT 메시지 전송됨");
            }

            // 세션 정리
            Session.getInstance().clear();
            
            // 소켓 닫기
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("소켓 정상 종료");
            }

        } catch (IOException e) {
            System.out.println("소켓 종료 중 오류 발생: " + e.getMessage());
        }
    }

    private void closeConnection(Socket socket, BufferedReader in, PrintWriter out) {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
