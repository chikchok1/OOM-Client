package Controller;

import Model.Session;
import View.ClassroomReservationApproval;
import org.junit.jupiter.api.AfterEach;
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
    private Session mockSession;

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

        // ✅ 싱글턴 인스턴스 초기화
        Session.resetInstance();
        mockSession = Session.getInstance();
    }

    @AfterEach
    void tearDown() {
        Session.resetInstance();
    }

    @Test
    void testApproveReservation() {
        model.addRow(new Object[]{"1", "09:00", "Monday", "A101", "홍길동"});
        table.setRowSelectionInterval(0, 0);

        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw, true);
        StringReader sr = new StringReader("APPROVE_SUCCESS\n");
        BufferedReader in = new BufferedReader(sr);

        try (MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(out);
            mockSession.setIn(in);

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

        try (MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(out);
            mockSession.setIn(in);

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

        try (MockedStatic<JOptionPane> dialogMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(out);
            mockSession.setIn(in);

            dialogMock.when(() -> JOptionPane.showMessageDialog(any(), anyString())).then(inv -> null);

            new ClassroomReservationApprovalController(mockView);

            assertEquals(0, model.getRowCount());
        }
    }
}
