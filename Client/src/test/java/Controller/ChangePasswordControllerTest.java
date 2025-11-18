package Controller;

import Model.Session;
import View.ChangePasswordView;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

class ChangePasswordControllerTest {

    @Mock
    private ChangePasswordView mockView;
    @Mock
    private NetworkFacade mockNetworkFacade;

    private ChangePasswordController controller;
    private MockedStatic<Session> sessionMock;
    private MockedStatic<JOptionPane> jOptionPaneMock;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        controller = new ChangePasswordController(mockView);

        // 리플렉션으로 Facade Mock 주입
        Field facadeField = ChangePasswordController.class.getDeclaredField("networkFacade");
        facadeField.setAccessible(true);
        facadeField.set(controller, mockNetworkFacade);

        // Session 모킹 (현재 로그인한 사용자 ID 필요)
        sessionMock = mockStatic(Session.class);
        sessionMock.when(Session::getLoggedInUserId).thenReturn("S1234");

        // JOptionPane 팝업창 억제
        jOptionPaneMock = mockStatic(JOptionPane.class);
        
        // Headless 모드 설정 (그래픽 환경이 없는 테스트 서버 대비)
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void tearDown() {
        sessionMock.close();
        jOptionPaneMock.close();
    }

    @Test
    void testChangePasswordSuccess() throws Exception {
        // Given
        when(mockView.getPresentPassword()).thenReturn("oldPass");
        when(mockView.getChangePassword()).thenReturn("newPass");
        
        // Facade가 성공 응답을 리턴한다고 가정
        when(mockNetworkFacade.changePassword("S1234", "oldPass", "newPass"))
                .thenReturn("PASSWORD_CHANGED");

        // ViewFactory 모킹 (화면 전환 방지)
        try (MockedStatic<ViewFactory> viewFactoryMock = mockStatic(ViewFactory.class);
             MockedStatic<GraphicsEnvironment> graphicsMock = mockStatic(GraphicsEnvironment.class)) {
            
            graphicsMock.when(GraphicsEnvironment::isHeadless).thenReturn(false); 
            JFrame mockFrame = mock(JFrame.class);
            viewFactoryMock.when(() -> ViewFactory.createMainView(anyChar())).thenReturn(mockFrame);

            // When
            controller.changePassword();

            // Then
            jOptionPaneMock.verify(() -> 
                JOptionPane.showMessageDialog(mockView, "비밀번호가 성공적으로 변경되었습니다.")
            );
            verify(mockView).dispose(); // 현재 창이 닫혔는지 확인
        }
    }

    @Test
    void testUserNotFound() throws Exception {
        // Given
        when(mockView.getPresentPassword()).thenReturn("oldPass");
        when(mockView.getChangePassword()).thenReturn("newPass");
        
        // Facade가 실패 응답 리턴
        when(mockNetworkFacade.changePassword(anyString(), anyString(), anyString()))
                .thenReturn("USER_NOT_FOUND");

        // When
        controller.changePassword();

        // Then
        jOptionPaneMock.verify(() -> 
            JOptionPane.showMessageDialog(mockView, "사용자 정보를 찾을 수 없습니다.")
        );
        verify(mockView, never()).dispose(); // 창이 닫히면 안 됨
    }
}