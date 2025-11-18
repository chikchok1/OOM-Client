package Model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @AfterEach
    void resetSession() {
        Session.clear(); // 싱글톤 초기화 메서드 사용
    }

    @Test
    void testSetAndGetLoggedInUserId_StaticBridge() {
        String testId = "S20230001";
        Session.setLoggedInUserId(testId);
        assertEquals(testId, Session.getLoggedInUserId());
        // 싱글톤 인스턴스
        assertEquals(testId, Session.getInstance().getLoggedInUserId());
    }
    
    @Test
    void testSingletonInstance() {
        Session s1 = Session.getInstance();
        Session s2 = Session.getInstance();
        
        assertSame(s1, s2, "Session 객체는 싱글톤이어야 하므로 같은 인스턴스여야 합니다.");
    }
}