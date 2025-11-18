package Controller;

import View.MembershipView;
import View.LoginForm;
import common.model.MembershipModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.event.ActionListener;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MembershipControllerTest {

    @Mock private MembershipView mockView;
    @Mock private MembershipModel mockModel;
    @Mock private LoginForm mockLoginForm;
    @Mock private NetworkFacade mockNetworkFacade;

    private MembershipController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new MembershipController(mockView, mockModel, mockLoginForm);
        
        // Facade Mock 주입
        Field field = MembershipController.class.getDeclaredField("networkFacade");
        field.setAccessible(true);
        field.set(controller, mockNetworkFacade);
    }

    @Test
    void testActionListenerAttached() {
        verify(mockView).setCustomActionListener(any(ActionListener.class));
    }

    @Test
    void testRegisterSuccess() throws Exception {
        // Given: 유효한 입력값
        when(mockView.getName()).thenReturn("홍길동");
        when(mockView.getStudentId()).thenReturn("S123");
        when(mockView.getPassword()).thenReturn("1234");
        
        // Facade 응답 설정
        when(mockNetworkFacade.register("홍길동", "S123", "1234")).thenReturn("SUCCESS");
    }
}