package Controller;

import Model.Session;
import View.LoginForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock(lenient = true)
    private LoginForm mockView;

    @Mock
    private NetworkFacade mockNetworkFacade; // Facade 모킹

    @InjectMocks
    private LoginController loginController;

    @BeforeEach
    void setup() throws Exception {
        // 1. View 기본 동작 설정
        when(mockView.getUserId()).thenReturn("S20230001");
        when(mockView.getPassword()).thenReturn("1234");

        // 2. 리플렉션을 사용하여 Controller 내부의 private networkFacade 필드를 Mock 객체로 교체
        // (생성자에서 new NetworkFacade()를 하기 때문에 테스트에서 이를 가로채기 위함)
        Field facadeField = LoginController.class.getDeclaredField("networkFacade");
        facadeField.setAccessible(true);
        facadeField.set(loginController, mockNetworkFacade);
    }

    @Test
    void shouldShowErrorMessage_whenInputFieldsAreEmpty() {
        when(mockView.getUserId()).thenReturn("");
        when(mockView.getPassword()).thenReturn("");

        loginController.handleLogin();

        verify(mockView).showMessage("아이디와 비밀번호를 모두 입력하세요.");
    }

    @Test
    void shouldShowServerErrorMessage_whenServerIsUnavailable() throws Exception {
        // Given: Facade가 null을 반환 (연결 실패 시뮬레이션)
        when(mockNetworkFacade.attemptLogin(anyString(), anyString())).thenReturn(null);

        // When
        loginController.handleLogin();

        // Then
        verify(mockView).showMessage(startsWith("서버로부터 응답이 없습니다"));
    }

    @Test
    void shouldLoginSuccessfully_andOpenMainView() throws Exception {
        // Given: 서버가 SUCCESS 응답을 줌
        when(mockNetworkFacade.attemptLogin(anyString(), anyString())).thenReturn("SUCCESS,홍길동");
        
        // ViewFactory와 Session의 정적 메서드 모킹 (GUI 뜨는 것 방지 및 세션 검증)
        try (MockedStatic<ViewFactory> viewFactoryMock = mockStatic(ViewFactory.class);
             MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
             
            // Session.getInstance() 동작 모킹 (호환성 계층)
            Session mockSessionInstance = mock(Session.class);
            sessionMock.when(Session::getInstance).thenReturn(mockSessionInstance);

            // ViewFactory가 JFrame을 반환하도록 설정
            JFrame mockMainFrame = mock(JFrame.class);
            viewFactoryMock.when(() -> ViewFactory.createMainView(anyChar())).thenReturn(mockMainFrame);

            // When
            loginController.handleLogin();

            // Then
            verify(mockView).showMessage("로그인 성공!");
            verify(mockView).dispose();
            verify(mockMainFrame).setVisible(true); // 메인 화면이 켜졌는지 확인
        }
    }
}