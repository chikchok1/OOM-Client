package Controller;

import Model.Session;
import View.ReservedRoomCancelView;
import View.Executive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReservedRoomCancelControllerTest {

    private ReservedRoomCancelView mockView;
    private JTable mockTable;
    private JButton mockCancelButton;
    private JButton mockBackButton;

    @BeforeEach
    void setUp() {
        mockView = mock(ReservedRoomCancelView.class);
        mockTable = new JTable(new DefaultTableModel(new Object[]{"User ID", "Time", "Day", "Room", "Name"}, 0));
        mockCancelButton = new JButton();
        mockBackButton = new JButton();

        when(mockView.getTable()).thenReturn(mockTable);
        when(mockView.getCancelButton()).thenReturn(mockCancelButton);
        when(mockView.getBackButton()).thenReturn(mockBackButton);
    }

    @Test
    void testCancelReservation_success() throws Exception {
        ((DefaultTableModel) mockTable.getModel()).addRow(new Object[]{"S123", "1교시", "월요일", "101호", "홍길동"});
        mockTable.setRowSelectionInterval(0, 0);

        StringReader serverInput = new StringReader("CANCEL_SUCCESS\nEND_\n");
        BufferedReader in = new BufferedReader(serverInput);
        PrintWriter out = new PrintWriter(new StringWriter(), true);

        try (
                MockedStatic<Session> sessionMock = mockStatic(Session.class);
                MockedStatic<JOptionPane> paneMock = mockStatic(JOptionPane.class)
        ) {
            sessionMock.when(Session::getOut).thenReturn(out);
            sessionMock.when(Session::getIn).thenReturn(in);
            sessionMock.when(Session::getLoggedInUserId).thenReturn("S123");
            sessionMock.when(Session::getLoggedInUserRole).thenReturn("학생");

            paneMock.when(() -> JOptionPane.showMessageDialog(any(), any())).then(invocation -> null);

            ReservedRoomCancelController controller = new ReservedRoomCancelController(mockView);
            mockCancelButton.getActionListeners()[0].actionPerformed(null);

            assertEquals(0, mockTable.getRowCount());
        }
    }

    @Test
    void testCancelReservation_noSelection() {
        try (
                MockedStatic<Session> sessionMock = mockStatic(Session.class);
                MockedStatic<JOptionPane> paneMock = mockStatic(JOptionPane.class)
        ) {
            sessionMock.when(Session::getOut).thenReturn(new PrintWriter(new StringWriter()));
            sessionMock.when(Session::getIn).thenReturn(new BufferedReader(new StringReader("END_\n")));
            sessionMock.when(Session::getLoggedInUserId).thenReturn("S123");
            sessionMock.when(Session::getLoggedInUserRole).thenReturn("학생");

            paneMock.when(() -> JOptionPane.showMessageDialog(any(), eq("취소할 예약을 선택하세요."))).then(invocation -> null);

            ReservedRoomCancelController controller = new ReservedRoomCancelController(mockView);
            mockTable.clearSelection();
            mockCancelButton.getActionListeners()[0].actionPerformed(null);
        }
    }

    @Test
    void testBackButton_disposesViewAndOpensExecutive() {
        try (
                MockedStatic<Session> sessionMock = mockStatic(Session.class);
                MockedConstruction<Executive> execMock = mockConstruction(Executive.class,
                        (mockExec, context) -> {
                            when(mockExec.getViewReservedButton()).thenReturn(new JButton());
                            when(mockExec.getJButton2()).thenReturn(new JButton());
                            when(mockExec.getJButton3()).thenReturn(new JButton());
                            when(mockExec.getJButton5()).thenReturn(new JButton());
                            when(mockExec.getJButton6()).thenReturn(new JButton());

                            doNothing().when(mockExec).setChangePasswordActionListener(any());
                        })
        ) {
            sessionMock.when(Session::getOut).thenReturn(new PrintWriter(new StringWriter()));
            sessionMock.when(Session::getIn).thenReturn(new BufferedReader(new StringReader("PENDING_COUNT:0\n")));
            sessionMock.when(Session::getLoggedInUserId).thenReturn("S123");
            sessionMock.when(Session::getLoggedInUserRole).thenReturn("학생");

            ReservedRoomCancelController controller = new ReservedRoomCancelController(mockView);
            mockBackButton.getActionListeners()[0].actionPerformed(null);

            verify(mockView).dispose();
            assertEquals(1, execMock.constructed().size());
        }
    }
}
