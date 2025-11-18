package Controller;

import View.Executive;
import View.RoomSelect;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ViewFactory {

    public static JFrame createMainView(char userType) {
        JFrame nextView = null;

        switch (userType) {
            case 'S': // 학생
            case 'P': // 교수
                RoomSelect roomSelect = new RoomSelect();
                new RoomSelectController(roomSelect); // 컨트롤러 연결
                nextView = roomSelect;
                break;

            case 'A': // 조교 (관리자)
                Executive executive = new Executive();
                new ExecutiveController(executive); // 컨트롤러 연결
                nextView = executive;
                break;

            default:
                throw new IllegalArgumentException("알 수 없는 사용자 유형: " + userType);
        }
        
        // 공통 종료 리스너 추가 (로그아웃 처리)
        if (nextView != null) {
            nextView.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    new NetworkFacade().logout(); // Facade를 통해 로그아웃
                }
            });
        }

        return nextView;
    }
}