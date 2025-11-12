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
import java.time.LocalDate;  //  추가
import java.time.ZoneId;     //  추가
import java.util.Date;       //  추가

/**
 *
 * @author Sunghoon
 */
public class ReservClassView extends javax.swing.JFrame {

    //  날짜 선택기 변수 추가
    private JDateChooser dateChooser;
    private javax.swing.JLabel dateLabel;

    /**
     * Creates new form ReservClass
     */
    public ReservClassView() {
        try {
    javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
} catch (Exception e) {
    e.printStackTrace();
}

        initComponents();
        initDatePicker();  // ✅ 날짜 선택기 초기화
        
        //  loadClassrooms() 제거 - Controller에서 처리할 예정
        //  Controller에서 모든 이벤트 처리
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
        dateLabel.setBounds(68, 235, 70, 25);  // "요일 선택" 위치
        dateChooser.setBounds(150, 230, 200, 30);  // Day ComboBox 위치
        
        getContentPane().add(dateLabel);
        getContentPane().add(dateChooser);
        
        // ✅ 기존 Day ComboBox는 숨기기
        Day.setVisible(false);
        jLabel4.setVisible(false);  // "요일 선택" 레이블도 숨기기
        
    
        
        // 날짜 변경 시 수용인원 업데이트 (컴트 처리로 변경)
        // dateChooser.addPropertyChangeListener("date", evt -> updateCapacityInfo());
    }
    


    /**
     * ✅ 선택한 날짜 가져오기
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

    // ✅ Controller에서 호출할 메소드
    public void loadClassrooms() {
        // ✅ 서버에서 강의실 목록 받아오기
        try {
            if (!Model.Session.getInstance().isConnected()) {
                System.err.println("서버 연결이 없습니다.");
                return;
            }
            
            java.io.PrintWriter out = Model.Session.getInstance().getOut();
            java.io.BufferedReader in = Model.Session.getInstance().getIn();
            
            out.println("GET_CLASSROOMS");
            out.flush();
            
            String response = in.readLine();
            if (response != null && response.startsWith("CLASSROOMS:")) {
                String classroomList = response.substring("CLASSROOMS:".length());
                String[] classrooms = classroomList.split(",");
                Class.setModel(new DefaultComboBoxModel<>(classrooms));
                System.out.println("강의실 로드 완료: " + classrooms.length + "개");
            } else {
                System.err.println("강의실 목록 조회 실패: " + response);
            }
            
        } catch (java.io.IOException e) {
            System.err.println("강의실 목록 조회 중 오류: " + e.getMessage());
        }
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

    public String getSelectedClassRoom() {
        return Class.getSelectedItem().toString();
    }

    public String getSelectedTime() {
        return Time.getSelectedItem().toString();
    }

    public String getPurpose() {
        return Purpose.getSelectedItem().toString().trim();
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

    public javax.swing.JComboBox<String> getClassComboBox() {
        return Class;
    }

    public void setCapacityInfoText(String text) {
        showpeoplenumber.setText(text);
    }

    public javax.swing.JComboBox<String> getDayComboBox() {
        return Day;
    }

    //  시간 선택 콤보박스 반환
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

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        Class = new javax.swing.JComboBox<>();
        Day = new javax.swing.JComboBox<>();
        Time = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        Purpose = new javax.swing.JComboBox<>();
        Reservation = new javax.swing.JButton();
        Before = new javax.swing.JButton();
        calendarPanel = new javax.swing.JPanel();
        calendarScrollPane = new javax.swing.JScrollPane();
        calendarTable = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        peoplenumber = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        showpeoplenumber = new javax.swing.JTextField();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("맑은 고딕", 0, 13)); // NOI18N
        jLabel1.setText("강의실 예약 정보 기입란");

        jLabel3.setText("강의실 선택");

        jLabel4.setText("요일 선택");

        jLabel5.setText("시간 선택");

        Class.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "908호", "912호", "913호", "914호" }));
        Class.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClassActionPerformed(evt);
            }
        });

        Day.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "월요일", "화요일", "수요일", "목요일", "금요일" }));
        Day.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DayActionPerformed(evt);
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

        Reservation.setText("예약");
        Reservation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReservationActionPerformed(evt);
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

        javax.swing.GroupLayout calendarPanelLayout = new javax.swing.GroupLayout(calendarPanel);
        calendarPanel.setLayout(calendarPanelLayout);
        calendarPanelLayout.setHorizontalGroup(
            calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(calendarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(calendarScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
        );
        calendarPanelLayout.setVerticalGroup(
            calendarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(calendarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(calendarScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel7.setText("사용 인원");

        peoplenumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peoplenumberActionPerformed(evt);
            }
        });

        jLabel2.setText("수용가능인원");

        showpeoplenumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showpeoplenumberActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(71, 71, 71)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Class, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(Day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(Purpose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(peoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Before, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(144, 144, 144)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(calendarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 66, Short.MAX_VALUE)
                                .addComponent(showpeoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(27, 27, 27))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Reservation, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(69, 69, 69))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(calendarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(showpeoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(60, 60, 60)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Reservation, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Before, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(Class, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(9, 9, 9)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Purpose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(peoplenumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ReservationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReservationActionPerformed

    }//GEN-LAST:event_ReservationActionPerformed

    private void TimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TimeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TimeActionPerformed

    private void PurposeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PurposeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_PurposeActionPerformed

    private void ClassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClassActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ClassActionPerformed

    private void BeforeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BeforeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_BeforeActionPerformed

    private void peoplenumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peoplenumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_peoplenumberActionPerformed

    private void showpeoplenumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showpeoplenumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showpeoplenumberActionPerformed

    private void DayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DayActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_DayActionPerformed

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
            java.util.logging.Logger.getLogger(ReservClassView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ReservClassView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ReservClassView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ReservClassView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ReservClassView().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Before;
    private javax.swing.JComboBox<String> Class;
    private javax.swing.JComboBox<String> Day;
    private javax.swing.JComboBox<String> Purpose;
    private javax.swing.JButton Reservation;
    private javax.swing.JComboBox<String> Time;
    private javax.swing.JPanel calendarPanel;
    private javax.swing.JScrollPane calendarScrollPane;
    private javax.swing.JTable calendarTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField peoplenumber;
    private javax.swing.JTextField showpeoplenumber;
    // End of variables declaration//GEN-END:variables
}


