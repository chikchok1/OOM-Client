package Controller;

import View.ClientAdmin;
import View.Executive;
import Model.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;

public class ClientAdminController {

    private final ClientAdmin view;

    public ClientAdminController(ClientAdmin view) {
        this.view = view;

        loadUsersFromServer(); // 서버에서 사용자 목록 로드

        view.getJButton2().addActionListener(e -> deleteSelectedUser());
        view.getJButton1().addActionListener(e -> updateSelectedUser());
        view.getJButton3().addActionListener(e -> goBackToExecutive());
    }

    protected void loadUsersFromServer() {
        DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
        model.setRowCount(0);  // 기존 데이터 초기화

        PrintWriter out = Session.getInstance().getOut();
        Util.MessageDispatcher dispatcher = Util.MessageDispatcher.getInstance();

        if (out == null || dispatcher == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        // GUI 스레드 블로킹 방지를 위해 백그라운드 스레드에서 실행
        new Thread(() -> {
            try {
                out.println("GET_ALL_USERS");
                out.flush();
                System.out.println("[ClientAdmin] GET_ALL_USERS 요청 전송");

                // MessageDispatcher를 통해 응답 수신
                String line;
                while ((line = dispatcher.waitForResponse(30)) != null) {
                    System.out.println("[ClientAdmin] 수신: " + line);
                    
                    if ("END_OF_USERS".equals(line)) {
                        System.out.println("[ClientAdmin] 사용자 목록 로드 완료");
                        break;
                    }

                    String[] tokens = line.split(",");
                    if (tokens.length == 3) {
                        // GUI 업데이트는 EDT에서 실행
                        SwingUtilities.invokeLater(() -> model.addRow(tokens));
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(view, "서버에서 사용자 목록을 불러오는 중 오류 발생: " + e.getMessage())
                );
            }
        }).start();
    }

    private void deleteSelectedUser() {
        JTable table = view.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(view, "삭제할 사용자를 선택하세요.");
            return;
        }

        String userId = (String) model.getValueAt(row, 1);

        PrintWriter out = Session.getInstance().getOut();
        Util.MessageDispatcher dispatcher = Util.MessageDispatcher.getInstance();
        
        if (out == null || dispatcher == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        // ✅ 백그라운드 스레드에서 실행
        new Thread(() -> {
            try {
                out.println("DELETE_USER," + userId);
                out.flush();

                // ✅ MessageDispatcher를 통해 응답 수신
                String response = dispatcher.waitForResponse(10);
                
                if (response == null) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버 응답 없음")
                    );
                    return;
                }

                // ✅ ERROR: 응답 처리
                if (response.startsWith("ERROR:")) {
                    String[] parts = response.split(":", 3);
                    String errorMessage = parts.length >= 3 ? parts[2] : "알 수 없는 오류";
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "삭제 실패: " + errorMessage)
                    );
                    return;
                }

                if ("DELETE_SUCCESS".equals(response)) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(view, "삭제 성공");
                        loadUsersFromServer();  // 전체 다시 로딩
                    });
                } else {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "삭제 실패: " + response)
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(view, "삭제 중 오류 발생: " + e.getMessage())
                );
            }
        }).start();
    }

    private void updateSelectedUser() {
        JTable table = view.getTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(view, "수정할 사용자를 선택하세요.");
            return;
        }

        String userId = (String) model.getValueAt(row, 1);
        String oldName = (String) model.getValueAt(row, 0);
        String oldPw = (String) model.getValueAt(row, 2);

        String newName = JOptionPane.showInputDialog(view, "새 이름:", oldName);
        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        String newPw = JOptionPane.showInputDialog(view, "새 비밀번호:", oldPw);
        if (newPw == null || newPw.trim().isEmpty()) {
            return;
        }

        PrintWriter out = Session.getInstance().getOut();
        Util.MessageDispatcher dispatcher = Util.MessageDispatcher.getInstance();
        
        if (out == null || dispatcher == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        // ✅ 백그라운드 스레드에서 실행
        new Thread(() -> {
            try {
                out.println("UPDATE_USER," + userId + "," + newName + "," + newPw);
                out.flush();
                System.out.println("[ClientAdmin] UPDATE_USER 요청 전송: " + userId);

                // ✅ MessageDispatcher를 통해 응답 수신
                String response = dispatcher.waitForResponse(10);
                System.out.println("[ClientAdmin] UPDATE 응답 수신: " + response);
                
                if (response == null) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버 응답 없음")
                    );
                    return;
                }

                // ✅ ERROR: 응답 처리
                if (response.startsWith("ERROR:")) {
                    String[] parts = response.split(":", 3);
                    String errorMessage = parts.length >= 3 ? parts[2] : "알 수 없는 오류";
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "수정 실패: " + errorMessage)
                    );
                    return;
                }

                if ("UPDATE_SUCCESS".equals(response)) {
                    SwingUtilities.invokeLater(() -> {
                        model.setValueAt(newName, row, 0);
                        model.setValueAt(newPw, row, 2);
                        JOptionPane.showMessageDialog(view, "수정 성공");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "수정 실패: " + response)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(view, "수정 요청 중 오류 발생: " + e.getMessage())
                );
            }
        }).start();
    }

    private void goBackToExecutive() {
        view.dispose();
        Executive executive = view.getExecutive();
        if (executive != null) {
            executive.setVisible(true);
        } else {
            System.err.println("Executive 인스턴스가 null입니다.");
        }
    }
}
