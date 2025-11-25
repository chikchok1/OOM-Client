package Util;

import Manager.ClientClassroomManager;
import common.dto.ClassroomDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 강의실/실습실 목록을 서버로부터 가져오는 유틸리티 클래스
 * ✅ 파일 직접 접근 제거 → 서버 통신으로 대체
 */
public class ClassroomFileReader {
    
    /**
     * CLASS 타입 강의실 목록 조회
     */
    public static List<String> loadClassrooms() {
        ClientClassroomManager manager = ClientClassroomManager.getInstance();
        String[] classrooms = manager.getClassroomNames();
        return Arrays.asList(classrooms);
    }
    
    /**
     * LAB 타입 실습실 목록 조회
     */
    public static List<String> loadLabs() {
        ClientClassroomManager manager = ClientClassroomManager.getInstance();
        String[] labs = manager.getLabNames();
        return Arrays.asList(labs);
    }
    
    /**
     * 모든 강의실/실습실 정보 조회 (타입 구분 없이)
     */
    public static List<RoomInfo> loadAllRooms() {
        ClientClassroomManager manager = ClientClassroomManager.getInstance();
        List<RoomInfo> rooms = new ArrayList<>();
        
        // 강의실 추가
        String[] classrooms = manager.getClassroomNames();
        for (String name : classrooms) {
            ClassroomDTO dto = manager.getClassroom(name);
            if (dto != null) {
                rooms.add(new RoomInfo(dto.name, dto.type, dto.capacity));
            }
        }
        
        // 실습실 추가
        String[] labs = manager.getLabNames();
        for (String name : labs) {
            ClassroomDTO dto = manager.getClassroom(name);
            if (dto != null) {
                rooms.add(new RoomInfo(dto.name, dto.type, dto.capacity));
            }
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
