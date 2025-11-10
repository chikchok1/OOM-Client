package Controller;

import View.ReservedRoomView;
import common.model.ReservedRoomModel;
import Model.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservedRoomControllerTest {

    @Mock private ReservedRoomView mockView;
    @Mock private ReservedRoomModel mockModel;
    @Mock private JButton mockCheckButton;
    @Mock private JComboBox<String> mockClassComboBox;
    @Mock private JComboBox<String> mockLabComboBox;
    @Mock private JButton mockBeforeButton;
    @Mock private JTable mockTable;

    private ReservedRoomController controller;
    private Session mockSession;

    @BeforeEach
    void setUp() {
        when(mockView.getCheckButton()).thenReturn(mockCheckButton);
        when(mockView.getClassComboBox()).thenReturn(mockClassComboBox);
        when(mockView.getLabComboBox()).thenReturn(mockLabComboBox);
        when(mockView.getBeforeButton()).thenReturn(mockBeforeButton);
        when(mockView.getTable()).thenReturn(mockTable);
        when(mockView.getSelectedRoom()).thenReturn("101호");
        when(mockView.isUpdating()).thenReturn(false);

        // ✅ 싱글턴 인스턴스 초기화
        Session.resetInstance();
        mockSession = Session.getInstance();

        controller = new ReservedRoomController(mockView);
    }

    @AfterEach
    void tearDown() {
        Session.resetInstance();
    }

    @Test
    void testActionListenersAttached() {
        verify(mockCheckButton).addActionListener(any(ActionListener.class));
        verify(mockClassComboBox).addActionListener(any(ActionListener.class));
        verify(mockLabComboBox).addActionListener(any(ActionListener.class));
        verify(mockBeforeButton).addActionListener(any(ActionListener.class));
    }

    @Test
    void testCheckButtonLoadsReservationWhenRoomSelected() {
        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(mockCheckButton).addActionListener(captor.capture());

        // ✅ 싱글턴 인스턴스에 직접 설정
        mockSession.setLoggedInUserId("S123");
        mockSession.setLoggedInUserName("홍길동");

        BufferedReader mockIn = new BufferedReader(new StringReader(
            "홍길동,101호,월요일,1교시(09:00~10:00),학부생,학생,예약됨\nEND_OF_RESERVATION\n"
        ));
        PrintWriter mockOut = new PrintWriter(new StringWriter());

        mockSession.setIn(mockIn);
        mockSession.setOut(mockOut);

        when(mockTable.getRowCount()).thenReturn(9);
        when(mockTable.getColumnCount()).thenReturn(6);
        when(mockTable.getValueAt(0, 1)).thenReturn("");

        ActionListener listener = captor.getValue();
        listener.actionPerformed(null);

        verify(mockTable).setValueAt("예약됨", 0, 1);
    }

    @Test
    void testProfessorSeesNameInsteadOfReserved() {
        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(mockCheckButton).addActionListener(captor.capture());

        // ✅ 싱글턴 인스턴스에 직접 설정
        mockSession.setLoggedInUserId("P456");
        mockSession.setLoggedInUserName("김교수");

        BufferedReader mockIn = new BufferedReader(new StringReader(
            "김교수,101호,화요일,2교시(10:00~11:00),교수,교수,예약됨\nEND_OF_RESERVATION\n"
        ));
        PrintWriter mockOut = new PrintWriter(new StringWriter());

        mockSession.setIn(mockIn);
        mockSession.setOut(mockOut);

        when(mockTable.getRowCount()).thenReturn(9);
        when(mockTable.getColumnCount()).thenReturn(6);
        when(mockTable.getValueAt(1, 2)).thenReturn("");

        ActionListener listener = captor.getValue();
        listener.actionPerformed(null);

        verify(mockTable).setValueAt("김교수", 1, 2);
    }

    @Test
    void testHandlesInvalidRoomName() {
        when(mockView.getSelectedRoom()).thenReturn("선택");

        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(mockCheckButton).addActionListener(captor.capture());

        ActionListener listener = captor.getValue();
        listener.actionPerformed(null);

        verify(mockTable, never()).setValueAt(any(), anyInt(), anyInt());
    }

    @Test
    void testHandlesIOException() throws IOException {
        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(mockCheckButton).addActionListener(captor.capture());

        BufferedReader mockIn = mock(BufferedReader.class);
        when(mockIn.readLine()).thenThrow(new IOException("서버 오류"));

        PrintWriter mockOut = new PrintWriter(new StringWriter());

        try (MockedStatic<JOptionPane> paneMock = mockStatic(JOptionPane.class)) {
            // ✅ 싱글턴 인스턴스에 직접 설정
            mockSession.setLoggedInUserId("S789");
            mockSession.setLoggedInUserName("테스터");
            mockSession.setIn(mockIn);
            mockSession.setOut(mockOut);
            
            paneMock.when(() -> JOptionPane.showMessageDialog(any(), any())).thenAnswer(inv -> null);

            ActionListener listener = captor.getValue();
            listener.actionPerformed(null);
        }
    }
}
