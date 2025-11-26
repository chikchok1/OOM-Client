package Controller;

import Model.Session;
import View.LoginForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.startsWith;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock(lenient = true)
    private LoginForm mockView;

    @InjectMocks
    private LoginController loginController;

    @BeforeEach
    void setup() {
        when(mockView.getUserId()).thenReturn("S20230001");
        when(mockView.getPassword()).thenReturn("1234");

        // ✅ 싱글턴 인스턴스 초기화 및 서버 연결 실패 상태 설정
        Session.resetInstance();
        Session.getInstance().setSocket(null);
        Session.getInstance().setOut(null);
        Session.getInstance().setIn(null);
    }

    /**
     * [테스트 목적] 아이디와 비밀번호 입력 칸이 비어 있을 때, 오류 메시지를 출력해야 한다.
     */
    @Test
    void shouldShowErrorMessage_whenInputFieldsAreEmpty() {
        when(mockView.getUserId()).thenReturn("");
        when(mockView.getPassword()).thenReturn("");

        loginController.handleLogin();

        verify(mockView).showMessage("아이디와 비밀번호를 모두 입력하세요.");
    }

    /**
     * [테스트 목적] 서버가 꺼져 있을 경우, "서버와 연결할 수 없습니다" 메시지를 출력해야 한다.
     */
    @Test
    void shouldShowServerErrorMessage_whenServerIsUnavailable() {
        try (MockedStatic<ClientFacade> facadeMock = mockStatic(ClientFacade.class)) {
            facadeMock.when(() -> ClientFacade.login(mockView))
                      .thenAnswer(invocation -> {
                          mockView.showMessage("서버와 연결할 수 없습니다: 테스트");
                          return false;
                      });

            loginController.handleLogin();

            verify(mockView).showMessage(startsWith("서버와 연결할 수 없습니다"));
        }
    }

    /**
     * [테스트 목적] 서버 연결 실패 시, "로그인 성공!" 메시지를 출력하지 않아야 한다.
     */
    @Test
    void shouldNotShowSuccessMessage_whenLoginFailsDueToServer() {
        try (MockedStatic<ClientFacade> facadeMock = mockStatic(ClientFacade.class)) {
            facadeMock.when(() -> ClientFacade.login(mockView))
                      .thenAnswer(invocation -> {
                          mockView.showMessage("서버와 연결할 수 없습니다: 테스트");
                          return false;
                      });

            loginController.handleLogin();

            verify(mockView, never()).showMessage("로그인 성공!");
            verify(mockView).showMessage(startsWith("서버와 연결할 수 없습니다"));
        }
    }

    /**
     * [테스트 목적] 서버 연결 실패 시, 세션에 사용자 정보가 저장되지 않아야 한다.
     */
    @Test
    void shouldNotSetSession_whenServerConnectionFails() {
        try (MockedStatic<ClientFacade> facadeMock = mockStatic(ClientFacade.class)) {
            facadeMock.when(() -> ClientFacade.login(mockView))
                      .thenAnswer(invocation -> {
                          mockView.showMessage("서버와 연결할 수 없습니다: 테스트");
                          return false;
                      });

            loginController.handleLogin();

            assertNull(Session.getInstance().getLoggedInUserId(), "세션에 사용자 ID가 없어야 함");
            assertNull(Session.getInstance().getLoggedInUserName(), "세션에 사용자 이름이 없어야 함");
        }
    }

    /**
     * [테스트 목적] 서버 연결 실패 시, view.dispose()가 호출되지 않아야 한다.
     */
    @Test
    void shouldNotDisposeView_whenLoginFailsDueToServer() {
        try (MockedStatic<ClientFacade> facadeMock = mockStatic(ClientFacade.class)) {
            facadeMock.when(() -> ClientFacade.login(mockView))
                      .thenAnswer(invocation -> {
                          mockView.showMessage("서버와 연결할 수 없습니다: 테스트");
                          return false;
                      });

            loginController.handleLogin();

            verify(mockView, never()).dispose();
        }
    }
}
