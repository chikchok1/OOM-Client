package Controller;

import Model.Session;
import common.utils.ConfigLoader;
import java.io.*;
import java.net.Socket;

public class NetworkFacade {

    // 1. 로그인 요청 (소켓 연결부터 응답까지 처리)
    public String attemptLogin(String id, String password) throws IOException {
        String serverIp = ConfigLoader.getProperty("server.ip");
        int serverPort = Integer.parseInt(ConfigLoader.getProperty("server.port"));
        
        Socket socket = new Socket(serverIp, serverPort);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // 프로토콜 조립
        out.println("LOGIN," + id + "," + password);
        String response = in.readLine();
        
        // 성공 시 세션에 소켓 정보 저장 (싱글톤 사용)
        if (response != null && response.startsWith("SUCCESS")) {
            Session session = Session.getInstance();
            session.setSocket(socket);
            session.setOut(out);
            session.setIn(in);
            
            // 초기화 메시지 전송
            out.println("INIT");
            out.flush();
        } else {
            // 실패 시 소켓 닫기
            socket.close();
        }
        return response;
    }

    // 2. 회원가입 요청 (일회성 연결)
    public String register(String name, String id, String password) throws IOException {
        String serverIp = ConfigLoader.getProperty("server.ip");
        int serverPort = Integer.parseInt(ConfigLoader.getProperty("server.port"));

        try (Socket socket = new Socket(serverIp, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println("REGISTER," + name + "," + id + "," + password);
            return in.readLine();
        }
    }

    // 3. 강의실 상태 업데이트
    public String updateRoomStatus(String roomNumber, String status) throws IOException {
        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();
        
        if (out == null || in == null) throw new IOException("Not Connected");

        out.println("UPDATE_ROOM_STATUS," + roomNumber + "," + status);
        out.flush();
        return in.readLine();
    }

    // 4. 비밀번호 변경
    public String changePassword(String userId, String currentPw, String newPw) throws IOException {
        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();

        if (out == null || in == null) throw new IOException("Not Connected");

        out.println("CHANGE_PASSWORD," + userId + "," + currentPw + "," + newPw);
        out.flush();
        return in.readLine();
    }

    // 5. 로그아웃
    public void logout() {
        Session session = Session.getInstance();
        try {
            PrintWriter out = session.getOut();
            if (out != null) {
                out.println("EXIT");
                out.flush();
            }
            // 소켓 종료 로직은 Session.clear()가 담당하거나 여기서 처리
            session.clear(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}