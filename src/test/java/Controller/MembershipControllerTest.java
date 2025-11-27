/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controller;

/**
 *
 * @author miju
 */
import View.MembershipView;
import View.LoginForm;
import common.model.MembershipModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.event.ActionListener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MembershipControllerTest {

    @Mock
    private MembershipView mockView;
    @Mock
    private MembershipModel mockModel;
    @Mock
    private LoginForm mockLoginForm;

    private MembershipController controller;

    @BeforeEach
    void setUp() {
        controller = new MembershipController(mockView, mockModel, mockLoginForm);
    }

    @Test
    void testActionListenerAttached() {
        verify(mockView).setCustomActionListener(any(ActionListener.class));
    }

    // 유효성 검사 메서드만 따로 래핑해서 테스트
    @Test
    void testValidId() {
        assertTrue(controller.isValidId("S123"));
        assertTrue(controller.isValidId("P999"));
        assertFalse(controller.isValidId("1234"));
        assertFalse(controller.isValidId("X123"));
    }

    @Test
    void testValidPassword() {
        assertTrue(controller.isValidPassword("1234"));
        assertTrue(controller.isValidPassword("abcd1234"));
        assertFalse(controller.isValidPassword("abc"));
        assertFalse(controller.isValidPassword("123456789"));
    }

    // 내부 private 메서드 테스트용 래퍼 클래스
    static class MembershipControllerTestHelper extends MembershipController {

        public MembershipControllerTestHelper() {
            super(mock(MembershipView.class), mock(MembershipModel.class), mock(LoginForm.class));
        }

        @Override
        public boolean isValidId(String id) {
            return new MembershipControllerTestHelper().superIsValidId(id);
        }

        @Override
        public boolean isValidPassword(String pw) {
            return new MembershipControllerTestHelper().superIsValidPassword(pw);
        }

        private boolean superIsValidId(String id) {
            return super.isValidId(id);
        }

        private boolean superIsValidPassword(String pw) {
            return super.isValidPassword(pw);
        }
    }
}
