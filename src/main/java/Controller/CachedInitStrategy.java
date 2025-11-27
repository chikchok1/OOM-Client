package Controller;

import Manager.ClientClassroomManager;
import Model.Session;
import Util.ReservationUtil;
import javax.swing.*;
import java.util.List;

/**
 * 미리 데이터를 캐싱해두는 초기화 전략
 * 
 * 사용 예시:
 * - 자주 사용하는 강의실의 데이터를 미리 로드
 * - 성능 최적화가 필요한 경우
 */
public class CachedInitStrategy implements InitializationStrategy {
    
    private final String preferredRoom;
    private final java.time.LocalDate preferredDate;
    
    /**
     * 생성자
     * @param preferredRoom 미리 로드할 강의실
     * @param preferredDate 미리 로드할 날짜
     */
    public CachedInitStrategy(String preferredRoom, java.time.LocalDate preferredDate) {
        this.preferredRoom = preferredRoom;
        this.preferredDate = preferredDate;
    }
    
    @Override
    public void initialize(AbstractReservationController controller) {
        System.out.println("[CachedInitStrategy] 캐시 기반 초기화 시작");
        
        controller.setupEventListeners();
        loadCachedData(controller);
    }
    
    private void loadCachedData(AbstractReservationController controller) {
        new Thread(() -> {
            synchronized (controller.serverLock) {
                // 1. 서버에서 최신 데이터 가져오기
                if (Session.getInstance().isConnected()) {
                    ClientClassroomManager.getInstance().refreshFromServer();
                }

                // 2. 강의실 목록 로드
                List<String> rooms = controller.loadRoomList();
                if (rooms.isEmpty()) {
                    SwingUtilities.invokeLater(() ->
                            controller.showMessage(controller.getRoomTypeName() + " 목록을 불러올 수 없습니다.")
                    );
                    return;
                }
                controller.setRoomList(rooms);

                // 3. 선호 강의실 또는 현재 선택된 강의실 사용
                String targetRoom = rooms.contains(preferredRoom) ? preferredRoom : controller.getSelectedRoom();
                boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(targetRoom);

                // 4. 선호 날짜 또는 내일 날짜 사용
                java.time.LocalDate targetDate = preferredDate != null ? preferredDate : java.time.LocalDate.now().plusDays(1);
                java.time.LocalDate weekStart = ReservationUtil.getWeekStart(targetDate);
                
                // 5. 예약 데이터 캐싱
                System.out.println("[CachedInitStrategy] 캐싱: " + targetRoom + ", " + targetDate);
                ReservationUtil.loadWeeklyReservationData(
                    controller.reservedMap, 
                    controller.statusMap, 
                    targetRoom, 
                    weekStart, 
                    weekStart.plusDays(6)
                );

                // 6. UI 업데이트
                String dateString = targetDate.toString();
                String day = controller.getSelectedDay();
                String time = controller.getSelectedTime();
                int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(targetRoom, dateString, time);

                final int finalReservedCapacity = reservedCapacity;
                final java.time.LocalDate finalWeekStart = weekStart;
                SwingUtilities.invokeLater(() -> {
                    JTable updatedTable = ReservationUtil.buildCalendarTableWithDates(
                            controller.reservedMap, controller.statusMap, targetRoom, isAvailable, finalWeekStart);
                    controller.updateCalendarTable(updatedTable);
                    controller.updateCapacityPanelWithData(targetRoom, day, time, finalReservedCapacity);
                    
                    System.out.println("[CachedInitStrategy] 초기화 완료");
                });
            }
        }).start();
    }
}
