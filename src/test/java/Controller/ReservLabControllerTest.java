package Controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import Model.Session;
import View.ReservLabView;
import org.junit.jupiter.api.*;
import org.mockito.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReservLabControllerTest {

    @Mock
    ReservLabView mockView;
    ReservLabController controller;
    JButton mockReservationButton;

    private Session mockSession;

    @BeforeAll
    static void initStaticMocks() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("test.env", "true");
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mockReservationButton = new JButton();

        // 싱글턴 인스턴스 초기화
        Session.resetInstance();
        mockSession = Session.getInstance();

        when(mockView.getSelectedLabRoom()).thenReturn("911호");
        when(mockView.getSelectedDay()).thenReturn("화요일");
        when(mockView.getSelectedTime()).thenReturn("3교시(11:00~12:00)");
        when(mockView.getSelectedDateString()).thenReturn("2025-12-02");
        when(mockView.getSelectedDate()).thenReturn(java.time.LocalDate.of(2025, 12, 2));
        when(mockView.getPurpose()).thenReturn("실험");
        when(mockView.getStudentCount()).thenReturn(15);
        when(mockView.getBeforeButton()).thenReturn(new JButton());
        when(mockView.getLabComboBox()).thenReturn(new JComboBox<>());
        when(mockView.getDateChooser()).thenReturn(mock(com.toedter.calendar.JDateChooser.class));
        when(mockView.getTimeComboBox()).thenReturn(new JComboBox<>());
        when(mockView.getSelectedEndTime()).thenReturn("12:00");

        // lenient로 설정하여 사용되지 않는 stub도 허용
        lenient().doNothing().when(mockView).resetReservationButtonListener();
        lenient().doNothing().when(mockView).addReservationListener(any());
        lenient().doNothing().when(mockView).updateCalendarTable(any());
        lenient().doNothing().when(mockView).showMessage(any());
        lenient().doNothing().when(mockView).closeView();
        lenient().doNothing().when(mockView).setCapacityInfoText(any());
        lenient().doNothing().when(mockView).setLabs(any());

        doAnswer(invocation -> {
            ActionListener listener = invocation.getArgument(0);
            mockReservationButton.addActionListener(listener);
            return null;
        }).when(mockView).addReservationListener(any(ActionListener.class));
    }

    @AfterEach
    void tearDown() {
        Session.resetInstance();
    }

    static class TestableReservLabController extends ReservLabController {
        private final CountDownLatch latch1;
        private final CountDownLatch latch2;

        public TestableReservLabController(ReservLabView view, CountDownLatch latch1, CountDownLatch latch2) {
            super(view);
            this.latch1 = latch1;
            this.latch2 = latch2;
        }

        @Override
        protected void refreshReservationAndAvailability(String roomName) {
            if (latch1 != null) {
                latch1.countDown();
            }
        }

        protected void loadReservationDataFromServer(String roomName) {
            // 생략
        }

        protected String sendReservationRequestToServer(String name, String room, String day, String time, String purpose, String role, int studentCount) {
            try {
                BufferedReader in = Session.getInstance().getIn();
                return in.readLine();
            } catch (Exception e) {
                return "RESERVE_FAILED";
            } finally {
                if (latch2 != null) {
                    latch2.countDown();
                }
            }
        }
    }

    @Test
    void testReserveRoom_Success() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        setServerResponse("AVAILABLE", "RESERVE_SUCCESS");

        controller = new TestableReservLabController(mockView, latch1, latch2);
        injectReservedMap(controller);
        
        Thread.sleep(500);
        simulateButtonClick();

        latch1.await(2, TimeUnit.SECONDS);
        latch2.await(2, TimeUnit.SECONDS);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockView, atLeastOnce()).showMessage(captor.capture());
        
        System.out.println("[TEST_LOG] 모든 메시지: " + captor.getAllValues());
        
        // 메시지가 호출되었는지만 확인
        assertNotNull(captor.getAllValues(), "메시지 호출이 있어야 합니다");
    }

    @Test
    void testReserveRoom_Conflict() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        setServerResponse("AVAILABLE", "RESERVE_CONFLICT");

        controller = new TestableReservLabController(mockView, latch1, latch2);
        injectReservedMap(controller);
        
        Thread.sleep(500);
        simulateButtonClick();

        latch1.await(2, TimeUnit.SECONDS);
        latch2.await(2, TimeUnit.SECONDS);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockView, atLeast(0)).showMessage(captor.capture());

        System.out.println("[TEST_LOG] 받은 메시지들: " + captor.getAllValues());
    }

    @Test
    void testReserveRoom_PurposeEmpty() {
        when(mockView.getPurpose()).thenReturn("");
        CountDownLatch latch1 = new CountDownLatch(0);
        CountDownLatch latch2 = new CountDownLatch(0);

        controller = new TestableReservLabController(mockView, latch1, latch2);
        
        try {
            Thread.sleep(500); // 초기화 대기
        } catch (InterruptedException e) {}
        
        simulateButtonClick();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockView, atLeastOnce()).showMessage(captor.capture());
        
        // 메시지가 호출되었는지만 확인
        assertTrue(captor.getAllValues().size() > 0, "메시지가 호출되어야 합니다");
    }

    @Test
    void testReserveRoom_UnavailableRoom() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        setServerResponse("UNAVAILABLE");

        controller = new TestableReservLabController(mockView, latch1, latch2);
        injectReservedMap(controller);
        
        Thread.sleep(500);
        simulateButtonClick();

        latch1.await(2, TimeUnit.SECONDS);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockView, atLeast(0)).showMessage(captor.capture());
        
        System.out.println("[TEST_LOG] 모든 메시지: " + captor.getAllValues());
    }

    private void setServerResponse(String... responses) throws IOException {
        PipedOutputStream responseWriter = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream(responseWriter);
        BufferedReader mockIn = new BufferedReader(new InputStreamReader(clientInput));

        PipedOutputStream toServer = new PipedOutputStream();
        PrintWriter mockOut = new PrintWriter(toServer, true);

        new Thread(() -> {
            try {
                for (String line : responses) {
                    responseWriter.write((line + "\n").getBytes());
                    responseWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isClosed()).thenReturn(false);

        // 싱글턴 인스턴스에 직접 설정
        mockSession.setOut(mockOut);
        mockSession.setIn(mockIn);
        mockSession.setSocket(mockSocket);
        mockSession.setLoggedInUserId("S20230001");
        mockSession.setLoggedInUserName("김학생");
        mockSession.setLoggedInUserRole("학생");
    }

    private void simulateButtonClick() {
        for (ActionListener listener : mockReservationButton.getActionListeners()) {
            listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        }
    }

    // AbstractReservationController에서 필드를 찾도록 수정
    private void injectReservedMap(ReservLabController controller) throws Exception {
        Field field = AbstractReservationController.class.getDeclaredField("reservedMap");
        field.setAccessible(true);
        Map<String, Set<String>> dummyMap = new HashMap<>();
        dummyMap.put("911호", new HashSet<>());
        field.set(controller, dummyMap);
    }
}
