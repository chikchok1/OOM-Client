package View;

import com.toedter.calendar.JDateChooser;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.*;

public class ReservedRoomView extends JFrame {
    private Executive executive;
    private RoomSelect roomSelect;
    private boolean isUpdating = false;
    //  날짜 선택기 변수 추가
    private JDateChooser dateChooser;
    private javax.swing.JLabel dateLabel;
    

    // 생성자: Executive 또는 RoomSelect 중 하나만 전달받음
    public ReservedRoomView(Executive executive) {
        initComponents();
        this.executive = executive;
        this.roomSelect = null;
        
        initDatePicker();      
        try {
    javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
} catch (Exception e) {
    e.printStackTrace();
}
            
   
    }
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
        
        // ✅ 좌측 상단에 배치
        dateLabel.setBounds(30, 50, 70, 25);  // x=30, y=50
        dateChooser.setBounds(110, 45, 200, 30);  // x=110, y=45
        
        getContentPane().add(dateLabel);
        getContentPane().add(dateChooser);
        
        
    
        
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
    
    /**
     * ✅ JDateChooser Getter (Controller에서 리스너 추가용)
     */
    public JDateChooser getDateChooser() {
        return dateChooser;
    }

    /**
     * ✅ ClientClassroomManager를 사용하여 강의실/실습실 목록 로드
     */
    public void loadRooms() {
        Manager.ClientClassroomManager manager = Manager.ClientClassroomManager.getInstance();
        
        // 강의실 목록 로드
        java.util.List<String> classrooms = manager.getClassrooms();
        if (classrooms != null && !classrooms.isEmpty()) {
            String[] classArray = new String[classrooms.size() + 1];
            classArray[0] = "선택";
            for (int i = 0; i < classrooms.size(); i++) {
                classArray[i + 1] = classrooms.get(i);
            }
            Class.setModel(new DefaultComboBoxModel<>(classArray));
            System.out.println("[ReservedRoomView] 강의실 로드 완료: " + classrooms.size() + "개");
        }
        
        // 실습실 목록 로드
        java.util.List<String> labs = manager.getLabs();
        if (labs != null && !labs.isEmpty()) {
            String[] labArray = new String[labs.size() + 1];
            labArray[0] = "선택";
            for (int i = 0; i < labs.size(); i++) {
                labArray[i + 1] = labs.get(i);
            }
            Lab.setModel(new DefaultComboBoxModel<>(labArray));
            System.out.println("[ReservedRoomView] 실습실 로드 완료: " + labs.size() + "개");
        }
    }
    
    public ReservedRoomView(RoomSelect roomSelect) {
        initComponents();
        this.roomSelect = roomSelect;
        this.executive = null;      
        initDatePicker();                
    }

    // ✔ getter로 컨트롤러에서 창 재활용 여부 판단
    public Executive getExecutive() {
        return executive;
    }

    public RoomSelect getRoomSelect() {
        return roomSelect;
    }

    // 뷰 UI 컴포넌트 접근용 Getter
    public JComboBox<String> getClassComboBox() {
        return Class;
    }

    public JComboBox<String> getLabComboBox() {
        return Lab;
    }

    public JButton getCheckButton() {
        return Check;
    }

    public JButton getBeforeButton() {
        return Before;
    }

    public JTable getTable() {
        return jTable1;
    }

    public boolean isUpdating() {
        return isUpdating;
    }

    public void setUpdating(boolean updating) {
        this.isUpdating = updating;
    }

    public String getSelectedRoom() {
        String classRoom = (String) Class.getSelectedItem();
        String labRoom = (String) Lab.getSelectedItem();
        return !"선택".equals(classRoom) ? classRoom : labRoom;
    }

    public void resetClassSelection() {
        Class.setSelectedItem("선택");
    }

    public void resetLabSelection() {
        Lab.setSelectedItem("선택");
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
        jTable1 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        Class = new javax.swing.JComboBox<>();
        Lab = new javax.swing.JComboBox<>();
        Check = new javax.swing.JButton();
        Before = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"1교시(09:00-10:00)", "", null, null, null, null, null, null},
                {"2교시(10:00-11:00)", null, null, null, null, null, null, null},
                {"3교시(11:00-12:00)", null, null, null, null, null, null, null},
                {"4교시(12:00-13:00)", null, null, null, null, null, null, null},
                {"5교시(13:00-14:00)", null, null, null, null, null, null, null},
                {"6교시(14:00-15:00)", null, null, null, null, null, null, null},
                {"7교시(15:00-16:00)", null, null, null, null, null, null, null},
                {"8교시(16:00-17:00)", null, null, null, null, null, null, null},
                {"9교시(17:00-18:00)", null, null, null, null, null, null, null}
            },
            new String [] {
                "", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jLabel1.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        jLabel1.setText("예약 내역 조회");

        jLabel2.setText("강의실");

        jLabel3.setText("실습실");

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

        Check.setText("조회");
        Check.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CheckActionPerformed(evt);
            }
        });

        Before.setText("이전");
        Before.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BeforeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(Class, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(Lab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(42, 42, 42))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 843, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(Before)
                                .addGap(515, 515, 515)
                                .addComponent(Check))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(294, 294, 294)
                        .addComponent(jLabel1)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(73, 73, 73)
                .addComponent(jLabel3)
                .addGap(60, 60, 60))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Class, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Lab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Check)
                    .addComponent(Before))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void CheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CheckActionPerformed
     
    }//GEN-LAST:event_CheckActionPerformed

    private void ClassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClassActionPerformed
      
    }//GEN-LAST:event_ClassActionPerformed

    private void LabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LabActionPerformed
    
    }//GEN-LAST:event_LabActionPerformed

    private void BeforeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BeforeActionPerformed
      
    }//GEN-LAST:event_BeforeActionPerformed

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
            java.util.logging.Logger.getLogger(ReservedRoomView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ReservedRoomView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ReservedRoomView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ReservedRoomView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                //new ReservedRoomView().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Before;
    private javax.swing.JButton Check;
    private javax.swing.JComboBox<String> Class;
    private javax.swing.JComboBox<String> Lab;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
