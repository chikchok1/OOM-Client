package Controller;

import Model.Session;
import View.ClassroomReservationApproval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.awt.event.ActionListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ClassroomReservationApprovalControllerTest {

    private ClassroomReservationApproval mockView;
    private JTable table;
    private DefaultTableModel model;
    private JButton approveButton;
    private JButton rejectButton;

    @BeforeEach
    void setUp() {
        approveButton = new JButton();
        rejectButton = new JButton();
        model = new DefaultTableModel(new Object[]{"ID", "Time", "Day", "Room", "Name"}, 0);
        table = new JTable(model);

        mockView = mock(ClassroomReservationApproval.class);
        when(mockView.getApproveButton()).thenReturn(approveButton);
        when(mockView.getRejectButton()).thenReturn(rejectButton);
        when(mockView.getTable()).thenReturn(table);
    }

    @Test
    void testApproveReservation() {
        model.addRow(new Object[]{"1", "09:00", "Monday", "A101", "홍길동"});
        table.setRowSelectionInterval(0, 0);

        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw, true);
        StringReader sr = new StringReader("APPROVE_SUCCESS\n");
        BufferedReader in = new BufferedReader(sr);

        try (
            MockedStatic<Session> sessionMock = mockStatic(Session.class);
            MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)
        ) {
            sessionMock.when(Session::getOut).thenReturn(out);
            sessionMock.when(Session::getIn).thenReturn(in);

            dialogMock.when(() -> JOptionPane.showMessageDialog(any(), anyString())).then(inv -> null);

            new ClassroomReservationApprovalController(mockView);

            for (ActionListener listener : approveButton.getActionListeners()) {
                listener.actionPerformed(null);
            }

            assertEquals(0, model.getRowCount());
        }
    }

    @Test
    void testRejectReservation() {
        model.addRow(new Object[]{"2", "13:00", "Tuesday", "B202", "김철수"});
        table.setRowSelectionInterval(0, 0);

        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw, true);
        StringReader sr = new StringReader("REJECT_SUCCESS\n");
        BufferedReader in = new BufferedReader(sr);

        try (
            MockedStatic<Session> sessionMock = mockStatic(Session.class);
            MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)
        ) {
            sessionMock.when(Session::getOut).thenReturn(out);
            sessionMock.when(Session::getIn).thenReturn(in);

            dialogMock.when(() -> JOptionPane.showMessageDialog(any(), anyString())).then(inv -> null);

            new ClassroomReservationApprovalController(mockView);

            for (ActionListener listener : rejectButton.getActionListeners()) {
                listener.actionPerformed(null);
            }

            assertEquals(0, model.getRowCount());
        }
    }

    @Test
    void testLoadAllRequests_withNoData() {
        StringReader input = new StringReader("END_OF_REQUESTS\n");
        BufferedReader in = new BufferedReader(input);
        PrintWriter out = new PrintWriter(new StringWriter());

        try (
            MockedStatic<Session> sessionMock = mockStatic(Session.class);
            MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)
        ) {
            sessionMock.when(Session::getOut).thenReturn(out);
            sessionMock.when(Session::getIn).thenReturn(in);

            dialogMock.when(() -> JOptionPane.showMessageDialog(any(), anyString())).then(inv -> null);

            new ClassroomReservationApprovalController(mockView);

            assertEquals(0, model.getRowCount());
        }
    }
}
