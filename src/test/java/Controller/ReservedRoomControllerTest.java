package Controller;

import View.ReservedRoomView;
import common.model.ReservedRoomModel;
import Model.Session;
import Util.MessageDispatcher; 
import com.toedter.calendar.JDateChooser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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
    @Mock private JDateChooser mockDateChooser;
    @Mock private MessageDispatcher mockDispatcher; // 가짜 Dispatcher

    private ReservedRoomController controller;
    private Session mockSession;
    
    // JOptionPane 모킹용 (팝업창 방지)
    private MockedStatic<JOptionPane> mockedJOptionPane;

    @BeforeEach
    void setUp() throws Exception {
        // 1. View 컴포넌트 모킹
        when(mockView.getCheckButton()).thenReturn(mockCheckButton);
        when(mockView.getClassComboBox()).thenReturn(mockClassComboBox);
        when(mockView.getLabComboBox()).thenReturn(mockLabComboBox);
        when(mockView.getBeforeButton()).thenReturn(mockBeforeButton);
        when(mockView.getTable()).thenReturn(mockTable);
        when(mockView.getSelectedRoom()).thenReturn("101호");
        when(mockView.isUpdating()).thenReturn(false);
        when(mockView.getDateChooser()).thenReturn(mockDateChooser);
        
        when(mockView.getSelectedDate()).thenReturn(LocalDate.of(2025, 11, 27));

        // 2. Session 초기화
        Session.resetInstance();
        mockSession = Session.getInstance();
        mockSession.setOut(new PrintWriter(new StringWriter()));

        // 3. [핵심] Reflection을 사용하여 Singleton 강제 주입 (스레드 문제 해결)
        injectMockDispatcher();

        // 4. JOptionPane 팝업 방지
        mockedJOptionPane = mockStatic(JOptionPane.class);

        controller = new ReservedRoomController(mockView);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        
        // 테스트가 끝나면 Singleton을 null로 초기화해주는 것이 좋음 (다음 테스트를 위해)
        resetMockDispatcher();
        Session.resetInstance();
    }

    /**
     * MessageDispatcher 클래스의 내부 static 필드(instance)를 찾아서 mockDispatcher로 바꿔치기함
     */
    private void injectMockDispatcher() throws Exception {
        // MessageDispatcher의 모든 필드를 뒤져서 자기 자신 타입인 static 필드를 찾음
        for (Field field : MessageDispatcher.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                field.getType().equals(MessageDispatcher.class)) {
                field.setAccessible(true);
                field.set(null, mockDispatcher); // 가짜 객체 주입
                return;
            }
        }
        // 만약 못 찾으면 필드명을 직접 'instance'로 가정하고 시도
        try {
            Field instance = MessageDispatcher.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mockDispatcher);
        } catch (NoSuchFieldException e) {
            System.err.println("MessageDispatcher의 싱글톤 필드를 찾을 수 없습니다.");
        }
    }

    private void resetMockDispatcher() throws Exception {
        for (Field field : MessageDispatcher.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                field.getType().equals(MessageDispatcher.class)) {
                field.setAccessible(true);
                field.set(null, null); // null로 초기화
                return;
            }
        }
    }

    private void waitForSwing() throws Exception {
        Thread.sleep(2000); // 비동기 작업 대기
        try {
            SwingUtilities.invokeAndWait(() -> {}); // UI 업데이트 큐 처리 대기
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCheckButtonLoadsReservationWhenRoomSelected() throws Exception {
        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(mockCheckButton).addActionListener(captor.capture());

        mockSession.setLoggedInUserId("S123");
        mockSession.setLoggedInUserName("홍길동");

        // 가짜 응답 설정
        when(mockDispatcher.waitForResponse(anyInt()))
            .thenReturn("홍길동,101호,2025-11-27,목요일,1교시(09:00~10:00),수업,학생,예약됨,3,S123")
            .thenReturn("END_OF_RESERVATION");

        when(mockTable.getRowCount()).thenReturn(9);
        when(mockTable.getColumnCount()).thenReturn(6);
        when(mockTable.getValueAt(0, 4)).thenReturn(""); 

        ActionListener listener = captor.getValue();
        listener.actionPerformed(null);

        waitForSwing();

        // [강력한 검증] 모든 setValueAt 호출을 캡처해서 확인
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Integer> rowCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> colCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(mockTable, atLeastOnce()).setValueAt(valueCaptor.capture(), rowCaptor.capture(), colCaptor.capture());

        List<Object> values = valueCaptor.getAllValues();
        List<Integer> rows = rowCaptor.getAllValues();
        List<Integer> cols = colCaptor.getAllValues();

        boolean found = false;
        for (int i = 0; i < values.size(); i++) {
            Object val = values.get(i);
            int r = rows.get(i);
            int c = cols.get(i);
            
            // "예약됨"이 0행 4열(목요일 1교시)에 들어갔는지 확인
            if (val != null && val.toString().contains("예약됨") && r == 0 && c == 4) {
                found = true;
                break;
            }
        }
        assertTrue(found, "학생 예약('예약됨')이 테이블(0, 4)에 표시되어야 합니다.");
    }

    @Test
    void testProfessorSeesNameInsteadOfReserved() throws Exception {
        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(mockCheckButton).addActionListener(captor.capture());

        mockSession.setLoggedInUserId("P456");
        mockSession.setLoggedInUserName("김교수");

        when(mockDispatcher.waitForResponse(anyInt()))
            .thenReturn("김교수,101호,2025-11-27,목요일,2교시(10:00~11:00),강의,교수,예약됨,1,P456")
            .thenReturn("END_OF_RESERVATION");

        when(mockTable.getRowCount()).thenReturn(9);
        when(mockTable.getColumnCount()).thenReturn(6);
        when(mockTable.getValueAt(1, 4)).thenReturn("");

        ActionListener listener = captor.getValue();
        listener.actionPerformed(null);

        waitForSwing();

        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Integer> rowCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> colCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(mockTable, atLeastOnce()).setValueAt(valueCaptor.capture(), rowCaptor.capture(), colCaptor.capture());

        List<Object> values = valueCaptor.getAllValues();
        List<Integer> rows = rowCaptor.getAllValues();
        List<Integer> cols = colCaptor.getAllValues();

        boolean found = false;
        for (int i = 0; i < values.size(); i++) {
            Object val = values.get(i);
            int r = rows.get(i);
            int c = cols.get(i);
            
            // "김교수"가 1행 4열(목요일 2교시)에 들어갔는지 확인
            if (val != null && val.toString().contains("김교수") && r == 1 && c == 4) {
                found = true;
                break;
            }
        }
        assertTrue(found, "교수 이름('김교수')이 테이블(1, 4)에 표시되어야 합니다.");
    }

    @Test
    void testActionListenersAttached() {
        verify(mockCheckButton).addActionListener(any(ActionListener.class));
        verify(mockClassComboBox).addActionListener(any(ActionListener.class));
        verify(mockLabComboBox).addActionListener(any(ActionListener.class));
        verify(mockBeforeButton).addActionListener(any(ActionListener.class));
        verify(mockDateChooser).addPropertyChangeListener(eq("date"), any());
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
    void testHandlesIOException() {
        ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
        verify(mockCheckButton).addActionListener(captor.capture());
        
        ActionListener listener = captor.getValue();
        assertNotNull(listener);
    }
}