package dao;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;
import com.sun.org.apache.regexp.internal.RE;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

class ImCourseService implements CourseService {

    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement(
                    "insert into Course(courseId,courseName,credit,classHour,grading) values (?,?,?,?,?)");
            PreparedStatement stmt2 = connection.prepareStatement("insert into CoursePre(courseId,preId,groupId) values (?,?,?)")) {
            stmt.setString(1,courseId);
            stmt.setString(2,courseName);
            stmt.setInt(3,credit);
            stmt.setInt(4,classHour);
            stmt.setString(5,grading.toString());
            stmt.execute();
            if (prerequisite == null){
                return;
            }
            String[][] temp = prerequisite.when(new Prerequisite.Cases<>() {
                @Override
                public String[][] match(AndPrerequisite self) {//AND关系
                    List<String[][]> children = self.terms.stream()
                            .map(term -> term.when(this)).collect(Collectors.toList());
                    int row = 1;
                    int col = 0;
                    for (int i=0;i<children.size();i++){//list中有n个二维数组
                        row = row*children.get(i).length;//获取二维数组的行
                        col = col+children.get(i)[0].length;//获取二维数组的列
                    }
                    String[][] ANDALL = new String[row][col];
                    int rowindex = 0;
                    int colindex = 0;
                    int rowor = row;
                    //下面的循环要循环n次，直到填充整个ANDALL二维数组
                    for (int i=0;i<children.size();i++){//按块循环n次,每次选中一块
                        //对于第i块
                        rowindex = 0;
                        while (rowindex<rowor){
                            for (int p=0;p<children.get(i).length;p++){//对第几行赋值
                                for (int x=rowindex;x<rowindex+row/children.get(i).length;x++){//复制到总表的哪一行，且复制几次
                                    for (int y=colindex,q=0;y<colindex+children.get(i)[0].length && q<children.get(i)[0].length;y++,q++){//总表和单表两列同时变化
                                        ANDALL[x][y] = children.get(i)[p][q];
                                    }
                                }
                                rowindex = rowindex + row/children.get(i).length;
                            }
                        }
                        row = row/children.get(i).length;
                        colindex = colindex + children.get(i)[0].length;
                    }
                    return ANDALL;
                }

                @Override
                public String[][] match(OrPrerequisite self) {
                    List<String[][]> children = self.terms.stream()
                            .map(term -> term.when(this)).collect(Collectors.toList());
                    int row = 0;
                    int col = 0;
                    int rowIndex = 0;
                    int colIndex = 0;
                    for (int i=0;i<children.size();i++){//选取list中最大的列数
                        col = Math.max(col,children.get(i)[0].length);
                        row += children.get(i).length;
                    }
                    String[][] ORAll = new String[row][col];
                    for (int i=0;i< children.size();i++){//每次顺序往下复制一块
                        for (int x=0;x<children.get(i).length && rowIndex<row;x++,rowIndex++){
                            colIndex = 0;
                            for (int y=0;y<children.get(i)[0].length && colIndex<children.get(i)[0].length;y++,colIndex++){
                                ORAll[rowIndex][colIndex] = children.get(i)[x][y];
                            }
                        }
                    }
                    return ORAll;
                }

                @Override
                public String[][] match(CoursePrerequisite self) {
                    String[][] courseAll = new String[1][1];
                    courseAll[0][0] = self.courseID;
                    return courseAll;
                }
            });
            //得到的temp是一行一个group
            for (int i=0;i<temp.length;i++){//i是groupid
                for (int j=0;j<temp[i].length;j++){
                    if (temp[i][j]!=null){
                        stmt2.setString(1,courseId);
                        stmt2.setString(2,temp[i][j]);
                        stmt2.setInt(3,i+1);
                    }
                }
            }
            stmt2.execute();
        } catch (SQLException| IntegrityViolationException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("addCourse successful！");
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        int id = 0;
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement(
                    "insert into CourseSection(courseId,semesterId,sectionName,totalCapacity,leftCapacity)" +
                            " values (?,?,?,?,?)")) {
            stmt.setString(1,courseId);
            stmt.setInt(2,semesterId);
            stmt.setString(3,sectionName);
            stmt.setInt(4,totalCapacity);
            stmt.setInt(5,totalCapacity);//插入时课的剩余量和初始量相同
            stmt.executeUpdate();
            PreparedStatement stmt1 = connection.prepareStatement("select * from CourseSection where courseId = ? and semesterId = ? and sectionName = ? and totalCapacity = ? and leftCapacity = ?");
            stmt1.setString(1,courseId);
            stmt1.setInt(2,semesterId);
            stmt1.setString(3,sectionName);
            stmt1.setInt(4,totalCapacity);
            stmt1.setInt(5,totalCapacity);
            ResultSet rs = stmt1.executeQuery();
            if (rs.next()){
                id = rs.getInt(1);
                //System.out.println("Coursesectionid自增id="+id);
            }
        } catch (SQLException | IntegrityViolationException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("addCourseSection successful！");
        return id;
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList,
                                     short classStart, short classEnd, String location) {
        int id = 0;
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement(
                    "insert into CourseSectionClass(sectionId,instructorId,dayOfWeek,weekList,classBegin,classEnd,location)" +
                            " values (?,?,?,ARRAY[?,?,?,?,?,?,?,?,?,?,?,?,?,?,?],?,?,?)")) {
            stmt.setInt(1,sectionId);
            stmt.setInt(2,instructorId);
            if (dayOfWeek == DayOfWeek.MONDAY){
                stmt.setString(3,"MONDAY");
            }else if (dayOfWeek == DayOfWeek.TUESDAY){
                stmt.setString(3,"TUESDAY");
            }else if (dayOfWeek == DayOfWeek.WEDNESDAY){
                stmt.setString(3,"WEDNESDAY");
            }else if (dayOfWeek == DayOfWeek.THURSDAY){
                stmt.setString(3,"THURSDAY");
            }else if (dayOfWeek == DayOfWeek.FRIDAY){
                stmt.setString(3,"FRIDAY");
            }else if (dayOfWeek == DayOfWeek.SATURDAY){
                stmt.setString(3,"SATURDAY");
            }else if (dayOfWeek == DayOfWeek.SUNDAY){
                stmt.setString(3,"SUNDAY");
            }
            int index = 4;
            for (Short s : weekList) {
                stmt.setString(index,s.toString());
                //System.out.print(s.toString()+" ");
                index++;
            }
            if (index<19){
                for (int i=index;i<19;i++){
                    stmt.setString(i,null);
                }
            }
            stmt.setShort(19,classStart);
            stmt.setShort(20,classEnd);
            stmt.setString(21,location);
            stmt.executeUpdate();
            PreparedStatement stmt1 = connection.prepareStatement("select max(courseSectionClassId) from CourseSectionClass");
            ResultSet rs = stmt1.executeQuery();
            if (rs.next()){
                id = rs.getInt(1);
                //System.out.println("Coursesectionclassid自增id="+id);
            }
        } catch (SQLException | IntegrityViolationException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("addCourseSectionClass successful！");
        return id;
    }

    /**
     * To remove an entity from the system, related entities dependent on this entity
     * (usually rows referencing the row to remove through foreign keys in a relational database)
     * shall be removed together.
     * More specifically, remove all related courseSection, all related CourseSectionClass and all related select course records
     * when a course has been removed
     * @param courseId
     */
    @Override
    public void removeCourse(String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from Course where courseId = ?");
             PreparedStatement stmt2 = connection.prepareStatement("delete from CourseSection where courseId = ?");
             PreparedStatement stmt7 = connection.prepareStatement("delete from Coursepre where courseId = ?");
             PreparedStatement stmt8 = connection.prepareStatement("delete from MajorCourse where courseId = ?");
             PreparedStatement stmt9 = connection.prepareStatement("delete from Coursepre where preId = ?")
        ) {
            PreparedStatement stmt3 = connection.prepareStatement("select s.sectionId from CourseSection s" +
                    " join CourseSectionClass CSC on s.sectionId = CSC.sectionId where s.courseId = ?");
            stmt3.setString(1,courseId);
            ResultSet rs = stmt3.executeQuery();
            PreparedStatement stmt4 = connection.prepareStatement("delete from CourseSectionClass where sectionId = ?");
            PreparedStatement stmt5 = connection.prepareStatement("delete from enrollCourseSection where sectionId = ?");
            PreparedStatement stmt6 = connection.prepareStatement("delete from enrollCourseGrade where sectionId = ?");
            while (rs.next()){
                stmt5.setInt(1,rs.getInt("sectionId"));
                stmt5.execute();
                stmt6.setInt(1,rs.getInt("sectionId"));
                stmt6.execute();
                stmt4.setInt(1,rs.getInt("sectionId"));
                stmt4.execute();
            }
            stmt9.setString(1,courseId);
            stmt9.execute();
            stmt7.setString(1,courseId);
            stmt7.execute();
            stmt8.setString(1,courseId);
            stmt8.execute();
            stmt2.setString(1,courseId);
            stmt2.execute();
            stmt.setString(1, courseId);
            stmt.execute();
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("removeCourse successful!");
    }

    /**
     *  To remove an entity from the system, related entities dependent on this entity (usually rows referencing the row to remove through foreign keys in a relational database)
     *  shall be removed together.
     *   More specifically, remove all related CourseSectionClass and all related select course records
     *   when a courseSection has been removed
     * @param sectionId
     */
    @Override
    public void removeCourseSection(int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from CourseSection where sectionId = ?");
             PreparedStatement stmt2 = connection.prepareStatement("delete from CourseSectionClass where sectionId = ?");
             PreparedStatement stmt3 = connection.prepareStatement("delete from enrollCourseSection where sectionId = ?");
             PreparedStatement stmt4 = connection.prepareStatement("delete from enrollCourseGrade where sectionId = ?")
        ) {
            stmt3.setInt(1,sectionId);
            stmt3.execute();
            stmt4.setInt(1,sectionId);
            stmt4.execute();
            stmt2.setInt(1, sectionId);
            stmt2.execute();
            stmt.setInt(1, sectionId);
            stmt.execute();
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();//打印异常信息
            System.exit(0);
        }
        //System.out.println("removeCourseSection successful!");
    }

    /**
     *  To remove an entity from the system, related entities dependent on this entity (usually rows referencing the row to remove through foreign keys in a relational database)
     *  shall be removed together.
     *  More specifically, only remove course section class
     * @param classId
     */
    @Override
    public void removeCourseSectionClass(int classId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("delete from CourseSectionClass where classId = ?")) {
            stmt.setInt(1, classId);
            stmt.execute();
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("removeCourseSectionClass successful!");
    }

    @Override
    public List<Course> getAllCourses() {//获取所有课程信息-->select
        List<Course> list = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from Course")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                Course course = new Course();
                course.id = rs.getString("courseId");
                course.name = rs.getString("courseName");
                course.credit = rs.getInt("credit");
                course.classHour = rs.getInt("classHour");
                if (rs.getString("grading").equals("PASS")){
                    course.grading = Course.CourseGrading.PASS_OR_FAIL;
                }else if (rs.getString("grading").equals("FAIL")){
                    course.grading = Course.CourseGrading.PASS_OR_FAIL;
                } else{
                    course.grading = Course.CourseGrading.HUNDRED_MARK_SCORE;
                }
                list.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("getAllCourses successful!");
        return list;
    }

    /**
     * Return all satisfied CourseSections.
     * We will compare the all other fields in CourseSection besides the id.
     * @param courseId if the key is non-existent, please throw an EntityNotFoundException.
     * @param semesterId if the key is non-existent, please throw an EntityNotFoundException.
     * @return
     */
    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId){
        List<CourseSection> list = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from CourseSection where courseId = ? and semesterId = ?")){
            stmt.setString(1,courseId);
            stmt.setInt(2,semesterId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                CourseSection courseSection = new CourseSection();
                courseSection.id = rs.getInt("sectionId");
                courseSection.name = rs.getString("name");
                courseSection.totalCapacity = rs.getInt("totalCapacity");
                courseSection.leftCapacity = rs.getInt("leftCapacity");
                list.add(courseSection);
            }
        } catch (EntityNotFoundException | SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("getCourseSectionsInSemester successful!");
        return list;
    }

    /**
     * If there is no Course about specific id, throw EntityNotFoundException.
     * @param sectionId if the key is non-existent, please throw an EntityNotFoundException.
     * @return
     */
    @Override
    public Course getCourseBySection(int sectionId){
        Course coursegetBySection = new Course();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from CourseSection where sectionId = ?")) {
            stmt.setInt(1,sectionId);
            //stmt.execute();
            ResultSet rs = stmt.executeQuery();
            coursegetBySection.id = rs.getString("courseId");
            coursegetBySection.name = rs.getString("courseName");
            coursegetBySection.credit = rs.getInt("credit");
            coursegetBySection.classHour = rs.getInt("classHour");
            coursegetBySection.grading = (Course.CourseGrading) rs.getObject("gradig");
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("getCourseBySections successful!");
        return coursegetBySection;
    }

    /**
     *
     * @param sectionId the id of {@code CourseSection}
     *                  if the key is non-existent, please throw an EntityNotFoundException.
     * @return
     */
    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        List<CourseSectionClass> list = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select * from CourseSectionClasses where sectionId = ?")) {
            stmt.setInt(1,sectionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                CourseSectionClass courseSectionClass = new CourseSectionClass();
                courseSectionClass.id = rs.getInt("id");
                courseSectionClass.instructor = (Instructor) rs.getObject("instructor");
                courseSectionClass.dayOfWeek = (DayOfWeek) rs.getObject("dayOfWeek");
                courseSectionClass.weekList = (Set<Short>) rs.getObject("weekList");
                courseSectionClass.classBegin = rs.getShort("classBegin");
                courseSectionClass.classEnd = rs.getShort("classEnd");
                courseSectionClass.location = rs.getString("location");
                list.add(courseSectionClass);
            }
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("getCourseSectionsClass successful!");
        return list;
    }

    /**
     *
     * @param classId if the key is non-existent, please throw an EntityNotFoundException.
     * @return
     */
    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        CourseSection courseSection = new CourseSection();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select s.sectionid,s.sectionName,s.totalCapacity,s.leftCapacity from coursesection s" +
                     "join CourseSectionClass CSC on s.sectionId = CSC.sectionId where CSC.courseSectionClassId = ?")) {
            stmt.setInt(1,classId);
            ResultSet rs = stmt.executeQuery();
            courseSection.id = rs.getInt("s.sectionId");
            courseSection.name = rs.getString("s.sectionName");
            courseSection.totalCapacity = rs.getInt("s.totalCapacity");
            courseSection.leftCapacity = rs.getInt("s.leftCapacity");
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("getCourseSectionsByClass successful!");
        return courseSection;
    }

    /**
     *
     * @param courseId  if the key is non-existent, please throw an EntityNotFoundException.
     * @param semesterId if the key is non-existent, please throw an EntityNotFoundException.
     * @return
     */
    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {//获取该学期选该门课课表的学生
        List<Student> list = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select userId,fullName,enrolledDate,major_id,M.name,department_id,D.name from CourseSection section" +
                     "join enrollCourseSection e on section.sectionId = e.sectionId" +
                     "join Student st on st.userid = e.studentId" +
                     "join Major M on st.major_id = M.majorId"+
                     "join Department D on D.departmentId = M.department_id"+
                     "where section.courseId = ? and section.semesterId = ?");
             ) {
            stmt.setInt(1,semesterId);
            stmt.setString(2,courseId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                Student student = new Student();
                Major major = new Major();
                Department department = new Department();
                student.id = rs.getInt("userId");
                student.fullName = rs.getString("fullName");
                student.enrolledDate = rs.getDate("enrolledDate");
                major.id = rs.getInt("major_id");
                major.name = rs.getString("M.name");
                department.id = rs.getInt("department_id");
                department.name = rs.getString("D.name");
                major.department = department;
                student.major = major;
                list.add(student);
            }
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        //System.out.println("getEnrolledStudentsInSemester successful!");
        return list;
    }
}
