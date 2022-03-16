package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.Entity;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CustomerRepository.class);

    private DataSource dataSource;

    public CustomerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Person createPersonFromResultSet(ResultSet rs){

        int id = 0;
        String firstName = null;
        String lastName = null;
        String personalNumber = null;
        String address = null;
        try {
            id = rs.getInt(1);
            firstName = rs.getString(2);
            lastName = rs.getString(3);
            personalNumber = rs.getString(4);
            address = rs.getString(5);
        } catch (SQLException e) {
            LOG.warn("CreatePersonFromResultSet failed", e);
        }
        return new Person(id, firstName, lastName, personalNumber, address);
    }

    public List<Customer> getCustomers() {
        List<com.zuehlke.securesoftwaredevelopment.domain.Customer> customers = new ArrayList<>();
        String query = "SELECT id, username FROM users";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                customers.add(createCustomer(rs));
            }

        } catch (SQLException e) {
            LOG.warn("GetCustomers failed", e);
        }
        return customers;
    }

    private com.zuehlke.securesoftwaredevelopment.domain.Customer createCustomer(ResultSet rs){
        try {
            return new Customer(rs.getInt(1), rs.getString(2));
        } catch (SQLException e) {
            LOG.warn("CreateCustomer failed", e);
        }
        return null;
    }

    public List<Restaurant> getRestaurants() {
        List<Restaurant> restaurants = new ArrayList<>();
        String query = "SELECT r.id, r.name, r.address, rt.name  FROM restaurant AS r JOIN restaurant_type AS rt ON r.typeId = rt.id ";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                restaurants.add(createRestaurant(rs));
            }

        } catch (SQLException e) {
            LOG.warn("GetRestaurants failed", e);
        }
        return restaurants;
    }

    private Restaurant createRestaurant(ResultSet rs){
        int id = 0;
        String name = null;
        String address = null;
        String type = null;
        try {
            id = rs.getInt(1);
            name = rs.getString(2);
            address = rs.getString(3);
            type = rs.getString(4);
        } catch (SQLException e) {
            LOG.warn("CreateRestaurant failed", e);
        }

        return new Restaurant(id, name, address, type);
    }


    public Object getRestaurant(String id) {
        String query = "SELECT r.id, r.name, r.address, rt.name  FROM restaurant AS r JOIN restaurant_type AS rt ON r.typeId = rt.id WHERE r.id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setString(1,id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return createRestaurant(rs);
            }

        } catch (SQLException e) {
            LOG.warn("GetRestaurant failed for ID: "+id, e);
        }
        return null;
    }

    public void deleteRestaurant(int id) {
        String query = "DELETE FROM restaurant WHERE id=?" ;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setInt(1, id);
            statement.executeUpdate();
            auditLogger.audit("Delete restaurant with id - "+ id);
        } catch (SQLException e) {
            LOG.warn("DeleteRestaurant failed for ID: "+id +"; by USER with ID: "+ SecurityUtil.getCurrentUser().getId(), e);
        }
    }

    public void updateRestaurant(RestaurantUpdate restaurantUpdate) {
        String query = "UPDATE restaurant SET name = ?, address=?, typeId =? WHERE id =?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setString(1,restaurantUpdate.getName());
            statement.setString(2,restaurantUpdate.getAddress());
            statement.setInt(3,restaurantUpdate.getRestaurantType());
            statement.setInt(4,restaurantUpdate.getId());
            statement.executeUpdate();
            auditLogger.auditChange(new Entity("UpdateRestaurant", String.valueOf(restaurantUpdate.getId()), "***", restaurantUpdate.getName() ));

        } catch (SQLException e) {
            LOG.warn("UpdateRestaurant failed for ID: " + restaurantUpdate.getId(), e);
        }

    }

    public Customer getCustomer(String id) {
        String sqlQuery = "SELECT id, username, password FROM users WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery)
        ) {
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return createCustomerWithPassword(rs);
            }

        } catch (SQLException e) {
            LOG.warn("GetCustomer failed for ID "+id, e);
        }
        return null;
    }

    private Customer createCustomerWithPassword(ResultSet rs){
        int id = 0;
        String username = null;
        String password = null;
        try {
            id = rs.getInt(1);
            username = rs.getString(2);
            password = rs.getString(3);
        } catch (SQLException e) {
            LOG.warn("createCustomerWithPassword failed ", e);
        }
        return new Customer(id, username, password);
    }


    public void deleteCustomer(String id) {
        String query = "DELETE FROM users WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, id);
            statement.executeUpdate();

            auditLogger.audit("Delete customer with id - "+ id);
        } catch (SQLException e) {
            LOG.warn("CustomerDelete failed for userID: "+id +"; by USER with ID: "+ SecurityUtil.getCurrentUser().getId(), e);
        }
    }

    public void updateCustomer(CustomerUpdate customerUpdate) {
        String query = "UPDATE users SET username = ?, password=? WHERE id =?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, customerUpdate.getUsername());
            statement.setString(2, customerUpdate.getPassword());
            statement.setInt(3, customerUpdate.getId());
            statement.executeUpdate();
            auditLogger.auditChange(new Entity("UpdateCustomer", String.valueOf(customerUpdate.getId()),"***", customerUpdate.getUsername() ));
        } catch (SQLException e) {
            LOG.warn("UpdateCustomer failed for userID: " + customerUpdate.getId(), e);
        }
    }

    public List<Address> getAddresses(String id) {
        String sqlQuery = "SELECT id, name FROM address WHERE userId= ? ";
        List<Address> addresses = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery)
        ) {
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                addresses.add(createAddress(rs));
            }

        } catch (SQLException e) {
            LOG.warn("GetAddresses failed for userID: " + id, e);
        }
        return addresses;
    }

    private Address createAddress(ResultSet rs) {
        int id = 0;
        String name = null;
        try {
            id = rs.getInt(1);
            name = rs.getString(2);
        } catch (SQLException e) {
            LOG.warn("createAddress failed", e);
        }
        return new Address(id, name);
    }

    public void deleteCustomerAddress(int id) {
        String query = "DELETE FROM address WHERE id=?" ;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setInt(1,id);
            statement.executeUpdate();

            auditLogger.audit("Delete customer address with id - "+ id);
        } catch (SQLException e) {
            LOG.warn("DeleteCustomerAddress failed for ID: " + id +"; by USER with ID: "+ SecurityUtil.getCurrentUser().getId(), e);
        }
    }

    public void updateCustomerAddress(Address address) {
        String query = "UPDATE address SET name = ? WHERE id =?" ;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1,address.getName() );
            statement.setInt(2,address.getId());
            statement.executeUpdate();
            auditLogger.audit("UpdateCustomerAddress "+address.getId());

        } catch (SQLException e) {
            LOG.warn("UpdateCustomerAddress failed for ID: " +address.getId() +"; ADDRESS: "+ address, e);
        }
    }

    public void putCustomerAddress(NewAddress newAddress) {
        String query = "INSERT INTO address (name, userId) VALUES (? , ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1,newAddress.getName() );
            statement.setInt(2,newAddress.getUserId());
            statement.executeUpdate();
            auditLogger.audit("putCustomerAddres to customer with id " + newAddress.getUserId());
        } catch (SQLException e) {
            LOG.warn("PutCustomerAddress failed for ADDRESS: " + newAddress, e);
        }
    }
}
