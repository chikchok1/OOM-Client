package Controller;

import View.*;
import Model.Session;
import Util.MessageDispatcher;
import common.utils.ConfigLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * 클라이언트 측 퍼사드: 로그인, 회원가입, 비밀번호 변경, 관리자 사용자 관리의
 * 네트워크/세션 처리 로직을 중앙으로 모아 컨트롤러들을 간단하게 만든다.
 */
public final class ClientFacade {

    private ClientFacade() {}

    public static boolean login(LoginForm view) {
        String id = view.getUserId();
        String password = view.getPassword();

        if (id.isEmpty() || password.isEmpty()) {
            view.showMessage("아이디와 비밀번호를 모두 입력하세요.");
            return false;
        }

        String serverIp = ConfigLoader.getProperty("server.ip");
        int serverPort = Integer.parseInt(ConfigLoader.getProperty("server.port"));

        try {
            Socket socket = new Socket(serverIp, serverPort);
            socket.setSoTimeout(30000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LOGIN," + id + "," + password);
            String response = in.readLine();

            if (response == null) {
                view.showMessage("서버로부터 응답이 없습니다.");
                closeSilent(socket, in, out);
                return false;
            }

            switch (response.split(",")[0]) {
                case "SERVER_BUSY":
                    view.showMessage("현재 접속 인원이 초과되었습니다. 나중에 다시 시도해주세요.");
                    closeSilent(socket, in, out);
                    return false;
                case "ALREADY_LOGGED_IN":
                    view.showMessage("이미 로그인된 사용자입니다. 다른 사용자 계정으로 로그인하거나 나중에 다시 시도하세요.");
                    closeSilent(socket, in, out);
                    return false;
                case "SUCCESS":
                    String userName = response.split(",").length > 1 ? response.split(",")[1] : "이름없음";

                    Session.getInstance().setLoggedInUserId(id);
                    Session.getInstance().setLoggedInUserName(userName);
                    Session.getInstance().setSocket(socket);
                    Session.getInstance().setIn(in);
                    Session.getInstance().setOut(out);

                    out.println("INIT");
                    out.flush();

                    String role = switch (id.charAt(0)) {
                        case 'S' -> "학생";
                        case 'P' -> "교수";
                        case 'A' -> "조교";
                        default -> "알 수 없음";
                    };
                    Session.getInstance().setLoggedInUserRole(role);

                    MessageDispatcher.startDispatcher(in);
                    MessageDispatcher.getInstance().setNotificationHandler(notificationMessage -> {
                        // 기존 알림 처리 로직 그대로 유지
                        try {
                            String[] parts = notificationMessage.split(",", 7);
                            if (parts.length < 7) return;
                            String typeStr = parts[1];
                            String notificationMsg = parts[2];
                            String room = parts[3];
                            String date = parts[4];
                            String day = parts[5];
                            String time = parts[6];

                            SwingUtilities.invokeLater(() -> {
                                String title = getNotificationTitle(typeStr);
                                StringBuilder sb = new StringBuilder();
                                sb.append(notificationMsg).append("\n\n");
                                sb.append("상세 정보:\n");
                                sb.append("강의실/실습실: ").append(room).append("\n");
                                sb.append("날짜: ").append(date).append(" (").append(day).append(")\n");
                                sb.append("시간: ").append(time);

                                int messageType = getMessageType(typeStr);
                                JOptionPane.showMessageDialog(null, sb.toString(), title, messageType);
                            });
                        } catch (Exception e) {
                            System.err.println("[알림] 처리 오류: " + e.getMessage());
                        }
                    });

                    view.showMessage("로그인 성공!");
                    view.dispose();
                    // 화면 전환은 기존 컨트롤러에서 처리하도록 남겨둠
                    return true;
                case "FAIL":
                default:
                    view.showMessage("로그인 실패: 아이디 또는 비밀번호가 틀렸습니다.");
                    closeSilent(socket, in, out);
                    return false;
            }

        } catch (IOException e) {
            view.showMessage("서버와 연결할 수 없습니다: " + e.getMessage());
            return false;
        }
    }

    /**
     * Programmatic login without UI (for integration tests/runners).
     * Returns true on SUCCESS and initializes Session + MessageDispatcher.
     */
    public static boolean login(String id, String password) {
        if (id == null || id.isEmpty() || password == null || password.isEmpty()) return false;

        String serverIp = common.utils.ConfigLoader.getProperty("server.ip");
        int serverPort = Integer.parseInt(common.utils.ConfigLoader.getProperty("server.port"));

        try {
            java.net.Socket socket = new java.net.Socket(serverIp, serverPort);
            socket.setSoTimeout(30000);

            java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));

            out.println("LOGIN," + id + "," + password);
            String response = in.readLine();

            if (response == null) {
                System.out.println("[ClientFacade] 서버 응답 없음");
                try { socket.close(); } catch (Exception ignored) {}
                return false;
            }

            switch (response.split(",")[0]) {
                case "SERVER_BUSY":
                    System.out.println("서버 과부하");
                    try { socket.close(); } catch (Exception ignored) {}
                    return false;
                case "ALREADY_LOGGED_IN":
                    System.out.println("이미 로그인됨");
                    try { socket.close(); } catch (Exception ignored) {}
                    return false;
                case "SUCCESS":
                    String userName = response.split(",").length > 1 ? response.split(",")[1] : "";
                    Session.getInstance().setLoggedInUserId(id);
                    Session.getInstance().setLoggedInUserName(userName);
                    Session.getInstance().setSocket(socket);
                    Session.getInstance().setIn(in);
                    Session.getInstance().setOut(out);

                    out.println("INIT"); out.flush();

                    String role = switch (id.charAt(0)) {
                        case 'S' -> "학생";
                        case 'P' -> "교수";
                        case 'A' -> "조교";
                        default -> "알 수 없음";
                    };
                    Session.getInstance().setLoggedInUserRole(role);

                    MessageDispatcher.startDispatcher(in);
                    MessageDispatcher.getInstance().setNotificationHandler(msg -> System.out.println("[NOTIF] " + msg));

                    System.out.println("[ClientFacade] 로그인 성공: " + id + " (" + userName + ")");
                    return true;
                default:
                    System.out.println("로그인 실패: " + response);
                    try { socket.close(); } catch (Exception ignored) {}
                    return false;
            }
        } catch (java.io.IOException e) {
            System.out.println("서버 연결 실패: " + e.getMessage());
            return false;
        }
    }

    private static String getNotificationTitle(String typeStr) {
        return switch (typeStr) {
            case "APPROVED" -> "✅ 예약 승인";
            case "REJECTED" -> "❌ 예약 거절";
            case "CHANGE_APPROVED" -> "✅ 예약 변경 승인";
            case "CHANGE_REJECTED" -> "❌ 예약 변경 거절";
            default -> "📢 알림";
        };
    }

    private static int getMessageType(String typeStr) {
        if (typeStr.contains("APPROVED")) return JOptionPane.INFORMATION_MESSAGE;
        if (typeStr.contains("REJECTED")) return JOptionPane.WARNING_MESSAGE;
        return JOptionPane.PLAIN_MESSAGE;
    }

    private static void closeSilent(Socket socket, BufferedReader in, PrintWriter out) {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public static boolean register(String name, String studentId, String password, MembershipView view, LoginForm loginForm) {
        if (name.isEmpty() || studentId.isEmpty() || password.isEmpty()) {
            view.showMessage("모든 필드를 입력해주세요.");
            return false;
        }
        if (!studentId.matches("[SPA][0-9]{3}")) {
            view.showMessage("아이디는 대문자 S/P/A + 숫자 3개로 구성되어야 합니다.\n예: S123");
            return false;
        }
        if (password.length() < 4 || password.length() > 8) {
            view.showMessage("비밀번호는 최소 4자리에서 최대 8자리여야 합니다.");
            return false;
        }

        String serverIp = ConfigLoader.getProperty("server.ip");
        int serverPort = Integer.parseInt(ConfigLoader.getProperty("server.port"));

        try (Socket socket = new Socket(serverIp, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("REGISTER," + name + "," + studentId + "," + password);
            String response = in.readLine();

            if ("SUCCESS".equals(response)) {
                view.showMessage("회원가입이 완료되었습니다.");
                view.disposeView();
                loginForm.setVisible(true);
                return true;
            } else if ("DUPLICATE".equals(response)) {
                view.showMessage("이미 존재하는 학번입니다. 다른 학번을 사용해주세요.");
                return false;
            } else {
                view.showMessage("회원가입 실패: " + response);
                return false;
            }

        } catch (IOException ex) {
            view.showMessage("서버와 연결할 수 없습니다: " + ex.getMessage());
            return false;
        }
    }

    public static void changePassword(ChangePasswordView view) {
        String currentPassword = view.getPresentPassword().trim();
        String newPassword = view.getChangePassword().trim();
        String userId = Session.getInstance().getLoggedInUserId();

        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(null, "모든 필드를 입력해주세요.");
            return;
        }

        PrintWriter out = Session.getInstance().getOut();
        if (out == null) {
            JOptionPane.showMessageDialog(null, "서버와 연결되어 있지 않습니다.");
            return;
        }

        String request = String.join(",", "CHANGE_PASSWORD", userId, currentPassword, newPassword);
        out.println(request);
        out.flush();

        // Use MessageDispatcher to receive response to avoid race with dispatcher reading the socket
        Util.MessageDispatcher dispatcher = Util.MessageDispatcher.getInstance();
        String response = null;
        if (dispatcher != null) {
            response = dispatcher.waitForResponse(10); // 10초 대기
        } else {
            // Fallback: attempt direct read (not preferred)
            try {
                java.io.BufferedReader in = Session.getInstance().getIn();
                if (in != null) response = in.readLine();
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(null, "서버 응답 오류: " + e.getMessage());
                return;
            }
        }

        if (response == null) {
            JOptionPane.showMessageDialog(null, "서버 응답 없음(타임아웃)");
            return;
        }

        switch (response) {
            case "PASSWORD_CHANGED":
                JOptionPane.showMessageDialog(null, "비밀번호가 성공적으로 변경되었습니다.");
                view.dispose();
                break;
            case "INVALID_CURRENT_PASSWORD":
                JOptionPane.showMessageDialog(null, "현재 비밀번호가 일치하지 않습니다.");
                break;
            case "USER_NOT_FOUND":
                JOptionPane.showMessageDialog(null, "사용자 정보를 찾을 수 없습니다.");
                break;
            default:
                JOptionPane.showMessageDialog(null, "비밀번호 변경 실패: " + response);
        }
    }

    /**
     * Programmatic, non-UI overload for tests and headless callers.
     * Returns true if password changed successfully.
     */
    public static boolean changePassword(String userId, String currentPassword, String newPassword) {
        if (userId == null || userId.isEmpty()) return false;

        PrintWriter out = Session.getInstance().getOut();
        if (out == null) return false;

        out.println(String.join(",", "CHANGE_PASSWORD", userId, currentPassword, newPassword));
        out.flush();

        Util.MessageDispatcher dispatcher = Util.MessageDispatcher.getInstance();
        String response = null;
        if (dispatcher != null) {
            response = dispatcher.waitForResponse(10);
        } else {
            try {
                BufferedReader in = Session.getInstance().getIn();
                if (in != null) response = in.readLine();
            } catch (IOException e) {
                return false;
            }
        }

        return "PASSWORD_CHANGED".equals(response);
    }

    public static void loadUsers(DefaultTableModel model) {
        model.setRowCount(0);
        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();

        if (out == null || dispatcher == null) return;

        new Thread(() -> {
            try {
                out.println("GET_ALL_USERS");
                out.flush();

                String line;
                while ((line = dispatcher.waitForResponse(30)) != null) {
                    if ("END_OF_USERS".equals(line)) break;
                    String[] tokens = line.split(",");
                    if (tokens.length == 3) {
                        SwingUtilities.invokeLater(() -> model.addRow(tokens));
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "서버에서 사용자 목록을 불러오는 중 오류 발생: " + e.getMessage())
                );
            }
        }).start();
    }

    public static void deleteUser(String userId, ClientAdmin view, Runnable onSuccess) {
        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        if (out == null || dispatcher == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        new Thread(() -> {
            try {
                out.println("DELETE_USER," + userId);
                out.flush();

                String response = dispatcher.waitForResponse(10);
                if (response == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(view, "서버 응답 없음"));
                    return;
                }

                if (response.startsWith("ERROR:")) {
                    String[] parts = response.split(":", 3);
                    String errorMessage = parts.length >= 3 ? parts[2] : "알 수 없는 오류";
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(view, "삭제 실패: " + errorMessage));
                    return;
                }

                if ("DELETE_SUCCESS".equals(response)) {
                    SwingUtilities.invokeLater(() -> {
                        // Ensure onSuccess runs even in headless environments where JOptionPane may throw
                        try {
                            onSuccess.run();
                        } finally {
                            try {
                                JOptionPane.showMessageDialog(view, "삭제 성공");
                            } catch (Throwable ignored) {
                                // ignore HeadlessException or other UI errors in tests
                            }
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(view, "삭제 실패: " + response));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(view, "삭제 중 오류 발생: " + e.getMessage()));
            }
        }).start();
    }

    public static void updateUser(String userId, String newName, String newPw, ClientAdmin view, int row, DefaultTableModel model) {
        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        if (out == null || dispatcher == null) {
            JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
            return;
        }

        new Thread(() -> {
            try {
                out.println("UPDATE_USER," + userId + "," + newName + "," + newPw);
                out.flush();

                String response = dispatcher.waitForResponse(10);
                if (response == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(view, "서버 응답 없음"));
                    return;
                }

                if (response.startsWith("ERROR:")) {
                    String[] parts = response.split(":", 3);
                    String errorMessage = parts.length >= 3 ? parts[2] : "알 수 없는 오류";
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(view, "수정 실패: " + errorMessage));
                    return;
                }

                if ("UPDATE_SUCCESS".equals(response)) {
                    SwingUtilities.invokeLater(() -> {
                        model.setValueAt(newName, row, 0);
                        model.setValueAt(newPw, row, 2);
                        JOptionPane.showMessageDialog(view, "수정 성공");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(view, "수정 실패: " + response));
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(view, "수정 요청 중 오류 발생: " + e.getMessage()));
            }
        }).start();
    }
}
