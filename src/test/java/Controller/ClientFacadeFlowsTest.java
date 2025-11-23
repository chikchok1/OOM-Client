package Controller;

import Model.Session;
import Util.MessageDispatcher;
import org.junit.jupiter.api.*;

import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ClientFacadeFlowsTest {

    @BeforeAll
    static void beforeAll() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("test.env", "true");
    }

    @AfterEach
    void tearDown() {
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        if (dispatcher != null) dispatcher.stopDispatcher();
        Session.resetInstance();
    }

    @Test
    void loadUsers_populatesTable() throws Exception {
        Session.resetInstance();
        Session s = Session.getInstance();

        // capture outgoing request
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        s.setOut(new java.io.PrintWriter(bout, true));

        // prepare input stream: two user lines then END_OF_USERS
        PipedOutputStream writer = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream(writer);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientInput));
        s.setIn(in);

        // start dispatcher
        MessageDispatcher.startDispatcher(in);

        // write responses asynchronously
        new Thread(() -> {
            try {
                writer.write(("u1,User One,pass1\n").getBytes());
                writer.flush();
                Thread.sleep(50);
                writer.write(("u2,User Two,pass2\n").getBytes());
                writer.flush();
                Thread.sleep(50);
                writer.write(("END_OF_USERS\n").getBytes());
                writer.flush();
            } catch (Exception ignored) {}
        }).start();

        DefaultTableModel model = new DefaultTableModel(new String[]{"id","name","pw"}, 0);

        ClientFacade.loadUsers(model);

        // wait up to 2s for rows to appear
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && model.getRowCount() < 2) {
            Thread.sleep(50);
        }

        assertEquals(2, model.getRowCount(), "Expected two users in model");
        assertEquals("u1", model.getValueAt(0,0));
        assertEquals("User One", model.getValueAt(0,1));
        assertEquals("u2", model.getValueAt(1,0));
    }

    @Test
    void deleteUser_callsOnSuccess_onDeleteSuccess() throws Exception {
        Session.resetInstance();
        Session s = Session.getInstance();

        s.setOut(new java.io.PrintWriter(new ByteArrayOutputStream(), true));

        PipedOutputStream writer = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream(writer);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientInput));
        s.setIn(in);
        MessageDispatcher.startDispatcher(in);

        CountDownLatch latch = new CountDownLatch(1);

        // prepare response
        new Thread(() -> {
            try {
                Thread.sleep(50);
                writer.write(("DELETE_SUCCESS\n").getBytes());
                writer.flush();
            } catch (Exception ignored) {}
        }).start();

        ClientFacade.deleteUser("u1", null, latch::countDown);

        boolean ok = latch.await(1500, TimeUnit.MILLISECONDS);
        assertTrue(ok, "onSuccess should be invoked on DELETE_SUCCESS response");
    }

    @Test
    void updateUser_updatesModel_onUpdateSuccess() throws Exception {
        Session.resetInstance();
        Session s = Session.getInstance();
        s.setOut(new java.io.PrintWriter(new ByteArrayOutputStream(), true));

        PipedOutputStream writer = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream(writer);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientInput));
        s.setIn(in);
        MessageDispatcher.startDispatcher(in);

        DefaultTableModel model = new DefaultTableModel(new String[]{"name","col1","pw"}, 0);
        model.addRow(new Object[]{"oldName","x","oldPw"});

        // response
        new Thread(() -> {
            try {
                Thread.sleep(50);
                writer.write(("UPDATE_SUCCESS\n").getBytes());
                writer.flush();
            } catch (Exception ignored) {}
        }).start();

        ClientFacade.updateUser("u1", "newName", "newPw", null, 0, model);

        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && !"newName".equals(model.getValueAt(0,0))) {
            Thread.sleep(50);
        }

        assertEquals("newName", model.getValueAt(0,0));
        assertEquals("newPw", model.getValueAt(0,2));
    }
}
