package Controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;

/**
 * Template Method Pattern 핵심 검증 테스트
 * AbstractReservationController의 템플릿 메서드 패턴 구현을 검증
 */
public class AbstractReservationControllerTest {

    // ============================================================
    // 1. 구조 검증 
    // ============================================================

    @Test
    @DisplayName("템플릿 메서드 processReservation()은 final이어야 한다")
    void testTemplateMethodIsFinal() throws NoSuchMethodException {
        Method method = AbstractReservationController.class
            .getDeclaredMethod("processReservation", ActionEvent.class);
        
        assertTrue(Modifier.isFinal(method.getModifiers()),
            "템플릿 메서드는 final이어야 알고리즘을 변경할 수 없음");
        assertTrue(Modifier.isProtected(method.getModifiers()),
            "템플릿 메서드는 protected여야 서브클래스에서 사용 가능");
    }

    @Test
    @DisplayName("Hook 메서드들은 protected이고 오버라이드 가능해야 한다")
    void testHookMethodsAreOverridable() throws Exception {
        // Hook 메서드 목록
        String[] hookMethods = {
            "validateReservationTime",
            "validateCapacity",
            "performAdditionalValidation",
            "showReservationSuccessMessage"
        };
        
        for (String methodName : hookMethods) {
            Method method = AbstractReservationController.class
                .getDeclaredMethod(methodName, 
                    AbstractReservationController.ReservationData.class);
            
            assertTrue(Modifier.isProtected(method.getModifiers()),
                methodName + "은 protected Hook 메서드여야 함");
            assertFalse(Modifier.isFinal(method.getModifiers()),
                methodName + "은 final이 아니어야 오버라이드 가능");
        }
    }

    @Test
    @DisplayName("공통 메서드들은 private으로 변경 불가능해야 한다")
    void testCommonMethodsArePrivate() throws Exception {
        // 공통 메서드와 파라미터 타입 매핑
        Map<String, Class<?>[]> commonMethods = new LinkedHashMap<>();
        commonMethods.put("collectReservationData", new Class<?>[]{});
        commonMethods.put("validateBasicInput", 
            new Class<?>[]{AbstractReservationController.ReservationData.class});
        commonMethods.put("validateDate", 
            new Class<?>[]{AbstractReservationController.ReservationData.class});
        commonMethods.put("submitReservation", 
            new Class<?>[]{AbstractReservationController.ReservationData.class});
        
        for (Map.Entry<String, Class<?>[]> entry : commonMethods.entrySet()) {
            String methodName = entry.getKey();
            Method method = AbstractReservationController.class
                .getDeclaredMethod(methodName, entry.getValue());
            
            assertTrue(Modifier.isPrivate(method.getModifiers()),
                methodName + "은 private이어야 서브클래스가 변경 불가");
        }
    }

    @Test
    @DisplayName("Primitive Operations는 abstract여야 한다")
    void testPrimitiveOperationsAreAbstract() throws Exception {
        // 필수 구현 메서드들
        String[] primitiveOps = {
            "getRoomTypeName",
            "getSelectedRoom",
            "getSelectedDateString",
            "getSelectedDate",
            "getSelectedDay",
            "getSelectedTime",
            "getSelectedEndTime",
            "getPurpose",
            "getStudentCount"
        };
        
        for (String methodName : primitiveOps) {
            Method method = AbstractReservationController.class
                .getDeclaredMethod(methodName);
            
            assertTrue(Modifier.isAbstract(method.getModifiers()),
                methodName + "은 abstract여야 서브클래스가 반드시 구현");
        }
    }

    @Test
    @DisplayName("AbstractReservationController는 abstract 클래스여야 한다")
    void testAbstractClassIsAbstract() {
        assertTrue(Modifier.isAbstract(AbstractReservationController.class.getModifiers()),
            "AbstractReservationController는 abstract 클래스여야 함");
    }

    @Test
    @DisplayName("ReservationData 내부 클래스는 static protected여야 한다")
    void testReservationDataIsStaticProtected() {
        Class<?> dataClass = AbstractReservationController.ReservationData.class;
        
        assertTrue(Modifier.isStatic(dataClass.getModifiers()),
            "ReservationData는 static 내부 클래스여야 함");
        assertTrue(Modifier.isProtected(dataClass.getModifiers()),
            "ReservationData는 protected여야 서브클래스에서 사용 가능");
    }

    // ============================================================
    // 2. 동작 검증
    // ============================================================

    @Test
    @DisplayName("템플릿 메서드는 정해진 순서대로 실행되어야 한다")
    void testTemplateMethodExecutionOrder() throws Exception {
        CallTrackingController controller = new CallTrackingController();
        ActionEvent mockEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "test");
        
        // Session 모킹 (role is null 오류 해결)
        Model.Session.resetInstance();
        Model.Session session = Model.Session.getInstance();
        session.setLoggedInUserName("테스트사용자");
        session.setLoggedInUserRole("학생");
        
        try {
            Method method = AbstractReservationController.class
                .getDeclaredMethod("processReservation", ActionEvent.class);
            method.setAccessible(true);
            method.invoke(controller, mockEvent);
            
            List<String> calls = controller.getCallOrder();
            
            // 주요 Primitive Operations가 호출되었는지 확인
            assertTrue(calls.contains("getSelectedDateString"), "날짜 문자열 조회됨");
            assertTrue(calls.contains("getSelectedDate"), "날짜 객체 조회됨");
            assertTrue(calls.contains("getSelectedRoom"), "방 이름 조회됨");
            assertTrue(calls.contains("getSelectedDay"), "요일 조회됨");
            assertTrue(calls.contains("getSelectedTime"), "시작 시간 조회됨");
            assertTrue(calls.contains("getSelectedEndTime"), "종료 시간 조회됨");
            assertTrue(calls.contains("getPurpose"), "사용 목적 조회됨");
            assertTrue(calls.contains("getStudentCount"), "인원수 조회됨");
        } finally {
            Model.Session.resetInstance();
        }
    }

    @Test
    @DisplayName("Hook 메서드 performAdditionalValidation은 기본값 true를 반환해야 한다")
    void testPerformAdditionalValidationDefaultBehavior() throws Exception {
        TestController controller = new TestController();
        
        AbstractReservationController.ReservationData testData = 
            new AbstractReservationController.ReservationData(
                "2025-12-01",
                LocalDate.of(2025, 12, 1),
                "테스트사용자",
                "908호",
                "월요일",
                "1교시(09:00~10:00)",
                "10:00",
                "테스트목적",
                30,
                "학생"
            );
        
        Method method = AbstractReservationController.class
            .getDeclaredMethod("performAdditionalValidation",
                AbstractReservationController.ReservationData.class);
        method.setAccessible(true);
        
        boolean result = (boolean) method.invoke(controller, testData);
        
        assertTrue(result, 
            "performAdditionalValidation의 기본 구현은 true를 반환해야 함");
    }

    @Test
    @DisplayName("ReservClassController는 showReservationSuccessMessage를 오버라이드해야 한다")
    void testReservClassControllerOverridesHook() throws Exception {
        Method method = ReservClassController.class
            .getDeclaredMethod("showReservationSuccessMessage",
                AbstractReservationController.ReservationData.class);
        
        assertEquals(ReservClassController.class, method.getDeclaringClass(),
            "ReservClassController는 Hook 메서드를 오버라이드함");
        assertTrue(Modifier.isProtected(method.getModifiers()),
            "오버라이드된 메서드도 protected 유지");
    }

    @Test
    @DisplayName("구체 클래스들은 Hook 메서드를 상속받아 사용할 수 있다")
    void testConcreteClassesCanUseHookMethods() throws Exception {
        // ReservLabController가 Hook 메서드에 접근할 수 있는지만 확인
        Method method = AbstractReservationController.class
            .getDeclaredMethod("showReservationSuccessMessage",
                AbstractReservationController.ReservationData.class);
        
        // 메서드가 protected이므로 상속받은 클래스에서 사용 가능
        assertTrue(Modifier.isProtected(method.getModifiers()),
            "Hook 메서드는 protected이므로 모든 서브클래스에서 사용 가능");
        
        // ReservLabController가 AbstractReservationController를 상속하는지 확인
        assertTrue(AbstractReservationController.class.isAssignableFrom(ReservLabController.class),
            "ReservLabController는 AbstractReservationController를 상속함");
    }

    // ============================================================
    // 3. 예외 처리 검증 
    // ============================================================

    @Test
    @DisplayName("null 입력값을 적절히 처리해야 한다")
    void testHandlesNullInput() {
        TestController controller = new TestController() {
            @Override
            protected String getPurpose() {
                return null;
            }
            
            @Override
            protected String getSelectedDateString() {
                return null;
            }
        };
        
        ActionEvent mockEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "test");
        
        // Session 모킹
        Model.Session.resetInstance();
        Model.Session session = Model.Session.getInstance();
        session.setLoggedInUserName("테스트사용자");
        session.setLoggedInUserRole("학생");
        
        try {
            assertDoesNotThrow(() -> {
                Method method = AbstractReservationController.class
                    .getDeclaredMethod("processReservation", ActionEvent.class);
                method.setAccessible(true);
                method.invoke(controller, mockEvent);
            }, "null 입력값에 대해 예외가 발생하지 않아야 함");
        } finally {
            Model.Session.resetInstance();
        }
    }

    @Test
    @DisplayName("빈 문자열 입력도 적절히 처리해야 한다")
    void testHandlesEmptyInput() {
        TestController controller = new TestController() {
            @Override
            protected String getPurpose() {
                return ""; // 빈 문자열
            }
        };
        
        ActionEvent mockEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "test");
        
        // Session 모킹
        Model.Session.resetInstance();
        Model.Session session = Model.Session.getInstance();
        session.setLoggedInUserName("테스트사용자");
        session.setLoggedInUserRole("학생");
        
        try {
            assertDoesNotThrow(() -> {
                Method method = AbstractReservationController.class
                    .getDeclaredMethod("processReservation", ActionEvent.class);
                method.setAccessible(true);
                method.invoke(controller, mockEvent);
            }, "빈 문자열에 대해 적절한 메시지를 보여줘야 함");
        } finally {
            Model.Session.resetInstance();
        }
    }

    @Test
    @DisplayName("validateBasicInput은 목적이 비어있으면 false를 반환해야 한다")
    void testValidateBasicInputWithEmptyPurpose() throws Exception {
        TestController controller = new TestController();
        
        AbstractReservationController.ReservationData data = 
            new AbstractReservationController.ReservationData(
                "2025-12-01",
                LocalDate.of(2025, 12, 1),
                "테스트사용자",
                "908호",
                "월요일",
                "1교시",
                "10:00",
                "", // 빈 목적
                30,
                "학생"
            );
        
        Method method = AbstractReservationController.class
            .getDeclaredMethod("validateBasicInput",
                AbstractReservationController.ReservationData.class);
        method.setAccessible(true);
        
        boolean result = (boolean) method.invoke(controller, data);
        
        assertFalse(result, "목적이 비어있으면 검증 실패해야 함");
    }

    @Test
    @DisplayName("validateBasicInput은 날짜가 null이면 false를 반환해야 한다")
    void testValidateBasicInputWithNullDate() throws Exception {
        TestController controller = new TestController();
        
        AbstractReservationController.ReservationData data = 
            new AbstractReservationController.ReservationData(
                null, // null 날짜
                LocalDate.of(2025, 12, 1),
                "테스트사용자",
                "908호",
                "월요일",
                "1교시",
                "10:00",
                "테스트목적",
                30,
                "학생"
            );
        
        Method method = AbstractReservationController.class
            .getDeclaredMethod("validateBasicInput",
                AbstractReservationController.ReservationData.class);
        method.setAccessible(true);
        
        boolean result = (boolean) method.invoke(controller, data);
        
        assertFalse(result, "날짜가 null이면 검증 실패해야 함");
    }

 

    /**
     * 호출 추적용 Controller
     */
    private static class CallTrackingController extends TestController {
        private final List<String> callOrder = new ArrayList<>();
        
        @Override
        protected String getSelectedDateString() {
            callOrder.add("getSelectedDateString");
            return super.getSelectedDateString();
        }
        
        @Override
        protected LocalDate getSelectedDate() {
            callOrder.add("getSelectedDate");
            return super.getSelectedDate();
        }
        
        @Override
        protected String getSelectedRoom() {
            callOrder.add("getSelectedRoom");
            return super.getSelectedRoom();
        }
        
        @Override
        protected String getSelectedDay() {
            callOrder.add("getSelectedDay");
            return super.getSelectedDay();
        }
        
        @Override
        protected String getSelectedTime() {
            callOrder.add("getSelectedTime");
            return super.getSelectedTime();
        }
        
        @Override
        protected String getSelectedEndTime() {
            callOrder.add("getSelectedEndTime");
            return super.getSelectedEndTime();
        }
        
        @Override
        protected String getPurpose() {
            callOrder.add("getPurpose");
            return super.getPurpose();
        }
        
        @Override
        protected int getStudentCount() {
            callOrder.add("getStudentCount");
            return super.getStudentCount();
        }
        
        public List<String> getCallOrder() {
            return callOrder;
        }
    }

    /**
     * 기본 테스트용 Controller
     */
    private static class TestController extends AbstractReservationController {
        
        @Override
        protected String getRoomTypeName() {
            return "테스트실";
        }

        @Override
        protected String getSelectedRoom() {
            return "TEST908";
        }

        @Override
        protected List<String> loadRoomList() {
            return Arrays.asList("TEST908");
        }

        @Override
        protected void setRoomList(List<String> rooms) {}

        @Override
        protected String getSelectedDateString() {
            return "2025-12-01";
        }

        @Override
        protected LocalDate getSelectedDate() {
            return LocalDate.of(2025, 12, 1);
        }

        @Override
        protected String getSelectedDay() {
            return "월요일";
        }

        @Override
        protected String getSelectedTime() {
            return "1교시(09:00~10:00)";
        }

        @Override
        protected String getSelectedEndTime() {
            return "10:00";
        }

        @Override
        protected String getPurpose() {
            return "테스트목적";
        }

        @Override
        protected int getStudentCount() {
            return 30;
        }

        @Override
        protected JButton getBeforeButton() {
            return new JButton();
        }

        @Override
        protected JComboBox<String> getRoomComboBox() {
            return new JComboBox<>();
        }

        @Override
        protected JComboBox<String> getTimeComboBox() {
            return new JComboBox<>();
        }

        @Override
        protected com.toedter.calendar.JDateChooser getDateChooser() {
            return null;
        }

        @Override
        protected void resetReservationButtonListener() {}

        @Override
        protected void addReservationListener(ActionListener listener) {}

        @Override
        protected void showMessage(String message) {
        }

        @Override
        protected void closeView() {}

        @Override
        protected void updateCalendarTable(JTable table) {}

        @Override
        protected void setCapacityInfoText(String text) {}
    }
}