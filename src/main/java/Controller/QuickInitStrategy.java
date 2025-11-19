package Controller;

import javax.swing.*;
import java.util.List;

/**
 * 빠른 초기화 전략 - 네트워크 호출 없이 로컬 데이터만 사용
 * 
 * 사용 예시:
 * - 오프라인 모드
 * - 테스트 환경
 * - 빠른 프로토타이핑
 */
public class QuickInitStrategy implements InitializationStrategy {
    
    @Override
    public void initialize(AbstractReservationController controller) {
        System.out.println("[QuickInitStrategy] 빠른 초기화 시작 (네트워크 호출 없음)");
        
        // 이벤트 리스너만 설정
        controller.setupEventListeners();
        
        // 로컬 데이터만 로드
        loadLocalDataOnly(controller);
    }
    
    private void loadLocalDataOnly(AbstractReservationController controller) {
        SwingUtilities.invokeLater(() -> {
            List<String> rooms = controller.loadRoomList();
            
            if (rooms.isEmpty()) {
                controller.showMessage(controller.getRoomTypeName() + " 목록을 불러올 수 없습니다.");
                return;
            }
            
            controller.setRoomList(rooms);
            controller.setCapacityInfoText("로컬 데이터 로드 완료");
            
            System.out.println("[QuickInitStrategy] 초기화 완료");
        });
    }
}
