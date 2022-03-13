package dao;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ImMajorService implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) {
        int id = 0;
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement(
                    "insert into Major(name,departmentId) values (?,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1,name);
            stmt.setInt(2,departmentId);
            stmt.executeUpdate();
            PreparedStatement stmt1 = connection.prepareStatement("select * from Major where name = ?");
            stmt1.setString(1,name);
            ResultSet rs = stmt1.executeQuery();
            if (rs.next()){
                id = rs.getInt(1);
                //System.out.println("Major自增id="+id);
            }
        } catch (SQLException | IntegrityViolationException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("addMajor successful！");
        return id;
    }

    @Override
    public void removeMajor(int majorId) {//移除专业时，相关的学生也应该被移除
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from Major where majorId = ?");
             PreparedStatement stmt2 = connection.prepareStatement("delete from Student where mojorId = ?")
        ) {
            stmt.setInt(1, majorId);
            stmt.execute();
            stmt2.setInt(1,majorId);
            stmt2.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("removeMajor successful!");
    }

    @Override
    public List<Major> getAllMajors() {
        List<Major> list = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from Major")){
           ResultSet rs = stmt.executeQuery();
           while (rs.next()){
               Major major = new Major();
               major.id = rs.getInt("majorId");
               major.name = rs.getString("name");
               list.add(major);
           }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //System.out.println("getAllMajors successful！");
        return list;
    }

    @Override
    public Major getMajor(int majorId) {
        Major major = new Major();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from Major where majorId = ?")){
            ResultSet rs = stmt.executeQuery();
            major.id = majorId;
            major.name = rs.getString("name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //System.out.println("getMajor(id) successful！");
        return major;
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into MajorCourse(majorId,courseId,property) values(?,?,?)")){
            stmt.setInt(1,majorId);
            stmt.setString(2,courseId);
            stmt.setString(3,"MAJOR_COMPULSORY");
            stmt.execute();
        } catch (SQLException | IntegrityViolationException e) {
            e.printStackTrace();
        }
        //System.out.println("addMajorCompulsoryCourse successful！");
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into MajorCourse(majorId,courseId,property) values(?,?,?)")){
            stmt.setInt(1,majorId);
            stmt.setString(2,courseId);
            stmt.setString(3,"MAJOR_ELECTIVE");
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //System.out.println("addMajorElectiveCourse successful！");
    }
}
