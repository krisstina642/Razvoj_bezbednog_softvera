package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

@Repository
public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);
    private static final AuditLogger AUDIT_LOG = AuditLogger.getAuditLogger(UserRepository.class);

    private DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User findUser(String username) {
        String query = "SELECT id, username, password FROM users WHERE username=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setString(1,username);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                String username1 = rs.getString(2);
                String password = rs.getString(3);
                return new User(id, username1, password);
            }
        } catch (SQLException e) {
            LOG.warn("FindUser failed for USERNAME: "+username, e);
        }
        return null;
    }

    public boolean validCredentials(String username, String password) {
        String query = "SELECT username FROM users WHERE username=? AND password=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1,username);
            statement.setString(2,password);
            ResultSet rs = statement.executeQuery();

            return rs.next();
        } catch (SQLException e) {
            LOG.warn("ValidCredentials failed for USERNAME: "+ username + "; PASSWORD: "+password, e);
        }
        return false;
    }

    public void delete(int userId) {
        String query = "DELETE FROM users WHERE id = ?" ;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setInt(1,userId);
            statement.executeUpdate();
            AuditLogger.getAuditLogger(UserRepository.class)
                    .audit("Delete customer with id - "+ userId);
        } catch (SQLException e) {
            LOG.warn("UserDelete failed for userID: "+userId +"; by USER with ID: "+ SecurityUtil.getCurrentUser().getId(), e);
        }
    }
}
