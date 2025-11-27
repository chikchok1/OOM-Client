package Controller;

import Model.Session;
import View.ChangePasswordView;
import View.RoomSelect;
import View.ReservClassView;
import View.ReservLabView;
import View.LoginForm;
import View.Reservationchangeview;
import View.ReservedRoomView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JOptionPane;

public class RoomSelectController {

    private RoomSelect view;

    public RoomSelectController(RoomSelect view) {
        System.out.println("RoomSelectController 연결됨");

        this.view = view;

        // 버튼 클릭 시 동작 연결
        this.view.setClassButtonActionListener(e -> openReservClass());
        this.view.setLabButtonActionListener(e -> openReservLab());
        this.view.setViewReservedActionListener(e -> openReservedClassRoom());
        this.view.setLogOutButtonActionListener(e -> handleLogout());

        System.out.println(">> setChangePasswordActionListener() 호출 전");
        this.view.setChangePasswordActionListener(e -> openChangePasswordView());
        System.out.println(">> setChangePasswordActionListener() 호출 완료");

        this.view.setReservationChangeActionListener(e -> openReservationChange());
    }

    private void openChangePasswordView() {
        ChangePasswordView changePasswordView = new ChangePasswordView(view);
        new ChangePasswordController(changePasswordView);
        changePasswordView.setVisible(true);
        view.setVisible(false);
    }

    private void openReservClass() {
        // ✅ 서버 연결 확인
        if (!isServerConnected()) {
            showConnectionError();
            return;
        }
        
        ReservClassView reservClassView = new ReservClassView();
        ReservClassController controller = new ReservClassController(reservClassView);
        reservClassView.setVisible(true);
        view.dispose();
    }

    private void openReservLab() {
        // ✅ 서버 연결 확인
        if (!isServerConnected()) {
            showConnectionError();
            return;
        }
        
        ReservLabView reservLabView = new ReservLabView();
        ReservLabController controller = new ReservLabController(reservLabView);
        reservLabView.setVisible(true);
        view.dispose();
    }

    private void openReservedClassRoom() {
        // ✅ 서버 연결 확인
        if (!isServerConnected()) {
            showConnectionError();
            return;
        }
        
        ReservedRoomView reservedRoomView = new ReservedRoomView(this.view);
        new ReservedRoomController(reservedRoomView);
        reservedRoomView.setVisible(true);
        view.setVisible(false);
    }

    private void openReservationChange() {
        // ✅ 서버 연결 확인 추가 - 이게 핵심!
        if (!isServerConnected()) {
            showConnectionError();
            return;
        }
        
        Reservationchangeview changeView = new Reservationchangeview();
        new ReservationchangeviewController(changeView);
        changeView.setVisible(true);
        view.dispose();
    }

    // ✅ 서버 연결 상태 확인 메서드
    private boolean isServerConnected() {
        return Session.getInstance().isConnected();
    }

    // ✅ 연결 오류 메시지 표시
    private void showConnectionError() {
        JOptionPane.showMessageDialog(view,
            "서버 연결이 끊어졌습니다.\n다시 로그인해주세요.",
            "연결 오류",
            JOptionPane.ERROR_MESSAGE);
        
        // 로그인 화면으로 강제 이동
        handleLogout();
    }

    private void logoutAndCloseSocket() {
        try {
            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();
            Socket socket = Session.getInstance().getSocket();

            if (out != null) {
                out.println("EXIT");
                out.flush();
                System.out.println("EXIT 메시지 전송됨");
            }

            if (in != null) {
                String response = in.readLine();
                if ("LOGOUT_SUCCESS".equals(response)) {
                    System.out.println("서버로부터 로그아웃 확인 받음");
                }
            }

            Session.getInstance().clear();

            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("소켓 정상 종료");
            }

        } catch (IOException e) {
            System.out.println("소켓 종료 중 오류 발생: " + e.getMessage());
        }
    }

    private void handleLogout() {
        System.out.println("로그아웃 버튼 클릭됨 - RoomSelect 종료 시도");
        
        // 서버에 로그아웃 요청 및 소켓 종료
        logoutAndCloseSocket();

        RoomSelect.destroyInstance();
        view.dispose();

        LoginForm loginForm = new LoginForm();
        new LoginController(loginForm);
        loginForm.setVisible(true);
    }
}
