package Controller;

import View.RoomAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomAdminControllerTest {

    @Mock private RoomAdmin mockView;
    @Mock private JButton mockConfirmButton;
    @Mock private JButton mockBackButton;
    @Mock private NetworkFacade mockNetworkFacade;

    @BeforeEach
    void setUp() throws Exception {
        when(mockView.getConfirmButton()).thenReturn(mockConfirmButton);
        when(mockConfirmButton.getActionListeners()).thenReturn(new ActionListener[0]);
        when(mockView.getJButton2()).thenReturn(mockBackButton);

        RoomAdminController controller = new RoomAdminController(mockView);
        
        // Facade 주입 (실제 로직 테스트 시 필요)
        Field field = RoomAdminController.class.getDeclaredField("networkFacade");
        field.setAccessible(true);
        field.set(controller, mockNetworkFacade);
    }

    @Test
    void testListenersAttached() {
        verify(mockConfirmButton, atLeastOnce()).addActionListener(any(ActionListener.class));
        verify(mockBackButton).addActionListener(any(ActionListener.class));
    }
}