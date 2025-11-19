package Observer;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

/**
 * 클라이언트에서 서버로부터 실시간 알림을 수신하는 리스너 스레드
 * 

 */
public class NotificationListener extends Thread {
    
    private final Socket socket;
    private final String userId;
    private final JFrame parentFrame;
    private volatile boolean running = true;
    
    public NotificationListener(Socket socket, String userId, JFrame parentFrame) {
        this.socket = socket;
        this.userId = userId;
        this.parentFrame = parentFrame;
        this.setDaemon(true);  // 메인 스레드 종료 시 함께 종료
        this.setName("NotificationListener-" + userId);
    }
    
    @Override
    public void run() {
        System.out.println("[알림 리스너] " + userId + " 알림 수신 시작");
        System.out.println("[알림 리스너] ️ 동기 응답 보호를 위해 임시 비활성화됨");
        System.out.println("[알림 리스너] 알림 기능을 사용하려면 아키텍처 리팩토링 필요");
        
        System.out.println("[알림 리스너] " + userId + " 대기 모드 (비활성화)");
    }
    
    /**
     * 서버로부터 받은 알림 메시지 처리
     * 프로토콜: NOTIFICATION,타입,메시지,강의실,날짜,요일,시간
     */
    private void handleNotification(String message) {
        try {
            String[] parts = message.split(",", 7);
            
            if (parts.length < 7) {
                System.err.println("[알림 리스너] 잘못된 알림 형식: " + message);
                return;
            }
            
            String typeStr = parts[1];
            String notificationMessage = parts[2];
            String room = parts[3];
            String date = parts[4];
            String day = parts[5];
            String time = parts[6];
            
            // UI 스레드에서 알림 다이얼로그 표시
            SwingUtilities.invokeLater(() -> {
                showNotificationDialog(typeStr, notificationMessage, room, date, day, time);
            });
            
        } catch (Exception e) {
            System.err.println("[알림 리스너] 알림 처리 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 알림 다이얼로그 표시
     */
    private void showNotificationDialog(String typeStr, String message, 
                                       String room, String date, String day, String time) {
        String title = getNotificationTitle(typeStr);
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n\n");
        sb.append("상세 정보:\n");
        sb.append("강의실/실습실: ").append(room).append("\n");
        sb.append("날짜: ").append(date).append(" (").append(day).append(")\n");
        sb.append("시간: ").append(time);
        
        int messageType = getMessageType(typeStr);
        
        JOptionPane.showMessageDialog(
            parentFrame,
            sb.toString(),
            title,
            messageType
        );
        
        System.out.println("[클라이언트 알림] " + title + ": " + message);
    }
    
    /**
     * 알림 유형에 따른 제목 반환
     */
    private String getNotificationTitle(String typeStr) {
        switch (typeStr) {
            case "APPROVED":
                return " 예약 승인";
            case "REJECTED":
                return " 예약 거절";
            case "CHANGE_APPROVED":
                return " 예약 변경 승인";
            case "CHANGE_REJECTED":
                return " 예약 변경 거절";
            default:
                return " 알림";
        }
    }
    
    /**
     * 알림 유형에 따른 메시지 타입 반환
     */
    private int getMessageType(String typeStr) {
        if (typeStr.contains("APPROVED")) {
            return JOptionPane.INFORMATION_MESSAGE;
        } else if (typeStr.contains("REJECTED")) {
            return JOptionPane.WARNING_MESSAGE;
        }
        return JOptionPane.PLAIN_MESSAGE;
    }
    
    /**
     * 리스너 종료
     */
    public void stopListening() {
        running = false;
        System.out.println("[알림 리스너] 종료 요청");
    }
}
