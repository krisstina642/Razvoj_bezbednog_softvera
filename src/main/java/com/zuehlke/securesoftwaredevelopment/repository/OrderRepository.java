package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private static final Logger LOG = LoggerFactory.getLogger(OrderRepository.class);

    private DataSource dataSource;

    public OrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public List<Food> getMenu(int id) {
        List<Food> menu = new ArrayList<>();
        String sqlQuery = "SELECT id, name FROM food WHERE restaurantId=?" ;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ) {
            statement.setInt(1,id);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                menu.add(createFood(rs));
            }

        } catch (SQLException e) {
            LOG.warn("GetMenu failed for ID: "+id, e);
        }

        return menu;
    }

    private Food createFood(ResultSet rs) {
        int id = 0;
        String name = null;
        try {
            id = rs.getInt(1);
            name = rs.getString(2);
        } catch (SQLException e) {
            LOG.warn("CreateFood failed", e);
        }
        return new Food(id, name);
    }

    public void insertNewOrder(NewOrder newOrder, int userId) {
        LocalDate date = LocalDate.now();
        String sqlQuery1 = "INSERT INTO delivery (isDone, userId, restaurantId, addressId, date, comment) values (FALSE, ?, ?, ?, ?, ?)";
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery1))
        {
            String datum=""+date.getYear()+"-"+date.getMonthValue()+"-"+date.getDayOfMonth();
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, newOrder.getRestaurantId());
            preparedStatement.setInt(3, newOrder.getAddress());
            preparedStatement.setString(4, datum);
            preparedStatement.setString(5, newOrder.getComment());
            preparedStatement.executeUpdate();

            String sqlQuery = "SELECT MAX(id) FROM delivery";
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sqlQuery);

            if (rs.next()) {

                int deliveryId = rs.getInt(1);
                sqlQuery = "INSERT INTO delivery_item (amount, foodId, deliveryId)" +
                        "values";
                for (int i = 0; i < newOrder.getItems().length; i++) {
                    FoodItem item = newOrder.getItems()[i];
                    String deliveryItem = "";
                    if (i > 0) {
                        deliveryItem = ",";
                    }
                    deliveryItem += "(" + item.getAmount() + ", " + item.getFoodId() + ", " + deliveryId + ")";
                    sqlQuery += deliveryItem;
                }
                statement.executeUpdate(sqlQuery);
                LOG.info("InsertNewOrder with ID: " + deliveryId );
            }

        } catch (SQLException e) {
            LOG.warn("InsertNewOrder failed", e);
        }


    }

    public Object getAddresses(int userId) {
        List<Address> addresses = new ArrayList<>();
        String sqlQuery = "SELECT id, name FROM address WHERE userId=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ) {
            statement.setInt(1,userId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                addresses.add(createAddress(rs));
            }

        } catch (SQLException e) {
            LOG.warn("GetAddresses failed for userID: "+userId, e);
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
            LOG.warn("CreateAddress failed", e);
        }
        return new Address(id, name);

    }
}
