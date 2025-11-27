package Controller;

import Model.Session;
import Util.MessageDispatcher;
import org.junit.jupiter.api.*;

import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

public class ClientFacadeTest {

    @BeforeAll
    static void beforeAll() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("test.env", "true");
    }

    @AfterEach
    void tearDown() {
        // clean session and dispatcher between tests
        Session.resetInstance();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        if (dispatcher != null) {
            dispatcher.stopDispatcher();
        }
    }

    @Test
    void changePassword_sendsRequestAndDisposesView_onSuccessResponse() throws Exception {
        // arrange
        Session.resetInstance();
        Session s = Session.getInstance();
        s.setLoggedInUserId("S123");

        // capture outgoing request
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bout, true);
        s.setOut(out);

        // prepare dispatcher input/response pipe
        PipedOutputStream responseWriter = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream(responseWriter);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientInput));
        s.setIn(in);

        // start dispatcher
        MessageDispatcher.startDispatcher(in);

        // response will be written after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(100);
                responseWriter.write(("PASSWORD_CHANGED\n").getBytes());
                responseWriter.flush();
            } catch (Exception ignored) {}
        }).start();

        // act: call programmatic overload (no GUI required)
        boolean ok = ClientFacade.changePassword("S123", "oldpw", "newpw");

        // assert - outgoing request contains the expected command and payload
        String sent = bout.toString();
        assertTrue(sent.startsWith("CHANGE_PASSWORD,"), "Expected CHANGE_PASSWORD request sent");
        assertTrue(sent.contains("S123,oldpw,newpw"), "Payload should contain userId and passwords");
        assertTrue(ok, "changePassword(...) should return success when dispatcher responds PASSWORD_CHANGED");
    }
}
