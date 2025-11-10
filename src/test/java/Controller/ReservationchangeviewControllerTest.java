package Controller;

import Model.Session;
import View.Reservationchangeview;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationchangeviewControllerTest {

    private Reservationchangeview mockView;
    private JTable table;
    private DefaultTableModel model;
    private JButton changeButton;
    private JButton cancelButton;
    private JButton backButton;
    private Session mockSession;

    @BeforeEach
    void setUp() {
        changeButton = new JButton();
        cancelButton = new JButton();
        backButton = new JButton();

        model = new DefaultTableModel(new Object[]{"ID", "Time", "Day", "Room", "Name"}, 0);
        table = new JTable(model);

        mockView = mock(Reservationchangeview.class);
        when(mockView.getReservationTable()).thenReturn(table);
        when(mockView.getSelectedDay()).thenReturn("화요일");
        when(mockView.getSelectedTime()).thenReturn("3교시");
        when(mockView.getSelectedRoom()).thenReturn("101호");

        doAnswer(inv -> {
            changeButton.addActionListener(inv.getArgument(0));
            return null;
        }).when(mockView).setChangeButtonActionListener(any());

        doAnswer(inv -> {
            cancelButton.addActionListener(inv.getArgument(0));
            return null;
        }).when(mockView).setCancelButtonActionListener(any());

        doAnswer(inv -> {
            backButton.addActionListener(inv.getArgument(0));
            return null;
        }).when(mockView).setBackButtonActionListener(any());

        // ✅ 싱글턴 인스턴스 초기화
        Session.resetInstance();
        mockSession = Session.getInstance();
    }

    @AfterEach
    void tearDown() {
        Session.resetInstance();
    }

    @Test
    void testReservationChange_success() {
        model.addRow(new Object[]{"S123", "2교시", "월요일", "201호", "홍길동"});
        table.setRowSelectionInterval(0, 0);

        StringReader sr = new StringReader("CHANGE_SUCCESS\n");
        BufferedReader in = new BufferedReader(sr);
        PrintWriter out = new PrintWriter(new StringWriter(), true);

        try (MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(out);
            mockSession.setIn(in);
            mockSession.setLoggedInUserId("S123");
            mockSession.setLoggedInUserName("홍길동");

            dialogMock.when(() -> JOptionPane.showMessageDialog(any(), anyString())).then(inv -> null);

            new ReservationchangeviewController(mockView);

            for (var listener : changeButton.getActionListeners()) {
                listener.actionPerformed(null);
            }

            assertEquals(0, model.getRowCount());
        }
    }

    @Test
    void testReservationCancel_success() {
        model.addRow(new Object[]{"S123", "2교시", "월요일", "201호", "홍길동"});
        table.setRowSelectionInterval(0, 0);

        StringReader sr = new StringReader("CANCEL_SUCCESS\n");
        BufferedReader in = new BufferedReader(sr);
        PrintWriter out = new PrintWriter(new StringWriter(), true);

        try (MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(out);
            mockSession.setIn(in);

            dialogMock.when(() -> JOptionPane.showMessageDialog(any(), anyString())).then(inv -> null);

            new ReservationchangeviewController(mockView);

            for (var listener : cancelButton.getActionListeners()) {
                listener.actionPerformed(null);
            }

            assertEquals(0, model.getRowCount());
        }
    }

    @Test
    void testReservationChange_conflict() {
        model.addRow(new Object[]{"S123", "2교시", "월요일", "201호", "홍길동"});
        table.setRowSelectionInterval(0, 0);

        BufferedReader in = mock(BufferedReader.class);
        PrintWriter out = new PrintWriter(new StringWriter(), true);

        try (MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(out);
            mockSession.setIn(in);
            mockSession.setLoggedInUserId("S123");
            mockSession.setLoggedInUserName("홍길동");

            when(in.readLine())
                .thenReturn("S123,2교시,월요일,201호,홍길동,X,Y")
                .thenReturn("CONFLICT")
                .thenReturn("END_OF_MY_RESERVATIONS");

            dialogMock.when(() -> JOptionPane.showMessageDialog(any(), contains("이미 해당 시간에 예약된")))
                      .then(inv -> null);

            new ReservationchangeviewController(mockView);

            for (var listener : changeButton.getActionListeners()) {
                listener.actionPerformed(null);
            }

            assertEquals(1, model.getRowCount());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
