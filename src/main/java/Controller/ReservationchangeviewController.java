package Controller;

import Model.Session;
import Util.MessageDispatcher;
import Util.ReservationUtil;
import View.Reservationchangeview;
import View.RoomSelect;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

public class ReservationchangeviewController extends AbstractReservationController {

    private Reservationchangeview view;

    // 변경할 기존 예약 정보
    private List<ReservationInfo> originalReservations = new ArrayList<>();

    public ReservationchangeviewController(Reservationchangeview view) {
        this.view = view;

        // 이벤트 리스너 등록
        this.view.setChangeButtonActionListener(new ChangeReservationListener());
        this.view.setBackButtonActionListener(e -> handleBack());
        this.view.setCancelButtonActionListener(new CancelReservationListener());

        // 테이블 행 클릭 시 정보 채우기
        this.view.getReservationTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = view.getReservationTable().getSelectedRow();
                if (selectedRow >= 0 && selectedRow < originalReservations.size()) {
                    ReservationInfo info = originalReservations.get(selectedRow);

                    view.setReservationId(info.userId);

                    // 로그 출력
                    System.out.printf("[테이블 클릭] 선택된 예약: [%s] %s (%s, %s, %s, %s, 인원:%d)%n",
                            info.fileType, info.name, info.userId, info.room, info.date, info.time, info.studentCount);

                    //  팝업 대신 콘솔 로그만 출력 (사용자 경험 개선)
                }
            }
        });

        // 강의실 선택 변경 시 (Class 콤보박스)
        this.view.getClassRoomTypeComboBox().addActionListener(e -> {
            String selected = (String) view.getClassRoomTypeComboBox().getSelectedItem();
            if (selected != null && !selected.equals("선택")) {
                // Lab 콤보박스를 "선택"으로 초기화
                view.getLabRoomTypeComboBox().setSelectedIndex(0);

                String newSelectedRoom = view.getSelectedClassRoom();
                refreshReservationAndAvailability(newSelectedRoom);
            }
        });

// 실습실 선택 변경 시 (Lab 콤보박스)
        this.view.getLabRoomTypeComboBox().addActionListener(e -> {
            String selected = (String) view.getLabRoomTypeComboBox().getSelectedItem();
            if (selected != null && !selected.equals("선택")) {
                // Class 콤보박스를 "선택"으로 초기화
                view.getClassRoomTypeComboBox().setSelectedIndex(0);

                String newSelectedRoom = view.getSelectedClassRoom();
                refreshReservationAndAvailability(newSelectedRoom);
            }
        });

        // 날짜 선택 변경 시
        this.view.getDateChooser().addPropertyChangeListener("date", evt -> {
            String newSelectedRoom = view.getSelectedClassRoom();
            refreshReservationAndAvailability(newSelectedRoom);
        });

        // 시간 선택 변경 시
        this.view.getTimeComboBox().addActionListener(e -> updateCapacityPanel());

        // 초기화 작업
        new Thread(() -> {
            // ✅ 서버 연결 먼저 확인
            if (!Session.getInstance().isConnected()) {
                System.err.println("[초기화] 서버 연결 없음 - 초기화 중단");
                SwingUtilities.invokeLater(() -> {
                    view.showMessage("서버에 연결되어 있지 않습니다.");
                    view.dispose();
                });
                return;
            }
            
            synchronized (serverLock) {
                // ✅ 클라이언트용 ClassroomManager 사용
                if (Session.getInstance().isConnected()) {
                    Manager.ClientClassroomManager.getInstance().refreshFromServer();
                }

                // 강의실 목록 로드 (UI 스레드에서 동기 처리)
                try {
                    SwingUtilities.invokeAndWait(() -> view.loadClassrooms());
                    System.out.println("[초기화] 강의실 목록 로드 완료");
                } catch (Exception ex) {
                    System.err.println("[초기화] 강의실 목록 로드 실패: " + ex.getMessage());
                    ex.printStackTrace();
                }

                // 승인된 예약 목록 로드
                loadApprovedReservations();

                // 초기 데이터 로드 - "선택" 상태일 때는 서버 요청 건너뛰기
                String selectedRoom = view.getSelectedClassRoom();

                java.time.LocalDate selectedDate = view.getSelectedDate();
                if (selectedDate == null) {
                    selectedDate = java.time.LocalDate.now().plusDays(1);
                }

                // "선택"이 선택된 경우 캠린더 초기화만 수행
                if ("선택".equals(selectedRoom)) {
                    System.out.println("[초기화] 강의실 미선택 상태 - 캠린더 초기화만 수행");
                    final java.time.LocalDate finalWeekStart = ReservationUtil.getWeekStart(selectedDate);
                    SwingUtilities.invokeLater(() -> {
                        JTable emptyTable = ReservationUtil.buildCalendarTableWithDates(
                                new java.util.HashMap<>(), new java.util.HashMap<>(),
                                "선택", true, finalWeekStart);
                        view.updateCalendarTable(emptyTable);
                        view.setCapacityInfoText("강의실을 먼저 선택해주세요.");
                    });
                    return;
                }

                // ✅ ReservationUtil 사용
                boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(selectedRoom);
                java.time.LocalDate weekStart = ReservationUtil.getWeekStart(selectedDate);
                java.time.LocalDate weekEnd = weekStart.plusDays(6);
                ReservationUtil.loadWeeklyReservationData(reservedMap, statusMap, selectedRoom, weekStart, weekEnd);

                String dateString = selectedDate.toString();
                String day = view.getSelectedDay();
                String time = view.getSelectedTime();
                int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(selectedRoom, dateString, time);

                // UI 업데이트
                final int finalReservedCapacity = reservedCapacity;
                final java.time.LocalDate finalWeekStart = weekStart;
                SwingUtilities.invokeLater(() -> {
                    // ✅ ReservationUtil 사용
                    JTable updatedTable = ReservationUtil.buildCalendarTableWithDates(
                            reservedMap, statusMap, selectedRoom, isAvailable, finalWeekStart);
                    view.updateCalendarTable(updatedTable);
                    updateCapacityPanelWithData(selectedRoom, day, time, finalReservedCapacity);
                });
            }
        }).start();
    }

    /**
     * 승인된 예약 목록 로드 (테이블에 표시)
     */
   /**
 * 승인된 예약 목록 로드 (테이블에 표시)
 */
private void loadApprovedReservations() {
    //  서버 연결 먼저 확인
    if (!Session.getInstance().isConnected()) {
        System.err.println("[loadApprovedReservations] 서버 연결 없음 - 로드 중단");
        return;
    }
    
    String sessionUserId = Session.getInstance().getLoggedInUserId();
    if (sessionUserId == null || sessionUserId.isEmpty()) {
        System.err.println("[loadApprovedReservations] 사용자 ID 없음");
        SwingUtilities.invokeLater(() -> {
            view.showMessage("사용자 로그인 정보를 찾을 수 없습니다.");
        });
        return;
    }

    DefaultTableModel model = (DefaultTableModel) view.getReservationTable().getModel();
    model.setRowCount(0);
    originalReservations.clear();

    try {
        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();

        if (out == null || dispatcher == null) {
            System.err.println("[loadApprovedReservations] 서버 스트림 없음");
            return;
        }

        // 승인된 예약 요청
        out.println("VIEW_APPROVED_RESERVATIONS");
        out.flush();
        System.out.println("[loadApprovedReservations] 요청 전송: VIEW_APPROVED_RESERVATIONS");

        int count = 0;
        int maxAttempts = 100; // 최대 100개까지 읽기
        int attempts = 0;

        while (attempts < maxAttempts) {
            attempts++;
            
            // ✅ MessageDispatcher 큐에서 응답 읽기 (30초 타임아웃)
            String line = dispatcher.waitForResponse(30);
            
            if (line == null) {
                System.err.println("[loadApprovedReservations] 타임아웃 - 응답 없음");
                break;
            }

            System.out.println("[loadApprovedReservations] 읽은 라인: " + line);
            
            // 종료 신호
            if (line.equals("END_OF_APPROVED_RESERVATIONS")) {
                System.out.println("[loadApprovedReservations] 종료 신호 받음");
                break;
            }
            
            // 성공 응답 건너뛰기
            if (line.equals("VIEW_APPROVED_RESERVATIONS_SUCCESS")) {
                System.out.println("[loadApprovedReservations] 성공 응답 확인");
                continue;
            }
            
            // 예약 데이터만 처리 (CLASS, 또는 LAB,으로 시작)
            if (!line.startsWith("CLASS,") && !line.startsWith("LAB,")) {
                System.out.println("[loadApprovedReservations] 예약 데이터 아님, 건너뜀: " + line);
                continue;
            }

            String[] parts = line.split(",");
            if (parts.length >= 11) {
                try {
                    String fileType = parts[0].trim();
                    String name = parts[1].trim();
                    String room = parts[2].trim();
                    String date = parts[3].trim();
                    String day = parts[4].trim();
                    String time = parts[5].trim();
                    String purpose = parts[6].trim();
                    String role = parts[7].trim();
                    String status = parts[8].trim();
                    int studentCount = Integer.parseInt(parts[9].trim());
                    String reservedUserId = parts[10].trim();
                    
                    // 테이블 표시
                    model.addRow(new Object[]{
                        room, date, day, time, purpose, studentCount + "명", status, reservedUserId
                    });
                    
                    // 원본 저장
                    originalReservations.add(new ReservationInfo(
                            fileType, reservedUserId, name, room, date, day,
                            time, purpose, role, studentCount
                    ));
                    count++;
                    
                    System.out.println("[loadApprovedReservations] 예약 추가: " + room + " " + date + " " + time);
                    
                } catch (Exception ex) {
                    System.err.println("[loadApprovedReservations] 파싱 오류: " + line + " - " + ex.getMessage());
                }
            } else {
                System.err.println("[loadApprovedReservations] 잘못된 형식 (길이=" + parts.length + "): " + line);
            }
        }

        System.out.println("[loadApprovedReservations] 승인된 예약 " + count + "개 로드 완료");

        if (count == 0) {
            SwingUtilities.invokeLater(() -> {
                view.showMessage("예약 내역이 없습니다.\n\n캘린더를 통해 새로운 예약을 진행해주세요.");
            });
        }

    } catch (Exception e) {
        System.err.println("[loadApprovedReservations] 오류: " + e.getMessage());
        e.printStackTrace();
        SwingUtilities.invokeLater(() -> {
            view.showMessage("예약 목록 조회 실패: " + e.getMessage());
        });
    }
}

    /**
     * 뒤로가기
     */
    private void handleBack() {
        view.dispose();
        RoomSelect roomSelect = RoomSelect.getInstance();
        new RoomSelectController(roomSelect);
        roomSelect.setVisible(true);
    }

    /**
     * 예약 변경 리스너
     */
    class ChangeReservationListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("[변경버튼] 클릭됨");
            // 1. 테이블에서 선택된 예약 확인
            int selectedRow = view.getReservationTable().getSelectedRow();
            System.out.println("[변경버튼] 선택된 행: " + selectedRow);
            if (selectedRow == -1) {
                view.showMessage("변경할 예약을 선택해주세요.");
                System.out.println("[변경버튼] 예약 미선택으로 중단");
                return;
            }

            if (selectedRow >= originalReservations.size()) {
                view.showMessage("예약 정보를 찾을 수 없습니다.");
                return;
            }

            ReservationInfo original = originalReservations.get(selectedRow);

            // 2. 새로운 예약 정보 수집
            String selectedDateString = view.getSelectedDateString();
            System.out.println("[변경버튼] 선택된 날짜: " + selectedDateString);
            if (selectedDateString == null) {
                view.showMessage("예약 날짜를 선택해주세요.");
                System.out.println("[변경버튼] 날짜 미선택으로 중단");
                return;
            }

            // 최소 하루 전 예약 검증
            java.time.LocalDate selectedDate = view.getSelectedDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate tomorrow = today.plusDays(1);

            if (selectedDate.isBefore(tomorrow)) {
                view.showMessage("최소 하루 전에 예약해야 합니다.");
                return;
            }

            String newRoom = view.getSelectedRoom();
            String selectedDay = view.getSelectedDay();
            String selectedStartTime = view.getSelectedTime();
            String selectedEndTime = view.getSelectedEndTime();
            String purpose = view.getPurpose();
            int studentCount = view.getStudentCount();

            System.out.println("[변경버튼] 수집된 정보:");
            System.out.println("  - 강의실: " + newRoom);
            System.out.println("  - 날짜: " + selectedDateString + " (" + selectedDay + ")");
            System.out.println("  - 시간: " + selectedStartTime + " ~ " + selectedEndTime);
            System.out.println("  - 목적: " + purpose);
            System.out.println("  - 인원: " + studentCount);

            if (purpose == null || purpose.isEmpty()) {
                view.showMessage("사용 목적을 입력해주세요.");
                System.out.println("[변경버튼] 목적 미입력으로 중단");
                return;
            }

            // 3. 시간 범위 검증
            int startHour = ReservationUtil.parseTimeToHour(selectedStartTime);
            int endHour = ReservationUtil.parseTimeToHour(selectedEndTime);

            System.out.println("[변경버튼] 시간 파싱: 시작=" + startHour + "교시, 종료=" + endHour + "교시");

            if (startHour > endHour) {
                view.showMessage("종료 시간은 시작 시간보다 늦어야 합니다.");
                System.out.println("[변경버튼] 시간 역순으로 중단");
                return;
            }

            int duration = endHour - startHour + 1;
            String userRole = Session.getInstance().getLoggedInUserRole();

            System.out.println("[변경버튼] 예약 시간: " + duration + "시간, 역할: " + userRole);

            if (userRole.equals("학생") && duration > 2) {
                view.showMessage("학생은 최대 2시간까지만 예약 가능합니다.\n선택: " + duration + "시간");
                System.out.println("[변경버튼] 학생 2시간 초과로 중단");
                return;
            }
            if (!userRole.equals("학생") && duration > 3) {
                view.showMessage("교수/세미나/학회는 최대 3시간까지만 예약 가능합니다.\n선택: " + duration + "시간");
                System.out.println("[변경버튼] 교수/기타 3시간 초과로 중단");
                return;
            }

            // 4. 수용 인원 체크
            Manager.ClientClassroomManager manager = Manager.ClientClassroomManager.getInstance();
            if (!manager.checkCapacity(newRoom, studentCount)) {
                common.dto.ClassroomDTO classroom = manager.getClassroom(newRoom);
                view.showMessage(String.format(
                        "예약 불가!\n\n"
                        + "요청 인원: %d명\n"
                        + "최대 허용: %d명\n\n"
                        + "(이 강의실은 수용 인원 %d명의 50%%인 %d명까지만 예약 가능합니다)",
                        studentCount,
                        classroom.getAllowedCapacity(),
                        classroom.capacity,
                        classroom.getAllowedCapacity()
                ));
                return;
            }

            // 5. 확인 메시지
            int confirm = JOptionPane.showConfirmDialog(
                    view,
                    String.format(
                            "예약을 변경하시겠습니까?\n\n"
                            + "【기존 예약】\n"
                            + "강의실: %s\n"
                            + "날짜: %s (%s)\n"
                            + "시간: %s\n"
                            + "인원: %d명\n\n"
                            + "【변경 후】\n"
                            + "강의실: %s\n"
                            + "날짜: %s (%s)\n"
                            + "시간: %s ~ %s\n"
                            + "인원: %d명\n\n"
                            + "️ 기존 예약이 자동으로 취소되고\n"
                            + "새로운 예약이 대기 상태로 신청됩니다.\n"
                            + "조교의 승인이 다시 필요합니다.",
                            original.room, original.date, original.day, original.time, original.studentCount,
                            newRoom, selectedDateString, selectedDay, selectedStartTime, selectedEndTime, studentCount
                    ),
                    "예약 변경 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                System.out.println("[변경버튼] 사용자가 취소함");
                return;
            }

            System.out.println("[변경버튼] 사용자 확인 완료, 서버 요청 시작");

            // 6. 비동기로 서버 통신
            new Thread(() -> {
                synchronized (serverLock) {
                    try {
                        // 6-1. 강의실 가용성 확인
                        boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(newRoom);
                        if (!isAvailable) {
                            SwingUtilities.invokeLater(()
                                    -> view.showMessage("선택하신 강의실은 현재 사용 불가능합니다.")
                            );
                            return;
                        }

                        // 6-2. 해당 시간 범위 모두 예약 가능한지 확인
                        for (int hour = startHour; hour <= endHour; hour++) {
                            String timeSlot = ReservationUtil.formatTimeSlot(hour);
                            if (ReservationUtil.isReservedOnDate(reservedMap, newRoom, selectedDate, timeSlot)) {
                                final String conflictTime = timeSlot;
                                SwingUtilities.invokeLater(()
                                        -> view.showMessage(conflictTime + "는 이미 예약되어 있습니다.")
                                );
                                return;
                            }
                        }

                        // 6-3. 기존 예약 삭제 + 새 예약 생성 (서버에서 처리)
                        String response = changeReservationOnServer(original, newRoom, selectedDateString,
                                selectedDay, selectedStartTime, selectedEndTime, purpose, userRole, studentCount);

                        if ("CHANGE_SUCCESS".equals(response)) {
                            SwingUtilities.invokeLater(() -> {
                                view.showMessage(String.format(
                                        "예약 변경이 완료되었습니다!\n\n"
                                        + "강의실: %s\n"
                                        + "날짜: %s\n"
                                        + "요일: %s\n"
                                        + "시간: %s ~ %s\n"
                                        + "사용 인원: %d명\n\n"
                                        + "조교의 승인을 기다려주세요.",
                                        newRoom, selectedDateString, selectedDay,
                                        selectedStartTime, selectedEndTime, studentCount
                                ));

                                // 목록 새로고침
                                loadApprovedReservations();
                            });
                        } else if (response != null && response.startsWith("CHANGE_FAILED_CONFLICT:")) {
                            // 중복 예약 시간 추출
                            String conflictTime = response.substring("CHANGE_FAILED_CONFLICT:".length());
                            SwingUtilities.invokeLater(()
                                    -> view.showMessage("예약 변경 실패!\n\n" + conflictTime + "는 이미 다른 예약이 있습니다.\n다른 시간대를 선택해주세요.")
                            );
                        } else if ("CHANGE_FAILED_NOT_FOUND".equals(response)) {
                            SwingUtilities.invokeLater(()
                                    -> view.showMessage("예약 변경 실패!\n\n기존 예약을 찾을 수 없습니다.\n이미 취소되었거나 변경되었을 수 있습니다.")
                            );
                        } else if ("CHANGE_FAILED_INVALID_FORMAT".equals(response)) {
                            SwingUtilities.invokeLater(()
                                    -> view.showMessage("예약 변경 실패!\n\n잘못된 예약 형식입니다.")
                            );
                        } else {
                            SwingUtilities.invokeLater(()
                                    -> view.showMessage("예약 변경 실패: " + response)
                            );
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(()
                                -> view.showMessage("오류 발생: " + ex.getMessage())
                        );
                    }
                }
            }).start();
        }
    }

    /**
     * 서버에 예약 변경 요청 (기존 삭제 + 새로 생성)
     */
    private String changeReservationOnServer(ReservationInfo original,
            String newRoom, String newDate, String newDay,
            String startTime, String endTime,
            String purpose, String role, int studentCount) {
        try {
            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();
            if (out == null || in == null) {
                return "CHANGE_FAILED";
            }

            String sessionUserName = Session.getInstance().getLoggedInUserName();
            String sessionUserRole = Session.getInstance().getLoggedInUserRole();
            String finalName = (sessionUserName != null && !sessionUserName.isEmpty())
                    ? sessionUserName : original.name;
            String finalRole = (role != null && !role.isEmpty())
                    ? role : (sessionUserRole != null ? sessionUserRole : original.role);

            String oldFileType = original.fileType;

            String newFileType;
            Manager.ClientClassroomManager mgr = Manager.ClientClassroomManager.getInstance();
            common.dto.ClassroomDTO classroom = mgr.getClassroom(newRoom);

            if (classroom != null && "LAB".equals(classroom.type)) {
                newFileType = "LAB";
            } else {
                newFileType = "CLASS";
            }

            String normOldRoom = ReservationUtil.normalizeRoomName(original.room);
            String normNewRoom = ReservationUtil.normalizeRoomName(newRoom);

            int startHour = ReservationUtil.parseTimeToHour(startTime);
            int endHour = ReservationUtil.parseTimeToHour(endTime);

            StringBuilder sb = new StringBuilder();
            sb.append("CHANGE_RESERVATION_FULL,");
            sb.append(oldFileType).append(",");
            sb.append(newFileType).append(",");
            sb.append(original.userId).append(",");
            sb.append(finalName).append(",");
            sb.append(normOldRoom).append(",");
            sb.append(original.date).append(",");
            sb.append(original.day).append(",");
            sb.append(original.time).append(",");

            for (int hour = startHour; hour <= endHour; hour++) {
                String timeSlot = ReservationUtil.formatTimeSlot(hour);
                sb.append(normNewRoom).append("|")
                        .append(newDate).append("|")
                        .append(newDay).append("|")
                        .append(timeSlot).append("|")
                        .append(purpose).append("|")
                        .append(finalRole).append("|")
                        .append(studentCount).append(";");
            }

            String command = sb.toString();
            out.println(command);
            out.flush();
            System.out.println("[changeReservationOnServer] 전송: " + command);

            String response = in.readLine();
            System.out.println("[changeReservationOnServer] 응답: " + response);

            if (response == null || response.isEmpty()) {
                return "CHANGE_FAILED_NO_RESPONSE";
            }

            return response;
        } catch (IOException e) {
            System.err.println("[changeReservationOnServer] 오류: " + e.getMessage());
            return "CHANGE_FAILED";
        }
    }

    /**
     * 예약 취소 리스너
     */
    /**
     * 예약 취소 리스너
     */
    class CancelReservationListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("[취소버튼] 클릭됨");

            // 1. 테이블에서 선택된 예약 확인
            int selectedRow = view.getReservationTable().getSelectedRow();
            System.out.println("[취소버튼] 선택된 행: " + selectedRow);

            if (selectedRow == -1) {
                view.showMessage("취소할 예약을 선택해주세요.");
                System.out.println("[취소버튼] 예약 미선택으로 중단");
                return;
            }

            if (selectedRow >= originalReservations.size()) {
                view.showMessage("예약 정보를 찾을 수 없습니다.");
                return;
            }

            ReservationInfo reservation = originalReservations.get(selectedRow);

            // 2. 확인 메시지
            int confirm = JOptionPane.showConfirmDialog(
                    view,
                    String.format(
                            "다음 예약을 취소하시겠습니까?\n\n"
                            + "강의실: %s\n"
                            + "날짜: %s (%s)\n"
                            + "시간: %s\n"
                            + "인원: %d명\n\n"
                            + "️ 취소 후 복구할 수 없습니다.",
                            reservation.room, reservation.date, reservation.day,
                            reservation.time, reservation.studentCount
                    ),
                    "예약 취소 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                System.out.println("[취소버튼] 사용자가 취소함");
                return;
            }

            System.out.println("[취소버튼] 사용자 확인 완료, 서버 요청 시작");

            // 3. 서버에 취소 요청
            new Thread(() -> {
                synchronized (serverLock) {
                    try {
                        PrintWriter out = Session.getInstance().getOut();
                        MessageDispatcher dispatcher = MessageDispatcher.getInstance();

                        if (out == null || dispatcher == null) {
                            SwingUtilities.invokeLater(()
                                    -> view.showMessage("서버 연결이 끊어졌습니다.")
                            );
                            return;
                        }

                        //  요청자 ID 추가 - CANCEL_RESERVATION,requesterId,userId,day,date,time,room,userName
                        String requesterId = Session.getInstance().getLoggedInUserId();
                        String command = String.format("CANCEL_RESERVATION,%s,%s,%s,%s,%s,%s,%s",
                                requesterId,       // ✅ 취소 요청한 사람
                                reservation.userId, // 예약 당사자
                                reservation.day,
                                reservation.date,
                                reservation.time,
                                reservation.room,
                                reservation.name
                        );

                        out.println(command);
                        out.flush();
                        System.out.println("[취소버튼] 서버 요청: " + command);

                        //  MessageDispatcher를 통해 응답 받기 (30초 타임아웃)
                        String response = dispatcher.waitForResponse(30);
                        System.out.println("[취소버튼] 서버 응답: " + response);

                        if (response == null) {
                            SwingUtilities.invokeLater(()
                                    -> view.showMessage("서버 응답 타임아웃")
                            );
                            return;
                        }

                    if ("CANCEL_SUCCESS".equals(response)) {
                        // ✅ 즉시 테이블과 목록에서 제거
                        final int rowToRemove = selectedRow;
                        SwingUtilities.invokeLater(() -> {
                            // ✅ 테이블 모델에서 행 제거
                            DefaultTableModel model = (DefaultTableModel) view.getReservationTable().getModel();
                            if (rowToRemove < model.getRowCount()) {
                                model.removeRow(rowToRemove);
                            }
                            // ✅ 메모리에서도 제거
                            if (rowToRemove < originalReservations.size()) {
                                originalReservations.remove(rowToRemove);
                            }
                            
                            view.showMessage(String.format(
                                    "예약이 취소되었습니다.\n\n"
                                    + "강의실: %s\n"
                                    + "날짜: %s (%s)\n"
                                    + "시간: %s",
                                    reservation.room, reservation.date,
                                    reservation.day, reservation.time
                            ));
                            try {
        Thread.sleep(300);
    } catch (InterruptedException ie) {
        ie.printStackTrace();
    }
                            //  캘린더 새로고침 (백그라운드 스레드)
                            new Thread(() -> {
                                synchronized (serverLock) {
                                                loadApprovedReservations();  

                                    try {
                                        // 100ms 대기 후 캘린더 갱신 (서버 처리 시간 고려)
                                        Thread.sleep(100);
                                        
                                        String selectedRoom = view.getSelectedClassRoom();
                                        java.time.LocalDate selectedDate = view.getSelectedDate();
                                        if (selectedDate == null) {
                                            selectedDate = java.time.LocalDate.now().plusDays(1);
                                        }

                                        boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(selectedRoom);
                                        java.time.LocalDate weekStart = ReservationUtil.getWeekStart(selectedDate);
                                        java.time.LocalDate weekEnd = weekStart.plusDays(6);
                                        ReservationUtil.loadWeeklyReservationData(reservedMap, statusMap, selectedRoom, weekStart, weekEnd);

                                        String dateString = selectedDate.toString();
                                        String day = view.getSelectedDay();
                                        String time = view.getSelectedTime();
                                        int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(selectedRoom, dateString, time);

                                        final int finalReservedCapacity = reservedCapacity;
                                        final java.time.LocalDate finalWeekStart = weekStart;
                                        SwingUtilities.invokeLater(() -> {
                                            JTable updatedTable = ReservationUtil.buildCalendarTableWithDates(
                                                    reservedMap, statusMap, selectedRoom, isAvailable, finalWeekStart);
                                            view.updateCalendarTable(updatedTable);
                                            updateCapacityPanelWithData(selectedRoom, day, time, finalReservedCapacity);
                                        });
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }).start();
                        });

                    } else if ("CANCEL_FAILED_NOT_FOUND".equals(response)) {
                        SwingUtilities.invokeLater(()
                                -> view.showMessage("해당 예약을 찾을 수 없습니다.\n이미 취소되었을 수 있습니다.")
                        );
                    } else {
                        SwingUtilities.invokeLater(()
                                -> view.showMessage("예약 취소 실패: " + response)
                        );
                    }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(()
                                -> view.showMessage("오류 발생: " + ex.getMessage())
                        );
                    }
                }
            }).start();
        }
    }

    @Override
    protected String getRoomTypeName() {
        return "강의실/실습실";
    }

    @Override
    protected String getSelectedRoom() {
        return view.getSelectedClassRoom();
    }

    @Override
    protected List<String> loadRoomList() {
        return new ArrayList<>();
    }

    @Override
    protected void setRoomList(List<String> rooms) {
        // Reservationchangeview는 자체적으로 loadClassrooms() 사용
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
        return null; // 사용 안 함
    }

    @Override
    protected JComboBox<String> getRoomComboBox() {
        return null; // 사용 안 함
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
        // 사용 안 함
    }

    @Override
    protected void addReservationListener(ActionListener listener) {
        // 사용 안 함
    }

    @Override
    protected void showMessage(String message) {
        view.showMessage(message);
    }

    @Override
    protected void closeView() {
        view.dispose();
    }

    @Override
    protected void updateCalendarTable(JTable table) {
        view.updateCalendarTable(table);
    }

    @Override
    protected void setCapacityInfoText(String text) {
        view.setCapacityInfoText(text);
    }

    /**
     * 예약 정보 클래스
     */
    static class ReservationInfo {

        String fileType;
        String userId;
        String name;
        String room;
        String date;
        String day;
        String time;
        String purpose;
        String role;
        int studentCount;

        ReservationInfo(String fileType, String userId, String name,
                String room, String date, String day, String time,
                String purpose, String role, int studentCount) {
            this.fileType = fileType;
            this.userId = userId;
            this.name = name;
            this.room = room;
            this.date = date;
            this.day = day;
            this.time = time;
            this.purpose = purpose;
            this.role = role;
            this.studentCount = studentCount;
        }
    }
}
