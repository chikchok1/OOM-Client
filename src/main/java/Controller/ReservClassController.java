package Controller;

import View.ReservClassView;
import Util.ClassroomFileReader;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;

/**
 * 강의실 예약 컨트롤러
 * 템플릿 메서드 패턴의 서브클래스
 */
public class ReservClassController extends AbstractReservationController {
    
    private final ReservClassView view;
    
    public ReservClassController(ReservClassView view) {
        this.view = view;
        initialize();
    }
    
    // ============================================================
    // Hook 메서드 오버라이드 (선택적)
    // ============================================================
    
    /**
     * 강의실 전용: 추가 검증 로직 예시
     
    @Override
    protected boolean performAdditionalValidation(ReservationData data) {
        // 강의실 특화 검증 로직
        // 예시: 908호는 교수/세미나만 예약 가능
        if (data.room.equals("908호") && data.userRole.equals("학생")) {
            showMessage("908호는 학생 예약이 불가능합니다.");
            return false;
        }
        
        return true;
    }
    */
    /**
     * 강의실 전용: 성공 메시지 커스터마이징
     */
    @Override
    protected void showReservationSuccessMessage(ReservationData data) {
        showMessage(String.format(
                " 강의실 예약 신청 완료!\n"
                        + "강의실: %s\n"
                        + "날짜: %s (%s)\n"
                        + "시간: %s ~ %s\n"
                        + "사용 인원: %d명\n"
                        + "용도: %s\n"
                        + " 조교의 승인을 기다려주세요.",
                data.room, data.dateString, data.day,
                data.startTime, data.endTime, data.studentCount, data.purpose
        ));
    }
    
    // ============================================================
    // 추상 메서드 구현 (필수)
    // ============================================================
    
    @Override
    protected String getRoomTypeName() {
        return "강의실";
    }
    
    @Override
    protected String getSelectedRoom() {
        return view.getSelectedClassRoom();
    }
    
    @Override
    protected List<String> loadRoomList() {
        return ClassroomFileReader.loadClassrooms();
    }
    
    @Override
    protected void setRoomList(List<String> rooms) {
        view.setClassrooms(rooms);
    }
    
    @Override
    protected String getSelectedDateString() {
        return view.getSelectedDateString();
    }
    
    @Override
    protected java.time.LocalDate getSelectedDate() {
        return view.getSelectedDate();
    }
    
    @Override
    protected String getSelectedDay() {
        return view.getSelectedDay();
    }
    
    @Override
    protected String getSelectedTime() {
        return view.getSelectedTime();
    }
    
    @Override
    protected String getSelectedEndTime() {
        return view.getSelectedEndTime();
    }
    
    @Override
    protected String getPurpose() {
        return view.getPurpose();
    }
    
    @Override
    protected int getStudentCount() {
        return view.getStudentCount();
    }
    
    @Override
    protected JButton getBeforeButton() {
        return view.getBeforeButton();
    }
    
    @Override
    protected JComboBox<String> getRoomComboBox() {
        return view.getClassComboBox();
    }
    
    @Override
    protected JComboBox<String> getTimeComboBox() {
        return view.getTimeComboBox();
    }
    
    @Override
    protected com.toedter.calendar.JDateChooser getDateChooser() {
        return view.getDateChooser();
    }
    
    @Override
    protected void resetReservationButtonListener() {
        view.resetReservationButtonListener();
    }
    
    @Override
    protected void addReservationListener(ActionListener listener) {
        view.addReservationListener(listener);
    }
    
    @Override
    protected void showMessage(String message) {
        view.showMessage(message);
    }
    
    @Override
    protected void closeView() {
        view.closeView();
    }
    
    @Override
    protected void updateCalendarTable(JTable table) {
        view.updateCalendarTable(table);
    }
    
    @Override
    protected void setCapacityInfoText(String text) {
        view.setCapacityInfoText(text);
    }
}