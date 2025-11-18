package Model;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Session {
    // 1. 싱글톤 패턴 적용 (New Architecture)
    private static Session instance;

    // 멤버 변수들을 static -> 인스턴스 변수로 변경
    private String loggedInUserId;
    private String loggedInUserName;
    private String loggedInUserRole;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private Session() {
    }

    public static synchronized Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    // 2. 인스턴스 메서드 

    public PrintWriter getOutputStream() {
        return out;
    }

    public void setOutputStream(PrintWriter out) {
        this.out = out;
    }

    public BufferedReader getInputStream() {
        return in;
    }

    public void setInputStream(BufferedReader in) {
        this.in = in;
    }

    public Socket getSocketInstance() {
        return socket;
    }

    public void setSocketInstance(Socket socket) {
        this.socket = socket;
    }

    // 사용자 정보 Getter/Setter
    public String getUserId() {
        return loggedInUserId;
    }

    public void setUserId(String id) {
        this.loggedInUserId = id;
    }

    public String getUserName() {
        return loggedInUserName;
    }

    public void setUserName(String name) {
        this.loggedInUserName = name;
    }

    public String getUserRole() {
        return loggedInUserRole;
    }

    public void setUserRole(String role) {
        this.loggedInUserRole = role;
    }

    // 3. 호환성 계층 (Legacy Support)
    // 기존 코드(ReservClassController)와 테스트(Test)가 깨지지 않게 함

    public static PrintWriter getOut() {
        return getInstance().getOutputStream(); // 싱글톤 인스턴스로 위임
    }

    public static void setOut(PrintWriter out) {
        getInstance().setOutputStream(out);
    }

    public static BufferedReader getIn() {
        return getInstance().getInputStream();
    }

    public static void setIn(BufferedReader in) {
        getInstance().setInputStream(in);
    }

    // 테스트 코드에서 Session.isConnected()를 사용하므로 유지
    public static boolean isConnected() {
        Socket s = getInstance().getSocketInstance();
        return s != null && !s.isClosed() &&
                getInstance().getOutputStream() != null &&
                getInstance().getInputStream() != null;
    }

    // 기존 Getter/Setter 호환성 유지
    public static String getLoggedInUserId() {
        return getInstance().getUserId();
    }

    public static void setLoggedInUserId(String id) {
        getInstance().setUserId(id);
    }

    public static String getLoggedInUserName() {
        return getInstance().getUserName();
    }

    public static void setLoggedInUserName(String name) {
        getInstance().setUserName(name);
    }

    public static String getLoggedInUserRole() {
        return getInstance().getUserRole();
    }

    public static void setLoggedInUserRole(String role) {
        getInstance().setUserRole(role);
    }

    public static Socket getSocket() {
        return getInstance().getSocketInstance();
    }

    public static void setSocket(Socket s) {
        getInstance().setSocketInstance(s);
    }

    public static void clear() {
        Session s = getInstance();
        s.setUserId(null);
        s.setUserName(null);
        s.setUserRole(null);

        try {
            if (s.getOutputStream() != null)
                s.getOutputStream().close();
            if (s.getInputStream() != null)
                s.getInputStream().close();
            if (s.getSocketInstance() != null && !s.getSocketInstance().isClosed())
                s.getSocketInstance().close();
        } catch (Exception e) {
            System.out.println("세션 정리 중 오류: " + e.getMessage());
        }

        s.setOutputStream(null);
        s.setInputStream(null);
        s.setSocketInstance(null);

        // instance 자체를 null로 만들지 않고 내용만 비워서 재사용 가능하게 하거나
        // instance = null; 로 초기화해도 됨 (여기서는 안전하게 내용만 비움)
    }
}