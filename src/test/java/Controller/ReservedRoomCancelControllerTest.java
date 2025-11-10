package Controller;

import Model.Session;
import View.ReservedRoomCancelView;
import View.Executive;
import org.junit.jupiter.api.AfterEach;
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
    private Session mockSession;

    @BeforeEach
    void setUp() {
        mockView = mock(ReservedRoomCancelView.class);
        mockTable = new JTable(new DefaultTableModel(new Object[]{"User ID", "Time", "Day", "Room", "Name"}, 0));
        mockCancelButton = new JButton();
        mockBackButton = new JButton();

        when(mockView.getTable()).thenReturn(mockTable);
        when(mockView.getCancelButton()).thenReturn(mockCancelButton);
        when(mockView.getBackButton()).thenReturn(mockBackButton);

        // ✅ 싱글턴 인스턴스 초기화
        Session.resetInstance();
        mockSession = Session.getInstance();
    }

    @AfterEach
    void tearDown() {
        Session.resetInstance();
    }

    @Test
    void testCancelReservation_success() throws Exception {
        ((DefaultTableModel) mockTable.getModel()).addRow(new Object[]{"S123", "1교시", "월요일", "101호", "홍길동"});
        mockTable.setRowSelectionInterval(0, 0);

        StringReader serverInput = new StringReader("CANCEL_SUCCESS\nEND_\n");
        BufferedReader in = new BufferedReader(serverInput);
        PrintWriter out = new PrintWriter(new StringWriter(), true);

        try (MockedStatic<JOptionPane> paneMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(out);
            mockSession.setIn(in);
            mockSession.setLoggedInUserId("S123");
            mockSession.setLoggedInUserRole("학생");

            paneMock.when(() -> JOptionPane.showMessageDialog(any(), any())).then(invocation -> null);

            ReservedRoomCancelController controller = new ReservedRoomCancelController(mockView);
            mockCancelButton.getActionListeners()[0].actionPerformed(null);

            assertEquals(0, mockTable.getRowCount());
        }
    }

    @Test
    void testCancelReservation_noSelection() {
        try (MockedStatic<JOptionPane> paneMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(new PrintWriter(new StringWriter()));
            mockSession.setIn(new BufferedReader(new StringReader("END_\n")));
            mockSession.setLoggedInUserId("S123");
            mockSession.setLoggedInUserRole("학생");

            paneMock.when(() -> JOptionPane.showMessageDialog(any(), eq("취소할 예약을 선택하세요."))).then(invocation -> null);

            ReservedRoomCancelController controller = new ReservedRoomCancelController(mockView);
            mockTable.clearSelection();
            mockCancelButton.getActionListeners()[0].actionPerformed(null);
        }
    }

    @Test
    void testBackButton_disposesViewAndOpensExecutive() {
        try (
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
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setOut(new PrintWriter(new StringWriter()));
            mockSession.setIn(new BufferedReader(new StringReader("PENDING_COUNT:0\n")));
            mockSession.setLoggedInUserId("S123");
            mockSession.setLoggedInUserRole("학생");

            ReservedRoomCancelController controller = new ReservedRoomCancelController(mockView);
            mockBackButton.getActionListeners()[0].actionPerformed(null);

            verify(mockView).dispose();
            assertEquals(1, execMock.constructed().size());
        }
    }
}
