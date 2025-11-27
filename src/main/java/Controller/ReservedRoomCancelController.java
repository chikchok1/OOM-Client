package Controller;

import Model.Session;
import Util.MessageDispatcher;
import View.Executive;
import View.ReservedRoomCancelView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;

public class ReservedRoomCancelController {

    private ReservedRoomCancelView view;
    private volatile boolean isLoading = false; // ✅ 로딩 중 플래그

    public ReservedRoomCancelController(ReservedRoomCancelView view) {
        this.view = view;

        loadUserReservations(); // 서버로부터 예약 목록 요청
        setCancelAction();      // 취소 버튼 처리
        setBackAction();        // 이전 버튼 처리
    }

    private void loadUserReservations() {
        // ✅ 이미 로딩 중이면 중복 요청 방지
        if (isLoading) {
            System.out.println("[ReservedRoomCancel] 이미 로딩 중 - 요청 무시");
            return;
        }
        
        DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
        model.setRowCount(0); // 테이블 초기화

        //  연결 상태 확인
        if (!Session.getInstance().isConnected()) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        String userId = Session.getInstance().getLoggedInUserId();
        String role = Session.getInstance().getLoggedInUserRole();

        if (out == null || dispatcher == null || userId == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않거나 로그인 정보가 없습니다.");
            return;
        }

        // ✅ GUI 스레드 블로킹 방지를 위해 백그라운드 스레드에서 실행
        new Thread(() -> {
            try {
                isLoading = true; // ✅ 로딩 시작
                
                // 조교는 전체 예약 조회, 나머지는 본인 예약만
                if ("조교".equals(role)) {
                    out.println("VIEW_ALL_RESERVATIONS");
                } else {
                    out.println("VIEW_MY_RESERVATIONS," + userId);
                }
                out.flush();
                System.out.println("[ReservedRoomCancel] 예약 목록 요청 전송");

                // ✅ 짧은 대기 (이전 응답이 큐에서 처리되도록)
                Thread.sleep(200);

                // MessageDispatcher를 통해 응답 수신
                String line;
                int receivedCount = 0;
                while ((line = dispatcher.waitForResponse(30)) != null) {
                    System.out.println("[ReservedRoomCancel] 수신 [" + receivedCount + "]: " + line);
                    
                    if (line.startsWith("END_")) {
                        System.out.println("[ReservedRoomCancel] 예약 목록 로드 완료 (총 " + receivedCount + "개)");
                        break;
                    }

                    // ✅ 서버 형식: userId,time,day,date,room,name,count
                    String[] parts = line.split(",");
                    if (parts.length >= 7) {
                        receivedCount++;
                        // GUI 업데이트는 EDT에서 실행
                        String[] rowData = parts;
                        SwingUtilities.invokeLater(() -> model.addRow(rowData));
                    } else if (parts.length == 5) {
                        // ✅ 구 형식 지원: userId,time,day,room,name
                        receivedCount++;
                        String[] newParts = new String[7];
                        System.arraycopy(parts, 0, newParts, 0, 3); // userId, time, day
                        newParts[3] = ""; // date (없음)
                        System.arraycopy(parts, 3, newParts, 4, 2); // room, name
                        newParts[6] = ""; // count (없음)
                        SwingUtilities.invokeLater(() -> model.addRow(newParts));
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("[ReservedRoomCancel] 스레드 인터럽트: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(view, "예약 목록 수신 중 오류 발생: " + e.getMessage())
                );
            } finally {
                isLoading = false; // ✅ 로딩 완료
            }
        }).start();
    }

    //  서버에 취소 요청 (MessageDispatcher 사용)
    private void setCancelAction() {
        view.getCancelButton().addActionListener(e -> {
            JTable table = view.getTable();
            int selectedRow = table.getSelectedRow();

            System.out.println("[취소버튼] 클릭됨");
            System.out.println("[취소버튼] 선택된 행: " + selectedRow);

            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(view, "취소할 예약을 선택하세요.");
                return;
            }

            // ✅ 테이블 형식: userId,time,day,date,room,name,count
            String userId = table.getValueAt(selectedRow, 0).toString();
            String time = table.getValueAt(selectedRow, 1).toString();
            String day = table.getValueAt(selectedRow, 2).toString();
            String date = table.getValueAt(selectedRow, 3).toString();
            String room = table.getValueAt(selectedRow, 4).toString();
            String userName = table.getValueAt(selectedRow, 5).toString();

            // 사용자 확인
            int confirm = JOptionPane.showConfirmDialog(
                view,
                String.format("다음 예약을 취소하시겠습니까?\n\n강의실: %s\n날짜: %s (%s)\n시간: %s", 
                    room, date, day, time),
                "예약 취소 확인",
                JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) {
                System.out.println("[취소버튼] 사용자가 취소를 취소함");
                return;
            }

            System.out.println("[취소버튼] 사용자 확인 완료, 서버 요청 시작");

            //  연결 상태 확인
            if (!Session.getInstance().isConnected()) {
                JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
                return;
            }

            // 버튼 비활성화 (중복 클릭 방지)
            view.getCancelButton().setEnabled(false);

            // 비동기 처리
            new Thread(() -> {
                try {
                    PrintWriter out = Session.getInstance().getOut();
                    MessageDispatcher dispatcher = MessageDispatcher.getInstance();

                    if (out == null) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
                            view.getCancelButton().setEnabled(true);
                        });
                        return;
                    }

                    // ✅ 서버 프로토콜: CANCEL_RESERVATION,requesterId,userId,day,date,time,room,userName
                    String requesterId = Session.getInstance().getLoggedInUserId();
                    
                    String command = String.format("CANCEL_RESERVATION,%s,%s,%s,%s,%s,%s,%s",
                        requesterId,     // 취소 요청한 사람 (조교)
                        userId,          // 예약 당사자
                        day, 
                        date,            // 날짜
                        time, 
                        room, 
                        userName
                    );
                    
                    System.out.println("[취소버튼] 서버 요청: " + command);
                    out.println(command);
                    out.flush();

                    // ✅ 짧은 대기 후 응답 수신
                    Thread.sleep(100);

                    // MessageDispatcher로 응답 대기 (30초 타임아웃)
                    String response = dispatcher.waitForResponse(30);

                    System.out.println("[취소버튼] 서버 응답: " + response);

                    SwingUtilities.invokeLater(() -> {
                        if (response == null) {
                            JOptionPane.showMessageDialog(view, "서버 응답 시간 초과");
                        } else if ("CANCEL_SUCCESS".equals(response)) {
                            // ✅ 테이블에서 해당 행만 삭제 (새로고침 대신)
                            ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
                            JOptionPane.showMessageDialog(view, "예약이 취소되었습니다.");
                        } else if ("CANCEL_FAILED_NOT_FOUND".equals(response)) {
                            JOptionPane.showMessageDialog(view, "취소할 예약을 찾을 수 없습니다.\n이미 취소되었거나 존재하지 않는 예약입니다.");
                            // ✅ 1초 대기 후 새로고침
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000);
                                    SwingUtilities.invokeLater(() -> loadUserReservations());
                                } catch (InterruptedException ex) {
                                    // 무시
                                }
                            }).start();
                        } else if (response.startsWith("END_")) {
                            // ✅ 잘못된 응답 - 큐에서 섞였음
                            JOptionPane.showMessageDialog(view, "예약 취소 실패: 응답 오류\n화면을 새로고침합니다.");
                            // 1초 대기 후 새로고침
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000);
                                    SwingUtilities.invokeLater(() -> loadUserReservations());
                                } catch (InterruptedException ex) {
                                    // 무시
                                }
                            }).start();
                        } else {
                            JOptionPane.showMessageDialog(view, "예약 취소 실패: " + response);
                        }
                        
                        // 버튼 다시 활성화
                        view.getCancelButton().setEnabled(true);
                    });
                } catch (InterruptedException ex) {
                    System.err.println("[취소버튼] 스레드 인터럽트: " + ex.getMessage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(view, "서버 통신 오류: " + ex.getMessage());
                        view.getCancelButton().setEnabled(true);
                    });
                }
            }).start();
        });
    }

    private void setBackAction() {
        view.getBackButton().addActionListener(e -> {
            view.dispose();
            Executive execView = new Executive();
            new ExecutiveController(execView);
            execView.setVisible(true);
        });
    }
}
