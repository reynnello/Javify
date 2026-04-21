package com.javify;

import com.javify.dao.UserDAO;
import com.javify.db.DatabaseManager;
import org.junit.jupiter.api.*;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {

    private static UserDAO userDAO;
    private static final String TEST_USER = "test_user_" + System.currentTimeMillis();
    private static final String TEST_PASS = "testpass123";

    @BeforeAll
    static void setup() {
        DatabaseManager.initDatabase();
        userDAO = new UserDAO(DatabaseManager.URL);
    }

    @Test
    @Order(1)
    void testRegisterSuccess() {
        boolean result = userDAO.register(TEST_USER, TEST_PASS);
        assertTrue(result, "Registration should succeed for new user");
    }

    @Test
    @Order(2)
    void testRegisterDuplicateUser() {
        boolean result = userDAO.register(TEST_USER, TEST_PASS);
        assertFalse(result, "Registration should fail for duplicate username");
    }

    @Test
    @Order(3)
    void testLoginSuccess() {
        int userId = userDAO.login(TEST_USER, TEST_PASS);
        assertTrue(userId > 0, "Login should return valid user ID");
    }

    @Test
    @Order(4)
    void testLoginWrongPassword() {
        int userId = userDAO.login(TEST_USER, "wrongpassword");
        assertEquals(-1, userId, "Login should fail with wrong password");
    }

    @Test
    @Order(5)
    void testLoginNonExistentUser() {
        int userId = userDAO.login("nonexistent_xyz", TEST_PASS);
        assertEquals(-1, userId, "Login should fail for non-existent user");
    }

    @Test
    @Order(6)
    void testLoginBlankFields() {
        assertEquals(-1, userDAO.login("", TEST_PASS));
        assertEquals(-1, userDAO.login(TEST_USER, ""));
        assertEquals(-1, userDAO.login(null, TEST_PASS));
    }

    @Test
    @Order(7)
    void testChangePassword() {
        String newPass = "newpass456";
        boolean result = userDAO.changePassword(
                userDAO.login(TEST_USER, TEST_PASS),
                TEST_PASS,
                newPass
        );
        assertTrue(result, "Password change should succeed");
        assertTrue(userDAO.login(TEST_USER, newPass) > 0, "Login with new password should work");
    }

    @AfterAll
    static void cleanup() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, TEST_USER);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}