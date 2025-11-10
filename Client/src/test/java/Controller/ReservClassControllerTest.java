package Controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import Model.Session;
import View.ReservClassView;
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
import java.util.function.Consumer;

public class ReservClassControllerTest {

    @Mock
    ReservClassView mockView;
    ReservClassController controller;
    JButton mockReservationButton;

    private static MockedStatic<Session> sessionMock;

    @BeforeAll
    static void initStaticMocks() {
        System.setProperty("java.awt.headless", "true");
        sessionMock = Mockito.mockStatic(Session.class);
        System.setProperty("test.env", "true");
    }

    @AfterAll
    static void closeStaticMocks() {
        sessionMock.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mockReservationButton = new JButton();

        when(mockView.getSelectedClassRoom()).thenReturn("908호");
        when(mockView.getSelectedDay()).thenReturn("월요일");
        when(mockView.getSelectedTime()).thenReturn("1교시(09:00~10:00)");
        when(mockView.getPurpose()).thenReturn("스터디");
        when(mockView.getStudentCount()).thenReturn(10);  // ✅ 추가
        when(mockView.getBeforeButton()).thenReturn(new JButton());
        when(mockView.getClassComboBox()).thenReturn(new JComboBox<>());
        when(mockView.getDayComboBox()).thenReturn(new JComboBox<>());  // ✅ 추가
        when(mockView.getTimeComboBox()).thenReturn(new JComboBox<>());  // ✅ 추가

        doNothing().when(mockView).resetReservationButtonListener();
        doNothing().when(mockView).addReservationListener(any());
        doNothing().when(mockView).updateCalendarTable(any());
        doNothing().when(mockView).showMessage(any());
        doNothing().when(mockView).closeView();
        doNothing().when(mockView).setCapacityInfoText(any());  // ✅ 추가

        doAnswer(invocation -> {
            ActionListener listener = invocation.getArgument(0);
            mockReservationButton.addActionListener(listener);
            return null;
        }).when(mockView).addReservationListener(any(ActionListener.class));
    }

    static class TestableReservClassController extends ReservClassController {
        private final CountDownLatch latch1;
        private final CountDownLatch latch2;

        public TestableReservClassController(ReservClassView view, CountDownLatch latch1, CountDownLatch latch2) {
            super(view);
            this.latch1 = latch1;
            this.latch2 = latch2;
        }

        @Override
        protected void refreshReservationAndAvailability(String roomName) {
            // 테스트를 위해 동기적으로 실행
            if (latch1 != null) {
                latch1.countDown();
            }
        }

        @Override
        protected void loadReservationDataFromServer(String roomName) {
            // 생략
        }

        @Override
        protected String sendReservationRequestToServer(String name, String room, String day, String time, String purpose, String role, int studentCount) {
            try {
                BufferedReader in = Session.getIn();
                return in.readLine();
            } catch (Exception e) {
                return "RESERVE_FAILED";
            } finally {
                latch2.countDown();
            }
        }

        // checkRoomAvailability 메소드 제거 (더 이상 사용하지 않음)
    }

 @Test
void testReserveRoom_Success() throws Exception {
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);
    setServerResponse("AVAILABLE", "RESERVE_SUCCESS");

    controller = new TestableReservClassController(mockView, latch1, latch2);
    injectReservedMap(controller);
    simulateButtonClick();

    latch1.await(2, TimeUnit.SECONDS);
    latch2.await(2, TimeUnit.SECONDS);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockView, atLeastOnce()).showMessage(captor.capture());  // <-- 핵심 수정
    String matchedMessage = captor.getAllValues().stream()
        .filter(msg -> msg.contains("예약이 완료되었습니다"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("예약 완료 메시지가 호출되지 않음"));

    System.out.println("[TEST_LOG] 예약 응답 메시지: " + matchedMessage);
    assertTrue(matchedMessage.contains("예약이 완료되었습니다"));
}


    @Test
    void testReserveRoom_Conflict() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        setServerResponse("AVAILABLE", "RESERVE_CONFLICT");

        controller = new TestableReservClassController(mockView, latch1, latch2);
        injectReservedMap(controller);
        simulateButtonClick();

        latch1.await(2, TimeUnit.SECONDS);
        latch2.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockView, timeout(1000)).showMessage(captor.capture());
        System.out.println("[TEST_LOG] 예약 응답 메시지: " + captor.getValue());
        assertTrue(captor.getValue().contains("이미 예약"));
    }

    @Test
    void testReserveRoom_PurposeEmpty() {
        when(mockView.getPurpose()).thenReturn("");
        CountDownLatch latch1 = new CountDownLatch(0);
        CountDownLatch latch2 = new CountDownLatch(0);

        controller = new TestableReservClassController(mockView, latch1, latch2);
        simulateButtonClick();

        verify(mockView).showMessage(contains("사용 목적을 입력해주세요"));
    }

    @Test
    void testReserveRoom_UnavailableRoom() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        setServerResponse("UNAVAILABLE");

        controller = new TestableReservClassController(mockView, latch1, latch2);
        injectReservedMap(controller);
        simulateButtonClick();

        latch1.await(2, TimeUnit.SECONDS);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockView, timeout(1000)).showMessage(captor.capture());
        System.out.println("[TEST_LOG] 예약 응답 메시지: " + captor.getValue());
        assertTrue(captor.getValue().contains("사용 불가능합니다"));
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

        sessionMock.when(Session::isConnected).thenReturn(true);  // ✅ 추가
        sessionMock.when(Session::getOut).thenReturn(mockOut);
        sessionMock.when(Session::getIn).thenReturn(mockIn);
        sessionMock.when(Session::getSocket).thenReturn(mockSocket);
        sessionMock.when(Session::getLoggedInUserId).thenReturn("S20230001");
        sessionMock.when(Session::getLoggedInUserName).thenReturn("김학생");
        sessionMock.when(Session::getLoggedInUserRole).thenReturn("학생");
    }

    private void simulateButtonClick() {
        for (ActionListener listener : mockReservationButton.getActionListeners()) {
            listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        }
    }

    private void injectReservedMap(ReservClassController controller) throws Exception {
        Field field = ReservClassController.class.getDeclaredField("reservedMap");
        field.setAccessible(true);
        Map<String, Set<String>> dummyMap = new HashMap<>();
        dummyMap.put("908호", new HashSet<>());
        field.set(controller, dummyMap);
    }
}
