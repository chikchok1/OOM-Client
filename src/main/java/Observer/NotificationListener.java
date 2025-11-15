package Observer;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * 클라이언트가 서버로부터 알림을 수신하는 리스너 스레드
 * NOTIFICATION 메시지만 처리하고, 나머지는 무시
 */
public class NotificationListener extends Thread {
    
    private final BufferedReader in;
    private final String userId;
    private final JFrame parentFrame;
    private volatile boolean running = true;
    
    public NotificationListener(BufferedReader in, String userId, JFrame parentFrame) {
        this.in = in;
        this.userId = userId;
        this.parentFrame = parentFrame;
        this.setDaemon(true);  // 메인 스레드 종료 시 같이 종료
        this.setName("NotificationListener-" + userId);
    }
    
    @Override
    public void run() {
        System.out.println("[알림리스너] " + userId + " 알림 수신 시작");
        
        // ⚠️ 주의: BufferedReader를 공유하면 안 되므로
        // 이 방법은 임시 방편입니다
        // 실제로는 별도 소켓을 사용해야 합니다
        
        System.out.println("[알림리스너] " + userId + " - 현재 구조상 BufferedReader 공유 문제로 비활성화됨");
        System.out.println("[알림리스너] " + userId + " - 알림 기능은 서버에서 전송되나 클라이언트에서 표시되지 않음");
        
        // 스레드는 유지하되 실제 읽기는 하지 않음
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        System.out.println("[알림리스너] " + userId + " 알림 수신 종료");
    }
    
    /**
     * 리스너 종료
     */
    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
