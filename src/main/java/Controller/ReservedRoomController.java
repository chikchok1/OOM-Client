package Controller;

import iterator.ReservationGroup;
import iterator.Iterator;
import Model.Session;
import Service.ReservationService;
import View.ReservedRoomView;
import View.RoomSelect;
import java.io.PrintWriter;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import java.time.LocalDate;
import java.time.DayOfWeek;

/**
 * [Iterator Pattern: Client]
 * - Aggregate(ReservationGroup)와 Iterator 인터페이스를 사용하여 데이터를 처리합니다.
 * - 데이터의 내부 저장 구조(ArrayList 등)를 알 필요 없이 순회할 수 있습니다.
 */

public class ReservedRoomController {

    private ReservedRoomView view;
    
    private volatile boolean isLoading = false;

    public ReservedRoomController(ReservedRoomView view) {
        this.view = view;
        
        // ✅ 강의실/실습실 목록 초기화
        initializeRoomList();
        
        addListeners();
    }
    
    /**
     * ✅ 강의실/실습실 목록 초기화
     */
    private void initializeRoomList() {
        new Thread(() -> {
            Manager.ClientClassroomManager manager = Manager.ClientClassroomManager.getInstance();
            
            // 서버로부터 최신 데이터 가져오기
            if (manager.refreshFromServer()) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    view.loadRooms();
                    System.out.println("[ReservedRoomController] 강의실 목록 초기화 완료");
                });
            } else {
                System.err.println("[ReservedRoomController] 강의실 목록 로드 실패");
            }
        }).start();
    }

    private void addListeners() {
        // [1] "확인" 버튼 클릭 시
        view.getCheckButton().addActionListener(e -> {
            String selectedRoom = view.getSelectedRoom();
            if (!"선택".equals(selectedRoom) && !isLoading) {
                loadReservedRooms(selectedRoom);
            }
        });

        // [2] 강의실 선택 콤보박스
        view.getClassComboBox().addActionListener(e -> {
            if (view.isUpdating() || isLoading) {
                return;
            }
            view.setUpdating(true);
            view.resetLabSelection();
            String selectedRoom = view.getSelectedRoom();
            if (!"선택".equals(selectedRoom)) {
                loadReservedRooms(selectedRoom);
            }
            view.setUpdating(false);
        });

        // [3] 실습실 선택 콤보박스
        view.getLabComboBox().addActionListener(e -> {
            if (view.isUpdating() || isLoading) {
                return;
            }
            view.setUpdating(true);
            view.resetClassSelection();
            String selectedRoom = view.getSelectedRoom();
            if (!"선택".equals(selectedRoom)) {
                loadReservedRooms(selectedRoom);
            }
            view.setUpdating(false);
        });
        
        // [4] 날짜 선택기 리스너 추가
        view.getDateChooser().addPropertyChangeListener("date", evt -> {
            if (isLoading) return;
            String selectedRoom = view.getSelectedRoom();
            if (!"선택".equals(selectedRoom)) {
                System.out.println("[날짜 변경] 선택된 날짜: " + view.getSelectedDateString());
                loadReservedRooms(selectedRoom);
            }
        });

        // [5] 이전 버튼 : 사용자 권한(교수/학생)에 따라 적절한 화면으로 이동
        view.getBeforeButton().addActionListener(e -> {
            view.dispose();
            String userId = Session.getInstance().getLoggedInUserId();

            if (userId != null && userId.startsWith("A")) {
                if (view.getExecutive() != null) {
                    view.getExecutive().setVisible(true);
                } else {
                    System.err.println("[오류] Executive 인스턴스가 null입니다.");
                }
            } else {
                RoomSelect roomSelect = RoomSelect.getInstance();
                new RoomSelectController(roomSelect);
                roomSelect.setVisible(true);
            }
        });
    }

    private void loadReservedRooms(String selectedRoom) {
        if (isLoading) {
            System.out.println("[경고] 이미 데이터를 불러오는 중입니다.");
            return;
        }
        
        isLoading = true;
        System.out.println("[시작] " + selectedRoom + " 데이터 요청");
        
        JTable table = view.getTable();
        
        // UI 초기화 (테이블 비우기)
        javax.swing.SwingUtilities.invokeLater(() -> {
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int col = 1; col < table.getColumnCount(); col++) {
                    table.setValueAt("", row, col);
                }
            }
        });
        
        // 서버 통신은 비동기 스레드에서 처리
        new Thread(() -> {
            try {
                // ============================================
                // [캘린더] 선택된 날짜의 주간 계산
                // ============================================
               LocalDate selectedDate = view.getSelectedDate();
                
                if (selectedDate == null) {
                    System.err.println("❌ [오류] 뷰에서 날짜를 가져오지 못했습니다. (null 반환됨)");
                    // 강제로 진행하지 않고 여기서 멈춰서 오동작(다른 날짜 표시)을 방지합니다.
                    isLoading = false; 
                    return; 
                } else {
                    System.out.println("✅ [정상] 인식된 날짜: " + selectedDate);
                }
                
                LocalDate weekStart = getWeekStart(selectedDate);
                LocalDate weekEnd = weekStart.plusDays(6);
                
                System.out.println("[예약 로드] 주간 범위: " + weekStart + " ~ " + weekEnd);
                
                String userId = Session.getInstance().getLoggedInUserId();
                String userName = Session.getInstance().getLoggedInUserName();
                boolean isPrivileged = userId.startsWith("P") || userId.startsWith("A");
                
                System.out.println("[사용자] ID=" + userId + ", 이름=" + userName + ", 권한=" + (isPrivileged ? "교수/조교" : "학생"));
                
                PrintWriter out = Session.getInstance().getOut();
                Util.MessageDispatcher dispatcher = Util.MessageDispatcher.getInstance();

                if (out == null || dispatcher == null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(view, "서버와 연결되어 있지 않습니다.");
                    });
                    return;
                }

                // ============================================
                // [서버 요청] 주간 범위를 서버에 전달
                // ============================================
                String request = String.format("VIEW_RESERVATION,%s,%s,%s,%s", 
                    userId, selectedRoom, weekStart.toString(), weekEnd.toString());
                    
                System.out.println("[요청 전송] " + request);
                out.println(request);
                out.flush();

                // ============================================
                // [Iterator 패턴] MessageDispatcher로 응답 받기
                // ============================================
                ReservationGroup reservationGroup = new ReservationGroup();
                
                int lineCount = 0;
                int maxAttempts = 100;
                int attempts = 0;
                
                while (attempts < maxAttempts) {
                    attempts++;
                    String line = dispatcher.waitForResponse(30); // 30초 타임아웃
                    
                    if (line == null) {
                        System.out.println("[타임아웃] 응답 없음");
                        break;
                    }
                    
                    lineCount++;
                    System.out.println("[수신 " + lineCount + "] " + line);
                    
                    if (line.equals("END_OF_RESERVATION")) {
                        System.out.println("[종료 신호 수신]");
                        break;
                    }

                    String[] tokens = line.split(",");
                    if (tokens.length < 10) {
                        System.out.println("[경고] 필드 부족: " + line);
                        continue;
                    }

                    try {
                        // CSV를 DTO로 변환
                        Service.ReservationService.ReservationDTO dto = 
                            new Service.ReservationService.ReservationDTO(
                                tokens[0].trim(),  // fileType
                                tokens.length > 9 ? tokens[9].trim() : "",  // userId
                                tokens[0].trim(),  // name
                                tokens[1].trim(),  // room
                                tokens[2].trim(),  // date
                                tokens[3].trim(),  // day
                                tokens[4].trim(),  // time
                                tokens[5].trim(),  // purpose
                                tokens.length > 6 ? tokens[6].trim() : "",  // role
                                tokens.length > 8 ? Integer.parseInt(tokens[8].trim()) : 0  // count
                            );

                        // ============================================
                        // [필터링] 선택된 강의실 + 주간 범위
                        // ============================================
                        if (dto.getRoom().equals(selectedRoom) || 
                            dto.getRoom().equals(selectedRoom + "호") ||
                            (dto.getRoom() + "호").equals(selectedRoom)) {
                            
                            // 날짜 범위 체크 (서버에서 이미 필터링하지만 이중 체크)
                            LocalDate reservationDate = LocalDate.parse(dto.getDate());
                            if (!reservationDate.isBefore(weekStart) && !reservationDate.isAfter(weekEnd)) {
                                reservationGroup.addReservation(dto);
                                System.out.println("[추가] " + dto.getName() + " / " + dto.getDate() + " / " + dto.getDay() + " / " + dto.getTime());
                            } else {
                                System.out.println("[날짜 제외] " + dto.getDate() + " (범위: " + weekStart + " ~ " + weekEnd + ")");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[파싱 오류] " + line + " - " + e.getMessage());
                    }
                }
                
                System.out.println("[총 수신] " + lineCount + "개");

                // ============================================
                // [Iterator 패턴] 데이터 순회하여 UI 업데이트
                // ============================================
                Iterator iterator = reservationGroup.createIterator();
                
                final boolean finalIsPrivileged = isPrivileged;
                final String finalUserName = userName;
                
                javax.swing.SwingUtilities.invokeLater(() -> {
                    int processedCount = 0;
                    
                    while (iterator.hasNext()) {
                        Service.ReservationService.ReservationDTO dto = 
                            (Service.ReservationService.ReservationDTO) iterator.next();
                        
                        if (dto == null) {
                            System.out.println("[경고] null 데이터");
                            continue;
                        }
                        
                        processedCount++;
                        
                        int col = getDayColumn(dto.getDay());
                        int row = getPeriodRow(dto.getTime());
                        
                        System.out.println("[처리 " + processedCount + "] " + dto.getName() + " / " + dto.getDay() + " / " + dto.getTime());
                        System.out.println("[위치] row=" + row + ", col=" + col);
                        
                        if (col != -1 && row != -1) {
                            String current = (String) table.getValueAt(row, col);
                            String name = dto.getName();
                            
                            System.out.println("[비교] 예약자=" + name + ", 로그인사용자=" + finalUserName);
                            
                            if (finalIsPrivileged) {
                                // 교수/조교: 모든 예약자 이름 표시
                                if (current == null || current.isEmpty()) {
                                    table.setValueAt(name, row, col);
                                    System.out.println("[교수/조교] 테이블 업데이트: " + name);
                                } else if (!current.contains(name)) {
                                    table.setValueAt(current + ", " + name, row, col);
                                    System.out.println("[교수/조교] 테이블 추가: " + name);
                                }
                            } else {
                                // 학생: 모든 예약을 "예약됨"으로 표시 (예약자 이름은 숨김)
                                if (current == null || current.isEmpty()) {
                                    table.setValueAt("예약됨", row, col);
                                    System.out.println("[학생] 테이블 업데이트: 예약됨 (예약자: " + name + ")");
                                } else if (!current.contains("예약됨")) {
                                    // 이미 "예약됨"이 있으면 중복 표시하지 않음
                                    table.setValueAt("예약됨", row, col);
                                    System.out.println("[학생] 테이블 유지: 예약됨 (예약자: " + name + ")");
                                }
                            }
                            
                            table.repaint();
                        } else {
                            System.out.println("[경고] 잘못된 위치 정보");
                        }
                    }
                    
                    System.out.println("========================================");
                    System.out.println("[총 처리된 예약 수] " + processedCount);
                    System.out.println("========================================");
                });
                
            } catch (Exception e) {
                System.err.println("[오류] " + e.getMessage());
                e.printStackTrace();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(view, "데이터 조회 실패: " + e.getMessage());
                });
            } finally {
                isLoading = false;
                System.out.println("[완료] " + selectedRoom + " 데이터 로딩 완료\n");
            }
        }).start();
    }

    /**
     * 주의 시작일 (월요일) 계산
     */
    private LocalDate getWeekStart(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int daysToSubtract = dayOfWeek.getValue() - 1;
        return date.minusDays(daysToSubtract);
    }

    private int getDayColumn(String day) {
        return switch (day) {
            case "월요일" -> 1;
            case "화요일" -> 2;
            case "수요일" -> 3;
            case "목요일" -> 4;
            case "금요일" -> 5;
            case "토요일" -> 6;
            case "일요일" -> 7;
            default -> -1;
        };
    }

    private int getPeriodRow(String period) {
        return switch (period) {
            case "1교시(09:00~10:00)" -> 0;
            case "2교시(10:00~11:00)" -> 1;
            case "3교시(11:00~12:00)" -> 2;
            case "4교시(12:00~13:00)" -> 3;
            case "5교시(13:00~14:00)" -> 4;
            case "6교시(14:00~15:00)" -> 5;
            case "7교시(15:00~16:00)" -> 6;
            case "8교시(16:00~17:00)" -> 7;
            case "9교시(17:00~18:00)" -> 8;
            default -> -1;
        };
    }
}