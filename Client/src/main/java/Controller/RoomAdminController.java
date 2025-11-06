package Controller;

import View.RoomAdmin;
import View.Executive;
import Model.Session;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class RoomAdminController {

    private RoomAdmin view;

    public RoomAdminController(RoomAdmin view) {
        this.view = view;
        initListeners();
        view.getJButton2().addActionListener(e -> goBackToExecutive());
    }

    private void initListeners() {
        for (ActionListener al : view.getConfirmButton().getActionListeners()) {
            view.getConfirmButton().removeActionListener(al);
        }

        view.getConfirmButton().addActionListener(e -> {
            String roomNumber = view.getRoomNumberField().getText().trim();
            String status = (String) view.getStatusComboBox().getSelectedItem();

            if (roomNumber.isEmpty()) {
                JOptionPane.showMessageDialog(view, "강의실 번호를 입력하세요.");
                return;
            }

            new Thread(() -> {
                PrintWriter out = Session.getOut();
                BufferedReader in = Session.getIn();

                if (out == null || in == null) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다. 로그인 상태를 확인해주세요.")
                    );
                    return;
                }

                try {
                    String command = "UPDATE_ROOM_STATUS," + roomNumber + "," + status;
                    out.println(command);
                    out.flush();
                    System.out.println("[클라이언트 - RoomAdmin] 서버로 전송: " + command);

                    String response = in.readLine();
                    System.out.println("[클라이언트 - RoomAdmin] 서버로부터 응답: " + response);

                    SwingUtilities.invokeLater(() -> {
                        if (response == null) {
                            JOptionPane.showMessageDialog(view, "서버 응답이 없습니다. 연결이 끊겼거나 서버가 종료되었습니다.");
                            return;
                        }

                        switch (response) {
                            case "ROOM_STATUS_UPDATED" -> {
                                JOptionPane.showMessageDialog(view, "강의실 상태가 성공적으로 업데이트되었습니다.");
                                view.dispose(); // 현재 창 닫기

                                Executive executiveView = view.getExecutive(); // 다시 띄우기
                                if (executiveView != null) {
                                    executiveView.setVisible(true);
                                } else {
                                    System.err.println("[오류] Executive 인스턴스가 null입니다.");
                                }
                            }
                            case "INVALID_UPDATE_FORMAT" -> {
                                JOptionPane.showMessageDialog(view, "서버 요청 형식이 잘못되었습니다. 관리자에게 문의하세요.");
                            }
                            case "UPDATE_FAILED_SERVER_ERROR" -> {
                                JOptionPane.showMessageDialog(view, "서버 내부 오류로 인해 실패했습니다. 다시 시도해주세요.");
                            }
                            default -> {
                                if (response.startsWith("UNKNOWN_COMMAND")) {
                                    JOptionPane.showMessageDialog(view, "알 수 없는 명령입니다: " + response);
                                } else {
                                    JOptionPane.showMessageDialog(view, "강의실 상태 업데이트 실패: " + response);
                                }
                            }
                        }
                    });

                } catch (IOException ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(view,
                            "서버와의 통신 중 오류가 발생했습니다: " + ex.getMessage() +
                            "\n네트워크 연결을 확인하거나 다시 로그인해주세요.")
                    );
                }
            }).start();
        });
    }

    private void goBackToExecutive() {
        view.dispose();

        Executive executiveView = view.getExecutive();
        if (executiveView != null) {
            executiveView.setVisible(true);
        } else {
            System.err.println("[오류] Executive 인스턴스가 null입니다.");
        }
    }
}
