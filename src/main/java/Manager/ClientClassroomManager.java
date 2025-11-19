package Manager;

import Model.Session;
import Util.MessageDispatcher;
import common.dto.ClassroomDTO;
import java.util.*;

/**
 * 클라이언트 전용 강의실/실습실 관리자 (Singleton Pattern)
 * 서버로부터 데이터를 가져와 캐싱
 */
public class ClientClassroomManager {
    
    private static volatile ClientClassroomManager instance;
    
    private Map<String, ClassroomDTO> classrooms;
    
    private ClientClassroomManager() {
        classrooms = new HashMap<>();
    }
    
    public static ClientClassroomManager getInstance() {
        if (instance == null) {
            synchronized (ClientClassroomManager.class) {
                if (instance == null) {
                    instance = new ClientClassroomManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 서버로부터 모든 강의실 정보 로드
     */
    public synchronized boolean refreshFromServer() {
        if (!Session.getInstance().isConnected()) {
            System.err.println("[클라이언트] 서버 연결 없음");
            return false;
        }
        
        try {
            // 강의실 목록 가져오기
            List<ClassroomDTO> classroomList = getClassroomsFromServer();
            
            // 실습실 목록 가져오기
            List<ClassroomDTO> labList = getLabsFromServer();
            
            // 캐시 업데이트
            classrooms.clear();
            for (ClassroomDTO dto : classroomList) {
                classrooms.put(dto.name, dto);
            }
            for (ClassroomDTO dto : labList) {
                classrooms.put(dto.name, dto);
            }
            
            System.out.println(String.format(
                "[클라이언트] 강의실 정보 로드 완료: 강의실 %d개, 실습실 %d개",
                classroomList.size(), labList.size()
            ));
            
            return true;
            
        } catch (Exception e) {
            System.err.println("[클라이언트] 서버로부터 데이터 로드 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 서버로부터 강의실 목록 가져오기
     */
    private List<ClassroomDTO> getClassroomsFromServer() {
        Session session = Session.getInstance();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        
        session.getOut().println("GET_CLASSROOMS");
        session.getOut().flush();
        
        String response = dispatcher.waitForResponse(10);
        
        if (response == null) {
            System.err.println("[클라이언트] GET_CLASSROOMS 타임아웃");
            return new ArrayList<>();
        }
        
        return parseClassroomList(response);
    }
    
    /**
     * 서버로부터 실습실 목록 가져오기
     */
    private List<ClassroomDTO> getLabsFromServer() {
        Session session = Session.getInstance();
        MessageDispatcher dispatcher = MessageDispatcher.getInstance();
        
        session.getOut().println("GET_LABS");
        session.getOut().flush();
        
        String response = dispatcher.waitForResponse(10);
        
        if (response == null) {
            System.err.println("[클라이언트] GET_LABS 타임아웃");
            return new ArrayList<>();
        }
        
        return parseClassroomList(response);
    }
    
    /**
     * 서버 응답 파싱
     * 형식: CLASSROOMS,name1,type1,capacity1,name2,type2,capacity2,...
     */
    private List<ClassroomDTO> parseClassroomList(String response) {
        List<ClassroomDTO> result = new ArrayList<>();
        
        if (response.startsWith("CLASSROOMS,") || response.startsWith("LABS,")) {
            String[] parts = response.split(",");
            
            // 첫 번째는 명령어이므로 스킵
            for (int i = 1; i < parts.length; i += 3) {
                if (i + 2 < parts.length) {
                    String name = parts[i].trim();
                    String type = parts[i + 1].trim();
                    int capacity = Integer.parseInt(parts[i + 2].trim());
                    
                    result.add(new ClassroomDTO(name, type, capacity));
                }
            }
        }
        
        return result;
    }
    
    /**
     * 특정 강의실 정보 조회 (캐시에서)
     */
    public ClassroomDTO getClassroom(String name) {
        return classrooms.get(name);
    }
    
    /**
     * 수용 인원 체크
     */
    public boolean checkCapacity(String roomName, int requestedCount) {
        ClassroomDTO dto = classrooms.get(roomName);
        if (dto == null) {
            System.err.println("[클라이언트] 알 수 없는 강의실: " + roomName);
            return false;
        }
        
        int allowedCapacity = dto.getAllowedCapacity();
        boolean isAllowed = requestedCount <= allowedCapacity;
        
        System.out.println(String.format(
            "[클라이언트 수용인원체크] %s: 최대 %d명, 허용 %d명(50%%), 요청 %d명 → %s",
            roomName, dto.capacity, allowedCapacity, requestedCount,
            isAllowed ? "승인" : "거부"
        ));
        
        return isAllowed;
    }
    
    /**
     * 모든 강의실 이름 목록
     */
    public String[] getClassroomNames() {
        List<String> names = new ArrayList<>();
        for (ClassroomDTO dto : classrooms.values()) {
            if (dto.isClassroom()) {
                names.add(dto.name);
            }
        }
        Collections.sort(names);
        return names.toArray(new String[0]);
    }
    
    /**
     * 모든 실습실 이름 목록
     */
    public String[] getLabNames() {
        List<String> names = new ArrayList<>();
        for (ClassroomDTO dto : classrooms.values()) {
            if (dto.isLab()) {
                names.add(dto.name);
            }
        }
        Collections.sort(names);
        return names.toArray(new String[0]);
    }
    
    /**
     * 모든 강의실/실습실 DTO 목록 반환 (정렬됨: 강의실 먼저, 실습실 나중)
     */
    public List<ClassroomDTO> getAllClassrooms() {
        List<ClassroomDTO> result = new ArrayList<>();
        List<ClassroomDTO> classroomList = new ArrayList<>();
        List<ClassroomDTO> labList = new ArrayList<>();
        
        // 강의실과 실습실 분류
        for (ClassroomDTO dto : classrooms.values()) {
            if (dto.isClassroom()) {
                classroomList.add(dto);
            } else if (dto.isLab()) {
                labList.add(dto);
            }
        }
        
        // 각각 이름순으로 정렬
        classroomList.sort(Comparator.comparing(dto -> dto.name));
        labList.sort(Comparator.comparing(dto -> dto.name));
        
        // 강의실 먼저, 실습실 나중 순서로 합치기
        result.addAll(classroomList);
        result.addAll(labList);
        
        return result;
    }
    
    /**
     * 캐시 초기화
     */
    public void clear() {
        classrooms.clear();
    }
}
