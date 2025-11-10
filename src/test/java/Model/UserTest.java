package Model;
/**
 *
 * @author minju
 */
import common.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserConstructorAndGetter() {
        // given
        String userId = "S20230001";
        String password = "pass1234";
        String name = "홍길동";

        // when
        User user = new User(userId, password, name);

        // then
        assertEquals(userId, user.getUserId());
        assertEquals(password, user.getPassword());
        assertEquals(name, user.getName());
    }
}
