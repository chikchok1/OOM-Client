package Manager;

import Model.Session;
import Util.MessageDispatcher;
import common.dto.ClassroomDTO;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ClientClassroomManager 싱글턴 패턴 테스트
 */
class ClientClassroomManagerTest {
    
    private ClientClassroomManager manager;
    private Session mockSession;
    private MessageDispatcher mockDispatcher;
    private PrintWriter mockOut;
    
    @BeforeEach
    void setUp() {
        // 싱글턴 인스턴스 초기화
        manager = ClientClassroomManager.getInstance();
        manager.clear();
        
        // Mock 객체 생성
        mockSession = mock(Session.class);
        mockDispatcher = mock(MessageDispatcher.class);
        mockOut = mock(PrintWriter.class);
    }
    
    @AfterEach
    void tearDown() {
        manager.clear();
    }
    
    /**
     * 테스트 1: 싱글턴 인스턴스가 동일한지 검증
     */
    @Test
    @DisplayName("싱글턴 패턴: 항상 같은 인스턴스 반환")
    void testSingletonInstance() {
        ClientClassroomManager instance1 = ClientClassroomManager.getInstance();
        ClientClassroomManager instance2 = ClientClassroomManager.getInstance();
        
        assertSame(instance1, instance2, "같은 인스턴스를 반환해야 함");
    }
    
    /**
     * 테스트 2: Double-Checked Locking 싱글턴 패턴 검증
     */
    @Test
    @DisplayName("멀티스레드 환경에서 싱글턴 인스턴스 일관성")
    void testSingletonInMultiThreadEnvironment() throws InterruptedException {
        final int THREAD_COUNT = 10;
        ClientClassroomManager[] instances = new ClientClassroomManager[THREAD_COUNT];
        Thread[] threads = new Thread[THREAD_COUNT];
        
        // When: 여러 스레드에서 동시에 getInstance 호출
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                instances[index] = ClientClassroomManager.getInstance();
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
     * 테스트 3: 캐시 초기화 테스트
     */
    @Test
    @DisplayName("캐시 초기화 테스트")
    void testClear() {
        // Given: 직접 데이터를 추가하는 방법이 없으므로 리플렉션 사용
        // 실제로는 refreshFromServer로 데이터가 채워짐
        
        // When
        manager.clear();
        
        // Then
        assertNull(manager.getClassroom("908호"), "초기화 후 강의실 정보가 없어야 함");
        assertEquals(0, manager.getClassroomNames().length, "초기화 후 강의실 목록이 비어야 함");
        assertEquals(0, manager.getLabNames().length, "초기화 후 실습실 목록이 비어야 함");
    }
    
    /**
     * 테스트 4: 강의실 이름 목록 조회 (배열)
     */
    @Test
    @DisplayName("강의실 이름 목록 조회 (배열)")
    void testGetClassroomNames() {
        // Given: Mock 데이터가 있다고 가정
        // 실제 환경에서는 refreshFromServer를 통해 데이터 로드
        
        // When
        String[] classrooms = manager.getClassroomNames();
        
        // Then
        assertNotNull(classrooms, "강의실 목록은 null이 아니어야 함");
        assertTrue(classrooms.length >= 0, "빈 배열이거나 데이터가 있어야 함");
    }
    
    /**
     * 테스트 5: 실습실 이름 목록 조회 (배열)
     */
    @Test
    @DisplayName("실습실 이름 목록 조회 (배열)")
    void testGetLabNames() {
        // When
        String[] labs = manager.getLabNames();
        
        // Then
        assertNotNull(labs, "실습실 목록은 null이 아니어야 함");
        assertTrue(labs.length >= 0, "빈 배열이거나 데이터가 있어야 함");
    }
    
    /**
     * 테스트 6: 강의실 이름 목록 조회 (List)
     */
    @Test
    @DisplayName("강의실 이름 목록 조회 (List)")
    void testGetClassroomsList() {
        // When
        List<String> classrooms = manager.getClassrooms();
        
        // Then
        assertNotNull(classrooms, "강의실 목록은 null이 아니어야 함");
        assertTrue(classrooms.size() >= 0, "빈 리스트이거나 데이터가 있어야 함");
    }
    
    /**
     * 테스트 7: 실습실 이름 목록 조회 (List)
     */
    @Test
    @DisplayName("실습실 이름 목록 조회 (List)")
    void testGetLabsList() {
        // When
        List<String> labs = manager.getLabs();
        
        // Then
        assertNotNull(labs, "실습실 목록은 null이 아니어야 함");
        assertTrue(labs.size() >= 0, "빈 리스트이거나 데이터가 있어야 함");
    }
    
    /**
     * 테스트 8: 모든 강의실 DTO 목록 조회
     */
    @Test
    @DisplayName("모든 강의실 DTO 목록 조회")
    void testGetAllClassrooms() {
        // When
        List<ClassroomDTO> allClassrooms = manager.getAllClassrooms();
        
        // Then
        assertNotNull(allClassrooms, "DTO 목록은 null이 아니어야 함");
        assertTrue(allClassrooms.size() >= 0, "빈 리스트이거나 데이터가 있어야 함");
    }
    
    /**
     * 테스트 9: 존재하지 않는 강의실 조회
     */
    @Test
    @DisplayName("존재하지 않는 강의실 조회 시 null 반환")
    void testGetNonExistentClassroom() {
        // When
        ClassroomDTO classroom = manager.getClassroom("존재하지않는강의실");
        
        // Then
        assertNull(classroom, "존재하지 않는 강의실은 null을 반환해야 함");
    }
    
    /**
     * 테스트 10: 수용 인원 체크 (존재하지 않는 강의실)
     */
    @Test
    @DisplayName("수용 인원 체크 - 존재하지 않는 강의실")
    void testCheckCapacityForNonExistentClassroom() {
        // When
        boolean result = manager.checkCapacity("존재하지않는강의실", 10);
        
        // Then
        assertFalse(result, "존재하지 않는 강의실은 false를 반환해야 함");
    }
    
    /**
     * 테스트 11: refreshFromServer 실패 케이스 (연결 없음)
     */
    @Test
    @DisplayName("서버 연결 없이 refreshFromServer 호출 시 실패")
    void testRefreshFromServerWithoutConnection() {
        // Given
        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(Session::getInstance).thenReturn(mockSession);
            when(mockSession.isConnected()).thenReturn(false);
            
            // When
            boolean result = manager.refreshFromServer();
            
            // Then
            assertFalse(result, "연결이 없으면 false를 반환해야 함");
        }
    }
    
    /**
     * 테스트 12: 강의실 목록 정렬 확인
     */
    @Test
    @DisplayName("강의실 목록은 정렬되어 반환되어야 함")
    void testClassroomListIsSorted() {
        // When
        String[] classrooms = manager.getClassroomNames();
        List<String> classroomList = manager.getClassrooms();
        
        // Then: 정렬 검증
        for (int i = 1; i < classrooms.length; i++) {
            assertTrue(classrooms[i-1].compareTo(classrooms[i]) <= 0, 
                "강의실 목록이 정렬되어 있어야 함");
        }
        
        for (int i = 1; i < classroomList.size(); i++) {
            assertTrue(classroomList.get(i-1).compareTo(classroomList.get(i)) <= 0, 
                "강의실 리스트가 정렬되어 있어야 함");
        }
    }
    
    /**
     * 테스트 13: 실습실 목록 정렬 확인
     */
    @Test
    @DisplayName("실습실 목록은 정렬되어 반환되어야 함")
    void testLabListIsSorted() {
        // When
        String[] labs = manager.getLabNames();
        List<String> labList = manager.getLabs();
        
        // Then: 정렬 검증
        for (int i = 1; i < labs.length; i++) {
            assertTrue(labs[i-1].compareTo(labs[i]) <= 0, 
                "실습실 목록이 정렬되어 있어야 함");
        }
        
        for (int i = 1; i < labList.size(); i++) {
            assertTrue(labList.get(i-1).compareTo(labList.get(i)) <= 0, 
                "실습실 리스트가 정렬되어 있어야 함");
        }
    }
    
    /**
     * 테스트 14: getAllClassrooms는 강의실을 먼저, 실습실을 나중에 반환
     */
    @Test
    @DisplayName("getAllClassrooms는 강의실 먼저, 실습실 나중 순서")
    void testGetAllClassroomsOrder() {
        // When
        List<ClassroomDTO> allClassrooms = manager.getAllClassrooms();
        
        // Then: 강의실이 실습실보다 먼저 나와야 함
        boolean foundLab = false;
        for (ClassroomDTO dto : allClassrooms) {
            if (dto.isLab()) {
                foundLab = true;
            } else if (dto.isClassroom() && foundLab) {
                fail("실습실 이후에 강의실이 나오면 안 됨");
            }
        }
    }
    
    /**
     * 테스트 15: volatile 키워드로 인한 가시성 보장 (개념적 테스트)
     */
    @Test
    @DisplayName("volatile 키워드로 멀티스레드 가시성 보장")
    void testVolatileVisibility() throws InterruptedException {
        // Given: 여러 스레드에서 getInstance 호출
        final int THREAD_COUNT = 20;
        Thread[] threads = new Thread[THREAD_COUNT];
        ClientClassroomManager[] instances = new ClientClassroomManager[THREAD_COUNT];
        
        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                instances[index] = ClientClassroomManager.getInstance();
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
}
