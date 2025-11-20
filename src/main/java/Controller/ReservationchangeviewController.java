package Controller;

import Controller.ReservationControllerUtil.ValidationResult;
import Model.Session;
import Service.ReservationService;
import Service.ReservationService.*;
import Util.MessageDispatcher;
import Util.ReservationUtil;
import View.Reservationchangeview;
import View.RoomSelect;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * 
 * 서비스 레이어와 유틸리티 클래스를 활용하여 코드 길이를 대폭 단축
 */
public class ReservationchangeviewController extends AbstractReservationController {

    // ============ 상수 정의 ============
    private static final int NOTIFICATION_DELAY_MS = 500;
    private static final int CALENDAR_REFRESH_DELAY_MS = 300;
    private static final String ROOM_SELECTION_PLACEHOLDER = "선택";
    
    // ============ 필드 ============
    private final Reservationchangeview view;
    private final ReservationService reservationService;
    private final List<ReservationDTO> reservations = new ArrayList<>();
    private volatile boolean isProcessing = false;

    // ============ 생성자 ============
    public ReservationchangeviewController(Reservationchangeview view) {
        this(view, new ReservationService());
    }
    
    // 테스트 가능하도록 서비스 주입 지원
    public ReservationchangeviewController(Reservationchangeview view, ReservationService service) {
        this.view = view;
        this.reservationService = service;
        initializeController();
    }

    // ============ 초기화 ============
    
    private void initializeController() {
        setupChangeViewListeners();
        setupNotificationHandler();
        setupTableSelectionListener();
        setupRoomSelectionListeners();
        setupDateTimeListeners();
        performAsyncInitialization();
    }

    /**
     * 예약 변경 화면 전용 리스너 설정
     * (AbstractReservationController의 setupEventListeners와는 다름)
     */
    private void setupChangeViewListeners() {
        view.setChangeButtonActionListener(new ChangeReservationListener());
        view.setBackButtonActionListener(e -> handleBack());
        view.setCancelButtonActionListener(new CancelReservationListener());
    }

    private void setupNotificationHandler() {
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        if (dispatcher != null) {
            dispatcher.setNotificationHandler(msg -> {
                System.out.println("[예약변경화면] 알림 수신: " + msg);
                SwingUtilities.invokeLater(() -> {
                    sleepSafely(NOTIFICATION_DELAY_MS);
                    loadReservations();
                });
            });
        }
    }

    private void setupTableSelectionListener() {
        view.getReservationTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = view.getReservationTable().getSelectedRow();
                if (selectedRow >= 0 && selectedRow < reservations.size()) {
                    ReservationDTO info = reservations.get(selectedRow);
                    view.setReservationId(info.getUserId());
                    System.out.printf("[테이블 클릭] 선택: %s (%s, %s, %s)%n",
                            info.getName(), info.getRoom(), info.getDate(), info.getTime());
                }
            }
        });
    }

    private void setupRoomSelectionListeners() {
        view.getClassRoomTypeComboBox().addActionListener(e -> 
            handleRoomSelection(view.getClassRoomTypeComboBox(), view.getLabRoomTypeComboBox())
        );
        
        view.getLabRoomTypeComboBox().addActionListener(e -> 
            handleRoomSelection(view.getLabRoomTypeComboBox(), view.getClassRoomTypeComboBox())
        );
    }

    private void handleRoomSelection(JComboBox<String> selected, JComboBox<String> other) {
        String selectedItem = (String) selected.getSelectedItem();
        if (selectedItem != null && !selectedItem.equals(ROOM_SELECTION_PLACEHOLDER)) {
            other.setSelectedIndex(0);
            refreshReservationAndAvailability(view.getSelectedClassRoom());
        }
    }

    private void setupDateTimeListeners() {
        view.getDateChooser().addPropertyChangeListener("date", 
            evt -> refreshReservationAndAvailability(view.getSelectedClassRoom())
        );
        view.getTimeComboBox().addActionListener(e -> updateCapacityPanel());
    }

    private void performAsyncInitialization() {
        new Thread(() -> {
            if (!Session.getInstance().isConnected()) {
                showErrorAndClose("서버에 연결되어 있지 않습니다.");
                return;
            }

            synchronized (serverLock) {
                Manager.ClientClassroomManager.getInstance().refreshFromServer();
                loadClassroomsSync();
                loadReservations();
                loadInitialCalendar();
            }
        }).start();
    }

    private void showErrorAndClose(String message) {
        SwingUtilities.invokeLater(() -> {
            view.showMessage(message);
            view.dispose();
        });
    }

    private void loadClassroomsSync() {
        try {
            SwingUtilities.invokeAndWait(() -> view.loadClassrooms());
        } catch (Exception ex) {
            System.err.println("[초기화] 강의실 목록 로드 실패: " + ex.getMessage());
        }
    }

    private void loadInitialCalendar() {
        String selectedRoom = view.getSelectedClassRoom();
        java.time.LocalDate selectedDate = view.getSelectedDate();
        if (selectedDate == null) {
            selectedDate = java.time.LocalDate.now().plusDays(1);
        }

        if (ROOM_SELECTION_PLACEHOLDER.equals(selectedRoom)) {
            showEmptyCalendar(selectedDate);
            return;
        }

        loadCalendarData(selectedRoom, selectedDate);
    }

    private void showEmptyCalendar(java.time.LocalDate date) {
        java.time.LocalDate weekStart = ReservationUtil.getWeekStart(date);
        SwingUtilities.invokeLater(() -> {
            JTable emptyTable = ReservationUtil.buildCalendarTableWithDates(
                    new HashMap<>(), new HashMap<>(), ROOM_SELECTION_PLACEHOLDER, true, weekStart);
            view.updateCalendarTable(emptyTable);
            view.setCapacityInfoText("강의실을 먼저 선택해주세요.");
        });
    }

    private void loadCalendarData(String room, java.time.LocalDate date) {
        boolean isAvailable = ReservationUtil.checkRoomAvailabilitySync(room);
        java.time.LocalDate weekStart = ReservationUtil.getWeekStart(date);
        java.time.LocalDate weekEnd = weekStart.plusDays(6);
        
        ReservationUtil.loadWeeklyReservationData(reservedMap, statusMap, room, weekStart, weekEnd);
        
        int reservedCapacity = ReservationUtil.getApprovedReservedCountForDate(
                room, date.toString(), view.getSelectedTime());

        SwingUtilities.invokeLater(() -> {
            JTable table = ReservationUtil.buildCalendarTableWithDates(
                    reservedMap, statusMap, room, isAvailable, weekStart);
            view.updateCalendarTable(table);
            updateCapacityPanelWithData(room, view.getSelectedDay(), 
                    view.getSelectedTime(), reservedCapacity);
        });
    }

    // ============ 예약 목록 로드 ============
    
    private void loadReservations() {
        try {
            List<ReservationDTO> list = reservationService.getApprovedReservations();
            updateReservationTable(list);
            
            if (list.isEmpty()) {
                SwingUtilities.invokeLater(() -> 
                    view.showMessage("예약 내역이 없습니다.\n\n캘린더를 통해 새로운 예약을 진행해주세요.")
                );
            }
        } catch (Exception e) {
            System.err.println("[loadReservations] 오류: " + e.getMessage());
            SwingUtilities.invokeLater(() -> 
                view.showMessage("예약 목록 조회 실패: " + e.getMessage())
            );
        }
    }

    private void updateReservationTable(List<ReservationDTO> list) {
        SwingUtilities.invokeLater(() -> {
            DefaultTableModel model = (DefaultTableModel) view.getReservationTable().getModel();
            model.setRowCount(0);
            reservations.clear();
            
            for (ReservationDTO dto : list) {
                model.addRow(new Object[]{
                    dto.getRoom(), dto.getDate(), dto.getDay(), dto.getTime(),
                    dto.getPurpose(), dto.getStudentCount() + "명", "승인됨", dto.getUserId()
                });
                reservations.add(dto);
            }
        });
    }

    // ============ 예약 변경 리스너 ============
    
    class ChangeReservationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isProcessing) {
                view.showMessage("예약 변경을 처리 중입니다.\n잠시만 기다려주세요.");
                return;
            }

            ReservationDTO selected = getSelectedReservation();
            if (selected == null) return;

            ChangeData changeData = collectChangeData(selected);
            if (changeData == null) return;

            if (!validateChange(changeData)) return;

            if (!confirmChange(selected, changeData)) return;

            processChangeAsync(selected, changeData);
        }

        private ReservationDTO getSelectedReservation() {
            int row = view.getReservationTable().getSelectedRow();
            if (row == -1) {
                view.showMessage("변경할 예약을 선택해주세요.");
                return null;
            }
            if (row >= reservations.size()) {
                view.showMessage("예약 정보를 찾을 수 없습니다.");
                return null;
            }
            return reservations.get(row);
        }

        private ChangeData collectChangeData(ReservationDTO original) {
            String dateStr = view.getSelectedDateString();
            if (dateStr == null) {
                view.showMessage("예약 날짜를 선택해주세요.");
                return null;
            }

            java.time.LocalDate date = view.getSelectedDate();
            String room = view.getSelectedRoom();
            String day = view.getSelectedDay();
            String startTime = view.getSelectedTime();
            String endTime = view.getSelectedEndTime();
            String purpose = view.getPurpose();
            int count = view.getStudentCount();

            if (purpose == null || purpose.isEmpty()) {
                view.showMessage("사용 목적을 입력해주세요.");
                return null;
            }

            return new ChangeData(date, dateStr, room, day, startTime, endTime, purpose, count);
        }

        private boolean validateChange(ChangeData data) {
            // 날짜 검증
            ValidationResult dateValidation = ReservationControllerUtil.validateReservationDate(data.date);
            if (!dateValidation.isValid) {
                view.showMessage(dateValidation.errorMessage);
                return false;
            }

            // 시간 범위 검증
            String userRole = Session.getInstance().getLoggedInUserRole();
            ValidationResult timeValidation = ReservationControllerUtil.validateTimeRange(
                    data.startTime, data.endTime, userRole);
            if (!timeValidation.isValid) {
                view.showMessage(timeValidation.errorMessage);
                return false;
            }

            // 수용 인원 검증
            ValidationResult capacityValidation = ReservationControllerUtil.validateCapacity(
                    data.room, data.count);
            if (!capacityValidation.isValid) {
                view.showMessage(capacityValidation.errorMessage);
                return false;
            }

            return true;
        }

        private boolean confirmChange(ReservationDTO original, ChangeData data) {
            String message = String.format(
                    "예약을 변경하시겠습니까?\n\n"
                    + "【기존 예약】\n강의실: %s\n날짜: %s (%s)\n시간: %s\n인원: %d명\n\n"
                    + "【변경 후】\n강의실: %s\n날짜: %s (%s)\n시간: %s ~ %s\n인원: %d명\n\n"
                    + "️ 기존 예약이 자동으로 취소되고\n새로운 예약이 대기 상태로 신청됩니다.\n"
                    + "조교의 승인이 다시 필요합니다.",
                    original.getRoom(), original.getDate(), original.getDay(),
                    original.getTime(), original.getStudentCount(),
                    data.room, data.dateStr, data.day,
                    data.startTime, data.endTime, data.count
            );

            int confirm = JOptionPane.showConfirmDialog(view, message, "예약 변경 확인",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            return confirm == JOptionPane.YES_OPTION;
        }

        private void processChangeAsync(ReservationDTO original, ChangeData data) {
            new Thread(() -> {
                isProcessing = true;
                try {
                    String userRole = Session.getInstance().getLoggedInUserRole();
                    ChangeReservationRequest request = new ChangeReservationRequest(
                            original, data.date, data.dateStr, data.room, data.day,
                            data.startTime, data.endTime, data.purpose, userRole, 
                            data.count, reservedMap
                    );

                    ChangeReservationResponse response = reservationService.changeReservation(request);

                    SwingUtilities.invokeLater(() -> {
                        if (response.isSuccess()) {
                            view.showMessage(String.format(
                                    "예약 변경이 완료되었습니다!\n\n강의실: %s\n날짜: %s\n요일: %s\n"
                                    + "시간: %s ~ %s\n사용 인원: %d명\n\n조교의 승인을 기다려주세요.",
                                    data.room, data.dateStr, data.day,
                                    data.startTime, data.endTime, data.count
                            ));
                            // 테이블과 캘린더 모두 갱신
                            refreshAfterChange();
                        } else {
                            view.showMessage("예약 변경 실패!\n\n" + response.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> 
                        view.showMessage("오류 발생: " + ex.getMessage())
                    );
                } finally {
                    isProcessing = false;
                }
            }).start();
        }
    }

    // ============ 예약 취소 리스너 ============
    
    class CancelReservationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ReservationDTO selected = getSelectedReservation();
            if (selected == null) return;

            if (!confirmCancel(selected)) return;

            processCancelAsync(selected);
        }

        private ReservationDTO getSelectedReservation() {
            int row = view.getReservationTable().getSelectedRow();
            if (row == -1) {
                view.showMessage("취소할 예약을 선택해주세요.");
                return null;
            }
            if (row >= reservations.size()) {
                view.showMessage("예약 정보를 찾을 수 없습니다.");
                return null;
            }
            return reservations.get(row);
        }

        private boolean confirmCancel(ReservationDTO reservation) {
            String message = String.format(
                    "다음 예약을 취소하시겠습니까?\n\n강의실: %s\n날짜: %s (%s)\n시간: %s\n"
                    + "인원: %d명\n\n️ 취소 후 복구할 수 없습니다.",
                    reservation.getRoom(), reservation.getDate(), reservation.getDay(),
                    reservation.getTime(), reservation.getStudentCount()
            );

            int confirm = JOptionPane.showConfirmDialog(view, message, "예약 취소 확인",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            return confirm == JOptionPane.YES_OPTION;
        }

        private void processCancelAsync(ReservationDTO reservation) {
            int selectedRow = view.getReservationTable().getSelectedRow();
            
            new Thread(() -> {
                synchronized (serverLock) {
                    CancelReservationResponse response = 
                            reservationService.cancelReservation(reservation);

                    SwingUtilities.invokeLater(() -> {
                        if (response.isSuccess()) {
                            removeFromTable(selectedRow);
                            view.showMessage(String.format(
                                    "예약이 취소되었습니다.\n\n강의실: %s\n날짜: %s (%s)\n시간: %s",
                                    reservation.getRoom(), reservation.getDate(),
                                    reservation.getDay(), reservation.getTime()
                            ));
                            refreshCalendarAfterCancel();
                        } else {
                            view.showMessage("예약 취소 실패: " + response.getMessage());
                        }
                    });
                }
            }).start();
        }

        private void removeFromTable(int row) {
            DefaultTableModel model = (DefaultTableModel) view.getReservationTable().getModel();
            if (row < model.getRowCount()) {
                model.removeRow(row);
            }
            if (row < reservations.size()) {
                reservations.remove(row);
            }
        }

        private void refreshCalendarAfterCancel() {
            sleepSafely(CALENDAR_REFRESH_DELAY_MS);
            new Thread(() -> {
                synchronized (serverLock) {
                    loadReservations();
                    sleepSafely(100);
                    
                    String room = view.getSelectedClassRoom();
                    java.time.LocalDate date = view.getSelectedDate();
                    if (date == null) date = java.time.LocalDate.now().plusDays(1);
                    
                    loadCalendarData(room, date);
                }
            }).start();
        }
    }
    
    /**
     * 예약 변경 후 테이블과 캘린더를 모두 갱신
     */
    private void refreshAfterChange() {
        new Thread(() -> {
            sleepSafely(500);  // 서버 응답 처리 대기
            
            synchronized (serverLock) {
                // 1. 예약 목록(테이블) 갱신
                loadReservations();
                System.out.println("[변경완료] 예약 목록 새로고침 완료");
                
                sleepSafely(100);
                
                // 2. 캘린더 갱신
                String room = view.getSelectedClassRoom();
                java.time.LocalDate date = view.getSelectedDate();
                if (date == null) {
                    date = java.time.LocalDate.now().plusDays(1);
                }
                
                loadCalendarData(room, date);
                System.out.println("[변경완료] 캘린더 새로고침 완료");
            }
        }).start();
    }
    
    private void handleBack() {
        view.dispose();
        RoomSelect roomSelect = RoomSelect.getInstance();
        new RoomSelectController(roomSelect);
        roomSelect.setVisible(true);
    }

    private void sleepSafely(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ============ 내부 클래스 ============
    
    private static class ChangeData {
        final java.time.LocalDate date;
        final String dateStr;
        final String room;
        final String day;
        final String startTime;
        final String endTime;
        final String purpose;
        final int count;

        ChangeData(java.time.LocalDate date, String dateStr, String room, String day,
                String startTime, String endTime, String purpose, int count) {
            this.date = date;
            this.dateStr = dateStr;
            this.room = room;
            this.day = day;
            this.startTime = startTime;
            this.endTime = endTime;
            this.purpose = purpose;
            this.count = count;
        }
    }

    // ============ AbstractReservationController 오버라이드 ============
    
    @Override protected String getRoomTypeName() { return "강의실/실습실"; }
    @Override protected String getSelectedRoom() { return view.getSelectedClassRoom(); }
    @Override protected List<String> loadRoomList() { return new ArrayList<>(); }
    @Override protected void setRoomList(List<String> rooms) { }
    @Override protected String getSelectedDateString() { return view.getSelectedDateString(); }
    @Override protected java.time.LocalDate getSelectedDate() { return view.getSelectedDate(); }
    @Override protected String getSelectedDay() { return view.getSelectedDay(); }
    @Override protected String getSelectedTime() { return view.getSelectedTime(); }
    @Override protected String getSelectedEndTime() { return view.getSelectedEndTime(); }
    @Override protected String getPurpose() { return view.getPurpose(); }
    @Override protected int getStudentCount() { return view.getStudentCount(); }
    @Override protected JButton getBeforeButton() { return null; }
    @Override protected JComboBox<String> getRoomComboBox() { return null; }
    @Override protected JComboBox<String> getTimeComboBox() { return view.getTimeComboBox(); }
    @Override protected com.toedter.calendar.JDateChooser getDateChooser() { return view.getDateChooser(); }
    @Override protected void resetReservationButtonListener() { }
    @Override protected void addReservationListener(ActionListener listener) { }
    @Override protected void showMessage(String message) { view.showMessage(message); }
    @Override protected void closeView() { view.dispose(); }
    @Override protected void updateCalendarTable(JTable table) { view.updateCalendarTable(table); }
    @Override protected void setCapacityInfoText(String text) { view.setCapacityInfoText(text); }
}
