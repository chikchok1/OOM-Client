package Controller;

import common.model.MembershipModel;
import Model.Session;
import Util.MessageDispatcher; // ✅ 추가
import View.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
 

public class LoginController {

    private LoginForm view;
    private MembershipView membershipView;

    public LoginController(LoginForm view) {
        this.view = view;

        this.view.addLoginListener(e -> handleLogin());
        this.view.addJoinListener(e -> openMembership());
        this.view.enableEnterKeyForLogin(e -> handleLogin());
    }

    public void handleLogin() {
        boolean ok = ClientFacade.login(view);
        if (!ok) return;

        // 로그인 성공 시 세션에 저장된 사용자 ID로 화면 전환
        String loggedId = Session.getInstance().getLoggedInUserId();
        if (loggedId != null && !loggedId.isEmpty()) {
            openUserMainView(loggedId.charAt(0));
        }
    }

    
    // Notification helpers are handled in ClientFacade

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
            Socket socket = Session.getInstance().getSocket();
            String userId = Session.getInstance().getLoggedInUserId();
            
            // ✅ MessageDispatcher 종료
            MessageDispatcher dispatcher = MessageDispatcher.getInstance();
            if (dispatcher != null) {
                dispatcher.stopDispatcher();
                System.out.println("[LoginController] MessageDispatcher 종료: " + userId);
            }

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

    // connection cleanup is handled centrally by Session.clear() / ClientFacade
}
