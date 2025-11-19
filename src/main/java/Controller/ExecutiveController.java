package Controller;

import View.Executive;
import View.ReservedRoomView;
import View.LoginForm;
import View.RoomAdmin;
import View.ClientAdmin;
import View.ChangePasswordView;
import View.ClassroomReservationApproval;
import Model.Session;
import Util.MessageDispatcher; // ✅ 추가

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

        // ✅ 예약 요청 알림 - 백그라운드 스레드에서 실행
        if (!hasShownAlert) {
            new Thread(() -> {
                int count = getPendingRequestCountFromServer();
                if (count > 0) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                executive,
                                "현재 대기 중인 예약 요청이 총 " + count + "건 있습니다.",
                                "예약 요청 알림",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    });
                }
                hasShownAlert = true;
            }).start();
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
        ChangePasswordView changePasswordView = new ChangePasswordView(executive);
        new ChangePasswordController(changePasswordView);
        changePasswordView.setVisible(true);
        executive.setVisible(false);
    }

    private void logout() {
        try {
            PrintWriter out = Session.getInstance().getOut();
            MessageDispatcher dispatcher = MessageDispatcher.getInstance();

            if (out != null) {
                out.println("EXIT");
                out.flush();
            }

            if (dispatcher != null) {
                // ✅ MessageDispatcher를 통해 응답 대기
                String response = dispatcher.waitForResponse(5);
                System.out.println("서버 응답: " + response);
            }
        } catch (Exception ex) {
            System.out.println("로그아웃 중 오류: " + ex.getMessage());
        } finally {
            Session.getInstance().clear();
            executive.dispose();

            LoginForm loginForm = new LoginForm();
            new LoginController(loginForm);
            loginForm.setVisible(true);
        }
    }

    /**
     * ✅ MessageDispatcher를 사용하여 서버로부터 대기 중인 예약 요청 수 조회
     */
    private int getPendingRequestCountFromServer() {
        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();

        if (out == null || dispatcher == null) {
            System.out.println("서버 연결이 없습니다.");
            return 0;
        }

        try {
            out.println("COUNT_PENDING_REQUEST");
            out.flush();
            
            // ✅ MessageDispatcher를 통해 응답 대기 (30초 타임아웃)
            String response = dispatcher.waitForResponse(30);
            
            if (response != null && response.startsWith("PENDING_COUNT:")) {
                return Integer.parseInt(response.split(":")[1].trim());
            } else {
                System.out.println("서버 응답 형식 오류: " + response);
            }
        } catch (NumberFormatException e) {
            System.out.println("서버 응답 파싱 오류: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("서버 응답 오류: " + e.getMessage());
        }
        return 0;
    }
}
