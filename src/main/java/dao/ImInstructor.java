package dao;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.InstructorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class ImInstructor implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement("insert into Instructor(userId,firstname,lastname) values (?,?,?)")){
            stmt.setInt(1,userId);
            stmt.setString(2,firstName);
            stmt.setString(3,lastName);
            stmt.execute();
            //System.out.println("Add Instructor successful!");
        }catch (SQLException | IntegrityViolationException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        List<CourseSection> CourseSectionList = new LinkedList<>();
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
        PreparedStatement stmt = connection.prepareStatement
                ("select cs.sectionId,cs.sectionName,cs.totalCapacity,cs.leftCapacity \n" +
                        "from CourseSection cs join CourseSectionClass csc on cs.sectionId = csc.sectionId \n" +
                        "where csc.instructorId = ? and cs.semesterId = ?")){
            stmt.setInt(1,instructorId);
            stmt.setInt(2,semesterId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                CourseSection newCourseSection = new CourseSection();
                newCourseSection.id = rs.getInt("sectionId");
                newCourseSection.name = rs.getString("sectionName");
                newCourseSection.totalCapacity = rs.getInt("totalCapacity");
                newCourseSection.leftCapacity = rs.getInt("leftCapacity");
                CourseSectionList.add(newCourseSection);
            }
        }catch (SQLException | EntityNotFoundException e){
            e.printStackTrace();
        }
        return CourseSectionList;
    }
}
