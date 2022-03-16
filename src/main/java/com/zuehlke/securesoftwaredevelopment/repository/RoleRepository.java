package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoleRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RoleRepository.class);

    private final DataSource dataSource;

    public RoleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Role> findByUserId(int userId) {
        List<Role> roles = new ArrayList<>();
        String query = "SELECT id, name FROM roles WHERE id IN (SELECT roleId FROM user_to_roles WHERE userId=?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setInt(1,userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                roles.add(new Role(id, name));
            }
        } catch (SQLException e) {
            LOG.warn("FindByUserId failed for ID: "+userId, e);
        }
        return roles;
    }
}
