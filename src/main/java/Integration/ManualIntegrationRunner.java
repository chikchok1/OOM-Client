package Integration;

import Controller.ClientFacade;
import Model.Session;

/**
 * Simple manual integration runner for the parts under test:
 * - Programmatic login
 * - changePassword via ClientFacade.changePassword(userId, current, new)
 *
 * Usage: run with working server running on configured host/port.
 */
public class ManualIntegrationRunner {
    public static void main(String[] args) throws Exception {
        // default test credentials (adjust if needed)
        String userId = "S20230001";
        String password = "abc123";

        System.out.println("[Runner] 로그인 시도: " + userId);
        boolean logged = ClientFacade.login(userId, password);
        if (!logged) {
            System.err.println("[Runner] 로그인 실패, 종료");
            return;
        }

        // change password -> then restore
        String newPw = "tmp_integ_pw";

        System.out.println("[Runner] 비밀번호 변경 시도: " + password + " -> " + newPw);
        boolean changed = ClientFacade.changePassword(userId, password, newPw);
        System.out.println("[Runner] 변경 결과: " + changed);

        if (changed) {
            // restore original
            System.out.println("[Runner] 비밀번호 복원 시도");
            boolean restored = ClientFacade.changePassword(userId, newPw, password);
            System.out.println("[Runner] 복원 결과: " + restored);
        }

        // cleanup session
        Session.getInstance().clear();
        System.out.println("[Runner] 종료");
    }
}
