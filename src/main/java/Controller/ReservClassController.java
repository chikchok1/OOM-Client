package Controller;

import Util.ClassroomFileReader;
import View.ReservClassView;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;

/**
 * 강의실 예약 컨트롤러
 * AbstractReservationController를 상속하여 강의실 전용 기능만 구현
 */
public class ReservClassController extends AbstractReservationController {

    private ReservClassView view;

    public ReservClassController(ReservClassView view) {
        this.view = view;
        initialize(); // 부모 클래스의 초기화 메서드 호출
    }

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
