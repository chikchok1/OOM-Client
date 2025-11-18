package Controller;

import View.RoomAdmin;
import View.Executive;
import javax.swing.*;
import java.io.IOException;

public class RoomAdminController {

    private RoomAdmin view;
    private NetworkFacade networkFacade;

    public RoomAdminController(RoomAdmin view) {
        this.view = view;
        this.networkFacade = new NetworkFacade();
        initListeners();
        view.getJButton2().addActionListener(e -> goBackToExecutive());
    }

    private void initListeners() {
        // (기존 리스너 제거 로직 생략 가능하면 생략)
        
        view.getConfirmButton().addActionListener(e -> {
            String roomNumber = view.getRoomNumberField().getText().trim();
            String status = (String) view.getStatusComboBox().getSelectedItem();

            if (roomNumber.isEmpty()) {
                JOptionPane.showMessageDialog(view, "강의실 번호를 입력하세요.");
                return;
            }

            new Thread(() -> {
                try {
                    // [Facade 사용] 상세한 프로토콜 문자열(UPDATE_ROOM...)을 몰라도 됨
                    String response = networkFacade.updateRoomStatus(roomNumber, status);

                    SwingUtilities.invokeLater(() -> {
                        if (response == null) {
                            JOptionPane.showMessageDialog(view, "서버 응답 없음.");
                            return;
                        }
                        if (response.equals("ROOM_STATUS_UPDATED")) {
                            JOptionPane.showMessageDialog(view, "업데이트 성공.");
                            goBackToExecutive();
                        } else {
                            JOptionPane.showMessageDialog(view, "업데이트 실패: " + response);
                        }
                    });
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "오류 발생: " + ex.getMessage())
                    );
                }
            }).start();
        });
    }

    private void goBackToExecutive() {
        view.dispose();
        Executive executiveView = view.getExecutive();
        if (executiveView != null) executiveView.setVisible(true);
    }
}