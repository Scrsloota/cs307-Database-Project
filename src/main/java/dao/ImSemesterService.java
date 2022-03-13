package dao;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class ImSemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        int index = 0;
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into Semester(name,beginDate,endDate) values (?,?,?)")) {
            //stmt.setInt(1,index);//自增id
            stmt.setString(1, name);
            stmt.setDate(2, begin);
            stmt.setDate(3, end);
            stmt.executeUpdate();
            PreparedStatement stmt1 = connection.prepareStatement("select * from Semester where name = ?");
            stmt1.setString(1,name);
            ResultSet rs = stmt1.executeQuery();
            if (rs.next()){
                index = rs.getInt(1);
            }
        } catch (SQLException | IntegrityViolationException e) {
            e.printStackTrace();
        }
        //System.out.println("addSemester successful!");
        return index;
    }

    @Override
    public void removeSemester(int semesterId) {
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
        PreparedStatement stmt = connection.prepareStatement("select sectionid from coursesection where semesterid = ?");
            PreparedStatement stmt1 = connection.prepareStatement("delete from coursesectionclass where sectionid = ?");
            PreparedStatement stmt2 = connection.prepareStatement("delete from enrollCourseSection where sectionid = ?");
            PreparedStatement stmt3 = connection.prepareStatement("delete from enrollcoursegrade where sectionid = ?");
        PreparedStatement stmt4 = connection.prepareStatement("delete from semester where semesterid = ?")
        ){
            stmt.setInt(1,semesterId);
            ResultSet rs1 = stmt.executeQuery();
            while (rs1.next()){
                int index = rs1.getInt("sectionId");
                stmt1.setInt(1,index);
                stmt1.executeUpdate();
                stmt2.setInt(1,index);
                stmt2.executeUpdate();
                stmt3.setInt(1,index);
                stmt3.executeUpdate();
            }
            stmt4.setInt(1,semesterId);
            stmt4.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        List<Semester> semesters = new LinkedList<>();
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement("select * from Semester")){
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                Semester curSemester = new Semester();
                curSemester.id = rs.getInt("semesterId");
                curSemester.name = rs.getString("name");
                curSemester.begin = rs.getDate("beginDate");
                curSemester.end = rs.getDate("endDate");
                semesters.add(curSemester);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return semesters;
    }

    @Override
    public Semester getSemester(int semesterId) {
        Semester curSemester = new Semester();
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement("select * from Semester where semesterId = ?")){
            stmt.setInt(1,semesterId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                curSemester.id = rs.getInt("SemesterId");
                curSemester.name = rs.getString("name");
                curSemester.begin = rs.getDate("beginDate");
                curSemester.end = rs.getDate("endDate");
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return curSemester;
    }
}
