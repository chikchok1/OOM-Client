package Model;

import org.junit.jupiter.api.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Session 싱글턴 패턴 테스트
 */
class SessionTest {
    
    private Session session;
    
    @BeforeEach
    void setUp() {
        // 싱글턴 인스턴스 초기화
        Session.resetInstance();
        session = Session.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        // 세션 정리
        session.clear();
        Session.resetInstance();
    }
    
    /**
     * 테스트 1: 싱글턴 인스턴스가 동일한지 검증
     */
    @Test
    @DisplayName("싱글턴 패턴: 항상 같은 인스턴스 반환")
    void testSingletonInstance() {
        Session instance1 = Session.getInstance();
        Session instance2 = Session.getInstance();
        
        assertSame(instance1, instance2, "같은 인스턴스를 반환해야 함");
    }
    
    /**
     * 테스트 2: Double-Checked Locking 싱글턴 패턴 검증
     */
    @Test
    @DisplayName("멀티스레드 환경에서 싱글턴 인스턴스 일관성")
    void testSingletonInMultiThreadEnvironment() throws InterruptedException {
        // Given
        Session.resetInstance(); // 초기화
        
        final int THREAD_COUNT = 10;
        Session[] instances = new Session[THREAD_COUNT];
        Thread[] threads = new Thread[THREAD_COUNT];
        
        // When: 여러 스레드에서 동시에 getInstance 호출
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                instances[index] = Session.getInstance();
            });
            threads[i].start();
        }
        
        // 모든 스레드 대기
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: 모든 인스턴스가 동일해야 함
        for (int i = 1; i < THREAD_COUNT; i++) {
            assertSame(instances[0], instances[i], 
                "멀티스레드 환경에서도 같은 인스턴스를 반환해야 함");
        }
    }
    
    /**
     * 테스트 3: 사용자 ID 설정 및 조회
     */
    @Test
    @DisplayName("사용자 ID 설정 및 조회")
    void testSetAndGetLoggedInUserId() {
        // Given
        String userId = "testUser";
        
        // When
        session.setLoggedInUserId(userId);
        
        // Then
        assertEquals(userId, session.getLoggedInUserId(), "설정한 사용자 ID가 반환되어야 함");
    }
    
    /**
     * 테스트 4: 사용자 이름 설정 및 조회
     */
    @Test
    @DisplayName("사용자 이름 설정 및 조회")
    void testSetAndGetLoggedInUserName() {
        // Given
        String userName = "홍길동";
        
        // When
        session.setLoggedInUserName(userName);
        
        // Then
        assertEquals(userName, session.getLoggedInUserName(), "설정한 사용자 이름이 반환되어야 함");
    }
    
    /**
     * 테스트 5: 사용자 역할 설정 및 조회
     */
    @Test
    @DisplayName("사용자 역할 설정 및 조회")
    void testSetAndGetLoggedInUserRole() {
        // Given
        String role = "ADMIN";
        
        // When
        session.setLoggedInUserRole(role);
        
        // Then
        assertEquals(role, session.getLoggedInUserRole(), "설정한 역할이 반환되어야 함");
    }
    
    /**
     * 테스트 6: 소켓 설정 및 조회
     */
    @Test
    @DisplayName("소켓 설정 및 조회")
    void testSetAndGetSocket() {
        // Given
        Socket mockSocket = mock(Socket.class);
        
        // When
        session.setSocket(mockSocket);
        
        // Then
        assertSame(mockSocket, session.getSocket(), "설정한 소켓이 반환되어야 함");
    }
    
    /**
     * 테스트 7: PrintWriter 설정 및 조회
     */
    @Test
    @DisplayName("PrintWriter 설정 및 조회")
    void testSetAndGetOut() {
        // Given
        PrintWriter mockOut = new PrintWriter(new StringWriter());
        
        // When
        session.setOut(mockOut);
        
        // Then
        assertSame(mockOut, session.getOut(), "설정한 PrintWriter가 반환되어야 함");
    }
    
    /**
     * 테스트 8: BufferedReader 설정 및 조회
     */
    @Test
    @DisplayName("BufferedReader 설정 및 조회")
    void testSetAndGetIn() {
        // Given
        BufferedReader mockIn = new BufferedReader(new StringReader(""));
        
        // When
        session.setIn(mockIn);
        
        // Then
        assertSame(mockIn, session.getIn(), "설정한 BufferedReader가 반환되어야 함");
    }
    
    /**
     * 테스트 9: 연결 상태 확인 - 연결되지 않은 상태
     */
    @Test
    @DisplayName("연결 상태 확인 - 연결되지 않은 상태")
    void testIsConnectedWhenNotConnected() {
        // When
        boolean connected = session.isConnected();
        
        // Then
        assertFalse(connected, "초기 상태는 연결되지 않은 상태여야 함");
    }
    
    /**
     * 테스트 10: 연결 상태 확인 - 완전히 연결된 상태
     */
    @Test
    @DisplayName("연결 상태 확인 - 완전히 연결된 상태")
    void testIsConnectedWhenFullyConnected() {
        // Given
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isClosed()).thenReturn(false);
        
        PrintWriter mockOut = new PrintWriter(new StringWriter());
        BufferedReader mockIn = new BufferedReader(new StringReader(""));
        
        session.setSocket(mockSocket);
        session.setOut(mockOut);
        session.setIn(mockIn);
        
        // When
        boolean connected = session.isConnected();
        
        // Then
        assertTrue(connected, "모든 연결이 설정되면 true를 반환해야 함");
    }
    
    /**
     * 테스트 11: 연결 상태 확인 - 소켓이 닫힌 상태
     */
    @Test
    @DisplayName("연결 상태 확인 - 소켓이 닫힌 상태")
    void testIsConnectedWhenSocketClosed() {
        // Given
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isClosed()).thenReturn(true);
        
        PrintWriter mockOut = new PrintWriter(new StringWriter());
        BufferedReader mockIn = new BufferedReader(new StringReader(""));
        
        session.setSocket(mockSocket);
        session.setOut(mockOut);
        session.setIn(mockIn);
        
        // When
        boolean connected = session.isConnected();
        
        // Then
        assertFalse(connected, "소켓이 닫히면 false를 반환해야 함");
    }
    
    /**
     * 테스트 12: 세션 정리 (clear)
     */
    @Test
    @DisplayName("세션 정리 테스트")
    void testClear() {
        // Given
        session.setLoggedInUserId("testUser");
        session.setLoggedInUserName("홍길동");
        session.setLoggedInUserRole("USER");
        
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isClosed()).thenReturn(false);
        
        PrintWriter mockOut = new PrintWriter(new StringWriter());
        BufferedReader mockIn = new BufferedReader(new StringReader(""));
        
        session.setSocket(mockSocket);
        session.setOut(mockOut);
        session.setIn(mockIn);
        
        // When
        session.clear();
        
        // Then
        assertNull(session.getLoggedInUserId(), "사용자 ID가 초기화되어야 함");
        assertNull(session.getLoggedInUserName(), "사용자 이름이 초기화되어야 함");
        assertNull(session.getLoggedInUserRole(), "역할이 초기화되어야 함");
        assertNull(session.getSocket(), "소켓이 초기화되어야 함");
        assertNull(session.getOut(), "PrintWriter가 초기화되어야 함");
        assertNull(session.getIn(), "BufferedReader가 초기화되어야 함");
    }
    
    /**
     * 테스트 13: resetInstance 테스트
     */
    @Test
    @DisplayName("resetInstance로 싱글턴 인스턴스 초기화")
    void testResetInstance() {
        // Given
        Session instance1 = Session.getInstance();
        instance1.setLoggedInUserId("testUser");
        
        // When
        Session.resetInstance();
        Session instance2 = Session.getInstance();
        
        // Then
        assertNotSame(instance1, instance2, "resetInstance 후 새로운 인스턴스가 생성되어야 함");
        assertNull(instance2.getLoggedInUserId(), "새 인스턴스는 초기화된 상태여야 함");
    }
    
    /**
     * 테스트 14: 부분적인 연결 상태 - 소켓만 설정
     */
    @Test
    @DisplayName("부분적인 연결 상태 - 소켓만 설정")
    void testIsConnectedWithSocketOnly() {
        // Given
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isClosed()).thenReturn(false);
        session.setSocket(mockSocket);
        
        // When
        boolean connected = session.isConnected();
        
        // Then
        assertFalse(connected, "소켓만 있으면 연결된 것이 아님");
    }
    
    /**
     * 테스트 15: 부분적인 연결 상태 - out과 in만 설정
     */
    @Test
    @DisplayName("부분적인 연결 상태 - out과 in만 설정")
    void testIsConnectedWithStreamOnly() {
        // Given
        PrintWriter mockOut = new PrintWriter(new StringWriter());
        BufferedReader mockIn = new BufferedReader(new StringReader(""));
        
        session.setOut(mockOut);
        session.setIn(mockIn);
        
        // When
        boolean connected = session.isConnected();
        
        // Then
        assertFalse(connected, "스트림만 있고 소켓이 없으면 연결된 것이 아님");
    }
    
    /**
     * 테스트 16: volatile 키워드로 인한 가시성 보장 (개념적 테스트)
     */
    @Test
    @DisplayName("volatile 키워드로 멀티스레드 가시성 보장")
    void testVolatileVisibility() throws InterruptedException {
        // Given
        Session.resetInstance();
        final int THREAD_COUNT = 20;
        Thread[] threads = new Thread[THREAD_COUNT];
        Session[] instances = new Session[THREAD_COUNT];
        
        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                instances[index] = Session.getInstance();
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: 모든 스레드가 같은 인스턴스를 받아야 함
        for (int i = 1; i < THREAD_COUNT; i++) {
            assertSame(instances[0], instances[i]);
        }
    }
    
    /**
     * 테스트 17: 동시성 테스트 - 여러 스레드에서 세션 정보 설정
     */
    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 세션 정보 설정")
    void testConcurrentSessionUpdates() throws InterruptedException {
        // Given
        final int THREAD_COUNT = 10;
        Thread[] threads = new Thread[THREAD_COUNT];
        
        // When: 여러 스레드에서 세션 정보 설정
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                Session s = Session.getInstance();
                s.setLoggedInUserId("user" + index);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: 마지막으로 설정된 값이 저장되어 있어야 함
        assertNotNull(session.getLoggedInUserId(), "사용자 ID가 설정되어 있어야 함");
        assertTrue(session.getLoggedInUserId().startsWith("user"), 
            "user로 시작하는 ID가 설정되어 있어야 함");
    }
    
    /**
     * 테스트 18: 세션 정보 일괄 설정 테스트
     */
    @Test
    @DisplayName("세션 정보 일괄 설정 테스트")
    void testSetAllSessionInfo() {
        // Given
        String userId = "testUser";
        String userName = "테스트유저";
        String role = "ADMIN";
        
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isClosed()).thenReturn(false);
        
        PrintWriter mockOut = new PrintWriter(new StringWriter());
        BufferedReader mockIn = new BufferedReader(new StringReader(""));
        
        // When
        session.setLoggedInUserId(userId);
        session.setLoggedInUserName(userName);
        session.setLoggedInUserRole(role);
        session.setSocket(mockSocket);
        session.setOut(mockOut);
        session.setIn(mockIn);
        
        // Then
        assertEquals(userId, session.getLoggedInUserId());
        assertEquals(userName, session.getLoggedInUserName());
        assertEquals(role, session.getLoggedInUserRole());
        assertSame(mockSocket, session.getSocket());
        assertSame(mockOut, session.getOut());
        assertSame(mockIn, session.getIn());
        assertTrue(session.isConnected());
    }
}
