package com.cybergame.repository.sql;

import com.cybergame.model.entity.ServiceItem;
import com.cybergame.repository.ServiceItemRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceItemRepositorySQL implements ServiceItemRepository {

    @Override
    public void save(ServiceItem s) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO services
                (id, name, price, locked)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    price  = VALUES(price),
                    locked = VALUES(locked)
             """)) {

            ps.setInt(1, s.getServiceId());
            ps.setString(2, s.getName());
            ps.setDouble(3, s.getUnitPrice());
            ps.setBoolean(4, s.isLocked());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ServiceItem> findAll() {

        List<ServiceItem> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM services")) {

            while (rs.next()) {

                ServiceItem s = new ServiceItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price")
                );

                // 🔒 map locked
                if (rs.getBoolean("locked")) {
                    s.lock();
                }

                list.add(s);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    @Override
    public void delete(ServiceItem s) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "DELETE FROM services WHERE id=?")) {

            ps.setInt(1, s.getServiceId());
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
