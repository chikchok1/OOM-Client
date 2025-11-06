/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controller;

/**
 *
 * @author minju
 */

import View.RoomAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.swing.*;
import java.awt.event.ActionListener;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomAdminControllerTest {

    @Mock private RoomAdmin mockView;
    @Mock private JButton mockConfirmButton;
    @Mock private JButton mockBackButton;

    @BeforeEach
    void setUp() {
        when(mockView.getConfirmButton()).thenReturn(mockConfirmButton); // ✅ 추가
        when(mockConfirmButton.getActionListeners()).thenReturn(new ActionListener[0]); // ✅ 필수
        when(mockView.getJButton2()).thenReturn(mockBackButton);

        new RoomAdminController(mockView);
    }

    @Test
    void testListenersAttached() {
        verify(mockConfirmButton, atLeastOnce()).addActionListener(any(ActionListener.class));
        verify(mockBackButton).addActionListener(any(ActionListener.class));
    }
}
