/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package Main;

import Controller.LoginController;
import View.LoginForm;

/**
 *
 * @author YangJinWon
 */
public class Main {
    public static void main(String[] args) {
        try {
        // Nimbus Look and Feel 설정 (예쁘게 보이게)
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                javax.swing.UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
    } catch (Exception e) {
        System.out.println("Look and Feel 설정 실패: 기본값 사용");
    }
        // 로그인 화면부터 실행
        LoginForm loginForm = new LoginForm();
        //UserDAO userDAO = new UserDAO();

        //LoginController 생성하면서 loginForm에 회원가입 로직도 붙일 예정
        new LoginController(loginForm);

        loginForm.setVisible(true);
    }
    
    }


