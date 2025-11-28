package Util;

import Model.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 서버로부터 받는 모든 메시지를 라우팅하는 디스패처
 * - NOTIFICATION 메시지 → 알림 핸들러로 전달
 * - 일반 응답 메시지 → 동기 요청 대기 큐로 전달
 */
public class MessageDispatcher extends Thread {
    
    private static MessageDispatcher instance;
    private static final Object lock = new Object(); // 추가적인 동기화 보장
    private final BufferedReader in;
    private final BlockingQueue<String> responseQueue;
    private Consumer<String> notificationHandler;
    private volatile boolean running = true;
    
    private MessageDispatcher(BufferedReader in) {
        this.in = in;
        this.responseQueue = new LinkedBlockingQueue<>();
        this.setDaemon(true);
        this.setName("MessageDispatcher");
    }
    
    /**
     * 싱글톤 인스턴스 생성 및 시작
     * Thread-safe: 동시 호출 시에도 하나의 인스턴스만 생성되도록 보장
     */
    public static void startDispatcher(BufferedReader in) {
        synchronized (lock) {
            // 인스턴스가 없으면 새로 생성
            if (instance == null) {
                instance = new MessageDispatcher(in);
                instance.start();
                
                // 스레드가 실제로 시작될 때까지 대기
                try {
                    Thread.sleep(50); // 스레드 시작 보장
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                System.out.println("[MessageDispatcher] 시작됨");
            } else if (!instance.isAlive()) {
                // 기존 인스턴스가 종료되었다면 새로 생성
                instance = new MessageDispatcher(in);
                instance.start();
                
                // 스레드가 실제로 시작될 때까지 대기
                try {
                    Thread.sleep(50); // 스레드 시작 보장
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                System.out.println("[MessageDispatcher] 시작됨");
            }
            // 이미 실행 중인 인스턴스가 있으면 아무것도 하지 않음
        }
    }
    
    /**
     * 싱글톤 인스턴스 반환
     */
    public static MessageDispatcher getInstance() {
        synchronized (lock) {
            return instance;
        }
    }
    
    /**
     * 알림 핸들러 등록
     */
    public void setNotificationHandler(Consumer<String> handler) {
        this.notificationHandler = handler;
    }
    
    /**
     * 동기 응답 대기 (타임아웃 적용)
     * @param timeoutSeconds 타임아웃 (초)
     * @return 서버 응답 (타임아웃 시 null)
     */
    public String waitForResponse(int timeoutSeconds) {
        try {
            return responseQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("[MessageDispatcher] 응답 대기 중단: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 메시지 수신 및 라우팅
     * SocketTimeoutException을 무시하고 계속 실행
     */
    @Override
    public void run() {
        System.out.println("[MessageDispatcher] 메시지 수신 시작");
        
        while (running && !isInterrupted()) {
            try {
                String message = in.readLine();
                
                if (message == null) {
                    System.out.println("[MessageDispatcher] 서버 연결 종료");
                    break;
                }
                
                // 메시지 라우팅
                if (message.startsWith("NOTIFICATION,")) {
                    // 알림 메시지 → 알림 핸들러로 전달
                    if (notificationHandler != null) {
                        notificationHandler.accept(message);
                    } else {
                        System.out.println("[MessageDispatcher] 알림 핸들러 미등록: " + message);
                    }
                } else {
                    // 일반 응답 → 동기 대기 큐로 전달
                    responseQueue.offer(message);
                    System.out.println("[MessageDispatcher] 응답 큐에 추가: " + message);
                }
                
            } catch (SocketTimeoutException e) {
                // 타임아웃은 정상 동작 - running 체크 후 계속 실행
                // 메시지가 없을 때 발생하므로 무시하고 다음 readLine() 호출
                if (!running || isInterrupted()) {
                    System.out.println("[MessageDispatcher] 타임아웃 중 종료 요청 감지");
                    break;
                }
                continue;
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("[MessageDispatcher] 오류: " + e.getMessage());
                    break;
                }
            }
        }
        
        System.out.println("[MessageDispatcher] 종료됨");
    }
    
    /**
     * 디스패처 중지
     */
    public void stopDispatcher() {
        running = false;
        interrupt();
        System.out.println("[MessageDispatcher] 중지 요청");
    }
    
    /**
     * 디스패처 재시작
     */
    public static void restart() {
        synchronized (lock) {
            if (instance != null) {
                instance.stopDispatcher();
                try {
                    instance.join(1000); // 스레드 종료 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                instance = null;
            }
            
            if (Session.getInstance().isConnected()) {
                startDispatcher(Session.getInstance().getIn());
            }
        }
    }
    
    /**
     * 테스트용: 인스턴스 초기화
     */
    public static void resetForTest() {
        synchronized (lock) {
            if (instance != null) {
                instance.stopDispatcher();
                try {
                    instance.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                instance = null;
            }
        }
    }
}
