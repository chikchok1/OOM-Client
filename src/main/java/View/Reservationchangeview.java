/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package View;

import com.toedter.calendar.JDateChooser;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author YangJinWonx
 */
public class Reservationchangeview extends javax.swing.JFrame {

    // 상수 정의
    private static final String[] TIME_SLOTS = {
        "1교시", "2교시", "3교시", "4교시", "5교시",
        "6교시", "7교시", "8교시", "9교시"
    };
    private static final String[] CALENDAR_COLUMNS = {"교시", "월", "화", "수", "목", "금", "토", "일"};
    private static final String[] DAY_NAMES = {"월", "화", "수", "목", "금", "토", "일"};
    private static final String[] PURPOSE_OPTIONS = {
        "학과 행사", "팀 프로젝트", "스터디", "동아리 모임", "강의 준비", "기타"
    };
    private static final String DEFAULT_ROOM = "908호";
    private static final String SELECT_OPTION = "선택";

    // Lab 강의실 목록 (하드코딩 대신 상수로 분리)
    private static final String[] LAB_ROOMS = {"911호", "915호", "916호", "918호"};

    private JDateChooser dateChooser;
    private javax.swing.JLabel dateLabel;
    private javax.swing.JLabel purposeLabel;
    private javax.swing.JComboBox<String> purposeComboBox;

    /**
     * Creates new form Reservationchangeview
     */
    public Reservationchangeview() {
        initComponents();
        initDatePicker();
        initCalendar();
        initPurpose();
        initCapacityLabel();
    }

    public void setReservationId(String id) {
        jTextField1.setText(id);
    }

    public void setChangeButtonActionListener(java.awt.event.ActionListener listener) {
        jButton1.addActionListener(listener);
    }

    //추가된 부분: 뒤로가기 버튼 리스너 연결 메서드
    public void setBackButtonActionListener(java.awt.event.ActionListener listener) {
        jButton2.addActionListener(listener); // 뒤로가기 버튼
    }

    public void setCancelButtonActionListener(java.awt.event.ActionListener listener) {
        jButton3.addActionListener(listener);
    }

    public String getReservationId() {
        return jTextField1.getText().trim();
    }

    public String getSelectedTime() {
        return (String) jComboBox2.getSelectedItem();
    }

    public javax.swing.JTable getReservationTable() {
        return jTable1;
    }

    /**
     * 예약 변경용 강의실 선택 (jComboBox3 사용)
     */
    public String getSelectedRoom() {
        if (jComboBox3.getSelectedItem() != null) {
            return jComboBox3.getSelectedItem().toString();
        }
        return DEFAULT_ROOM;
    }

    public String getChangeNumber() {
        return changenumber.getText().trim();
    }

    private void initDatePicker() {
        dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd (E)");
        dateChooser.setPreferredSize(new java.awt.Dimension(200, 30));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Date minDate = Date.from(tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant());
        dateChooser.setMinSelectableDate(minDate);
        dateChooser.setDate(minDate);

        LocalDate maxDate = LocalDate.now().plusMonths(1);
        Date maxDateLimit = Date.from(maxDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        dateChooser.setMaxSelectableDate(maxDateLimit);

        dateLabel = new javax.swing.JLabel("예약 날짜:");
        dateLabel.setFont(new java.awt.Font("맑은 고딕", 0, 12));
        dateLabel.setBounds(63, 300, 70, 25);
        dateChooser.setBounds(138, 305, 200, 30);

        getContentPane().add(dateLabel);
        getContentPane().add(dateChooser);
    }

    private void initCalendar() {
        // GUI 빌더로 이미 생성된 calendarPanel, calendarScrollPane, calendarTable 사용
        // 초기 테이블 모델 설정
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                TIME_SLOTS.length, CALENDAR_COLUMNS.length
        );
        model.setColumnIdentifiers(CALENDAR_COLUMNS);

        // 교시 열 채우기
        for (int i = 0; i < TIME_SLOTS.length; i++) {
            model.setValueAt(TIME_SLOTS[i], i, 0);
        }

        calendarTable.setModel(model);
        calendarTable.setRowHeight(30);
        calendarTable.setShowGrid(true);
        calendarTable.setGridColor(java.awt.Color.GRAY);

        // 첫 번째 열(교시) 너비 고정
        javax.swing.table.TableColumn firstColumn = calendarTable.getColumnModel().getColumn(0);
        firstColumn.setPreferredWidth(60);
        firstColumn.setMaxWidth(60);
        firstColumn.setMinWidth(60);

        System.out.println("[initCalendar] 초기 캘린더 테이블 설정 완료");
    }

    private void initPurpose() {
        purposeLabel = new javax.swing.JLabel("사용 목적:");
        purposeLabel.setFont(new java.awt.Font("맑은 고딕", 0, 12));
        purposeLabel.setBounds(45, 835, 80, 25);

        purposeComboBox = new javax.swing.JComboBox<>();
        purposeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(PURPOSE_OPTIONS));
        purposeComboBox.setBounds(130, 835, 150, 30);

        getContentPane().add(purposeLabel);
        getContentPane().add(purposeComboBox);
    }

    private void initCapacityLabel() {
        showpeoplenumber.setEditable(false);
        showpeoplenumber.setText("수용인원 정보 로딩 중...");
    }

    public LocalDate getSelectedDate() {
        Date date = dateChooser.getDate();
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public String getSelectedDateString() {
        LocalDate date = getSelectedDate();
        return date != null ? date.toString() : null;
    }

    public String getSelectedDay() {
        LocalDate date = getSelectedDate();
        if (date == null) {
            return DAY_NAMES[0]; // 기본값: 월
        }
        int dayOfWeek = date.getDayOfWeek().getValue();
        return DAY_NAMES[dayOfWeek - 1] + "요일";
    }

    /**
     * 주어진 강의실이 실습실인지 확인
     */
    private boolean isLabRoom(String room) {
        for (String labRoom : LAB_ROOMS) {
            if (labRoom.equals(room)) {
                return true;
            }
        }
        return false;
    }

    public void loadClassrooms() {
        int maxRetries = 3;
        int retryDelay = 500; // 500ms

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("[loadClassrooms] 시작 (시도 " + attempt + "/" + maxRetries + ") - ClientClassroomManager 사용");

                Manager.ClientClassroomManager manager = Manager.ClientClassroomManager.getInstance();
                java.util.List<common.dto.ClassroomDTO> allRooms = manager.getAllClassrooms();

                if (allRooms == null || allRooms.isEmpty()) {
                    System.err.println("[loadClassrooms] 강의실 정보 없음 (시도 " + attempt + ")");

                    //  마지막 시도가 아니면 재시도
                    if (attempt < maxRetries) {
                        System.out.println("[loadClassrooms] " + retryDelay + "ms 후 재시도...");
                        Thread.sleep(retryDelay);
                        continue;
                    }
                    return;
                }

                System.out.println("[loadClassrooms] 받은 강의실 수: " + allRooms.size());

                // 강의실과 실습실 분리
                java.util.List<String> classList = new java.util.ArrayList<>();
                java.util.List<String> labList = new java.util.ArrayList<>();
                java.util.List<String> allRoomsList = new java.util.ArrayList<>();

                classList.add(SELECT_OPTION);
                labList.add(SELECT_OPTION);

                for (common.dto.ClassroomDTO classroom : allRooms) {
                    String roomName = classroom.name;

                    // 실습실 판별
                    if ("LAB".equals(classroom.type)) {
                        labList.add(roomName);
                        System.out.println("[loadClassrooms] 실습실 추가: " + roomName);
                    } else {
                        classList.add(roomName);
                        System.out.println("[loadClassrooms] 강의실 추가: " + roomName);
                    }
                }

                //  강의실 먼저, 실습실 나중 순서로 전체 목록 구성
                // "선택" 제외하고 추가
                for (int i = 1; i < classList.size(); i++) {
                    allRoomsList.add(classList.get(i));
                }
                for (int i = 1; i < labList.size(); i++) {
                    allRoomsList.add(labList.get(i));
                }

                System.out.println("[loadClassrooms] 강의실 총 " + (classList.size() - 1) + "개, 실습실 총 " + (labList.size() - 1) + "개");

                // 오른쪽 콤보박스 설정 (예약 현황 조회용)
                Class.setModel(new DefaultComboBoxModel<>(classList.toArray(new String[0])));
                Lab.setModel(new DefaultComboBoxModel<>(labList.toArray(new String[0])));

                // 왼쪽 jComboBox3 설정 (예약 변경용) - 강의실 먼저, 실습실 나중
                jComboBox3.setModel(new DefaultComboBoxModel<>(allRoomsList.toArray(new String[0])));

                System.out.println("[loadClassrooms] 콤보박스 설정 완료");
                return;
            } catch (Exception e) {
                System.err.println("[loadClassrooms] 강의실 목록 조회 중 오류 (시도 " + attempt + "): " + e.getMessage());
                e.printStackTrace();

                //  마지막 시도가 아니면 재시도
                if (attempt < maxRetries) {
                    try {
                        System.out.println("[loadClassrooms] " + retryDelay + "ms 후 재시도...");
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
        /**
         * 예약 현황 조회용 강의실 선택 (Class/Lab 콤보박스 사용) 오른쪽 콤보박스에서 선택한 강의실의 예약 현황을 보여줌
         */
    public String getSelectedClassRoom() {
        String classSelection = (String) Class.getSelectedItem();
        String labSelection = (String) Lab.getSelectedItem();

        // Class에서 선택한 경우
        if (classSelection != null && !SELECT_OPTION.equals(classSelection)) {
            return classSelection;
        }

        // Lab에서 선택한 경우
        if (labSelection != null && !SELECT_OPTION.equals(labSelection)) {
            return labSelection;
        }

        // 둘 다 "선택"인 경우 기본값 반환
        return SELECT_OPTION;
    }

    public String getSelectedEndTime() {
        return EndTime.getSelectedItem().toString();
    }

    public String getPurpose() {
        return purposeComboBox.getSelectedItem().toString().trim();
    }

    public int getStudentCount() {
        try {
            if (changenumber == null) {
                return 1;
            }
            String text = changenumber.getText();
            if (text == null || text.trim().isEmpty()) {
                return 1;
            }
            int count = Integer.parseInt(text.trim());
            return count < 1 ? 1 : count;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void updateCalendarTable(javax.swing.JTable newTable) {
        calendarScrollPane.setViewportView(newTable);
        calendarTable = newTable;
    }

    public void setCapacityInfoText(String text) {
        showpeoplenumber.setText(text);
    }

    public javax.swing.JComboBox<String> getTimeComboBox() {
        return jComboBox2;
    }

    public javax.swing.JComboBox<String> getClassComboBox() {
        return jComboBox3;
    }

    public javax.swing.JComboBox<String> getEndTimeComboBox() {
        return EndTime;
    }

    public JDateChooser getDateChooser() {
        return dateChooser;
    }

    /**
     * Class 콤보박스 반환
     */
    public javax.swing.JComboBox<String> getClassRoomTypeComboBox() {
        return Class;
    }

    /**
     * Lab 콤보박스 반환
     */
    public javax.swing.JComboBox<String> getLabRoomTypeComboBox() {
        return Lab;
    }

    /**
     * 선택된 방 타입 반환 (CLASS 또는 LAB)
     */
    public String getSelectedRoomType() {
        String classSelection = (String) Class.getSelectedItem();
        String labSelection = (String) Lab.getSelectedItem();

        if (classSelection != null && !SELECT_OPTION.equals(classSelection)) {
            return "CLASS";
        }

        if (labSelection != null && !SELECT_OPTION.equals(labSelection)) {
            return "LAB";
        }

        return "CLASS"; // 기본값
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jComboBox2 = new javax.swing.JComboBox<>();
        jComboBox3 = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        EndTime = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        changenumber = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        calendarPanel = new javax.swing.JPanel();
        calendarScrollPane = new javax.swing.JScrollPane();
        calendarTable = new javax.swing.JTable();
        jLabel8 = new javax.swing.JLabel();
        showpeoplenumber = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        Class = new javax.swing.JComboBox<>();
        Lab = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("맑은 고딕", 1, 18)); // NOI18N
        jLabel1.setText("강의실 예약 변경 시스템");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "강의실", "날짜", "요일", "시간", "목적", "인원", "상태", "사용자ID"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        jScrollPane1.setViewportView(jScrollPane2);

        jLabel2.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel2.setText("예약 ID:");

        jLabel4.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel4.setText("시작 시간: ");

        jLabel5.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel5.setText("강의실: ");

        jTextField1.setText("변경할 ID를 입력하세요");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1교시(09:00~10:00)", "2교시(10:00~11:00)", "3교시(11:00~12:00)", "4교시(12:00~13:00)", "5교시(13:00~14:00)", "6교시(14:00~15:00)", "7교시(15:00~16:00)", "8교시(16:00~17:00)" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "908호", "912호", "913호", "914호", "911호", "915호", "916호", "918호" }));
        jComboBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox3ActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel3.setText("예약 요일:");

        jLabel7.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel7.setText("종료 시간: ");

        EndTime.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1교시(09:00~10:00)", "2교시(10:00~11:00)", "3교시(11:00~12:00)", "4교시(12:00~13:00)", "5교시(13:00~14:00)", "6교시(14:00~15:00)", "7교시(15:00~16:00)", "8교시(16:00~17:00)" }));
        EndTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EndTimeActionPerformed(evt);
            }
        });

        jButton1.setText("변경");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel6.setText("변경 인원:");

        changenumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changenumberActionPerformed(evt);
            }
        });

        jButton3.setText("취소");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel3)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(EndTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(54, 54, 54)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(changenumber, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(18, 18, 18)
                        .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                .addComponent(jButton3)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(13, 13, 13)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel6)
                    .addComponent(changenumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(EndTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1)
                            .addComponent(jButton3))
                        .addContainerGap())))
        );

        jButton2.setText("이전");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(jLabel1))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 616, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2))
                .addContainerGap(10, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2)
                .addGap(4, 4, 4))
        );

        calendarTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        calendarScrollPane.setViewportView(calendarTable);

        javax.swing.GroupLayout calendarPanelLayout = new javax.swing.GroupLayout(calendarPanel);
        calendarPanel.setLayout(calendarPanelLayout);
        calendarPanelLayout.setHorizontalGroup(
            calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, calendarPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(calendarScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        calendarPanelLayout.setVerticalGroup(
            calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(calendarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(calendarScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel8.setText("수용가능인원");

        showpeoplenumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showpeoplenumberActionPerformed(evt);
            }
        });

        jLabel9.setText("강의실");

        jLabel10.setText("실습실");

        Class.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "선택", "908호", "912호", "913호", "914호" }));
        Class.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClassActionPerformed(evt);
            }
        });

        Lab.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "선택", "911호", "915호", "916호", "918호" }));
        Lab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LabActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(calendarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(showpeoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel9)
                                .addGap(58, 58, 58)
                                .addComponent(jLabel10)
                                .addGap(24, 24, 24))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(Class, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(28, 28, 28)
                                .addComponent(Lab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(28, 28, 28))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Class, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Lab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(calendarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(showpeoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox3ActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

    private void changenumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changenumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_changenumberActionPerformed

    private void EndTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EndTimeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_EndTimeActionPerformed

    private void showpeoplenumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showpeoplenumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showpeoplenumberActionPerformed

    private void ClassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClassActionPerformed

    }//GEN-LAST:event_ClassActionPerformed

    private void LabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LabActionPerformed

    }//GEN-LAST:event_LabActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Reservationchangeview.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Reservationchangeview.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Reservationchangeview.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Reservationchangeview.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Reservationchangeview().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> Class;
    private javax.swing.JComboBox<String> EndTime;
    private javax.swing.JComboBox<String> Lab;
    private javax.swing.JPanel calendarPanel;
    private javax.swing.JScrollPane calendarScrollPane;
    private javax.swing.JTable calendarTable;
    private javax.swing.JTextField changenumber;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField showpeoplenumber;
    // End of variables declaration//GEN-END:variables
}
