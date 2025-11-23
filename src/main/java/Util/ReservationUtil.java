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
     * @param date 기준 날짜
     * @return 해당 주의 월요일 날짜
     */
    public static LocalDate getWeekStart(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int daysToSubtract = dayOfWeek.getValue() - 1; // 월요일=1
        return date.minusDays(daysToSubtract);
    }
    
    /**
     * 시간 문자열을 교시 번호로 변환
     * @param timeString "1교시(09:00~10:00)" 또는 "1교시" 형식
     * @return 교시 번호 (1~9), 파싱 실패 시 1 반환
     */
    public static int parseTimeToHour(String timeString) {
        try {
            // "교시" 앞의 숫자만 가져오기
            int kyosiIndex = timeString.indexOf("교시");
            if (kyosiIndex > 0) {
                String hourStr = timeString.substring(0, kyosiIndex).replaceAll("[^0-9]", "");
                return Integer.parseInt(hourStr);
            }
            // "교시"가 없으면 첫 숫자만 추출
            String hourStr = timeString.replaceAll("[^0-9]", "");
            if (hourStr.length() > 0) {
                return Integer.parseInt(hourStr.substring(0, 1));
            }
            return 1;
        } catch (NumberFormatException e) {
            System.err.println("[parseTimeToHour] 파싱 실패: " + timeString);
            return 1; // 기본값
        }
    }
    
    /**
     * 교시 번호를 시간 문자열로 포맷팅
     * @param hour 교시 번호 (1~9)
     * @return "1교시(09:00~10:00)" 형식의 문자열
     */
    public static String formatTimeSlot(int hour) {
        if (hour >= 1 && hour <= 9) {
            return TIME_SLOTS[hour - 1];
        }
        return hour + "교시";
    }
    
    /**
     * LocalDate를 요일 문자열로 변환
     * @param date 날짜
     * @return "월", "화", "수", "목", "금", "토", "일"
     */
    public static String getDayName(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1(월)~7(일)
        return DAY_NAMES[dayOfWeek - 1];
    }
    
    /**
     * 방 이름 정규화 (끝에 "호" 붙이기)
     * @param roomName 원본 방 이름
     * @return 정규화된 방 이름 (예: "908" -> "908호")
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
     * @param reservedMap 예약 맵 (방 이름 -> 예약된 시간 Set)
     * @param room 방 이름
     * @param date 날짜
     * @param time 시간 (교시)
     * @return 예약 여부
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
     * @param statusMap 상태 맵 (방 이름 -> 시간_상태 Map)
     * @param room 방 이름
     * @param date 날짜
     * @param time 시간 (교시)
     * @return 예약 상태 (null, "예약됨", "대기중")
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
     * @param roomName 방 이름
     * @return 사용 가능 여부 (true: 가능, false: 불가능)
     */
    public static boolean checkRoomAvailabilitySync(String roomName) {
        try {
            // 1. 세션 연결 상태 확인
            if (!Session.getInstance().isConnected()) {
                System.err.println("[checkRoomAvailabilitySync] 서버 연결 없음 - 기본값 true 반환");
                return true;
            }

            PrintWriter out = Session.getInstance().getOut();
            BufferedReader in = Session.getInstance().getIn();

            if (out == null || in == null) {
                System.err.println("[checkRoomAvailabilitySync] 통신 객체가 null - 기본값 true 반환");
                return true;
            }

            // 2. 버퍼에 남은 이전 메시지 비우기
            try {
                Socket socket = Session.getInstance().getSocket();
                if (socket != null) {
                    socket.setSoTimeout(100); // 100ms 타임아웃
                    while (in.ready()) {
                        String discarded = in.readLine();
                        System.out.println("[checkRoomAvailabilitySync] 버퍼에서 버린 메시지: " + discarded);
                    }
                    socket.setSoTimeout(5000); // 다시 5초로 설정
                }
            } catch (Exception e) {
                // 버퍼 비우기 실패는 무시
            }

            // 3. 방 이름 정제
            String cleanRoomName = normalizeRoomName(roomName);

            // 4. 서버에 명령 전송
            String command = "CHECK_ROOM_STATUS," + cleanRoomName;
            out.println(command);
            out.flush();
            System.out.println("[checkRoomAvailabilitySync] 서버 전송: " + command);

            // 5. 응답 대기
            String response = null;
            try {
                response = in.readLine();
                System.out.println("[checkRoomAvailabilitySync] 서버 응답: " + response);
            } catch (java.net.SocketTimeoutException e) {
                System.err.println("[checkRoomAvailabilitySync] 타임아웃 - 기본값 true 반환");
                return true;
            }

            if (response == null || response.isEmpty()) {
                System.err.println("[checkRoomAvailabilitySync] 응답 없음 - 기본값 true 반환");
                return true;
            }

            // 6. SUCCESS 응답 처리 (로그인 잔류 응답인 경우)
            if ("SUCCESS".equals(response)) {
                System.out.println("[checkRoomAvailabilitySync] SUCCESS는 잔류 응답 - 재시도");
                out.println(command);
                out.flush();
                response = in.readLine();
                System.out.println("[checkRoomAvailabilitySync] 재시도 응답: " + response);
            }

            // 7. 최종 응답 처리
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

        } catch (IOException e) {
            System.err.println("[checkRoomAvailabilitySync] IOException: " + e.getMessage());
            return true;
        } catch (Exception e) {
            System.err.println("[checkRoomAvailabilitySync] 예외: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 서버로부터 주간 예약 데이터 로드 (상태 정보 포함)
     * @param reservedMap 예약 맵 (방 이름 -> 예약된 시간 Set)
     * @param statusMap 상태 맵 (방 이름 -> 시간_상태 Map)
     * @param roomName 방 이름
     * @param weekStart 주 시작일
     * @param weekEnd 주 종료일
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
        BufferedReader in = Session.getInstance().getIn();

        if (out == null || in == null) {
            System.err.println("[loadWeeklyReservationData] 입출력 스트림이 null");
            return;
        }

        try {
            String normalizedRoom = normalizeRoomName(roomName);
            // Thread-safe Set으로 초기화
            reservedMap.put(normalizedRoom, ConcurrentHashMap.newKeySet());
            statusMap.put(normalizedRoom, new ConcurrentHashMap<>());

            System.out.printf("[loadWeeklyReservationData] %s %s ~ %s 예약 정보 요청%n", 
                normalizedRoom, weekStart.toString(), weekEnd.toString());

            // 주간 예약 조회 요청
            out.println(String.format("VIEW_WEEKLY_RESERVATION,%s,%s,%s", 
                roomName, weekStart.toString(), weekEnd.toString()));
            out.flush();

            String line;
            int readCount = 0;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_OF_RESERVATION")) {
                    break;
                }

                String[] parts = line.split(",");
                // 9개 필드: name,room,date,day,time,purpose,role,status,studentCount
                if (parts.length >= 9) {
                    String status = parts[7].trim();
                    if (status.equals("예약됨") || status.equals("대기중")) {
                        String room = normalizeRoomName(parts[1].trim());
                        String dateString = parts[2].trim();  // 날짜
                        String day = parts[3].trim().replace("요일", "");  // 요일
                        String time = parts[4].trim();
                        if (time.length() >= 3) {
                            time = time.substring(0, 3);
                        }

                        String key = dateString + "_" + day + "_" + time;
                        
                        // Thread-safe: ConcurrentHashMap.newKeySet() 사용
                        reservedMap.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet())
                            .add(key);
                        
                        // 상태 정보 저장
                        statusMap.computeIfAbsent(room, k -> new ConcurrentHashMap<>())
                            .put(key, status);
                        
                        readCount++;
                    }
                }
            }

            System.out.printf("[loadWeeklyReservationData] %s - %d개 완료%n", normalizedRoom, readCount);

        } catch (IOException e) {
            System.err.println("[loadWeeklyReservationData] 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 서버로부터 주간 예약 데이터 로드 (상태 정보 없는 버전 - 하위 호환성)
     * @param reservedMap 예약 맵 (방 이름 -> 예약된 시간 Set)
     * @param roomName 방 이름
     * @param weekStart 주 시작일
     * @param weekEnd 주 종료일
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
     * @param room 방 이름
     * @param dateString 날짜 문자열 (YYYY-MM-DD)
     * @param time 시간 (교시)
     * @return 승인된 예약 인원 수
     */
    public static int getApprovedReservedCountForDate(String room, String dateString, String time) {
        if (!Session.getInstance().isConnected()) {
            System.err.println("[getApprovedReservedCountForDate] 서버 연결 없음");
            return 0;
        }

        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();

        try {
            out.println(String.format("GET_RESERVED_COUNT_BY_DATE,%s,%s,%s", room, dateString, time));
            out.flush();

            String response = in.readLine();
            if (response != null && response.startsWith("RESERVED_COUNT:")) {
                int count = Integer.parseInt(response.substring("RESERVED_COUNT:".length()));
                System.out.println(String.format("[getApprovedReservedCountForDate] %s %s %s = %d명", 
                    room, dateString, time, count));
                return count;
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[getApprovedReservedCountForDate] 오류: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 서버로 예약 요청 전송 (Builder Pattern 사용)
     * @param request ReservationRequest 객체
     * @return 서버 응답 ("RESERVE_SUCCESS" 등)
     */
    public static String sendReservationRequestToServer(ReservationRequest request) {
        if (!Session.getInstance().isConnected()) {
            System.err.println("[sendReservationRequestToServer] 서버 연결 없음");
            return "RESERVE_FAILED";
        }
        PrintWriter out = Session.getInstance().getOut();
        BufferedReader in = Session.getInstance().getIn();
        try {
            // Builder Pattern으로 생성된 객체에서 프로토콜 문자열 가져오기
            String requestLine = request.toProtocolString();
            System.out.println("[sendReservationRequestToServer] 요청: " + request);
            
            out.println(requestLine);
            out.flush();
            
            String response = in.readLine();
            if (response == null) {
                System.err.println("[sendReservationRequestToServer] 서버 응답 없음");
                return "RESERVE_FAILED";
            }
            System.out.println("[sendReservationRequestToServer] 서버 응답: " + response);
            return response;
        } catch (IOException e) {
            System.err.println("[sendReservationRequestToServer] 오류: " + e.getMessage());
            return "RESERVE_FAILED";
        }
    }
    
    /**
     * 서버로 예약 요청 전송 (레거시 메서드 - 하위 호환성)
     * @deprecated Builder Pattern을 사용하는 sendReservationRequestToServer(ReservationRequest)를 사용하세요
     */
    @Deprecated
    public static String sendReservationRequestToServer(String name, String room, 
                                                       String dateString, String day, 
                                                       String time, String purpose, 
                                                       String role, int studentCount) {
        // Builder Pattern으로 변환하여 호출
        // ★ 여기가 수정된 부분입니다: userId를 추가로 가져와서 설정함
        String userId = Session.getInstance().getLoggedInUserId(); 
        
        ReservationRequest request = new ReservationRequest.Builder(name, room, dateString)
            .day(day)
            .time(time)
            .purpose(purpose)
            .userRole(role)
            .studentCount(studentCount)
            .userId(userId) // ★ 여기도 추가됨
            .build();
        
        return sendReservationRequestToServer(request);
    }

    /**
     * 날짜가 포함된 캘린더 테이블 생성 (상태 정보 포함)
     * @param reservedMap 예약 맵
     * @param statusMap 상태 맵
     * @param room 방 이름
     * @param roomAvailable 방 사용 가능 여부
     * @param weekStart 주 시작일
     * @return 생성된 JTable
     */
    public static JTable buildCalendarTableWithDates(Map<String, Set<String>> reservedMap,
                                                    Map<String, Map<String, String>> statusMap,
                                                    String room, 
                                                    boolean roomAvailable, 
                                                    LocalDate weekStart) {
        String[] columnNames = {"교시", "월", "화", "수", "목", "금", "토", "일"};
        String[] times = {"1교시", "2교시", "3교시", "4교시", "5교시", "6교시", "7교시", "8교시", "9교시"};

        // 각 요일의 날짜 계산 (월~일 7일)
        LocalDate[] weekDates = new LocalDate[7];
        for (int i = 0; i < 7; i++) {
            weekDates[i] = weekStart.plusDays(i);
        }

        DefaultTableModel model = new DefaultTableModel(times.length, columnNames.length);
        
        // 헤더에 날짜 포함
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
                        
                        // 상태에 따른 색상 구분
                        String status = getReservationStatus(statusMap, room, date, time);
                        
                        if ("예약됨".equals(status)) {
                            // 확정된 예약 = 초록색
                            cell.setBackground(new Color(76, 175, 80));  // Material Green
                            cell.setText("");
                        } else if ("대기중".equals(status)) {
                            // 대기중인 예약 = 노란색
                            cell.setBackground(new Color(255, 235, 59));  // Material Yellow
                            cell.setText("");
                        } else {
                            // 비어있음 = 흰색
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
     * @param reservedMap 예약 맵
     * @param room 방 이름
     * @param roomAvailable 방 사용 가능 여부
     * @param weekStart 주 시작일
     * @return 생성된 JTable
     */
    public static JTable buildCalendarTableWithDates(Map<String, Set<String>> reservedMap,
                                                    String room, 
                                                    boolean roomAvailable, 
                                                    LocalDate weekStart) {
        Map<String, Map<String, String>> dummyStatusMap = new ConcurrentHashMap<>();
        return buildCalendarTableWithDates(reservedMap, dummyStatusMap, room, roomAvailable, weekStart);
    }
    
}