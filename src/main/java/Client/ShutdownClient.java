package Client;
import java.io.*;
import java.net.*;

public class ShutdownClient {
    public static void main(String[] args) {
        try (
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("SHUTDOWN"); // 서버 종료 명령 전송

            String response = in.readLine(); // 서버 응답 수신
            if (response != null) {
                System.out.println("서버 응답: " + response);
            }

        } catch (IOException e) {
            System.err.println("서버 종료 요청 중 오류: " + e.getMessage());
        }
    }
}
