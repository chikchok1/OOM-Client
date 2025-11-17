/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package View;

import com.toedter.calendar.JDateChooser;  // ✅ 추가
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.DefaultComboBoxModel;
import java.time.LocalDate;  // ✅ 추가
import java.time.ZoneId;     // ✅ 추가
import java.util.Date;       // ✅ 추가


public class ReservLabView extends javax.swing.JFrame {

    // ✅ 날짜 선택기 변수 추가
    private JDateChooser dateChooser;
    private javax.swing.JLabel dateLabel;

    /**
     * Creates new form ReservLab
     */
    public ReservLabView() {
        try {
            javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        initComponents();
        initDatePicker();  // ✅ 날짜 선택기 초기화
        initPurposeComboBox();  // ✅ 사용 목적 초기화
        initLabComboBox();  // ✅ 실습실 콤보박스 초기화
        
        // ✅ loadLabs() 제거 - Controller에서 처리할 예정
        // ✅ Controller에서 모든 이벤트 처리
    }

    /**
     * 사용 목적 ComboBox 초기화 - 편집 가능하게 설정
     */
    private void initPurposeComboBox() {
        // ComboBox를 편집 가능하게 설정
        Purpose.setEditable(true);

        // 기본 선택지 설정
        Purpose.setModel(new DefaultComboBoxModel<>(new String[]{
            "실습",
            "프로젝트",
            "스터디",
            "세미나",
            "동아리 활동",
            "기타"
        }));

        System.out.println("[ReservLabView] 사용 목적 ComboBox 초기화 완료 (편집 가능)");
    }

    /**
     * 실습실 ComboBox 초기화 - 편집 가능하게 설정
     */
    private void initLabComboBox() {
        // ComboBox를 편집 가능하게 설정
        Lab.setEditable(true);

        // 기본 실습실 4개 설정 (Classrooms.txt에서 로드될 때까지 임시)
        Lab.setModel(new DefaultComboBoxModel<>(new String[]{
            "911호",
            "915호",
            "916호",
            "918호"
        }));

        System.out.println("[ReservLabView] 실습실 ComboBox 초기화 완료 (편집 가능)");
    }

    /**
     * 날짜 선택기 초기화
     */
    private void initDatePicker() {
        // 날짜 선택기 생성
        dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd (E)");  // 요일 포함
        dateChooser.setPreferredSize(new java.awt.Dimension(200, 30));
        
        // 최소 날짜: 내일부터
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Date minDate = Date.from(tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant());
        dateChooser.setMinSelectableDate(minDate);
        dateChooser.setDate(minDate);  //  기본값: 내일로 설정
        
        // 최대 날짜: 1개월 후
        LocalDate maxDate = LocalDate.now().plusMonths(1);
        Date maxDateLimit = Date.from(maxDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        dateChooser.setMaxSelectableDate(maxDateLimit);
        
        // 레이블 생성
        dateLabel = new javax.swing.JLabel("예약 날짜:");
        dateLabel.setFont(new java.awt.Font("맑은 고딕", 0, 12));
        
        // ✅ 기존 Day ComboBox 위치에 추가 (절대 좌표 기반)
        dateLabel.setBounds(20, 290, 70, 25);  // "요일 선택" 위치
        dateChooser.setBounds(102, 285, 200, 30);  // Day ComboBox 위치
        
        getContentPane().add(dateLabel);
        getContentPane().add(dateChooser);
        
        // 날짜 변경 시 수용인원 업데이트 (Controller에서 처리로 변경)
        // dateChooser.addPropertyChangeListener("date", evt -> updateCapacityInfo());
    }

    /**
     *  선택한 날짜 가져오기
     */
    public LocalDate getSelectedDate() {
        Date date = dateChooser.getDate();
        if (date == null) {
            return null;
        }
        return date.toInstant()
                   .atZone(ZoneId.systemDefault())
                   .toLocalDate();
    }
    
    /**
     * ✅ 날짜를 문자열로 반환 ("2025-11-12" 형식)
     */
    public String getSelectedDateString() {
        LocalDate date = getSelectedDate();
        return date != null ? date.toString() : null;
    }

    /**
     * ✅ 요일 문자열 반환 (호환성 유지용)
     * 실제 날짜에서 요일을 계산
     */
    public String getSelectedDay() {
        LocalDate date = getSelectedDate();
        if (date == null) {
            return "월";  // 기본값
        }
        
        // 요일 한글 변환
        String[] dayNames = {"월", "화", "수", "목", "금", "토", "일"};
        int dayOfWeek = date.getDayOfWeek().getValue();  // 1(월)~7(일)
        return dayNames[dayOfWeek - 1] + "요일";
    }

    /**
     * 실습실 목록을 ComboBox에 설정
     * Controller에서 데이터를 전달받아 표시만 함
     * 편집 가능 설정 유지
     * @param labs 실습실 이름 목록
     */
    public void setLabs(java.util.List<String> labs) {
        if (labs == null || labs.isEmpty()) {
            System.err.println("[ReservLabView] 실습실 목록이 비어있습니다.");
            // 기본 실습실 4개 유지
            Lab.setModel(new DefaultComboBoxModel<>(new String[]{
                "911호",
                "915호",
                "916호",
                "918호"
            }));
        } else {
            Lab.setModel(new DefaultComboBoxModel<>(labs.toArray(new String[0])));
            System.out.println("[ReservLabView] 실습실 " + labs.size() + "개 표시 완료");
        }
        // 편집 가능 설정 유지
        Lab.setEditable(true);
    }

    public int getStudentCount() {
        try {
            if (peoplenumber == null) {
                System.out.println("[getStudentCount] peoplenumber is null, returning 1");
                return 1;
            }
            String text = peoplenumber.getText();
            if (text == null || text.trim().isEmpty()) {
                System.out.println("[getStudentCount] text is empty, returning 1");
                return 1;
            }
            int count = Integer.parseInt(text.trim());
            if (count < 1) {
                System.out.println("[getStudentCount] count < 1, returning 1");
                return 1;
            }
            System.out.println("[getStudentCount] returning " + count);
            return count;
        } catch (NumberFormatException e) {
            System.out.println("[getStudentCount] NumberFormatException, returning 1");
            return 1;
        }
    }

    // 버튼 리스너 등록
    public void addReservationListener(ActionListener listener) {
        Reservation.addActionListener(listener); // 버튼에 바로 리스너 붙이기
    }

    public String getSelectedLabRoom() {
        Object selected = Lab.getSelectedItem();
        if (selected == null) {
            return null;
        }
        String room = selected.toString().trim();
        return normalizeRoomName(room);
    }

    // 호환성 유지를 위한 메서드
    public String getSelectedClassRoom() {
        return getSelectedLabRoom();
    }

    private String normalizeRoomName(String room) {
        if (room == null) {
            return null;
        }

        room = room.trim();

        // 911호:LAB → 911호
        if (room.contains(":")) {
            room = room.substring(0, room.indexOf(":"));
        }

        // 혹시 모를 예외 처리
        if (!room.endsWith("호")) {
            room = room + "호";
        }

        return room;
    }

    public String getSelectedTime() {
        return Time.getSelectedItem().toString();
    }

    // ✅ 종료 시간 가져오기
    public String getSelectedEndTime() {
        return EndTime.getSelectedItem().toString();
    }

    // ✅ 종료 시간 콤보박스 반환
    public javax.swing.JComboBox<String> getEndTimeComboBox() {
        return EndTime;
    }

    public String getPurpose() {
        Object selected = Purpose.getSelectedItem();
        if (selected == null) {
            return "";
        }

        String purpose = selected.toString().trim();

        // "기타"를 선택했는데 추가 입력이 없으면 경고
        if (purpose.equals("기타") || purpose.isEmpty()) {
            return "";  // Controller에서 검증하도록
        }

        return purpose;
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void closeView() {
        this.dispose();
    }

    public void resetReservationButtonListener() {
        for (ActionListener al : Reservation.getActionListeners()) {
            Reservation.removeActionListener(al);
        }
    }

    public javax.swing.JButton getBeforeButton() {
        return Before;
    }

    public void updateCalendarTable(JTable table) {
        calendarScrollPane.setViewportView(table);
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    public javax.swing.JComboBox<String> getLabComboBox() {
        return Lab;
    }

    public void setCapacityInfoText(String text) {
        showpeoplenumber.setText(text);
    }
    
    // ✅ 시간 선택 콤보박스 반환
    public javax.swing.JComboBox<String> getTimeComboBox() {
        return Time;
    }

    public com.toedter.calendar.JDateChooser getDateChooser() {
        return dateChooser;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        Lab = new javax.swing.JComboBox<>();
        Time = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        Purpose = new javax.swing.JComboBox<>();
        Before = new javax.swing.JButton();
        calendarPanel = new javax.swing.JPanel();
        calendarScrollPane = new javax.swing.JScrollPane();
        calendarTable = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        showpeoplenumber = new javax.swing.JTextField();
        Reservation = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        peoplenumber = new javax.swing.JTextField();
        EndTime = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("맑은 고딕", 0, 13)); // NOI18N
        jLabel1.setText("실습실 예약 정보 기입란");

        jLabel3.setText("실습실 선택");

        jLabel5.setText("시작 시간 선택");

        Lab.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "911호", "915호", "916호", "918호" }));
        Lab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LabActionPerformed(evt);
            }
        });

        Time.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1교시(09:00~10:00)", "2교시(10:00~11:00)", "3교시(11:00~12:00)", "4교시(12:00~13:00)", "5교시(13:00~14:00)", "6교시(14:00~15:00)", "7교시(15:00~16:00)", "8교시(16:00~17:00)", "9교시(17:00~18:00)" }));
        Time.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TimeActionPerformed(evt);
            }
        });

        jLabel6.setText("사용 목적");

        Purpose.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "학과 행사", "동아리 행사", "프로젝트 회의", "스터디" }));
        Purpose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PurposeActionPerformed(evt);
            }
        });

        Before.setText("이전");
        Before.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BeforeActionPerformed(evt);
            }
        });

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

        jLabel2.setText("수용가능인원");

        Reservation.setText("예약");
        Reservation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReservationActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout calendarPanelLayout = new javax.swing.GroupLayout(calendarPanel);
        calendarPanel.setLayout(calendarPanelLayout);
        calendarPanelLayout.setHorizontalGroup(
            calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(calendarPanelLayout.createSequentialGroup()
                .addGroup(calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, calendarPanelLayout.createSequentialGroup()
                        .addGap(76, 76, 76)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(showpeoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, calendarPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Reservation, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(calendarScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 475, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        calendarPanelLayout.setVerticalGroup(
            calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(calendarPanelLayout.createSequentialGroup()
                .addComponent(calendarScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(29, 29, 29)
                .addGroup(calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(showpeoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 66, Short.MAX_VALUE)
                .addComponent(Reservation, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel7.setText("사용 인원");

        peoplenumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peoplenumberActionPerformed(evt);
            }
        });

        EndTime.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1교시(09:00~10:00)", "2교시(10:00~11:00)", "3교시(11:00~12:00)", "4교시(12:00~13:00)", "5교시(13:00~14:00)", "6교시(14:00~15:00)", "7교시(15:00~16:00)", "8교시(16:00~17:00)", "9교시(17:00~18:00)" }));
        EndTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EndTimeActionPerformed(evt);
            }
        });

        jLabel8.setText("종료 시간 선택");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(55, 55, 55)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(Before, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(231, 231, 231))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(jLabel8)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(Purpose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(EndTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(peoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Lab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addComponent(calendarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(39, 39, 39))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(Lab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addGap(9, 9, 9)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(EndTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(Purpose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(peoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Before, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                        .addComponent(calendarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ReservationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReservationActionPerformed

    }//GEN-LAST:event_ReservationActionPerformed

    private void LabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LabActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_LabActionPerformed

    private void TimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TimeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TimeActionPerformed

    private void PurposeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PurposeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_PurposeActionPerformed

    private void BeforeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BeforeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_BeforeActionPerformed

    private void peoplenumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peoplenumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_peoplenumberActionPerformed

    private void EndTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EndTimeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_EndTimeActionPerformed

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
            java.util.logging.Logger.getLogger(ReservLabView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ReservLabView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ReservLabView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ReservLabView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ReservLabView().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Before;
    private javax.swing.JComboBox<String> EndTime;
    private javax.swing.JComboBox<String> Lab;
    private javax.swing.JComboBox<String> Purpose;
    private javax.swing.JButton Reservation;
    private javax.swing.JComboBox<String> Time;
    private javax.swing.JPanel calendarPanel;
    private javax.swing.JScrollPane calendarScrollPane;
    private javax.swing.JTable calendarTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JTextField peoplenumber;
    private javax.swing.JTextField showpeoplenumber;
    // End of variables declaration//GEN-END:variables
}
