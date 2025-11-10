package Model;
/**
 *
 * @author minju
 */
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @AfterEach
    void resetSession() {
        // 테스트 간 영향 방지 (초기화)
        Session.setLoggedInUserId(null);
    }

    @Test
    void testSetAndGetLoggedInUserId() {
        // given
        String testId = "S20230001";

        // when
        Session.setLoggedInUserId(testId);

        // then
        assertEquals(testId, Session.getLoggedInUserId());
    }
}
