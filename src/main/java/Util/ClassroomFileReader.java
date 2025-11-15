package Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classrooms.txt 파일을 읽어서 강의실/실습실 목록을 반환하는 유틸리티 클래스
 */
public class ClassroomFileReader {
    
    private static final String FILE_PATH = "data/Classrooms.txt";
    
    /**
     * CLASS 타입 강의실 목록 조회
     */
    public static List<String> loadClassrooms() {
        return loadRoomsByType("CLASS");
    }
    
    /**
     * LAB 타입 실습실 목록 조회
     */
    public static List<String> loadLabs() {
        return loadRoomsByType("LAB");
    }
    
    /**
     * 특정 타입의 방 목록 조회
     * @param roomType "CLASS" 또는 "LAB"
     * @return 방 이름 목록
     */
    private static List<String> loadRoomsByType(String roomType) {
        List<String> rooms = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 주석이나 빈 줄 건너뛰기
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // 형식: 이름,타입(CLASS/LAB),수용인원
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String roomName = parts[0].trim();
                    String type = parts[1].trim();
                    
                    // 지정된 타입만 추가
                    if (roomType.equals(type)) {
                        rooms.add(roomName);
                    }
                }
            }
            
            System.out.println(roomType + " 목록 로드 완료: " + rooms.size() + "개");
            
        } catch (IOException e) {
            System.err.println(roomType + " 목록 로드 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
        
        return rooms;
    }
    
    /**
     * 모든 강의실/실습실 정보 조회 (타입 구분 없이)
     */
    public static List<RoomInfo> loadAllRooms() {
        List<RoomInfo> rooms = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String name = parts[0].trim();
                    String type = parts[1].trim();
                    int capacity = Integer.parseInt(parts[2].trim());
                    
                    rooms.add(new RoomInfo(name, type, capacity));
                }
            }
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("전체 방 목록 로드 중 오류: " + e.getMessage());
        }
        
        return rooms;
    }
    
    /**
     * 방 정보를 담는 내부 클래스
     */
    public static class RoomInfo {
        public final String name;
        public final String type;
        public final int capacity;
        
        public RoomInfo(String name, String type, int capacity) {
            this.name = name;
            this.type = type;
            this.capacity = capacity;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%s, %d명)", name, type, capacity);
        }
    }
}
