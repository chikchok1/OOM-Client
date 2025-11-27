package Controller;

import View.Executive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.swing.*;
import java.awt.event.ActionListener;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExecutiveControllerTest {

    @Mock private Executive mockExecutive;
    @Mock private JButton mockViewReservedButton;
    @Mock private JButton mockJButton2;
    @Mock private JButton mockJButton3;
    @Mock private JButton mockJButton5;
    @Mock private JButton mockJButton6;

    @BeforeEach
    void setUp() {
        // 버튼 getter mock 설정
        when(mockExecutive.getViewReservedButton()).thenReturn(mockViewReservedButton);
        when(mockExecutive.getJButton2()).thenReturn(mockJButton2);
        when(mockExecutive.getJButton3()).thenReturn(mockJButton3);
        when(mockExecutive.getJButton5()).thenReturn(mockJButton5);
        when(mockExecutive.getJButton6()).thenReturn(mockJButton6);

        // JOptionPane.showMessageDialog() mock 처리
        mockStaticJOptionPane();

        // 실제 파일 I/O 우회
        mockStaticCountRequest();

        // ExecutiveController 생성
        new ExecutiveController(mockExecutive);
    }

    @Test
    @DisplayName("모든 버튼에 ActionListener가 정상 등록되어야 한다")
    void shouldAttachActionListenersToAllButtons() {
        verify(mockViewReservedButton).addActionListener(any(ActionListener.class));
        verify(mockJButton2).addActionListener(any(ActionListener.class));
        verify(mockJButton3).addActionListener(any(ActionListener.class));
        verify(mockJButton5).addActionListener(any(ActionListener.class));
        verify(mockJButton6).addActionListener(any(ActionListener.class));
    }

    // ------- Helper methods for mocking -------
    
   private void mockStaticJOptionPane() {
    MockedStatic<JOptionPane> mocked = mockStatic(JOptionPane.class);
    mocked.when(() -> JOptionPane.showMessageDialog(
            any(), anyString(), anyString(), anyInt()
    )).thenAnswer(invocation -> null);  // 아무 동작도 하지 않음
}

    private void mockStaticCountRequest() {
        // countPendingRequests()는 테스트에 영향 없도록 우회
        // 방법 1: ExecutiveController 분리된 테스트 전용 서브클래스를 사용하거나,
        // 방법 2: hasShownAlert=true로 고정 설정 (더 간단한 방법)

        // static 변수 초기화
        try {
            var field = ExecutiveController.class.getDeclaredField("hasShownAlert");
            field.setAccessible(true);
            field.set(null, true); // 알림이 이미 출력된 것처럼 설정
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
