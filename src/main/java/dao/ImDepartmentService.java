package dao;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;
import cn.edu.sustech.cs307.service.MajorService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImDepartmentService implements DepartmentService {
    /**
     *  if adding a new department which has the same name with an existing department,
     *  it should throw an {@code IntegrityViolationException}
     * @param name
     * @return
     */
    @Override
    public int addDepartment(String name) {
        int id = 0;
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement("insert into Department(name) values (?)");
            ) {
            stmt.setString(1,name);
            stmt.executeUpdate();
            PreparedStatement stmt1 = connection.prepareStatement("select * from Department where name = ?");
            stmt1.setString(1,name);
            ResultSet rs = stmt1.executeQuery();
            if (rs.next()){
                id = rs.getInt("departmentid");
                //System.out.println("Department自增id="+id);
            }
        }catch (SQLException | IntegrityViolationException e) {
            e.printStackTrace();
        }
        //System.out.println("addDepartment successful!");
        return id;
    }

    @Override
    public void removeDepartment(int departmentId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from Department where departmentId = ?");
             PreparedStatement stmt1 = connection.prepareStatement("select majorId from Major where departmentid = ?");
             PreparedStatement stmt2 = connection.prepareStatement("delete from Major where majorId = ?");
             PreparedStatement stmt3 = connection.prepareStatement("delete from Student where majorId = ?")) {
            stmt1.setInt(1,departmentId);
            ResultSet rs1 = stmt1.executeQuery();//要删除的专业
            while (rs1.next()){
                stmt3.setInt(1,rs1.getInt("majorId"));
                stmt3.execute();
                stmt2.setInt(1,rs1.getInt("majorId"));
                stmt2.execute();
            }
            stmt.setInt(1, departmentId);
            stmt.execute();
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("removeDepartment successful!");
    }

    @Override
    public List<Department> getAllDepartments() {
        List<Department> list = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from Department")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                Department department = new Department();
                department.id = rs.getInt("departmentId");
                department.name = rs.getString("name");
                list.add(department);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("getAllDepartments successful!");
        return list;
    }

    @Override
    public Department getDepartment(int departmentId) {
        Department department = new Department();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from Department where departmentId = ?")) {
            stmt.setInt(1,departmentId);
            ResultSet rs = stmt.executeQuery();
            department.id = rs.getInt("departmentId");
            department.name = rs.getString("name");
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("getDepartment successful!");
        return department;
    }
}
