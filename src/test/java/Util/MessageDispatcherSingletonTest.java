package Util;

import Model.Session;
import org.junit.jupiter.api.*;
import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageDispatcher 싱글턴 패턴 테스트
 * 
 * 역할 분담:
 * - 싱글턴 패턴 담당: getInstance() 및 인스턴스 유일성 테스트
 * - 기타 담당: 메시지 라우팅, 큐 처리 등 비즈니스 로직 테스트
 */
class MessageDispatcherSingletonTest {
    
    @BeforeEach
    void setUp() {
        // 각 테스트 시작 전 인스턴스 초기화
        MessageDispatcher.resetForTest();
    }
    
    @AfterEach
    void tearDown() {
        // 각 테스트 후 디스패처 정리
        MessageDispatcher.resetForTest();
    }
    
    /**
     * 테스트 1: 싱글턴 인스턴스 생성 확인
     */
    @Test
    @DisplayName("싱글턴 패턴: startDispatcher 후 getInstance로 동일 인스턴스 반환")
    void testSingletonInstanceCreation() {
        // Given
        BufferedReader mockReader = new BufferedReader(new StringReader(""));
        
        // When
        MessageDispatcher.startDispatcher(mockReader);
        MessageDispatcher instance1 = MessageDispatcher.getInstance();
        MessageDispatcher instance2 = MessageDispatcher.getInstance();
        
        // Then
        assertNotNull(instance1, "인스턴스가 생성되어야 함");
        assertSame(instance1, instance2, "같은 인스턴스를 반환해야 함");
    }
    
    /**
     * 테스트 2: 중복 startDispatcher 호출 시 동일 인스턴스 유지
     */
    @Test
    @DisplayName("중복 startDispatcher 호출 시 기존 인스턴스 유지")
    void testDuplicateStartDispatcher() {
        // Given
        BufferedReader mockReader1 = new BufferedReader(new StringReader(""));
        BufferedReader mockReader2 = new BufferedReader(new StringReader(""));
        
        // When
        MessageDispatcher.startDispatcher(mockReader1);
        MessageDispatcher instance1 = MessageDispatcher.getInstance();
        
        MessageDispatcher.startDispatcher(mockReader2);
        MessageDispatcher instance2 = MessageDispatcher.getInstance();
        
        // Then
        assertSame(instance1, instance2, "기존 인스턴스를 유지해야 함");
    }
    
    /**
     * 테스트 3: 디스패처 종료 후 재시작 시 새로운 인스턴스 생성
     */
    @Test
    @DisplayName("디스패처 종료 후 재시작 시 새로운 인스턴스 생성")
    void testRestartAfterStop() throws InterruptedException {
        // Given
        BufferedReader mockReader1 = new BufferedReader(new StringReader(""));
        MessageDispatcher.startDispatcher(mockReader1);
        MessageDispatcher instance1 = MessageDispatcher.getInstance();
        
        // When: 종료 후 재시작
        instance1.stopDispatcher();
        Thread.sleep(200); // 종료 대기
        
        BufferedReader mockReader2 = new BufferedReader(new StringReader(""));
        MessageDispatcher.startDispatcher(mockReader2);
        MessageDispatcher instance2 = MessageDispatcher.getInstance();
        
        // Then
        assertNotSame(instance1, instance2, "종료 후 재시작하면 새로운 인스턴스가 생성되어야 함");
    }
    
    /**
     * 테스트 4: getInstance() null 체크
     */
    @Test
    @DisplayName("startDispatcher 호출 전 getInstance는 null")
    void testGetInstanceBeforeStart() {
        // When
        MessageDispatcher instance = MessageDispatcher.getInstance();
        
        // Then
        assertNull(instance, "startDispatcher 호출 전에는 null이어야 함");
    }
    
    /**
     * 테스트 5: 싱글턴 인스턴스의 스레드 생존 확인
     */
    @Test
    @DisplayName("싱글턴 인스턴스는 백그라운드 스레드로 실행")
    void testInstanceIsRunningThread() throws InterruptedException {
        // Given
        BufferedReader mockReader = new BufferedReader(new StringReader(""));
        
        // When
        MessageDispatcher.startDispatcher(mockReader);
        MessageDispatcher instance = MessageDispatcher.getInstance();
        
        // Then
        assertNotNull(instance, "인스턴스가 생성되어야 함");
        assertTrue(instance.isAlive(), "백그라운드 스레드로 실행되어야 함");
        assertEquals("MessageDispatcher", instance.getName(), "스레드 이름이 설정되어야 함");
    }
    
    /**
     * 테스트 6: synchronized 메서드를 통한 thread-safe 보장
     * 참고: 이 테스트는 Thread 객체의 동일성이 아닌 MessageDispatcher 인스턴스의 동일성을 검증합니다.
     */
    @Test
    @DisplayName("멀티스레드 환경에서 startDispatcher thread-safe 보장")
    void testThreadSafeStartDispatcher() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final MessageDispatcher[] instances = new MessageDispatcher[THREAD_COUNT];
        final Thread[] threads = new Thread[THREAD_COUNT];
        
        // When: 여러 스레드에서 동시에 startDispatcher 호출
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                BufferedReader mockReader = new BufferedReader(new StringReader(""));
                MessageDispatcher.startDispatcher(mockReader);
                instances[index] = MessageDispatcher.getInstance();
            });
            threads[i].start();
        }
        
        // 모든 스레드 대기
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: 모든 MessageDispatcher 인스턴스가 동일해야 함 (Thread 객체가 아님)
        // Thread 객체는 다를 수 있지만, MessageDispatcher 인스턴스 자체는 같아야 함
        for (int i = 1; i < THREAD_COUNT; i++) {
            assertEquals(instances[0].getId(), instances[i].getId(), 
                "동시 호출에도 모두 같은 MessageDispatcher 인스턴스를 반환해야 함 (스레드 ID로 검증)");
        }
    }
    
    /**
     * 테스트 7: 싱글턴 인스턴스 유일성 보장
     */
    @Test
    @DisplayName("프로그램 전체에서 단 하나의 디스패처만 실행")
    void testSingleDispatcherAcrossProgram() {
        // Given
        BufferedReader mockReader = new BufferedReader(new StringReader(""));
        
        // When
        MessageDispatcher.startDispatcher(mockReader);
        MessageDispatcher dispatcher1 = MessageDispatcher.getInstance();
        MessageDispatcher dispatcher2 = MessageDispatcher.getInstance();
        MessageDispatcher dispatcher3 = MessageDispatcher.getInstance();
        
        // Then: 모두 같은 인스턴스
        assertSame(dispatcher1, dispatcher2);
        assertSame(dispatcher2, dispatcher3);
        assertNotNull(dispatcher1);
    }
}
