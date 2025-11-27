package Controller;

import Model.Session;
import Util.MessageDispatcher;
import View.Executive;
import View.RoomAddDelete;
import common.dto.ClassroomDTO;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class RoomAddDeleteController {
    private RoomAddDelete view;
    private BufferedReader in;
    private PrintWriter out;
    private MessageDispatcher dispatcher;

    public RoomAddDeleteController(RoomAddDelete view) {
        this.view = view;
        Session session = Session.getInstance();
        this.in = session.getIn();
        this.out = session.getOut();
        this.dispatcher = MessageDispatcher.getInstance();

        initController();
        loadRoomLists();
    }

    private void initController() {
        // 추가 버튼 이벤트
        view.getAddRoomButton().addActionListener(e -> addRoom());

        // 삭제 버튼 이벤트
        view.getDeleteRoomButton().addActionListener(e -> deleteRoom());
        
        // 이전 버튼 이벤트
        view.getJButton3().addActionListener(e -> goBack());
    }

    /**
     * 강의실/실습실 목록 로드
     */
    private void loadRoomLists() {
        try {
            // 강의실 목록 로드
            List<ClassroomDTO> classrooms = getClassroomsFromServer();
            view.updateClassroomList(classrooms);

            // 실습실 목록 로드
            List<ClassroomDTO> labs = getLabsFromServer();
            view.updateLabList(labs);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(view,
                    "강의실 목록을 불러오는데 실패했습니다: " + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
    * 강의실 추가
    */
    private void addRoom() {
    try {
    String roomName = view.getRoomName();
    String selectedType = view.getSelectedRoomType(); // "강의실" or "실습실"

    // 입력 검증
    if (roomName == null || roomName.trim().isEmpty()) {
    JOptionPane.showMessageDialog(view,
    "호실을 입력해주세요.",
    "입력 오류",
    JOptionPane.WARNING_MESSAGE);
    return;
    }

    // 타입 변환 (강의실 -> CLASS, 실습실 -> LAB)
    String type = selectedType.equals("강의실") ? "CLASS" : "LAB";

    // 기본 수용인원 30명
    int capacity = 30;

    // 서버에 추가 요청
    String request = String.format("ADD_CLASSROOM,%s,%s,%d",
    roomName, type, capacity);

    out.println(request);
    out.flush();
    
            // MessageDispatcher로부터 응답 대기 (5초 타임아웃)
    String response = dispatcher.waitForResponse(5);

    if (response != null && response.startsWith("SUCCESS")) {
    JOptionPane.showMessageDialog(view,
    "강의실이 추가되었습니다.",
                        "성공",
            JOptionPane.INFORMATION_MESSAGE);

                // ✅ ClientClassroomManager 캠시 업데이트
                Manager.ClientClassroomManager.getInstance().refreshFromServer();
                
                // 목록 새로고침
    loadRoomLists();

                // 입력 필드 초기화
        view.clearRoomName();

    } else if (response == null) {
    JOptionPane.showMessageDialog(view,
            "서버 응답 시간 초과. 다시 시도해주세요.",
    "타임아웃",
    JOptionPane.ERROR_MESSAGE);
    } else {
        String errorMsg = response.contains("|")
                    ? response.substring(response.indexOf("|") + 1)
                : "추가에 실패했습니다.";
        JOptionPane.showMessageDialog(view,
        errorMsg,
        "추가 실패",
        JOptionPane.ERROR_MESSAGE);
        }

    } catch (Exception e) {
    e.printStackTrace();
    JOptionPane.showMessageDialog(view,
    "강의실 추가 중 오류가 발생했습니다: " + e.getMessage(),
                "오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 강의실 삭제
     */
    private void deleteRoom() {
        try {
            // 선택된 강의실 또는 실습실 가져오기
            String selectedClassroom = view.getSelectedClassroom();
            String selectedLab = view.getSelectedLab();

            String roomToDelete = null;

            if (selectedClassroom != null && !selectedClassroom.isEmpty()) {
                roomToDelete = selectedClassroom;
            } else if (selectedLab != null && !selectedLab.isEmpty()) {
                roomToDelete = selectedLab;
            }

            if (roomToDelete == null || roomToDelete.isEmpty()) {
                JOptionPane.showMessageDialog(view,
                        "삭제할 강의실/실습실을 선택해주세요.",
                        "선택 오류",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 삭제 확인
            int confirm = JOptionPane.showConfirmDialog(view,
                    roomToDelete + "을(를) 정말 삭제하시겠습니까?\n" +
                            "이 강의실에 예약 건이 있으면 삭제할 수 없습니다.",
                    "삭제 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            // 서버에 삭제 요청
            String request = String.format("DELETE_CLASSROOM,%s", roomToDelete);

            out.println(request);
            out.flush();
            
            // MessageDispatcher로부터 응답 대기 (5초 타임아웃)
            String response = dispatcher.waitForResponse(5);

            if (response != null && response.startsWith("SUCCESS")) {
                JOptionPane.showMessageDialog(view,
                        "강의실이 삭제되었습니다.",
                        "성공",
                        JOptionPane.INFORMATION_MESSAGE);

                // ✅ ClientClassroomManager 캠시 업데이트
                Manager.ClientClassroomManager.getInstance().refreshFromServer();
                
                // 목록 새로고침
                loadRoomLists();

            } else if (response == null) {
                JOptionPane.showMessageDialog(view,
                        "서버 응답 시간 초과. 다시 시도해주세요.",
                        "타임아웃",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                String errorMsg = response.contains("|")
                    ? response.substring(response.indexOf("|") + 1)
                    : "삭제에 실패했습니다.";
                JOptionPane.showMessageDialog(view,
                        errorMsg,
                        "삭제 실패",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view,
                    "강의실 삭제 중 오류가 발생했습니다: " + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 서버에서 강의실 목록 가져오기
     */
    private List<ClassroomDTO> getClassroomsFromServer() throws IOException {
        out.println("GET_CLASSROOMS");
        out.flush();
        
        // MessageDispatcher로부터 응답 대기 (5초 타임아웃)
        String response = dispatcher.waitForResponse(5);

        List<ClassroomDTO> classrooms = new ArrayList<>();

        if (response != null && !response.isEmpty() && !response.startsWith("LABS")) {
            // "CLASSROOMS,908호,CLASS,30,..." 형식의 응답 처리
            if (response.startsWith("CLASSROOMS,")) {
                response = response.substring("CLASSROOMS,".length());
            }
            
            String[] rooms = response.split(",");
            for (int i = 0; i < rooms.length; i += 3) {
                if (i + 2 < rooms.length) {
                    try {
                        classrooms.add(new ClassroomDTO(
                                rooms[i],     // name
                                rooms[i + 1], // type
                                Integer.parseInt(rooms[i + 2]) // capacity
                        ));
                    } catch (Exception e) {
                        System.err.println("강의실 파싱 오류: " + e.getMessage());
                    }
                }
            }
        }

        return classrooms;
    }

    /**
     * 서버에서 실습실 목록 가져오기
     */
    private List<ClassroomDTO> getLabsFromServer() throws IOException {
        out.println("GET_LABS");
        out.flush();
        
        // MessageDispatcher로부터 응답 대기 (5초 타임아웃)
        String response = dispatcher.waitForResponse(5);

        List<ClassroomDTO> labs = new ArrayList<>();

        if (response != null && !response.isEmpty() && !response.startsWith("CLASSROOMS")) {
            // "LABS,911호,LAB,30,..." 형식의 응답 처리
            if (response.startsWith("LABS,")) {
                response = response.substring("LABS,".length());
            }
            
            String[] rooms = response.split(",");
            for (int i = 0; i < rooms.length; i += 3) {
                if (i + 2 < rooms.length) {
                    try {
                        labs.add(new ClassroomDTO(
                                rooms[i],     // name
                                rooms[i + 1], // type
                                Integer.parseInt(rooms[i + 2]) // capacity
                        ));
                    } catch (Exception e) {
                        System.err.println("실습실 파싱 오류: " + e.getMessage());
                    }
                }
            }
        }

        return labs;
    }
    
    /**
     * 이전 버튼 - Executive 화면으로 돌아가기
     */
    private void goBack() {
        Executive executive = view.getExecutive();
        view.dispose(); // 현재 RoomAddDelete 창 닫기
        
        if (executive != null) {
            executive.setVisible(true); // Executive 창 다시 보이기
        }
    }
}
