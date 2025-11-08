package Controller;

import View.ClientAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientAdminControllerTest {

    @Mock private ClientAdmin mockView;
    @Mock private JButton mockButton1;
    @Mock private JButton mockButton2;
    @Mock private JButton mockButton3;
    @Mock private JTable mockTable;
    @Mock private DefaultTableModel mockTableModel;

    private ClientAdminController controller;

    @BeforeEach
    void setUp() {
        // 뷰의 컴포넌트들이 리턴되도록 설정
        when(mockView.getJButton1()).thenReturn(mockButton1);
        when(mockView.getJButton2()).thenReturn(mockButton2);
        when(mockView.getJButton3()).thenReturn(mockButton3);
        when(mockView.getTable()).thenReturn(mockTable);
        when(mockTable.getModel()).thenReturn(mockTableModel);

        // loadUsersFromServer를 비활성화한 테스트용 컨트롤러 생성
        controller = new ClientAdminController(mockView) {
            @Override
            protected void loadUsersFromServer() {
                // 서버 통신을 막는다
            }
        };
    }

    @Test
    void testButtonListenersAreAttached() {
        // 버튼 리스너가 각각 연결되었는지 검증
        verify(mockButton1, atLeastOnce()).addActionListener(any()); // 수정 버튼
        verify(mockButton2, atLeastOnce()).addActionListener(any()); // 삭제 버튼
        verify(mockButton3, atLeastOnce()).addActionListener(any()); // 이전 버튼
    }
}
