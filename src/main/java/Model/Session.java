package Model;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 세션 관리 클래스 (Singleton Pattern 적용)
 * 로그인한 사용자의 정보와 서버 연결을 전역에서 관리
 */
public class Session {
    // ✅ Singleton 인스턴스
    private static volatile Session instance;
    
    // ✅ 인스턴스 변수로 변경
    private String loggedInUserId;
    private String loggedInUserName;
    private String loggedInUserRole;
    
    // 소켓, 입출력 스트림
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    // ✅ private 생성자 (외부에서 new Session() 불가)
    private Session() {
        // 초기화 로직 (필요 시)
    }
    
    // ✅ Singleton 인스턴스 반환 (Double-Checked Locking)
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
     */
    public void clear() {
        loggedInUserId = null;
        loggedInUserName = null;
        loggedInUserRole = null;

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
            System.out.println("세션 정리 중 오류: " + e.getMessage());
        }

        out = null;
        in = null;
        socket = null;
    }
    
    /**
     * 싱글톤 인스턴스 초기화 (테스트용)
     * ⚠️ 프로덕션 코드에서는 사용하지 마세요!
     */
    public static void resetInstance() {
        if (instance != null) {
            instance.clear();
            instance = null;
        }
    }
}
