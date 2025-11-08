package Controller;

import Model.Session;
import View.ChangePasswordView;
import org.junit.jupiter.api.*;
import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;

import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ChangePasswordControllerTest {

    private ChangePasswordView mockView;
    private ChangePasswordController controller;
    private MockedStatic<Session> sessionMock;
    private MockedStatic<JOptionPane> mockJOptionPane;

    @BeforeEach
    void setUp() {
        mockView = mock(ChangePasswordView.class);
        doNothing().when(mockView).dispose();
        controller = new ChangePasswordController(mockView);

        sessionMock = mockStatic(Session.class);
        sessionMock.when(Session::getLoggedInUserId).thenReturn("S1234");

        mockJOptionPane = mockStatic(JOptionPane.class);
        mockJOptionPane.when(() -> JOptionPane.showMessageDialog(any(), any()))
                       .thenAnswer(invocation -> null);
    }

    @AfterEach
    void tearDown() {
        if (sessionMock != null) sessionMock.close();
        if (mockJOptionPane != null) mockJOptionPane.close(); // 꼭 닫아야 중복 방지됨
    }

    @Test
    void testChangePasswordSuccess() throws IOException, InterruptedException {
        when(mockView.getPresentPassword()).thenReturn("oldpass");
        when(mockView.getChangePassword()).thenReturn("newpass");

        PipedOutputStream serverInput = new PipedOutputStream();
        PrintWriter mockOut = new PrintWriter(serverInput, true);
        PipedInputStream clientOutput = new PipedInputStream(serverInput);

        PipedOutputStream clientInput = new PipedOutputStream();
        BufferedReader mockIn = new BufferedReader(new InputStreamReader(new PipedInputStream(clientInput)));

        sessionMock.when(Session::getOut).thenReturn(mockOut);
        sessionMock.when(Session::getIn).thenReturn(mockIn);

        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                Thread.sleep(200);
                clientInput.write("PASSWORD_CHANGED\n".getBytes());
                clientInput.flush();
                latch.countDown();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try (
            MockedStatic<GraphicsEnvironment> graphicsMock = mockStatic(GraphicsEnvironment.class);
            MockedConstruction<View.RoomSelect> roomSelectMock = mockConstruction(View.RoomSelect.class);
            MockedConstruction<View.Executive> executiveMock = mockConstruction(View.Executive.class)
        ) {
            graphicsMock.when(GraphicsEnvironment::isHeadless).thenReturn(false);
            GraphicsEnvironment fakeEnv = mock(GraphicsEnvironment.class);
            graphicsMock.when(GraphicsEnvironment::getLocalGraphicsEnvironment).thenReturn(fakeEnv);
            when(fakeEnv.getDefaultScreenDevice()).thenReturn(mock(java.awt.GraphicsDevice.class));

            controller.changePassword();
            latch.await(1, TimeUnit.SECONDS);
            verify(mockView, atLeastOnce()).dispose();
        }
    }

    @Test
    void testUserNotFound() throws IOException, InterruptedException {
        when(mockView.getPresentPassword()).thenReturn("oldpass");
        when(mockView.getChangePassword()).thenReturn("newpass");

        PipedOutputStream serverInput = new PipedOutputStream();
        PrintWriter mockOut = new PrintWriter(serverInput, true);
        PipedInputStream clientOutput = new PipedInputStream(serverInput);

        PipedOutputStream clientInput = new PipedOutputStream();
        BufferedReader mockIn = new BufferedReader(new InputStreamReader(new PipedInputStream(clientInput)));

        sessionMock.when(Session::getOut).thenReturn(mockOut);
        sessionMock.when(Session::getIn).thenReturn(mockIn);

        new Thread(() -> {
            try {
                Thread.sleep(100);
                clientInput.write("USER_NOT_FOUND\n".getBytes());
                clientInput.flush();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        controller.changePassword();
        verify(mockView, never()).dispose();
    }

    @Test
    void testEmptyFields() {
        when(mockView.getPresentPassword()).thenReturn("");
        when(mockView.getChangePassword()).thenReturn("");

        controller.changePassword();

        mockJOptionPane.verify(() ->
            JOptionPane.showMessageDialog(any(), eq("모든 필드를 입력해주세요.")),
            times(1)
        );
    }
}
