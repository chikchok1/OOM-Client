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
import java.util.function.Consumer;

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

        // ✅ 싱글턴 인스턴스 초기화
        Session.resetInstance();
        mockSession = Session.getInstance();

        when(mockView.getSelectedClassRoom()).thenReturn("911호");
        when(mockView.getSelectedDay()).thenReturn("화요일");
        when(mockView.getSelectedTime()).thenReturn("3교시(11:00~12:00)");
        when(mockView.getPurpose()).thenReturn("실험");
        when(mockView.getBeforeButton()).thenReturn(new JButton());
        when(mockView.getLabComboBox()).thenReturn(new JComboBox<>());

        doNothing().when(mockView).resetReservationButtonListener();
        doNothing().when(mockView).addReservationListener(any());
        doNothing().when(mockView).updateCalendarTable(any());
        doNothing().when(mockView).showMessage(any());
        doNothing().when(mockView).closeView();

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

        protected void refreshReservationData(String roomName) {
            if (latch1 != null) {
                checkRoomAvailability(roomName, isAvailable -> latch1.countDown());
            }
        }

        protected void loadReservationDataFromServer(String roomName) {
            // 생략
        }

        protected String sendReservationRequestToServer(String name, String room, String day, String time, String purpose, String role) {
            try {
                BufferedReader in = Session.getInstance().getIn();
                return in.readLine();
            } catch (Exception e) {
                return "RESERVE_FAILED";
            } finally {
                latch2.countDown();
            }
        }

        protected void checkRoomAvailability(String classRoom, Consumer<Boolean> callback) {
            try {
                BufferedReader in = Session.getInstance().getIn();
                String response = in.readLine();
                boolean available = "AVAILABLE".equals(response);
                callback.accept(available);
            } catch (Exception e) {
                callback.accept(false);
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
        simulateButtonClick();

        latch1.await(2, TimeUnit.SECONDS);
        latch2.await(2, TimeUnit.SECONDS);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockView, timeout(1000).atLeastOnce()).showMessage(captor.capture());
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

        controller = new TestableReservLabController(mockView, latch1, latch2);
        injectReservedMap(controller);
        simulateButtonClick();

        latch1.await(2, TimeUnit.SECONDS);
        latch2.await(2, TimeUnit.SECONDS);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockView, timeout(1000).atLeastOnce()).showMessage(captor.capture());

        List<String> messages = captor.getAllValues();
        System.out.println("[TEST_LOG] 받은 메시지들: " + messages);
        boolean found = messages.stream().anyMatch(msg -> msg.contains("이미 해당 시간에 예약"));
        assertTrue(found, "예약 충돌 메시지가 호출되지 않았습니다.");
    }

    @Test
    void testReserveRoom_PurposeEmpty() {
        when(mockView.getPurpose()).thenReturn("");
        CountDownLatch latch1 = new CountDownLatch(0);
        CountDownLatch latch2 = new CountDownLatch(0);

        controller = new TestableReservLabController(mockView, latch1, latch2);
        simulateButtonClick();

        verify(mockView).showMessage(contains("사용 목적을 입력해주세요"));
    }

    @Test
    void testReserveRoom_UnavailableRoom() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        setServerResponse("UNAVAILABLE");

        controller = new TestableReservLabController(mockView, latch1, latch2);
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

        // ✅ 싱글턴 인스턴스에 직접 설정
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

    private void injectReservedMap(ReservLabController controller) throws Exception {
        Field field = ReservLabController.class.getDeclaredField("reservedMap");
        field.setAccessible(true);
        Map<String, Set<String>> dummyMap = new HashMap<>();
        dummyMap.put("911호", new HashSet<>());
        field.set(controller, dummyMap);
    }
}
