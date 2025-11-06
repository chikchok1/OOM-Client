package Controller;

import View.Executive;
import View.ReservedRoomView;
import View.LoginForm;
import View.RoomAdmin;
import View.ClientAdmin;
import View.ChangePasswordView;
import View.ClassroomReservationApproval;
import Model.Session;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ExecutiveController {

    private final Executive executive;
    private static boolean hasShownAlert = false;

    public ExecutiveController(Executive executive) {
        this.executive = executive;

        this.executive.setChangePasswordActionListener(e -> openChangePasswordView());

        // 예약 요청 알림
        if (!hasShownAlert) {
            int count = getPendingRequestCountFromServer();
            if (count > 0) {
                JOptionPane.showMessageDialog(
                        executive,
                        "현재 대기 중인 예약 요청이 총 " + count + "건 있습니다.",
                        "예약 요청 알림",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
            hasShownAlert = true;
        }

        // [1] 예약 확인
        this.executive.getViewReservedButton().addActionListener(e -> openReservedRoomView());

        // [2] 강의실/실습실 관리
        this.executive.getJButton2().addActionListener(e -> openRoomAdminView());

        // [3] 로그아웃
        this.executive.getJButton3().addActionListener(e -> logout());

        // [4] 고객 관리
        this.executive.getJButton5().addActionListener(e -> openClientAdminView());

        // [5] 예약 승인
        this.executive.getJButton6().addActionListener(e -> openReservationApprovalView());
    }

    private void openReservedRoomView() {
        ReservedRoomView view = new ReservedRoomView(executive);
        new ReservedRoomController(view);
        view.setVisible(true);
        executive.setVisible(false);
    }

    private void openRoomAdminView() {
        RoomAdmin view = new RoomAdmin(executive);
        new RoomAdminController(view);
        view.setVisible(true);
        executive.setVisible(false);
    }

    private void openClientAdminView() {
        ClientAdmin view = new ClientAdmin(executive);
        new ClientAdminController(view);
        view.setVisible(true);
        executive.setVisible(false);
    }

    private void openReservationApprovalView() {
        ClassroomReservationApproval view = new ClassroomReservationApproval(executive);
        new ClassroomReservationApprovalController(view);
        view.setVisible(true);
        executive.setVisible(false);
    }

    private void openChangePasswordView() {
        ChangePasswordView changePasswordView = new ChangePasswordView(executive);  // ✅ Executive 전달
        new ChangePasswordController(changePasswordView);
        changePasswordView.setVisible(true);
        executive.setVisible(false);  // ✅ 창 숨김 (재사용 목적)
    }

    private void logout() {
        try {
            PrintWriter out = Session.getOut();
            BufferedReader in = Session.getIn();

            if (out != null) {
                out.println("EXIT");
                out.flush();
            }

            if (in != null) {
                String response = in.readLine();
                System.out.println("서버 응답: " + response);
            }
        } catch (Exception ex) {
            System.out.println("로그아웃 중 오류: " + ex.getMessage());
        } finally {
            Session.clear();
            executive.dispose();

            LoginForm loginForm = new LoginForm();
            new LoginController(loginForm);
            loginForm.setVisible(true);
        }
    }

    private int getPendingRequestCountFromServer() {
        PrintWriter out = Session.getOut();
        BufferedReader in = Session.getIn();

        if (out == null || in == null) {
            System.out.println("서버 연결이 없습니다.");
            return 0;
        }

        try {
            out.println("COUNT_PENDING_REQUEST");
            out.flush();
            String response = in.readLine();
            if (response != null && response.startsWith("PENDING_COUNT:")) {
                return Integer.parseInt(response.split(":")[1].trim());
            }
        } catch (IOException e) {
            System.out.println("서버 응답 오류: " + e.getMessage());
        }
        return 0;
    }
}
