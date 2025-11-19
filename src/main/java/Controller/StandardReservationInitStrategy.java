package Controller;

import Manager.ClientClassroomManager;
import Model.Session;
import Util.ReservationUtil;
import javax.swing.*;
import java.util.List;

/**
 * 일반 예약을 위한 초기화 전략 (ReservClassController, ReservLabController)
 */
public class StandardReservationInitStrategy implements InitializationStrategy {
    
    @Override
    public void initialize(AbstractReservationController controller) {
        controller.setupEventListeners();
        loadInitialDataForReservation(controller);
    }
    
    /**
     * 일반 예약용 초기 데이터 로드
     */
    private void loadInitialDataForReservation(AbstractReservationController controller) {
        new Thread(() -> {
            synchronized (controller.serverLock) {
                if (Session.getInstance().isConnected()) {
                    ClientClassroomManager.getInstance().refreshFromServer();
                }

                List<String> rooms = controller.loadRoomList();

                if (rooms.isEmpty()) {
                    SwingUtilities.invokeLater(() ->
                            controller.showMessage(controller.getRoomTypeName() + " 목록을 불러올 수 없습니다.\nClassrooms.txt 파일을 확인해주세요.")
                    );
                }

                controller.setRoomList(rooms);

                String selectedRoom = controller.getSelectedRoom();
                boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(selectedRoom);

                java.time.LocalDate selectedDate = controller.getSelectedDate();
                if (selectedDate == null) {
                    selectedDate = java.time.LocalDate.now().plusDays(1);
                }
                java.time.LocalDate weekStart = ReservationUtil.getWeekStart(selectedDate);
                java.time.LocalDate weekEnd = weekStart.plusDays(6);
                ReservationUtil.loadWeeklyReservationData(controller.reservedMap, controller.statusMap, selectedRoom, weekStart, weekEnd);

                String dateString = selectedDate.toString();
                String day = controller.getSelectedDay();
                String time = controller.getSelectedTime();
                int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(selectedRoom, dateString, time);

                final int finalReservedCapacity = reservedCapacity;
                final java.time.LocalDate finalWeekStart = weekStart;
                SwingUtilities.invokeLater(() -> {
                    JTable updatedTable = ReservationUtil.buildCalendarTableWithDates(
                            controller.reservedMap, controller.statusMap, selectedRoom, isAvailable, finalWeekStart);
                    controller.updateCalendarTable(updatedTable);
                    controller.updateCapacityPanelWithData(selectedRoom, day, time, finalReservedCapacity);
                });
            }
        }).start();
    }
}
