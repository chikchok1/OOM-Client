package Model;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Session {
    private static String loggedInUserId;
    private static String loggedInUserName;
    private static String loggedInUserRole; // 사용자 권한 추가

    // 소켓, 입출력 스트림 추가
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void setLoggedInUserId(String userId) {
        loggedInUserId = userId;
    }

    public static String getLoggedInUserId() {
        return loggedInUserId;
    }

    public static void setLoggedInUserName(String userName) {
        loggedInUserName = userName;
    }

    public static String getLoggedInUserName() {
        return loggedInUserName;
    }

    public static void setLoggedInUserRole(String role) {
        loggedInUserRole = role;
    }

    public static String getLoggedInUserRole() {
        return loggedInUserRole;
    }

    public static void setSocket(Socket s) {
        socket = s;
    }

    public static Socket getSocket() {
        return socket;
    }

    public static void setOut(PrintWriter o) {
        out = o;
    }

    public static PrintWriter getOut() {
        return out;
    }

    public static void setIn(BufferedReader i) {
        in = i;
    }

    public static BufferedReader getIn() {
        return in;
    }

     //  연결 상태 확인 헬퍼 메서드
    public static boolean isConnected() {
        return socket != null && !socket.isClosed() && out != null && in != null;
    }
    
    public static void clear() {
        loggedInUserId = null;
        loggedInUserName = null;
        loggedInUserRole = null; // 역할 초기화

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
}
