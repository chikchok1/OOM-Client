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
        String capacityStr = view.getCapacityField().getText().trim();

        if (roomNumber.isEmpty()) {
            JOptionPane.showMessageDialog(view, "강의실 번호를 입력하세요.");
            return;
        }

        // "호"가 없으면 자동으로 추가
        if (!roomNumber.endsWith("호")) {
            roomNumber = roomNumber + "호";
        }
        
        final String finalRoomNumber = roomNumber; // final 변수로 만들기

        new Thread(() -> {
            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            if (out == null || in == null) {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.")
                );
                return;
            }

            try {
                // 1. 상태 업데이트
                String command = "UPDATE_ROOM_STATUS," + finalRoomNumber + "," + status;
                out.println(command);
                out.flush();
                System.out.println("[RoomAdmin] 상태 변경 전송: " + command);

                String response = in.readLine();
                System.out.println("[RoomAdmin] 상태 변경 응답: " + response);

                // 2. 수용인원 업데이트 (입력된 경우)
                String capacityResponse = "NOT_UPDATED";
                if (!capacityStr.isEmpty()) {
                    try {
                        int capacity = Integer.parseInt(capacityStr);
                        String capacityCommand = "UPDATE_ROOM_CAPACITY," + finalRoomNumber + "," + capacity;
                        out.println(capacityCommand);
                        out.flush();
                        System.out.println("[RoomAdmin] 수용인원 변경 전송: " + capacityCommand);
                        
                        capacityResponse = in.readLine();
                        System.out.println("[RoomAdmin] 수용인원 변경 응답: " + capacityResponse);
                    } catch (NumberFormatException ex) {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(view, "수용인원은 숫자로 입력하세요."));
                        return;
                    }
                }

                // 3. 결과 처리
                final String finalCapacityResponse = capacityResponse;
                SwingUtilities.invokeLater(() -> {
                    if (response == null) {
                        JOptionPane.showMessageDialog(view, "서버 응답이 없습니다.");
                        return;
                    }

                    // 상태 업데이트 성공
                    if ("ROOM_STATUS_UPDATED".equals(response)) {
                        StringBuilder message = new StringBuilder();
                        message.append(finalRoomNumber).append(" 상태가 ").append(status).append("로 변경되었습니다.");
                        
                        // 수용인원도 업데이트했다면
                        if ("CAPACITY_UPDATED".equals(finalCapacityResponse)) {
                            message.append("\n수용인원도 ").append(capacityStr).append("명으로 변경되었습니다.");
                        } else if (!capacityStr.isEmpty() && !"NOT_UPDATED".equals(finalCapacityResponse)) {
                            if ("ROOM_NOT_FOUND".equals(finalCapacityResponse)) {
                                message.append("\n(주의: 수용인원 변경 실패 - 강의실을 찾을 수 없습니다)");
                            } else {
                                message.append("\n수용인원 변경 실패: ").append(finalCapacityResponse);
                            }
                        }
                        
                        JOptionPane.showMessageDialog(view, message.toString());
                        view.dispose();

                        Executive executiveView = view.getExecutive();
                        if (executiveView != null) {
                            executiveView.setVisible(true);
                        }
                    } else {
                        // 에러 처리
                        JOptionPane.showMessageDialog(view, "상태 업데이트 실패: " + response);
                    }
                });

            } catch (IOException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(view, "통신 오류: " + ex.getMessage())
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
    
    private void updateRoomSettings() {
    String roomNumber = view.getRoomNumberField().getText().trim();
    String status = (String) view.getStatusComboBox().getSelectedItem();
    String capacityStr = view.getCapacityField().getText().trim();
    
    if (roomNumber.isEmpty()) {
        JOptionPane.showMessageDialog(view, "강의실 번호를 입력하세요.");
        return;
    }

    new Thread(() -> {
        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();

        try {
            // 1. 상태 업데이트
            out.println("UPDATE_ROOM_STATUS," + roomNumber + "," + status);
            out.flush();
            String statusResponse = in.readLine();
            
            // 2. 수용인원 업데이트 (입력된 경우)
            if (!capacityStr.isEmpty()) {
                try {
                    int capacity = Integer.parseInt(capacityStr);
                    out.println("UPDATE_ROOM_CAPACITY," + roomNumber + "," + capacity);
                    out.flush();
                    String capacityResponse = in.readLine();
                    
                    SwingUtilities.invokeLater(() -> {
                        if ("CAPACITY_UPDATED".equals(capacityResponse)) {
                            JOptionPane.showMessageDialog(view, 
                                "강의실 설정이 업데이트되었습니다.\n" +
                                "상태: " + status + "\n" +
                                "수용인원: " + capacity + "명");
                        }
                    });
                } catch (NumberFormatException e) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "올바른 숫자를 입력하세요."));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }).start();
}
    
}
