package Util;

import common.builder.ReservationRequest;
import Model.Session;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.table.*;

/**
 * 강의실/실습실 예약 관련 공통 유틸리티 클래스
 * ReservClassController와 ReservLabController에서 공통으로 사용하는 메서드들을 모아둠
 */
public class ReservationUtil {
    
    // 요일 이름 배열 (월~일)
    private static final String[] DAY_NAMES = {"월", "화", "수", "목", "금", "토", "일"};
    
    // 교시 시간 슬롯 배열
    private static final String[] TIME_SLOTS = {
        "1교시(09:00~10:00)", "2교시(10:00~11:00)", "3교시(11:00~12:00)",
        "4교시(12:00~13:00)", "5교시(13:00~14:00)", "6교시(14:00~15:00)",
        "7교시(15:00~16:00)", "8교시(16:00~17:00)", "9교시(17:00~18:00)"
    };
    
    /**
     * 주의 시작일 계산 (월요일 기준)
     */
    public static LocalDate getWeekStart(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int daysToSubtract = dayOfWeek.getValue() - 1;
        return date.minusDays(daysToSubtract);
    }
    
    /**
     * 시간 문자열을 교시 번호로 변환
     */
    public static int parseTimeToHour(String timeString) {
        try {
            int kyosiIndex = timeString.indexOf("교시");
            if (kyosiIndex > 0) {
                String hourStr = timeString.substring(0, kyosiIndex).replaceAll("[^0-9]", "");
                return Integer.parseInt(hourStr);
            }
            String hourStr = timeString.replaceAll("[^0-9]", "");
            if (hourStr.length() > 0) {
                return Integer.parseInt(hourStr.substring(0, 1));
            }
            return 1;
        } catch (NumberFormatException e) {
            System.err.println("[parseTimeToHour] 파싱 실패: " + timeString);
            return 1;
        }
    }
    
    /**
     * 교시 번호를 시간 문자열로 포맷팅
     */
    public static String formatTimeSlot(int hour) {
        if (hour >= 1 && hour <= 9) {
            return TIME_SLOTS[hour - 1];
        }
        return hour + "교시";
    }
    
    /**
     * LocalDate를 요일 문자열로 변환
     */
    public static String getDayName(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return DAY_NAMES[dayOfWeek - 1];
    }
    
    /**
     * 방 이름 정규화 (끝에 "호" 붙이기)
     */
    public static String normalizeRoomName(String roomName) {
        if (roomName == null || roomName.isEmpty()) {
            return roomName;
        }
        
        String normalized = roomName.replace(" ", "").trim();
        
        if (!normalized.endsWith("호")) {
            normalized = normalized + "호";
        }
        
        return normalized;
    }
    
    /**
     * 특정 날짜/시간의 예약 여부 확인
     */
    public static boolean isReservedOnDate(Map<String, Set<String>> reservedMap, 
                                          String room, LocalDate date, String time) {
        room = normalizeRoomName(room);
        time = time.length() >= 3 ? time.substring(0, 3) : time;
        
        String day = getDayName(date);
        String key = date.toString() + "_" + day + "_" + time;
        
        Set<String> reservedTimes = reservedMap.get(room);
        boolean result = reservedTimes != null && reservedTimes.contains(key);
        
        if (result) {
            System.out.printf("[isReservedOnDate] %s %s %s = 예약됨%n", room, date, time);
        }
        
        return result;
    }
    
    /**
     * 특정 날짜/시간의 예약 상태 확인
     */
    public static String getReservationStatus(Map<String, Map<String, String>> statusMap,
                                             String room, LocalDate date, String time) {
        room = normalizeRoomName(room);
        time = time.length() >= 3 ? time.substring(0, 3) : time;
        
        String day = getDayName(date);
        String key = date.toString() + "_" + day + "_" + time;
        
        Map<String, String> timeStatus = statusMap.get(room);
        if (timeStatus != null) {
            return timeStatus.get(key);
        }
        return null;
    }
    
    /**
     * 서버로부터 방의 사용 가능 여부 확인 (동기 방식)
     * MessageDispatcher 사용
     */
    public static boolean checkRoomAvailabilitySync(String roomName) {
        try {
            if (!Session.getInstance().isConnected()) {
                System.err.println("[checkRoomAvailabilitySync] 서버 연결 없음");
                return true;
            }

            PrintWriter out = Session.getInstance().getOut();
            MessageDispatcher dispatcher = MessageDispatcher.getInstance();

            if (out == null || dispatcher == null) {
                System.err.println("[checkRoomAvailabilitySync] 통신 객체가 null");
                return true;
            }

            String cleanRoomName = normalizeRoomName(roomName);
            String command = "CHECK_ROOM_STATUS," + cleanRoomName;
            
            out.println(command);
            out.flush();
            System.out.println("[checkRoomAvailabilitySync] 서버 전송: " + command);

            // MessageDispatcher를 통해 응답 대기 (30초 타임아웃)
            String response = dispatcher.waitForResponse(30);
            
            if (response == null) {
                System.err.println("[checkRoomAvailabilitySync] 타임아웃 - 기본값 true 반환");
                return true;
            }
            
            System.out.println("[checkRoomAvailabilitySync] 서버 응답: " + response);

            switch (response) {
                case "AVAILABLE":
                    System.out.println("[checkRoomAvailabilitySync] " + cleanRoomName + " - 사용 가능");
                    return true;

                case "UNAVAILABLE":
                    System.out.println("[checkRoomAvailabilitySync] " + cleanRoomName + " - 사용 불가!");
                    return false;

                default:
                    System.err.println("[checkRoomAvailabilitySync] 알 수 없는 응답: " + response);
                    return true;
            }

        } catch (Exception e) {
            System.err.println("[checkRoomAvailabilitySync] 예외: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 서버로부터 주간 예약 데이터 로드 (상태 정보 포함)
     * MessageDispatcher 사용
     */
    public static void loadWeeklyReservationData(Map<String, Set<String>> reservedMap,
                                                Map<String, Map<String, String>> statusMap,
                                                String roomName, 
                                                LocalDate weekStart, 
                                                LocalDate weekEnd) {
        if (!Session.getInstance().isConnected()) {
            System.err.println("[loadWeeklyReservationData] 서버 연결 없음");
            return;
        }

        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();

        if (out == null || dispatcher == null) {
            System.err.println("[loadWeeklyReservationData] 입출력 스트림이 null");
            return;
        }

        try {
            String normalizedRoom = normalizeRoomName(roomName);
            reservedMap.put(normalizedRoom, ConcurrentHashMap.newKeySet());
            statusMap.put(normalizedRoom, new ConcurrentHashMap<>());

            System.out.printf("[loadWeeklyReservationData] %s %s ~ %s 예약 정보 요청%n", 
                normalizedRoom, weekStart.toString(), weekEnd.toString());

            out.println(String.format("VIEW_WEEKLY_RESERVATION,%s,%s,%s", 
                roomName, weekStart.toString(), weekEnd.toString()));
            out.flush();

            int readCount = 0;
            while (true) {
                // MessageDispatcher를 통해 응답 대기
                String line = dispatcher.waitForResponse(30);
                
                if (line == null) {
                    System.err.println("[loadWeeklyReservationData] 타임아웃");
                    break;
                }
                
                if (line.equals("END_OF_RESERVATION")) {
                    break;
                }

                String[] parts = line.split(",");
                if (parts.length >= 9) {
                    String status = parts[7].trim();
                    if (status.equals("예약됨") || status.equals("대기중")) {
                        String room = normalizeRoomName(parts[1].trim());
                        String dateString = parts[2].trim();
                        String day = parts[3].trim().replace("요일", "");
                        String time = parts[4].trim();
                        if (time.length() >= 3) {
                            time = time.substring(0, 3);
                        }

                        String key = dateString + "_" + day + "_" + time;
                        
                        reservedMap.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet())
                            .add(key);
                        
                        statusMap.computeIfAbsent(room, k -> new ConcurrentHashMap<>())
                            .put(key, status);
                        
                        readCount++;
                    }
                }
            }

            System.out.printf("[loadWeeklyReservationData] %s - %d개 완료%n", normalizedRoom, readCount);

        } catch (Exception e) {
            System.err.println("[loadWeeklyReservationData] 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 서버로부터 주간 예약 데이터 로드 (상태 정보 없는 버전 - 하위 호환성)
     */
    public static void loadWeeklyReservationData(Map<String, Set<String>> reservedMap,
                                                String roomName, 
                                                LocalDate weekStart, 
                                                LocalDate weekEnd) {
        Map<String, Map<String, String>> dummyStatusMap = new ConcurrentHashMap<>();
        loadWeeklyReservationData(reservedMap, dummyStatusMap, roomName, weekStart, weekEnd);
    }
    
    /**
     * 서버로부터 특정 날짜/시간의 승인된 예약 인원 수 조회
     * MessageDispatcher 사용
     */
    public static int getApprovedReservedCountForDate(String room, String dateString, String time) {
        if (!Session.getInstance().isConnected()) {
            System.err.println("[getApprovedReservedCountForDate] 서버 연결 없음");
            return 0;
        }

        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();

        if (out == null || dispatcher == null) {
            return 0;
        }

        try {
            out.println(String.format("GET_RESERVED_COUNT_BY_DATE,%s,%s,%s", room, dateString, time));
            out.flush();

            // MessageDispatcher를 통해 응답 대기
            String response = dispatcher.waitForResponse(30);
            
            if (response != null && response.startsWith("RESERVED_COUNT:")) {
                int count = Integer.parseInt(response.substring("RESERVED_COUNT:".length()));
                System.out.println(String.format("[getApprovedReservedCountForDate] %s %s %s = %d명", 
                    room, dateString, time, count));
                return count;
            }
        } catch (Exception e) {
            System.err.println("[getApprovedReservedCountForDate] 오류: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 서버로 예약 요청 전송 (Builder Pattern 사용)
     * MessageDispatcher 사용
     * @param request ReservationRequest 객체
     * @return 서버 응답 ("RESERVE_SUCCESS" 등)
     */
    public static String sendReservationRequestToServer(ReservationRequest request) {
        if (!Session.getInstance().isConnected()) {
            System.err.println("[sendReservationRequestToServer] 서버 연결 없음");
            return "RESERVE_FAILED";
        }

        PrintWriter out = Session.getInstance().getOut();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();

        if (out == null || dispatcher == null) {
            return "RESERVE_FAILED";
        }

        try {
            // Builder Pattern으로 생성된 객체에서 프로토콜 문자열 가져오기
            String requestLine = request.toProtocolString();
            System.out.println("[sendReservationRequestToServer] 요청: " + request);
            
            out.println(requestLine);
            out.flush();

            // MessageDispatcher를 통해 응답 대기
            String response = dispatcher.waitForResponse(30);
            
            if (response == null) {
                System.err.println("[sendReservationRequestToServer] 서버 응답 없음");
                return "RESERVE_FAILED";
            }
            System.out.println("[sendReservationRequestToServer] 서버 응답: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("[sendReservationRequestToServer] 오류: " + e.getMessage());
            return "RESERVE_FAILED";
        }
    }

    /**
     * 날짜가 포함된 캘린더 테이블 생성 (상태 정보 포함)
     */
    public static JTable buildCalendarTableWithDates(Map<String, Set<String>> reservedMap,
                                                    Map<String, Map<String, String>> statusMap,
                                                    String room, 
                                                    boolean roomAvailable, 
                                                    LocalDate weekStart) {
        String[] columnNames = {"교시", "월", "화", "수", "목", "금", "토", "일"};
        String[] times = {"1교시", "2교시", "3교시", "4교시", "5교시", "6교시", "7교시", "8교시", "9교시"};

        LocalDate[] weekDates = new LocalDate[7];
        for (int i = 0; i < 7; i++) {
            weekDates[i] = weekStart.plusDays(i);
        }

        DefaultTableModel model = new DefaultTableModel(times.length, columnNames.length);
        
        String[] headerWithDates = new String[columnNames.length];
        headerWithDates[0] = "교시";
        for (int i = 1; i < columnNames.length; i++) {
            LocalDate date = weekDates[i - 1];
            headerWithDates[i] = String.format("%s\n%02d/%02d", 
                columnNames[i], date.getMonthValue(), date.getDayOfMonth());
        }
        model.setColumnIdentifiers(headerWithDates);

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(Color.GRAY);

        TableColumn firstColumn = table.getColumnModel().getColumn(0);
        firstColumn.setPreferredWidth(60);
        firstColumn.setMaxWidth(60);
        firstColumn.setMinWidth(60);

        for (int i = 0; i < times.length; i++) {
            model.setValueAt(times[i], i, 0);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                                                         boolean isSelected, boolean hasFocus, 
                                                         int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

                if (column == 0) {
                    cell.setBackground(Color.LIGHT_GRAY);
                    cell.setHorizontalAlignment(JLabel.CENTER);
                    cell.setText(value != null ? value.toString() : "");
                } else {
                    if (!roomAvailable) {
                        cell.setBackground(Color.DARK_GRAY);
                        cell.setText("X");
                        cell.setForeground(Color.WHITE);
                    } else {
                        LocalDate date = weekDates[column - 1];
                        String time = times[row];
                        
                        String status = getReservationStatus(statusMap, room, date, time);
                        
                        if ("예약됨".equals(status)) {
                            cell.setBackground(new Color(76, 175, 80));
                            cell.setText("");
                        } else if ("대기중".equals(status)) {
                            cell.setBackground(new Color(255, 235, 59));
                            cell.setText("");
                        } else {
                            cell.setBackground(Color.WHITE);
                            cell.setText("");
                        }
                    }
                }
                return cell;
            }
        });

        return table;
    }
    
    /**
     * 날짜가 포함된 캘린더 테이블 생성 (상태 정보 없는 버전 - 하위 호환성)
     */
    public static JTable buildCalendarTableWithDates(Map<String, Set<String>> reservedMap,
                                                    String room, 
                                                    boolean roomAvailable, 
                                                    LocalDate weekStart) {
        Map<String, Map<String, String>> dummyStatusMap = new ConcurrentHashMap<>();
        return buildCalendarTableWithDates(reservedMap, dummyStatusMap, room, roomAvailable, weekStart);
    }
}
