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
        ChangePasswordView changePasswordView = new ChangePasswordView(view);  // RoomSelect 전달
        new ChangePasswordController(changePasswordView);
        changePasswordView.setVisible(true);
        view.setVisible(false);  // 재사용을 위해 숨김 처리
    }

    private void openReservClass() {
        ReservClassView reservClassView = new ReservClassView();
        ReservClassController controller = new ReservClassController(reservClassView);  // ✅ Controller 먼저 생성
        reservClassView.setVisible(true);
        view.dispose();
    }

    private void openReservLab() {
    ReservLabView reservLabView = new ReservLabView();
    ReservLabController controller = new ReservLabController(reservLabView); // ✅ 변수로 잡기
    reservLabView.setVisible(true);
    view.dispose();
}

    private void openReservedClassRoom() {
        ReservedRoomView reservedRoomView = new ReservedRoomView(this.view); // RoomSelect 전달
        new ReservedRoomController(reservedRoomView);
        reservedRoomView.setVisible(true);
        view.setVisible(false); // dispose()가 아닌 setVisible(false)로 창 재사용
    }

    private void openReservationChange() {
        Reservationchangeview changeView = new Reservationchangeview();
        new ReservationchangeviewController(changeView);
        changeView.setVisible(true);
        view.dispose();
    }

    private void logoutAndCloseSocket() {
        try {
            PrintWriter out = Session.getOut();
            BufferedReader in = Session.getIn();
            Socket socket = Session.getSocket();

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

            Session.clear();

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

        RoomSelect.destroyInstance(); // 인스턴스 초기화
        view.dispose();

        LoginForm loginForm = new LoginForm();
        new LoginController(loginForm);
        loginForm.setVisible(true);
    }
}
