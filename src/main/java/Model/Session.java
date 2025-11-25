package Model;

import Util.MessageDispatcher;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 세션 관리 클래스 (Singleton Pattern 적용)
 * 로그인한 사용자의 정보와 서버 연결을 전역에서 관리
 */
public class Session {
    private static volatile Session instance;
    
    private String loggedInUserId;
    private String loggedInUserName;
    private String loggedInUserRole;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private Session() {
    }
    
    public static Session getInstance() {
        if (instance == null) {
            synchronized (Session.class) {
                if (instance == null) {
                    instance = new Session();
                }
            }
        }
        return instance;
    }
    
    // ========== Getter / Setter ==========
    
    public void setLoggedInUserId(String userId) {
        this.loggedInUserId = userId;
    }

    public String getLoggedInUserId() {
        return loggedInUserId;
    }

    public void setLoggedInUserName(String userName) {
        this.loggedInUserName = userName;
    }

    public String getLoggedInUserName() {
        return loggedInUserName;
    }

    public void setLoggedInUserRole(String role) {
        this.loggedInUserRole = role;
    }

    public String getLoggedInUserRole() {
        return loggedInUserRole;
    }

    public void setSocket(Socket s) {
        this.socket = s;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setOut(PrintWriter o) {
        this.out = o;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void setIn(BufferedReader i) {
        this.in = i;
    }

    public BufferedReader getIn() {
        return in;
    }

    // ========== 유틸리티 메서드 ==========
    
    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && out != null && in != null;
    }
    
    /**
     * 세션 정리 (로그아웃 시 호출)
     * ✅ 리소스 해제 순서 최적화:
     * 1. MessageDispatcher 종료 요청
     * 2. Dispatcher가 안전하게 종료될 시간 대기
     * 3. 출력 스트림 닫기 (서버에 신호 전달)
     * 4. 입력 스트림 닫기
     * 5. 소켓 닫기
     */
    public void clear() {
        loggedInUserId = null;
        loggedInUserName = null;
        loggedInUserRole = null;
        
        // ✅ 1단계: MessageDispatcher 종료 요청
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        if (dispatcher != null) {
            dispatcher.stopDispatcher();
            
            // ✅ 2단계: Dispatcher가 안전하게 종료될 시간 대기
            // (SocketTimeoutException 처리 중일 수 있음)
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Session] clear() 대기 중 인터럽트");
            }
        }

        // ✅ 3~5단계: 스트림 및 소켓 닫기
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.out.println("[Session] 세션 정리 중 오류: " + e.getMessage());
        }

        out = null;
        in = null;
        socket = null;
    }
    
    /**
     * 싱글톤 인스턴스 초기화 (테스트용)
     */
    public static void resetInstance() {
        if (instance != null) {
            instance.clear();
            instance = null;
        }
    }
}
