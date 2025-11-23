package Service;

import Util.MessageDispatcher;
import common.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 퍼사드(Facade) 클래스.
 * 싱글턴 패턴을 사용하여 유일한 인스턴스를 제공합니다.
 * 이 클래스는 로그인, 회원가입, 사용자 관리 등 서버와의 통신을 캡슐화합니다.
 */
public class UserService {

    private static final UserService instance = new UserService();
    private final MessageDispatcher dispatcher;

    private UserService() {
        this.dispatcher = MessageDispatcher.getInstance();
    }

    public static UserService getInstance() {
        return instance;
    }

    /**
     * 사용자 로그인을 처리합니다.
     *
     * @param id       사용자 ID
     * @param password 사용자 비밀번호
     * @return 로그인 성공 시 User 객체, 실패 시 null
     */
    public User login(String id, String password) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", id);
        request.put("pw", password);

        Map<String, Object> response = dispatcher.sendAndReceive("login", request);

        if (response != null && "success".equals(response.get("status"))) {
            return (User) response.get("user");
        }
        return null;
    }

    /**
     * 회원가입을 처리합니다.
     *
     * @param id       사용자 ID
     * @param password 사용자 비밀번호
     * @param name     사용자 이름
     * @param type     사용자 유형
     * @return 회원가입 성공 시 true, 실패 시 false
     */
    public boolean register(String id, String password, String name, String type) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", id);
        request.put("pw", password);
        request.put("name", name);
        request.put("type", type);

        Map<String, Object> response = dispatcher.sendAndReceive("register", request);

        return response != null && "success".equals(response.get("status"));
    }

    /**
     * 모든 사용자 목록을 가져옵니다.
     *
     * @return 사용자 목록
     */
    @SuppressWarnings("unchecked")
    public List<User> getAllUsers() {
        Map<String, Object> response = dispatcher.sendAndReceive("getAllUsers", new HashMap<>());
        if (response != null && "success".equals(response.get("status"))) {
            return (List<User>) response.get("users");
        }
        return null;
    }

    /**
     * 사용자 정보를 수정합니다.
     *
     * @param user 수정할 사용자 정보
     * @return 수정 성공 시 true, 실패 시 false
     */
    public boolean updateUser(User user) {
        Map<String, Object> request = new HashMap<>();
        request.put("user", user);
        Map<String, Object> response = dispatcher.sendAndReceive("updateUser", request);
        return response != null && "success".equals(response.get("status"));
    }

    /**
     * 사용자를 삭제합니다.
     *
     * @param userId 삭제할 사용자 ID
     * @return 삭제 성공 시 true, 실패 시 false
     */
    public boolean deleteUser(String userId) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", userId);
        Map<String, Object> response = dispatcher.sendAndReceive("deleteUser", request);
        return response != null && "success".equals(response.get("status"));
    }

    /**
     * 비밀번호를 변경합니다.
     *
     * @param id          사용자 ID
     * @param oldPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @return 변경 성공 시 "success", 현재 비밀번호 불일치 시 "fail", 그 외 "error"
     */
    public String changePassword(String id, String oldPassword, String newPassword) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", id);
        request.put("old_pw", oldPassword);
        request.put("new_pw", newPassword);

        Map<String, Object> response = dispatcher.sendAndReceive("changePassword", request);

        if (response != null) {
            return (String) response.get("status");
        }
        return "error";
    }
}
