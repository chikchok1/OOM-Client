package Controller;

import Model.Session;
import Util.MessageDispatcher;
import View.ClassroomReservationApproval;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;

public class ClassroomReservationApprovalController {

    private ClassroomReservationApproval view;

    public ClassroomReservationApprovalController(ClassroomReservationApproval view) {
        this.view = view;

        // ✅ 백그라운드 스레드에서 데이터 로드
        loadAllRequestsAsync();
        
        setApproveButtonAction();
        setRejectButtonAction();
        addTableClickListener();
    }

    /**
     * ✅ 비동기로 예약 요청 목록 로드
     */
    private void loadAllRequestsAsync() {
        new Thread(() -> {
            loadAllRequests();
        }).start();
    }

    /**
     * ✅ MessageDispatcher를 사용하여 예약 요청 목록 로드
     */
    private void loadAllRequests() {
        //  연결 상태 확인
        if (!Session.getInstance().isConnected()) {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.")
            );
            return;
        }

        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();

        if (out == null || dispatcher == null) {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.")
            );
            return;
        }
        
        String userId = Session.getInstance().getLoggedInUserId();

        out.println("GET_RESERVATION_REQUESTS");
        out.flush();

        try {
            DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
            SwingUtilities.invokeLater(() -> model.setRowCount(0));

            while (true) {
                // ✅ MessageDispatcher를 통해 응답 대기
                String line = dispatcher.waitForResponse(30);
                
                if (line == null) {
                    System.err.println("[loadAllRequests] 타임아웃");
                    break;
                }
                
                if (line.equals("END_OF_REQUESTS")) {
                    break;
                }

                // ✅ 서버가 보낸 형식: id,time,date,day,room,name,studentCount
                String[] parts = line.split(",");
                if (parts.length == 7) {
                    // 정상적인 7개 필드
                    SwingUtilities.invokeLater(() -> model.addRow(parts));
                } else if (parts.length == 6) {
                    // 구 버전 호환성: 날짜가 없는 경우
                    String[] partsWithDate = new String[7];
                    partsWithDate[0] = parts[0]; // id
                    partsWithDate[1] = parts[1]; // time
                    partsWithDate[2] = "";       // date (비어있음)
                    partsWithDate[3] = parts[2]; // day
                    partsWithDate[4] = parts[3]; // room
                    partsWithDate[5] = parts[4]; // name
                    partsWithDate[6] = parts[5]; // studentCount
                    SwingUtilities.invokeLater(() -> model.addRow(partsWithDate));
                } else if (parts.length == 5) {
                    // 구 버전 호환성: 날짜와 학생 수가 없는 경우
                    String[] partsWithDateAndCount = new String[7];
                    partsWithDateAndCount[0] = parts[0]; // id
                    partsWithDateAndCount[1] = parts[1]; // time
                    partsWithDateAndCount[2] = "";       // date
                    partsWithDateAndCount[3] = parts[2]; // day
                    partsWithDateAndCount[4] = parts[3]; // room
                    partsWithDateAndCount[5] = parts[4]; // name
                    partsWithDateAndCount[6] = "1";      // studentCount
                    SwingUtilities.invokeLater(() -> model.addRow(partsWithDateAndCount));
                }
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(view, "서버 응답 오류: " + e.getMessage())
            );
        }
    }

    private void addTableClickListener() {
        view.getTable().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int selectedRow = view.getTable().getSelectedRow();
                if (selectedRow != -1) {
                    String reservationId = view.getTable().getValueAt(selectedRow, 0).toString();
                    view.setReservationId(reservationId);
                }
            }
        });
    }

    /**
     * ✅ 백그라운드 스레드에서 승인 처리
     */
    public void setApproveButtonAction() {
        view.getApproveButton().addActionListener(e -> {
            int selectedRow = view.getTable().getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(view, "승인할 행을 선택하세요.");
                return;
            }

            DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
            // ✅ 테이블 컬럼: 예약ID(0), 시간(1), 날짜(2), 요일(3), 강의실(4), 이름(5), 요청인원(6)
            String id = ((String) model.getValueAt(selectedRow, 0)).trim();
            String time = ((String) model.getValueAt(selectedRow, 1)).trim();
            String date = ((String) model.getValueAt(selectedRow, 2)).trim();
            String day = ((String) model.getValueAt(selectedRow, 3)).trim();
            String room = ((String) model.getValueAt(selectedRow, 4)).trim();
            String name = ((String) model.getValueAt(selectedRow, 5)).trim();

            // ✅ 백그라운드 스레드에서 실행
            new Thread(() -> {
                //  연결 상태 확인
                if (!Session.getInstance().isConnected()) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.")
                    );
                    return;
                }

                PrintWriter out = Session.getInstance().getOut();
                MessageDispatcher dispatcher = MessageDispatcher.getInstance();

                if (out == null || dispatcher == null) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.")
                    );
                    return;
                }

                out.println(String.format("APPROVE_RESERVATION,%s,%s,%s,%s,%s,%s", id, time, date, day, room, name));
                out.flush();

                try {
                    // ✅ MessageDispatcher를 통해 응답 대기
                    String response = dispatcher.waitForResponse(30);
                    
                    if (response == null) {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(view, "서버로부터 응답이 없습니다.")
                        );
                        return;
                    }

                    if ("APPROVE_SUCCESS".equals(response)) {
                        SwingUtilities.invokeLater(() -> {
                            model.removeRow(selectedRow);
                            JOptionPane.showMessageDialog(view, "예약 승인 완료되었습니다.");
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(view, "승인 실패: " + response)
                        );
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버 응답 오류: " + ex.getMessage())
                    );
                }
            }).start();
        });
    }

    /**
     * ✅ 백그라운드 스레드에서 거절 처리
     */
    public void setRejectButtonAction() {
        view.getRejectButton().addActionListener(e -> {
            int selectedRow = view.getTable().getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(view, "거절할 행을 선택하세요.");
                return;
            }

            DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
            // ✅ 테이블 컬럼: 예약ID(0), 시간(1), 날짜(2), 요일(3), 강의실(4), 이름(5), 요청인원(6)
            String id = ((String) model.getValueAt(selectedRow, 0)).trim();
            String time = ((String) model.getValueAt(selectedRow, 1)).trim();
            String date = ((String) model.getValueAt(selectedRow, 2)).trim();
            String day = ((String) model.getValueAt(selectedRow, 3)).trim();
            String room = ((String) model.getValueAt(selectedRow, 4)).trim();
            String name = ((String) model.getValueAt(selectedRow, 5)).trim();

            // ✅ 백그라운드 스레드에서 실행
            new Thread(() -> {
                //  연결 상태 확인
                if (!Session.getInstance().isConnected()) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.")
                    );
                    return;
                }

                PrintWriter out = Session.getInstance().getOut();
                MessageDispatcher dispatcher = MessageDispatcher.getInstance();

                if (out == null || dispatcher == null) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.")
                    );
                    return;
                }

                out.println(String.format("REJECT_RESERVATION,%s,%s,%s,%s,%s,%s", id, time, date, day, room, name));
                out.flush();

                try {
                    // ✅ MessageDispatcher를 통해 응답 대기
                    String response = dispatcher.waitForResponse(30);
                    
                    if (response == null) {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(view, "서버로부터 응답이 없습니다.")
                        );
                        return;
                    }

                    if ("REJECT_SUCCESS".equals(response)) {
                        SwingUtilities.invokeLater(() -> {
                            model.removeRow(selectedRow);
                            JOptionPane.showMessageDialog(view, "거절 처리 완료되었습니다.");
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(view, "거절 실패: " + response)
                        );
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(view, "서버 응답 오류: " + ex.getMessage())
                    );
                }
            }).start();
        });
    }
}
