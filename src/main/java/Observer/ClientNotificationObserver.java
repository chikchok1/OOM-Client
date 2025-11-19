package Observer;

import common.observer.ReservationNotification;
import common.observer.ReservationObserver;
import javax.swing.*;

/**
 * Concrete Observer - 클라이언트에서 알림을 받아 UI에 표시
 */
public class ClientNotificationObserver implements ReservationObserver {
    
    private final String userId;
    private final JFrame parentFrame;  // 알림을 표시할 부모 프레임
    
    public ClientNotificationObserver(String userId, JFrame parentFrame) {
        this.userId = userId;
        this.parentFrame = parentFrame;
    }
    
    @Override
    public void update(ReservationNotification notification) {
        // UI 스레드에서 실행
        SwingUtilities.invokeLater(() -> {
            showNotification(notification);
        });
    }
    
    /**
     * 알림을 다이얼로그로 표시
     */
    private void showNotification(ReservationNotification notification) {
        String title = getNotificationTitle(notification.getType());
        String message = formatNotificationMessage(notification);
        
        // JOptionPane으로 알림 표시
        int messageType = getMessageType(notification.getType());
        JOptionPane.showMessageDialog(
            parentFrame,
            message,
            title,
            messageType
        );
        
        System.out.println("[클라이언트 알림] " + title + ": " + message);
    }
    
    /**
     * 알림 유형에 따른 제목 반환
     */
    private String getNotificationTitle(ReservationNotification.NotificationType type) {
        switch (type) {
            case APPROVED:
                return "예약 승인";
            case REJECTED:
                return "예약 거절";
            case CHANGE_APPROVED:
                return "예약 변경 승인";
            case CHANGE_REJECTED:
                return "예약 변경 거절";
            case CANCELLED:
                return "예약 취소";
            default:
                return "알림";
        }
    }
    
    /**
     * 알림 메시지 포맷팅
     */
    private String formatNotificationMessage(ReservationNotification notification) {
        StringBuilder sb = new StringBuilder();
        sb.append(notification.getMessage()).append("\n\n");
        sb.append("상세 정보:\n");
        sb.append("강의실/실습실: ").append(notification.getRoom()).append("\n");
        sb.append("날짜: ").append(notification.getDate()).append(" (").append(notification.getDay()).append(")\n");
        sb.append("시간: ").append(notification.getTime()).append("\n");
        sb.append("처리 시간: ").append(notification.getTimestamp().toString());
        return sb.toString();
    }
    
    /**
     * 알림 유형에 따른 메시지 타입 반환
     */
    private int getMessageType(ReservationNotification.NotificationType type) {
        switch (type) {
            case APPROVED:
            case CHANGE_APPROVED:
                return JOptionPane.INFORMATION_MESSAGE;
            case REJECTED:
            case CHANGE_REJECTED:
            case CANCELLED:
                return JOptionPane.WARNING_MESSAGE;
            default:
                return JOptionPane.PLAIN_MESSAGE;
        }
    }
    
    public String getUserId() {
        return userId;
    }
}
