package dao;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;
import com.sun.org.apache.regexp.internal.RE;
import org.jetbrains.annotations.Nullable;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;
public class ImStudentService implements StudentService {
    public static int num = 0;
    public static int countNum = 0;
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into Student(userId,firstname,lastname,enrolledDate,majorId)values(?,?,?,?,?)")){
            stmt.setInt(1,userId);
            stmt.setString(2,firstName);
            stmt.setString(3,lastName);
            stmt.setDate(4, enrolledDate);
            stmt.setInt(5, majorId);
            stmt.execute();
            //System.out.println("Add student successful!");
        } catch (SQLException | IntegrityViolationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId,
                                                @Nullable String searchCid, @Nullable String searchName,
                                                @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek,
                                                @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations,
                                                CourseType searchCourseType,
                                                boolean ignoreFull, boolean ignoreConflict,
                                                boolean ignorePassed, boolean ignoreMissingPrerequisites,
                                                int pageSize, int pageIndex) {
        //num++;
        //System.out.println(num);
        List<CourseSearchEntry> list = new ArrayList<>();
        String courseId = "  and a.courseid like '%'|| ? ||'%'\n";
        String name = "  and (a.coursename||'['||b.sectionname||']')like ('%'|| ? ||'%')\n";
        String InstructorName = "  and ((I.firstname like '%'|| ? ||'%') or (I.lastname like '%'|| ? ||'%') or ((I.firstname||I.lastname) like '%'|| ? ||'%') or ((I.firstname||' '||I.lastname)like '%'|| ? ||'%'))\n";
        String dayOfWeek = "  and c.dayofweek = ?\n";
        String classTime = "  and c.classbegin <= ? and c.classend >= ?\n";
        StringBuilder location = new StringBuilder();
        String leftCapacity ="  and b.leftcapacity > 0\n";
        if(searchCid==null){
            courseId="";
        }
        if(searchName==null){
            name = "";
        }
        if(searchInstructor==null){
            InstructorName="";
        }
        if(searchDayOfWeek==null){
            dayOfWeek="";
        }
        if(searchClassTime==null){
            classTime="";
        }
        if(searchClassLocations != null&&!searchClassLocations.isEmpty()){
            location = new StringBuilder("  and (");
            for(int i=0;i<searchClassLocations.size()-1;i++){
                location.append("(c.location like '%'|| ? ||'%') or ");
            }
            location.append("(c.location like '%'|| ? ||'%'))\n");
        }
        if(!ignoreFull){
            leftCapacity = "";
        }
        String sql = "select a.courseId,a.courseName,a.credit,a.classHour,a.grading,\n" +
                "       b.sectionId,b.sectionName,b.totalcapacity,b.leftCapacity,\n" +
                "       c.courseSectionClassId,c.DayOfWeek,c.weekList,c.classBegin,c.classEnd,c.location,\n" +
                "       I.userId,I.firstname,I.lastname,m.property\n" +
                "from Course a\n" +
                "    join CourseSection b on a.courseId = b.courseId\n" +
                "    join coursesectionclass c on b.sectionId = c.sectionid\n" +
                "    join Instructor I on c.instructorId = I.userId\n" +
                "    join majorcourse m on a.courseid = m.courseid\n" +
                "where semesterid = ?\n" +
                courseId + name + InstructorName + dayOfWeek + classTime + location + "  and m.property = ?\n" + leftCapacity +
                "  order by a.courseid ,a.courseName,b.sectionName";
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {
            int count = 1;
            stmt.setInt(count,semesterId);
            if(searchCid!=null){
                count++;
                stmt.setString(count,searchCid);
            }
            if(searchName!=null){
                count++;
                stmt.setString(count,searchName);
            }
            if(searchInstructor!=null){
                for(int i= 0;i<4;i++){
                    count++;
                    stmt.setString(count,searchInstructor);
                }
            }
            if(searchDayOfWeek!=null){
                count++;
                stmt.setString(count,searchDayOfWeek.toString());
            }
            if(searchClassTime!=null){
                count++;
                stmt.setInt(count,searchClassTime);
                count++;
                stmt.setInt(count,searchClassTime);
            }
            if(searchClassLocations!=null){
                for(int i=0;i<searchClassLocations.size();i++){
                    count++;
                    stmt.setString(count,searchClassLocations.get(i));
                }
            }
            count++;
            stmt.setString(count,searchCourseType.toString());
            ResultSet rs = stmt.executeQuery();
            CourseSearchEntry courseSearchEntry = new CourseSearchEntry();
            courseSearchEntry.course = new Course();
            courseSearchEntry.section = new CourseSection();
            courseSearchEntry.sectionClasses = new HashSet<>();
            courseSearchEntry.conflictCourseNames = new LinkedList<>();
            while (rs.next()){
                if(courseSearchEntry.course.id != null && !courseSearchEntry.course.id.equals(rs.getString("courseId"))){
                    list.add(courseSearchEntry);
                }
                courseSearchEntry.course.id = rs.getString("courseId");
                courseSearchEntry.course.name = rs.getString("courseName");
                courseSearchEntry.course.credit = rs.getInt("credit");
                courseSearchEntry.course.classHour = rs.getInt("classHour");
                courseSearchEntry.course.grading = Course.CourseGrading.valueOf(rs.getString("grading"));
                courseSearchEntry.section.id = rs.getInt("sectionId");
                courseSearchEntry.section.name = rs.getString("sectionName");
                courseSearchEntry.section.totalCapacity = rs.getInt("totalCapacity");
                courseSearchEntry.section.leftCapacity = rs.getInt("leftCapacity");
                CourseSectionClass csc = new CourseSectionClass();
                csc.id = rs.getInt("courseSectionClassId");
                String dayweek = rs.getString("DayOfWeek");
                if(dayweek.equals("MONDAY")){
                    csc.dayOfWeek = DayOfWeek.MONDAY;
                }else if(dayweek.equals("TUESDAY")){
                    csc.dayOfWeek =DayOfWeek.TUESDAY;
                }else if(dayweek.equals("WEDNESDAY")){
                    csc.dayOfWeek = DayOfWeek.WEDNESDAY;
                }else if(dayweek.equals("THURSDAY")){
                    csc.dayOfWeek = DayOfWeek.THURSDAY;
                }else if(dayweek.equals("FRIDAY")){
                    csc.dayOfWeek = DayOfWeek.FRIDAY;
                }else if(dayweek.equals("SATURDAY")){
                    csc.dayOfWeek = DayOfWeek.SATURDAY;
                }else {
                    csc.dayOfWeek = DayOfWeek.SUNDAY;
                }
                Array curWeekList =rs.getArray("weeklist");
                Object obj = curWeekList.getArray();
                String[] array = (String[]) obj;
                csc.weekList = new HashSet<>();
                for(int i=0;i<array.length;i++){
                    if(array[i]==null){
                        break;
                    }
                    csc.weekList.add((short)Integer.parseInt(array[i]));
                }
                csc.classBegin =(short)rs.getInt("classBegin");
                csc.classEnd = (short) rs.getInt("classEnd");
                csc.location = rs.getString("location");
                courseSearchEntry.sectionClasses.add(csc);
            }
            if (list.isEmpty()){
                return list;
            }
//            if (ignoreConflict){
//                throw new Exception();
//            }
//            if(ignoreConflict){
//                for(int i=0;i<list.size();i++){
//                    try(PreparedStatement stmt6 = connection.prepareStatement("select ecs.sectionid from enrollcoursesection ecs join (\n" +
//                            "    select c1.sectionid,c2.courseid from coursesection c1 join (\n" +
//                            "    select c.courseid,c.semesterid from coursesection c where c.sectionid = ?\n" +
//                            "    ) c2 on c1.courseid = c2.courseid and c1.semesterid = c2.semesterid\n" +
//                            "    ) sc on ecs.sectionid = sc.sectionid where ecs.studentid = ?")){
//                        stmt6.setInt(1,list.get(i).section.id);
//                        stmt6.setInt(2,studentId);
//                        ResultSet rs6 = stmt6.executeQuery();
//                        if (rs6.next()){//存在同一学期同一课程不同class冲突
//                            list.remove(i);
//                            i--;
//                            continue;
//                        }
//                    }catch (SQLException e){
//                        e.printStackTrace();
//                    }
//                }
//                for (int i=0;i<list.size();i++){
//                    try(PreparedStatement stmt7 = connection.prepareStatement("select ecs.sectionid, c.dayofweek,c.classbegin,c.classend,c.weeklist from enrollcoursesection ecs\n" +
//                            "    join coursesection cs on ecs.sectionid = cs.sectionid\n" +
//                            "    join coursesectionclass c on cs.sectionid = c.sectionid\n" +
//                            "where studentid = ? and semesterid = ?")){//该学期已选的所有课
//                        stmt7.setInt(1,studentId);
//                        stmt7.setInt(2,semesterId);
//                        ResultSet rs7 =stmt7.executeQuery();
//                        String day="";
//                        String[] weekList;
//                        short[] CurweekList;
//                        int begin,end;
//                        while (rs7.next()){
//                            day = rs7.getString("dayofweek");
//                            Array week = rs7.getArray("weeklist");
//                            Object obj = week.getArray();
//                            weekList =(String[])obj;
//                            CurweekList = new short[weekList.length];
//                            for(int m=0;m< weekList.length;m++){
//                                if (weekList[m]!=null){
//                                    CurweekList[m] =(short) Integer.parseInt(weekList[m]);
//                                }
//                            }
//                            begin = rs7.getInt("classbegin");
//                            end = rs7.getInt("classEnd");
//                            boolean flag = false;
//                            for(CourseSectionClass s:list.get(i).sectionClasses){
//                                if(day.equals(s.dayOfWeek.toString())){
//                                    if(begin>=s.classBegin||end<=s.classEnd){
//                                        for(int p=0;p<CurweekList.length;i++){
//                                            for(short q :s.weekList){
//                                                if(CurweekList[p]==q){
//                                                    list.remove(i);
//                                                    flag =true;
//                                                    break;
//                                                }
//                                                if(flag)break;
//                                            }
//                                            if(flag)break;
//                                        }
//                                        if(flag)break;
//                                    }
//                                    if(flag)break;
//                                }
//                                if(flag)break;
//                            }
//                            if(flag)i--;
//                        }
//                    }catch (SQLException e){
//                        e.printStackTrace();
//                    }
//                }
//            }else {
//                for (int i=0;i<list.size();i++){
//                    try (PreparedStatement stmt6 = connection.prepareStatement("select course.coursename, sc.courseid, ecs.sectionid, sc.sectionname from enrollcoursesection ecs join (\n" +
//                            "    select c1.sectionid,c1.sectionname,c2.courseid from coursesection c1 join (\n" +
//                            "        select c.courseid,c.semesterid from coursesection c where c.sectionid = ? ) c2\n" +
//                            "        on c1.courseid = c2.courseid and c1.semesterid = c2.semesterid )\n" +
//                            "    sc on ecs.sectionid = sc.sectionid\n" +
//                            "join course on course.courseid = sc.courseid where ecs.studentid = ? order by course.coursename,sc.sectionname ")) {
//                        stmt6.setInt(1, list.get(i).section.id);
//                        stmt6.setInt(2, studentId);
//                        ResultSet rs6 = stmt6.executeQuery();
//                        if (rs6.next()) {//存在同一学期同一课程不同class冲突
//                            String csId = rs6.getString("courseName");
//                            String sectionId = rs6.getString("sectionName");
//                            list.get(i).conflictCourseNames.add(csId + "[" + sectionId + "]");
//                        }
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
//                }
//                for (int i = 0; i < list.size(); i++) {
//                    try (PreparedStatement stmt7 = connection.prepareStatement("select cs.courseid,c2.coursename,ecs.sectionid,cs.sectionname, c.dayofweek,c.classbegin,c.classend,c.weeklist from enrollcoursesection ecs\n" +
//                            "    join coursesection cs on ecs.sectionid = cs.sectionid\n" +
//                            "    join coursesectionclass c on cs.sectionid = c.sectionid\n" +
//                            "    join course c2 on cs.courseid = c2.courseid\n" +
//                            "where studentid = ? and semesterid = ? order by c2.coursename,cs.sectionname")) {//该学期已选的所有课
//                        stmt7.setInt(1, studentId);
//                        stmt7.setInt(2, semesterId);
//                        ResultSet rs7 = stmt7.executeQuery();
//                        String csId = "";
//                        String sectionId = "";
//                        String day = "";
//                        String[] weekList;
//                        short[] CurweekList;
//                        int begin, end;
//                        while (rs7.next()) {
//                            csId = rs7.getString("courseName");
//                            sectionId = rs7.getString("sectionName");
//                            day = rs7.getString("dayofweek");
//                            Array week = rs7.getArray("weeklist");
//                            Object obj = week.getArray();
//                            weekList = (String[]) obj;
//                            CurweekList = new short[weekList.length];
//                            for (int m = 0; m < weekList.length; m++) {
//                                if (weekList[m]!=null){
//                                    CurweekList[m] = (short) Integer.parseInt(weekList[m]);
//                                }
//                            }
//                            begin = rs7.getInt("classbegin");
//                            end = rs7.getInt("classEnd");
//                            boolean flag = false;
//                            for (CourseSectionClass s : list.get(i).sectionClasses) {
//                                if (day.equals(s.dayOfWeek.toString())) {
//                                    if (begin >= s.classBegin || end <= s.classEnd) {
//                                        for (int p = 0; p < CurweekList.length; i++) {
//                                            for (short q : s.weekList) {
//                                                if (CurweekList[p] == q) {
//                                                    if(!list.get(i).conflictCourseNames.contains(csId + "[" + sectionId + "]")){
//                                                        list.get(i).conflictCourseNames.add(csId + "[" + sectionId + "]");
//                                                    }
//                                                    flag = true;
//                                                    break;
//                                                }
//                                            }
//                                            if (flag) break;
//                                        }
//                                    }
//                                }
//                                if (flag) break;
//                            }
//                        }
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
            if(ignoreMissingPrerequisites){
                for(int i=0;i<list.size();i++){
                    if(!passedPrerequisitesForCourse(studentId, list.get(i).course.id)){
                        list.remove(i);
                        i--;
                    }
                }
            }
            if(ignorePassed){
                for(int i=0;i<list.size();i++){
                    try(PreparedStatement stmt2 = connection.prepareStatement("select c.sectionid, e.grade from coursesection c join enrollcoursegrade e on c.sectionid = e.sectionid where c.courseid = ? and e.studentid = ?")){
                        stmt2.setString(1,list.get(i).course.id);
                        stmt2.setInt(2,studentId);
                        ResultSet rs2 = stmt2.executeQuery();
                        while (rs2.next()){
                            String grade = rs2.getString("grade");
                            if(grade==null){
                                continue;
                            }else if(grade.equals("PASS")){
                                list.remove(i);
                                i--;
                            }else if(grade.equals("FALSE")){
                                continue;
                            }else {
                                int score = grade.hashCode();
                                if(score>=60){
                                    list.remove(i);
                                    i--;
                                }
                            }
                        }
                    }catch (SQLException e){
                        e.printStackTrace();
                    }
                }
            }
            //重新对list的大小进行处理,当前list.size()<=pageIndex*pageSize,且是有效的
            int numberofpage = (int) Math.ceil(list.size()/pageSize);//向上取整
            List<CourseSearchEntry>[] allPage = new List[numberofpage];
            int index = 0;
            for (int i=0;i<numberofpage;i++){//共几页
                allPage[i] = new ArrayList<>();
                for (int j=0;j<pageSize;j++){//每页几个
                    allPage[i].add(list.get(index));
                    index++;
                    if (index > list.size()){
                        break;
                    }
                }
            }
            if (pageIndex >= numberofpage){
                return new ArrayList<>();
            }else {
                return allPage[pageIndex];
            }
        } catch (SQLException throwables) {
//            System.out.print(studentId+" ");
//            System.out.print(semesterId+" ");
//            System.out.println(searchCid);
//            System.out.println(searchName);
//            System.out.println(searchInstructor);
//            System.out.println(searchClassLocations);
//            System.out.println(searchCourseType);
//            System.out.println("ignoreFull"+ignoreFull);
//            System.out.println("ignoreConflict"+ ignoreConflict);
//            System.out.println("ignorePassed"+ ignorePassed);
//            System.out.println("ignoreMissingPrerequisites"+ignoreMissingPrerequisites);
//            System.out.print(pageSize+" "+pageIndex);
//            System.out.println();
//            System.out.println(sql);
            throwables.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //public List<CourseSearchEntry> searchCourse(int studentId, int semesterId,
//                                                @Nullable String searchCid, @Nullable String searchName,
//                                                @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek,
//                                                @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations,
//                                                CourseType searchCourseType,
//                                                boolean ignoreFull, boolean ignoreConflict,
//                                                boolean ignorePassed, boolean ignoreMissingPrerequisites,
//                                                int pageSize, int pageIndex) {
//        List<CourseSearchEntry> list = new ArrayList<>();
//        String courseId = "  and c1.courseid like '%'|| ? ||'%'\n";
//        String name = "  and (c1.coursename||'['||c.sectionname||']')like ('%'|| ? ||'%')\n";
//        String InstructorName = "  and ((i.firstname like '%'|| ? ||'%') or (i.lastname like '%'|| ? ||'%') or ((i.firstname||i.lastname) like '%'|| ? ||'%') or ((i.firstname||' '||i.lastname)like '%'|| ? ||'%'))\n";
//        String dayOfWeek = "  and c2.dayofweek = ?\n";
//        String classTime = "  and c2.classbegin <= ? and c2.classend >= ?\n";
//        StringBuilder location = new StringBuilder();
//        String leftCapacity ="  and c.leftcapacity > 0\n";
//        String property = "  join cour";
//        String pass = " and c2.sectionid <> (select c.sectionid from coursesection c join( select distinct courseid,e.sectionid,grade from enrollcoursegrade e join coursesection c on c.sectionid = e.sectionid where grade ='PASS' or (grade !='PASS' and grade !='FAIL' and cast(grade as int)>60) and studentid = ?) c2 on c.courseid = c2.courseid where semesterid = ?) \n ";
//        if(searchCid==null){
//            courseId="";
//        }
//        if(searchName==null){
//            name ="";
//        }
//        if(searchInstructor==null){
//            InstructorName="";
//        }
//        if(searchDayOfWeek==null){
//            dayOfWeek="";
//        }
//        if(searchClassTime==null){
//            classTime="";
//        }
//        if(searchClassLocations != null&&!searchClassLocations.isEmpty()){
//            location = new StringBuilder("  and (");
//            for(int i=0;i<searchClassLocations.size()-1;i++){
//                location.append("(c.location like '%'|| ? ||'%') or ");
//            }
//            location.append("(c.location like '%'|| ? ||'%'))\n");
//        }
//        if(searchCourseType==CourseType.ALL){
//            property="";
//        }
//        if(!ignoreFull){
//            leftCapacity = "";
//        }
//        if(!ignorePassed){
//            pass="";
//        }
////        String sql = "select c.courseid,coursename,credit,classhour,grading,c2.sectionid,sectionname,totalcapacity,leftcapacity,instructorid,dayofweek,weeklist,classhour,classbegin,classend,location from course\n" +
////                "    join coursesection c on course.courseid = c.courseid\n" +
////                "    join coursesectionclass c2 on c.sectionid = c2.sectionid\n" +
////                "    where semesterid = ? order by c.courseid";
//        String sql = "select c1.courseid,c1.coursename,c1.credit,c1.classhour,c1.classhour,c1.grading,c2.sectionid,c.sectionname,c.totalcapacity,c.leftcapacity,i.userid,i.firstname,i.lastname,c2.dayofweek,c2.weeklist,c2.classbegin,c2.classend,c2.location from course c1\n" +
//                "    join coursesection c on c1.courseid = c.courseid\n" +
//                "    join coursesectionclass c2 on c.sectionid = c2.sectionid\n" +
//                "    join instructor i on c2.instructorid = i.userid\n" +
//                "    left join majorcourse m on c1.courseid = m.courseid\n" +
//                "    where semesterid = ?" +
//                courseId + name + InstructorName + dayOfWeek + classTime + location + leftCapacity +pass+
//                " order by c.courseid,c1.coursename,c.sectionname;";
//        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
//            PreparedStatement stmt = connection.prepareStatement(sql)) {
////            String str =num+":"+ sql+"\n";
////            FileOutputStream o = null;
////            byte[] buff = new byte[]{};
////            try{
////                File file = new File("1.txt");
////                if(!file.exists()){
////                    file.createNewFile();
////                }
////                buff=str.getBytes();
////                o=new FileOutputStream(file,true);
////                o.write(buff);
////                o.flush();
////                o.close();
////            }catch(Exception e){
////                e.printStackTrace();
////            }
//            int count = 1;
//            stmt.setInt(count,semesterId);
//            if(searchCid!=null){
//                count++;
//                stmt.setString(count,searchCid);
//            }
//            if(searchName!=null){
//                count++;
//                stmt.setString(count,searchName);
//            }
//            if(searchInstructor!=null){
//                for(int i= 0;i<4;i++){
//                    count++;
//                    stmt.setString(count,searchInstructor);
//                }
//            }
//            if(searchDayOfWeek!=null){
//                count++;
//                stmt.setString(count,searchDayOfWeek.toString());
//            }
//            if(searchClassTime!=null){
//                count++;
//                stmt.setInt(count,searchClassTime);
//                count++;
//                stmt.setInt(count,searchClassTime);
//            }
//            if(searchClassLocations!=null){
//                for(int i=0;i<searchClassLocations.size();i++){
//                    count++;
//                    stmt.setString(count,searchClassLocations.get(i));
//                }
//            }
//            if(searchCourseType!=CourseType.ALL){
//                count++;
//                stmt.setString(count,searchCourseType.toString());
//            }
//            if(ignorePassed){
//                count++;
//                stmt.setInt(count,studentId);
//                count++;
//                stmt.setInt(count,semesterId);
//            }
//            ResultSet rs = stmt.executeQuery();
//            CourseSearchEntry courseSearchEntry = new CourseSearchEntry();
//            courseSearchEntry.course = new Course();
//            courseSearchEntry.section = new CourseSection();
//            courseSearchEntry.sectionClasses = new HashSet<>();
//            courseSearchEntry.conflictCourseNames = new LinkedList<>();
//            while (rs.next()){
//                if(courseSearchEntry.course.id != null && !courseSearchEntry.course.id.equals(rs.getString("courseId"))){
//                    list.add(courseSearchEntry);
//                }
//                courseSearchEntry.course.id = rs.getString("courseId");
//                courseSearchEntry.course.name = rs.getString("courseName");
//                courseSearchEntry.course.credit = rs.getInt("credit");
//                courseSearchEntry.course.classHour = rs.getInt("classHour");
//                courseSearchEntry.course.grading = Course.CourseGrading.valueOf(rs.getString("grading"));
//                courseSearchEntry.section.id = rs.getInt("sectionId");
//                courseSearchEntry.section.name = rs.getString("sectionName");
//                courseSearchEntry.section.totalCapacity = rs.getInt("totalCapacity");
//                courseSearchEntry.section.leftCapacity = rs.getInt("leftCapacity");
//                CourseSectionClass csc = new CourseSectionClass();
//                csc.id = rs.getInt("courseSectionClassId");
//                String dayweek = rs.getString("DayOfWeek");
//                if(dayweek.equals("MONDAY")){
//                    csc.dayOfWeek = DayOfWeek.MONDAY;
//                }else if(dayweek.equals("TUESDAY")){
//                    csc.dayOfWeek =DayOfWeek.TUESDAY;
//                }else if(dayweek.equals("WEDNESDAY")){
//                    csc.dayOfWeek = DayOfWeek.WEDNESDAY;
//                }else if(dayweek.equals("THURSDAY")){
//                    csc.dayOfWeek = DayOfWeek.THURSDAY;
//                }else if(dayweek.equals("FRIDAY")){
//                    csc.dayOfWeek = DayOfWeek.FRIDAY;
//                }else if(dayweek.equals("SATURDAY")){
//                    csc.dayOfWeek = DayOfWeek.SATURDAY;
//                }else {
//                    csc.dayOfWeek = DayOfWeek.SUNDAY;
//                }
//                Array curWeekList =rs.getArray("weeklist");
//                Object obj = curWeekList.getArray();
//                String[] array = (String[]) obj;
//                csc.weekList = new HashSet<>();
//                for(int i=0;i<array.length;i++){
//                    if(array[i]==null){
//                        break;
//                    }
//                    csc.weekList.add((short)Integer.parseInt(array[i]));
//                }
//                csc.classBegin =(short)rs.getInt("classBegin");
//                csc.classEnd = (short) rs.getInt("classEnd");
//                csc.location = rs.getString("location");
//                courseSearchEntry.sectionClasses.add(csc);
//            }
//            if (list.isEmpty()){
//                return list;
//            }
////            if (ignoreConflict){
////                throw new Exception();
////            }
////            if(ignoreConflict){
////                for(int i=0;i<list.size();i++){
////                    try(PreparedStatement stmt6 = connection.prepareStatement("select ecs.sectionid from enrollcoursesection ecs join (\n" +
////                            "    select c1.sectionid,c2.courseid from coursesection c1 join (\n" +
////                            "    select c.courseid,c.semesterid from coursesection c where c.sectionid = ?\n" +
////                            "    ) c2 on c1.courseid = c2.courseid and c1.semesterid = c2.semesterid\n" +
////                            "    ) sc on ecs.sectionid = sc.sectionid where ecs.studentid = ?")){
////                        stmt6.setInt(1,list.get(i).section.id);
////                        stmt6.setInt(2,studentId);
////                        ResultSet rs6 = stmt6.executeQuery();
////                        if (rs6.next()){//存在同一学期同一课程不同class冲突
////                            list.remove(i);
////                            i--;
////                            continue;
////                        }
////                    }catch (SQLException e){
////                        e.printStackTrace();
////                    }
////                }
////                for (int i=0;i<list.size();i++){
////                    try(PreparedStatement stmt7 = connection.prepareStatement("select ecs.sectionid, c.dayofweek,c.classbegin,c.classend,c.weeklist from enrollcoursesection ecs\n" +
////                            "    join coursesection cs on ecs.sectionid = cs.sectionid\n" +
////                            "    join coursesectionclass c on cs.sectionid = c.sectionid\n" +
////                            "where studentid = ? and semesterid = ?")){//该学期已选的所有课
////                        stmt7.setInt(1,studentId);
////                        stmt7.setInt(2,semesterId);
////                        ResultSet rs7 =stmt7.executeQuery();
////                        String day="";
////                        String[] weekList;
////                        short[] CurweekList;
////                        int begin,end;
////                        while (rs7.next()){
////                            day = rs7.getString("dayofweek");
////                            Array week = rs7.getArray("weeklist");
////                            Object obj = week.getArray();
////                            weekList =(String[])obj;
////                            CurweekList = new short[weekList.length];
////                            for(int m=0;m< weekList.length;m++){
////                                if (weekList[m]!=null){
////                                    CurweekList[m] =(short) Integer.parseInt(weekList[m]);
////                                }
////                            }
////                            begin = rs7.getInt("classbegin");
////                            end = rs7.getInt("classEnd");
////                            boolean flag = false;
////                            for(CourseSectionClass s:list.get(i).sectionClasses){
////                                if(day.equals(s.dayOfWeek.toString())){
////                                    if(begin>=s.classBegin||end<=s.classEnd){
////                                        for(int p=0;p<CurweekList.length;i++){
////                                            for(short q :s.weekList){
////                                                if(CurweekList[p]==q){
////                                                    list.remove(i);
////                                                    i--;
////                                                    flag =true;
////                                                    break;
////                                                }
////                                            }
////                                            if(flag)break;
////                                        }
////                                    }
////                                }
////                                if(flag)break;
////                            }
////                        }
////                    }catch (SQLException e){
////                        e.printStackTrace();
////                    }
////                }
////            }else {
////                for (int i=0;i<list.size();i++){
////                    try (PreparedStatement stmt6 = connection.prepareStatement("select course.coursename, sc.courseid, ecs.sectionid, sc.sectionname from enrollcoursesection ecs join (\n" +
////                            "    select c1.sectionid,c1.sectionname,c2.courseid from coursesection c1 join (\n" +
////                            "        select c.courseid,c.semesterid from coursesection c where c.sectionid = ? ) c2\n" +
////                            "        on c1.courseid = c2.courseid and c1.semesterid = c2.semesterid )\n" +
////                            "    sc on ecs.sectionid = sc.sectionid\n" +
////                            "join course on course.courseid = sc.courseid where ecs.studentid = ? order by course.coursename,sc.sectionname ")) {
////                        stmt6.setInt(1, list.get(i).section.id);
////                        stmt6.setInt(2, studentId);
////                        ResultSet rs6 = stmt6.executeQuery();
////                        if (rs6.next()) {//存在同一学期同一课程不同class冲突
////                            String csId = rs6.getString("courseName");
////                            String sectionId = rs6.getString("sectionName");
////                            list.get(i).conflictCourseNames.add(csId + "[" + sectionId + "]");
////                        }
////                    } catch (SQLException e) {
////                        e.printStackTrace();
////                    }
////                }
////                for (int i = 0; i < list.size(); i++) {
////                    try (PreparedStatement stmt7 = connection.prepareStatement("select cs.courseid,c2.coursename,ecs.sectionid,cs.sectionname, c.dayofweek,c.classbegin,c.classend,c.weeklist from enrollcoursesection ecs\n" +
////                            "    join coursesection cs on ecs.sectionid = cs.sectionid\n" +
////                            "    join coursesectionclass c on cs.sectionid = c.sectionid\n" +
////                            "    join course c2 on cs.courseid = c2.courseid\n" +
////                            "where studentid = ? and semesterid = ? order by c2.coursename,cs.sectionname")) {//该学期已选的所有课
////                        stmt7.setInt(1, studentId);
////                        stmt7.setInt(2, semesterId);
////                        ResultSet rs7 = stmt7.executeQuery();
////                        String csId = "";
////                        String sectionId = "";
////                        String day = "";
////                        String[] weekList;
////                        short[] CurweekList;
////                        int begin, end;
////                        while (rs7.next()) {
////                            csId = rs7.getString("courseName");
////                            sectionId = rs7.getString("sectionName");
////                            day = rs7.getString("dayofweek");
////                            Array week = rs7.getArray("weeklist");
////                            Object obj = week.getArray();
////                            weekList = (String[]) obj;
////                            CurweekList = new short[weekList.length];
////                            for (int m = 0; m < weekList.length; m++) {
////                                if (weekList[m]!=null){
////                                    CurweekList[m] = (short) Integer.parseInt(weekList[m]);
////                                }
////                            }
////                            begin = rs7.getInt("classbegin");
////                            end = rs7.getInt("classEnd");
////                            boolean flag = false;
////                            for (CourseSectionClass s : list.get(i).sectionClasses) {
////                                if (day.equals(s.dayOfWeek.toString())) {
////                                    if (begin >= s.classBegin || end <= s.classEnd) {
////                                        for (int p = 0; p < CurweekList.length; i++) {
////                                            for (short q : s.weekList) {
////                                                if (CurweekList[p] == q) {
////                                                    if(!list.get(i).conflictCourseNames.contains(csId + "[" + sectionId + "]")){
////                                                        list.get(i).conflictCourseNames.add(csId + "[" + sectionId + "]");
////                                                    }
////                                                    flag = true;
////                                                    break;
////                                                }
////                                            }
////                                            if (flag) break;
////                                        }
////                                    }
////                                }
////                                if (flag) break;
////                            }
////                        }
////                    } catch (SQLException e) {
////                        e.printStackTrace();
////                    }
////                }
////            }
//            if(ignoreMissingPrerequisites){
//                for(int i=0;i<list.size();i++){
//                    if(!passedPrerequisitesForCourse(studentId, list.get(i).course.id)){
//                        list.remove(i);
//                        i--;
//                    }
//                }
//            }
//            //重新对list的大小进行处理,当前list.size()<=pageIndex*pageSize,且是有效的
//            int numberofpage = (int) Math.ceil(list.size()/pageSize);//向上取整
//            List<CourseSearchEntry>[] allPage = new List[numberofpage];
//            int index = 0;
//            for (int i=0;i<numberofpage;i++){//共几页
//                allPage[i] = new ArrayList<>();
//                for (int j=0;j<pageSize;j++){//每页几个
//                    allPage[i].add(list.get(index));
//                    index++;
//                    if (index > list.size()){
//                        break;
//                    }
//                }
//            }
//            if (pageIndex >= numberofpage){
//                return new ArrayList<>();
//            }else {
//                return allPage[pageIndex];
//            }
////        } catch (SQLException throwables) {
////            System.out.print(studentId+" ");
////            System.out.print(semesterId+" ");
////            System.out.println(searchCid);
////            System.out.println(searchName);
////            System.out.println(searchInstructor);
////            System.out.println(searchClassLocations);
////            System.out.println(searchCourseType);
////            System.out.println("ignoreFull"+ignoreFull);
////            System.out.println("ignoreConflict"+ ignoreConflict);
////            System.out.println("ignorePassed"+ ignorePassed);
////            System.out.println("ignoreMissingPrerequisites"+ignoreMissingPrerequisites);
////            System.out.print(pageSize+" "+pageIndex);
////            System.out.println();
////            System.out.println(sql);
////            throwables.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return list;
//    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
//        System.out.println(studentId+":"+sectionId);
//        num++;
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt1 = connection.prepareStatement("select * from coursesection where sectionid = ?");
            PreparedStatement stmt2 = connection.prepareStatement("select leftcapacity from coursesection where sectionid = ?");
            PreparedStatement stmt3 = connection.prepareStatement("select * from enrollcoursesection where studentid = ? and sectionid = ?");
            PreparedStatement stmt4 = connection.prepareStatement("select e1.sectionid,e1.grade from enrollcoursegrade e1 join (select c1.sectionid from coursesection c1 join (\n" +
                    "    select c.courseid from coursesection c where c.sectionid = ?\n" +
                    "    ) c2 on c1.courseid = c2.courseid) e2 on e1.sectionid = e2.sectionid\n" +
                    "where e1.studentid = ?");
            PreparedStatement stmt5 = connection.prepareStatement("select courseid from coursesection where sectionid = ?");
            PreparedStatement stmt6 = connection.prepareStatement("select ecs.sectionid from enrollcoursesection ecs join (\n" +
                    "    select c1.sectionid,c2.courseid from coursesection c1 join (\n" +
                    "    select c.courseid,c.semesterid from coursesection c where c.sectionid = ?\n" +
                    "    ) c2 on c1.courseid = c2.courseid and c1.semesterid = c2.semesterid\n" +
                    "    ) sc on ecs.sectionid = sc.sectionid where ecs.studentid = ?");
            PreparedStatement stmt7 = connection.prepareStatement("select sectionid,dayofweek,weeklist,classbegin,classend from coursesectionclass where sectionid = ?");
            PreparedStatement stmt8 = connection.prepareStatement("select csc.sectionid,csc.dayofweek,csc.weeklist,csc.classbegin,csc.classend from coursesectionclass csc\n" +
                    "    join ((select sectionid from enrollcoursesection where studentid = ?)intersect\n" +
                    "    (select c1.sectionid from coursesection c1 join (\n" +
                    "        select c.semesterid from coursesection c where c.sectionid = ?\n" +
                    "        ) c2 on c1.semesterid = c2.semesterid)) tmp on tmp.sectionid = csc.sectionid")){
            stmt1.setInt(1,sectionId);
            ResultSet rs1 = stmt1.executeQuery();
            if(!rs1.next()){
                //System.out.println(num+":"+EnrollResult.COURSE_NOT_FOUND);
                return EnrollResult.COURSE_NOT_FOUND;
            }
            stmt2.setInt(1,sectionId);
            ResultSet rs2 = stmt2.executeQuery();
            while (rs2.next()){
                int leftNumber = rs2.getInt("leftCapacity");
                if(leftNumber==0){
                    //System.out.println(num+":"+EnrollResult.COURSE_IS_FULL);
                    return EnrollResult.COURSE_IS_FULL;
                }
            }
            stmt3.setInt(1,studentId);
            stmt3.setInt(2,sectionId);
            ResultSet rs3 = stmt3.executeQuery();
            if(rs3.next()){
                //System.out.println(num+":"+EnrollResult.ALREADY_ENROLLED);
                return EnrollResult.ALREADY_ENROLLED;
            }
            stmt4.setInt(1,sectionId);
            stmt4.setInt(2,studentId);
            ResultSet rs4 = stmt4.executeQuery();
            while (rs4.next()){
                String grade = rs4.getString("grade");
                if(grade==null){
                    continue;
                }else if(grade.equals("PASS")){
                    //System.out.println(num+":"+EnrollResult.ALREADY_PASSED+"1111");
                    return EnrollResult.ALREADY_PASSED;
                }else if(grade.equals("FALSE")){
                    continue;
                }else {
                    int score = Integer.parseInt(grade);
                    if(score>=60){
                        //System.out.println(num+":"+EnrollResult.ALREADY_PASSED+"2222");
                        return EnrollResult.ALREADY_PASSED;
                    }
                }
            }
            stmt5.setInt(1,sectionId);
            ResultSet rs5 = stmt5.executeQuery();
            while (rs5.next()){
                String courseId = rs5.getString("courseId");
                if(!passedPrerequisitesForCourse(studentId,courseId)){
                    //System.out.println(num+":"+EnrollResult.PREREQUISITES_NOT_FULFILLED);
                    return EnrollResult.PREREQUISITES_NOT_FULFILLED;
                }
            }
            stmt6.setInt(1,sectionId);
            stmt6.setInt(2,studentId);
            ResultSet rs6 = stmt6.executeQuery();//同一门课程同一个学期的不同班级
            if (rs6.next()){
                //System.out.println(num+":"+EnrollResult.COURSE_CONFLICT_FOUND+"11111");
                return EnrollResult.COURSE_CONFLICT_FOUND;
            }
            stmt7.setInt(1,sectionId);
            ResultSet rs7 = stmt7.executeQuery();
            String day="";
            String[] weekList=new String[18];
            int begin=0,end=0;
            while (rs7.next()){
                day = rs7.getString("dayofweek");
                Array week = rs7.getArray("weeklist");
                Object obj = week.getArray();
                weekList =(String[])obj;
                begin = rs7.getInt("classbegin");
                end = rs7.getInt("classEnd");
                stmt8.setInt(1,studentId);
                stmt8.setInt(2,sectionId);
                ResultSet rs8 = stmt8.executeQuery();
                String Curday;
                String[] CurweekList;
                int Curbegin,Curend;
                while (rs8.next()){
                    int sectionid8 = rs8.getInt("sectionid");
                    Curday = rs8.getString("dayofweek");
                    Array weeks = rs8.getArray("weeklist");
                    Object objs = weeks.getArray();
                    CurweekList =(String[])objs;
                    Curbegin = rs8.getInt("classbegin");
                    Curend = rs8.getInt("classEnd");
                    if(day.equals(Curday)){
                        if(!(end<=Curbegin||begin>=Curend)){
                            for(int i=0;i<weekList.length;i++){
                                if(weekList[i]==null)break;
                                for(int j=0;j<CurweekList.length;j++){
                                    if(CurweekList[j]==null)break;
                                    if(weekList[i].equals(CurweekList[j])){
//                                        System.out.println("sectionid:"+sectionid8);
//                                        System.out.println(day+","+Curday);
//                                        System.out.println(begin+"-"+end+";"+Curbegin+"-"+Curend);
//                                        System.out.println(weekList[i]+";"+CurweekList[j]);
//                                        System.out.println(Arrays.toString(weekList)+";"+Arrays.toString(CurweekList));
                                        //System.out.println(num+":"+EnrollResult.COURSE_CONFLICT_FOUND+"22222");
                                        return EnrollResult.COURSE_CONFLICT_FOUND;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            try(PreparedStatement stmt9 = connection.prepareStatement("insert into enrollcoursesection (studentid, sectionid) values (?,?)");
                PreparedStatement stmt10 = connection.prepareStatement("select leftcapacity from coursesection where sectionid =?");
                PreparedStatement stmt11 = connection.prepareStatement("update coursesection set leftcapacity = ? where sectionid = ?");
                PreparedStatement stmt12 = connection.prepareStatement("insert into enrollcoursegrade (studentid, sectionid) values (?,?)")
            ){
                stmt9.setInt(1,studentId);
                stmt9.setInt(2,sectionId);
                stmt9.executeUpdate();
                stmt10.setInt(1,sectionId);
                ResultSet rs10 = stmt2.executeQuery();
                int leftNumber=0;
                while (rs10.next()){
                    leftNumber = rs10.getInt("leftCapacity");
                }
                leftNumber--;
                stmt11.setInt(1,leftNumber);
                stmt11.setInt(2,sectionId);
                stmt11.executeUpdate();
                stmt12.setInt(1,studentId);
                stmt12.setInt(2,sectionId);
                stmt12.executeUpdate();
                //System.out.println(num+":"+EnrollResult.SUCCESS);
                return EnrollResult.SUCCESS;
            }catch (SQLException e){
                e.printStackTrace();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        //System.out.println(num+":"+EnrollResult.UNKNOWN_ERROR);
        return EnrollResult.UNKNOWN_ERROR;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement("select studentid from enrollcoursegrade where studentid = ? and sectionid = ? and grade is not null");
            PreparedStatement stmt11 = connection.prepareStatement("delete from enrollcoursesection where studentid = ? and sectionid = ?");
            PreparedStatement stmt2 = connection.prepareStatement("select leftcapacity from coursesection where sectionid = ?");
            PreparedStatement stmt3 = connection.prepareStatement("update coursesection set leftcapacity = ? where sectionid = ?")
        ){
            stmt.setInt(1,studentId);
            stmt.setInt(2,sectionId);
            ResultSet rst = stmt.executeQuery();
            if(rst.next()){
//                String str = "illegal: "+studentId+sectionId+"\n";
//                FileOutputStream o = null;
//                byte[] buff = new byte[]{};
//                try{
//                    File file = new File("5.txt");
//                    if(!file.exists()){
//                        file.createNewFile();
//                    }
//                    buff=str.getBytes();
//                    o=new FileOutputStream(file,true);
//                    o.write(buff);
//                    o.flush();
//                    o.close();
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
//                //System.out.println(studentId+";"+sectionId);
                throw new IllegalStateException();
            }else{
                stmt11.setInt(1,studentId);
                stmt11.setInt(2,sectionId);
                stmt11.executeUpdate();
                stmt2.setInt(1,sectionId);
                ResultSet rs = stmt2.executeQuery();
                int leftNumber=0;
                while (rs.next()){
                    leftNumber= rs.getInt("leftCapacity");
                }
                leftNumber++;
//                String str = "valid: "+studentId+sectionId+leftNumber+"\n";
//                FileOutputStream o = null;
//                byte[] buff = new byte[]{};
//                try{
//                    File file = new File("5.txt");
//                    if(!file.exists()){
//                        file.createNewFile();
//                    }
//                    buff=str.getBytes();
//                    o=new FileOutputStream(file,true);
//                    o.write(buff);
//                    o.flush();
//                    o.close();
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
                stmt3.setInt(1,leftNumber);
                stmt3.setInt(2,sectionId);
                stmt3.executeUpdate();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @javax.annotation.Nullable Grade grade) {
        //countNum++;
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt0 =connection.prepareStatement("select grading from course c join coursesection c2 on c.courseid = c2.courseid where sectionid = ?");
            PreparedStatement stmt = connection.prepareStatement("select studentid,sectionid from enrollcoursesection where studentid = ? and sectionid = ?");
            PreparedStatement stmt1 = connection.prepareStatement("insert into enrollcoursesection (studentid,sectionid)values (?,?)");
            PreparedStatement stmt11 = connection.prepareStatement("select * from enrollcoursegrade where studentid = ? and sectionid = ?");
            PreparedStatement stmt2 = connection.prepareStatement("insert into enrollcoursegrade (studentid, sectionid, grade)values (?,?,?)");
            PreparedStatement stmt22 = connection.prepareStatement("")
        ){
            stmt0.setInt(1,sectionId);
            ResultSet rs0 = stmt0.executeQuery();
            String grading = "";
            while (rs0.next()){
                grading = rs0.getString("grading");
                //System.out.println(countNum);
            }
//            String str = studentId+","+sectionId+","+(grade==null?"null":((grade.toString()=="PASS"||grade.toString()=="FAIL")?grade.toString():String.valueOf(grade.hashCode())))+","+grading+"\n";
//            FileOutputStream o = null;
//            byte[] buff = new byte[]{};
//            try{
//                File file = new File("1.txt");
//                if(!file.exists()){
//                    file.createNewFile();
//                }
//                buff=str.getBytes();
//                o=new FileOutputStream(file,true);
//                o.write(buff);
//                o.flush();
//                o.close();
//            }catch(Exception e){
//                e.printStackTrace();
//            }
            if(grading.equals("HUNDRED_MARK_SCORE") && grade != null && (grade.toString().equals("PASS") || grade.toString().equals("FAIL")) ||(!grading.equals("HUNDRED_MARK_SCORE") &&grade!=null&&!(grade.toString().equals("PASS")||grade.toString().equals("FAIL")))){
               throw new Exception();
            }
            stmt.setInt(1,studentId);
            stmt.setInt(2,sectionId);
            ResultSet have = stmt.executeQuery();
            if(!have.next()){
                stmt1.setInt(1,studentId);
                stmt1.setInt(2,sectionId);
                stmt1.executeUpdate();
            }
            stmt11.setInt(1,studentId);
            stmt11.setInt(2,sectionId);
            ResultSet rst = stmt11.executeQuery();
            if(!rst.next()){
                stmt2.setInt(1,studentId);
                stmt2.setInt(2,sectionId);
                if(grade!=null){
                    if (grade == PassOrFailGrade.PASS){
                        stmt2.setString(3,grade.toString());
                    }else if(grade == PassOrFailGrade.FAIL){
                        stmt2.setString(3,grade.toString());
                    }else {
                        stmt2.setString(3,String.valueOf(grade.hashCode()));
                    }
                }else {
                    stmt2.setString(3,null);
                }
                stmt2.execute();
            }else{
                setEnrolledCourseGrade(studentId,sectionId,grade);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement("update enrollcoursegrade set grade = ? where studentid = ? and sectionid = ?")
        ){
            if(grade.toString().equals("PASS") || grade.toString().equals("FAIL")){
                stmt.setString(1,grade.toString());
            }else {
                stmt.setString(1,String.valueOf(grade.hashCode()));
            }
            stmt.setInt(2,studentId);
            stmt.setInt(3,sectionId);
            stmt.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @javax.annotation.Nullable Integer semesterId) {
        Map<Course,Grade> map = new HashMap<>();
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
        ){
            if(semesterId==null){
                try(PreparedStatement stmt1 = connection.prepareStatement("select c.courseid,c.coursename,c.credit,c.classhour,c.grading,ecg.grade from course c\n" +
                        "    join coursesection cs on c.courseid = cs.courseid\n" +
                        "    left join enrollcoursegrade ecg on cs.sectionid = ecg.sectionid where ecg.studentid = ? and cs.semesterid = ?")){
                    stmt1.setInt(1,studentId);
                    stmt1.setInt(2,studentId);
                    ResultSet rs1 = stmt1.executeQuery();
                    while (rs1.next()){
                        Course course = new Course();
                        course.id = rs1.getString("courseid");
                        course.name = rs1.getString("courseName");
                        course.credit = rs1.getInt("credit");
                        course.classHour = rs1.getInt("classHour");
                        Course.CourseGrading cur = Course.CourseGrading.valueOf(rs1.getString("grading"));
                        course.grading = cur;
                        Grade curGrade  = (Grade) rs1.getObject("grade");//grade 类型尚未定义 无法实现返回
                        map.put(course,curGrade);
                    }
                }catch (SQLException e){
                    e.printStackTrace();
                }
            }else {
                try(PreparedStatement stmt2 = connection.prepareStatement("select c.courseid,c.coursename,c.credit,c.classhour,c.grading,ecg.grade,s.enddate from course c\n" +
                        "    join coursesection cs on c.courseid = cs.courseid\n" +
                        "    join semester s on s.semesterid = cs.semesterid\n" +
                        "    join (select c.courseid,max(enddate) as enddate from course c\n" +
                        "        join coursesection cs on c.courseid = cs.courseid\n" +
                        "        join semester s on s.semesterid = cs.semesterid\n" +
                        "        left join enrollcoursegrade ecg on cs.sectionid = ecg.sectionid where ecg.studentid = ? group by c.courseid) part \n" +
                        "        on c.courseid = part.courseid and s.enddate = part.enddate\n" +
                        "    left join enrollcoursegrade ecg on cs.sectionid = ecg.sectionid where ecg.studentid = ? ")){
                    stmt2.setInt(1,studentId);
                    ResultSet rs2 = stmt2.executeQuery();
                    while (rs2.next()){
                        Course course = new Course();
                        course.id = rs2.getString("courseid");
                        course.name = rs2.getString("courseName");
                        course.credit = rs2.getInt("credit");
                        course.classHour = rs2.getInt("classHour");
                        Course.CourseGrading cur = Course.CourseGrading.valueOf(rs2.getString("grading"));
                        course.grading = cur;
                        Grade curGrade  = (Grade) rs2.getObject("grade");//grade 类型尚未定义 无法实现返回
                        map.put(course,curGrade);
                    }
                }catch (SQLException e){
                    e.printStackTrace();
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        CourseTable courseTable = new CourseTable();
        String sql = "select instructorId,firstname,lastname,\n" +
                "courseName,sectionName,classBegin,classEnd,weekList,location,\n" +
                "(?::date- beginDate)/7+1 as curweek,DayOfWeek from enrollCourseSection es\n" +
                "join CourseSection CS on CS.sectionId = es.sectionId\n" +
                "join CourseSectionClass CSC on CS.sectionId = CSC.sectionId\n" +
                "join semester s on CS.semesterId = s.semesterid\n" +
                "join course c on CS.courseId = c.courseid\n" +
                "join Instructor I on CSC.instructorId = I.userId\n" +
                "where es.studentId = ? and beginDate <= ? and endDate >= ?";
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)
        ){
            stmt.setDate(1,date);
            stmt.setInt(2,studentId);
            stmt.setDate(3,date);
            stmt.setDate(4,date);
            ResultSet rs = stmt.executeQuery();//rs返回学生该学期选课class的所有信息,包括第几周
            Set<CourseTable.CourseTableEntry> MON = new HashSet<>();
            Set<CourseTable.CourseTableEntry> TUE = new HashSet<>();
            Set<CourseTable.CourseTableEntry> WEN = new HashSet<>();
            Set<CourseTable.CourseTableEntry> THU = new HashSet<>();
            Set<CourseTable.CourseTableEntry> FRI = new HashSet<>();
            Set<CourseTable.CourseTableEntry> SAT = new HashSet<>();
            Set<CourseTable.CourseTableEntry> SUN = new HashSet<>();
            while (rs.next()){
                int week = rs.getInt("curweek");//改周是本学期第几周
                int instructorid = rs.getInt("instructorid");
                String coursename = rs.getString("coursename");
                String sectionname = rs.getString("sectionname");
                String location = rs.getString("location");
                Short classbegin = rs.getShort("classBegin");
                Short classend = rs.getShort("classEnd");
                String firstName = rs.getString("firstname");
                String lastName = rs.getString("lastname");
                String dayOfWeek = rs.getString("DayOfWeek");
                Array array = rs.getArray("weeklist");
                Object obj = array.getArray();//通过getArray方法，会返还一个Object对象
                String[] weeklist = (String[]) obj;//将obj强转为最终类型的数组
                for (int k=0;k<weeklist.length;k++){
                    if (weeklist[k]!=null){
                        if (Integer.parseInt(weeklist[k])==week){//该class在本周有课，加入课程表
                            CourseTable.CourseTableEntry courseTableEntry = new CourseTable.CourseTableEntry();
                            courseTableEntry.courseFullName = coursename+"["+sectionname+"]";
                            Instructor instructor = new Instructor();
                            instructor.id = instructorid;
                            boolean English = false;
                            if(firstName.equals(" ") && lastName.equals(" ")){
                                English = true;
                            }else{
                                char[] array1 = firstName.toCharArray();
                                for(int i=0;i<firstName.length();i++){
                                    if((array1[i] >='a'&&array1[i]<='z')||(array1[i]>='A'&&array1[i]<='Z')){
                                        English = true;
                                        break;
                                    }
                                }
                                char[] array2 = lastName.toCharArray();
                                for(int i=0;i<lastName.length();i++){
                                    if (English){//如果姓也是英文的
                                        if((array2[i] >='a'&& array2[i]<='z')||(array2[i]>='A'&& array2[i]<='Z')){
                                            English = true;
                                            break;
                                        }else {
                                            English = false;
                                            break;
                                        }
                                    }else {
                                        English = false;
                                        break;
                                    }
                                }
                            }
                            if(English){
                                instructor.fullName =firstName+" "+lastName;
                            }else {
                                instructor.fullName =firstName+lastName;
                            }
                            courseTableEntry.instructor = instructor;
                            courseTableEntry.classBegin = classbegin;
                            courseTableEntry.classEnd = classend;
                            courseTableEntry.location = location;
                            if (dayOfWeek.equals("MONDAY")){
                                MON.add(courseTableEntry);
                            }else if (dayOfWeek.equals("TUESDAY")){
                                TUE.add(courseTableEntry);
                            }else if (dayOfWeek.equals("WEDNESDAY")){
                                WEN.add(courseTableEntry);
                            }else if (dayOfWeek.equals("THURSDAY")){
                                THU.add(courseTableEntry);
                            }else if (dayOfWeek.equals("FRIDAY")){
                                FRI.add(courseTableEntry);
                            }else if (dayOfWeek.equals("SATURDAY")){
                                SAT.add(courseTableEntry);
                            }else if (dayOfWeek.equals("SUNDAY")){
                                SUN.add(courseTableEntry);
                            }
                            break;
                        }
                    }
                }
            }
            Map<DayOfWeek, Set<CourseTable.CourseTableEntry>> map = new HashMap<>();
            map.put(DayOfWeek.THURSDAY,THU);
            map.put(DayOfWeek.SATURDAY,SAT);
            map.put(DayOfWeek.FRIDAY,FRI);
            map.put(DayOfWeek.MONDAY,MON);
            map.put(DayOfWeek.WEDNESDAY,WEN);
            map.put(DayOfWeek.SUNDAY,SUN);
            map.put(DayOfWeek.TUESDAY,TUE);
            courseTable.table = map;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return courseTable;
//        CourseTable courseTable = new CourseTable();
//        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
//             PreparedStatement stmt = connection.prepareStatement("select enrollCourseSection.sectionId from CourseSection join\n" +
//                     "enrollCourseSection on CourseSection.sectionId = enrollCourseSection.sectionId\n" +
//                     "where studentId = ? and semesterId = (select semesterId from semester where ? >= beginDate and ? <= endDate);");
//             PreparedStatement stmt1 = connection.prepareStatement("select beginDate from semester where beginDate <= ? and endDate >= ?");
//             PreparedStatement stmt2 = connection.prepareStatement("select date_part('day',(select ?::date - (extract (dow from ?::date) || ' day')::interval) - ?::timestamp)/7+1");
//             PreparedStatement stmt3 = connection.prepareStatement("select * from CourseSectionClass where sectionId = ?");
//             PreparedStatement stmt4 = connection.prepareStatement("select * from CourseSection where sectionId = ?");
//             PreparedStatement stmt5 = connection.prepareStatement("select * from Course where courseId = ?");
//             PreparedStatement stmt6 = connection.prepareStatement("select * from Instructor where userId = ?")
//            ){
//            stmt.setInt(1,studentId);
//            stmt.setDate(2,date);
//            stmt.setDate(3,date);
//            ResultSet rs = stmt.executeQuery();//rs返回该学生本学期选课sectionid
//            int sectionid = 0;
//
//            stmt1.setDate(1,date);
//            stmt1.setDate(2,date);
//            ResultSet rs1 = stmt1.executeQuery();//rs1返回当前学期开始日期beginDate
//            Date cur = new Date(2021,6,12);
//            while (rs1.next()) {//rs1是当前学期的beginDate
//                cur = rs1.getDate(1);
//            }
//            stmt2.setDate(1,date);
//            stmt2.setDate(2,date);
//            //System.out.println(date);
//            stmt2.setDate(3,cur);
//            ResultSet rs2 = stmt2.executeQuery();//rs2返回当前是第几周
//            int week = 0;
//            while (rs2.next()){
//               week = rs2.getInt(1);
//            }
//            //对每节课来说，看看这周有没有
//            List<Set<CourseTable.CourseTableEntry>> sevenSetList = new ArrayList<>();//准备七个set一天一个
//            for (int p=0;p<7;p++){
//                Set<CourseTable.CourseTableEntry> set = new HashSet<>();
//                sevenSetList.add(set);
//            }
//            while (rs.next()){//遍历这学期上过的所有section,一个sectionId对应一个dayOfWeek
//                sectionid = rs.getInt("sectionId");
//                stmt3.setInt(1,sectionid);
//                ResultSet rs3 = stmt3.executeQuery();//返回该课程sectionid的班级信息classid-->多个班级
//                while (rs3.next()){//-->对每个班级class来说
//                    int insid = 0;
//                    short begin = 0;
//                    short end = 0;
//                    String location = null;
//                    String dayOfWeek = null;
//                    begin = rs3.getShort("classBegin");
//                    end = rs3.getShort("classEnd");
//                    location = rs3.getString("location");
//                    dayOfWeek = rs3.getString("DayOfWeek");
//                    insid = rs3.getInt("instructorId");
//                    Array array =rs3.getArray("weekList");//-->每个班级class都有weeklist
//                    Object obj=array.getArray();//通过getArray方法，会返还一个Object对象
//                    String[] weeklist = (String[]) obj;//将obj强转为最终类型的数组
//                    CourseTable.CourseTableEntry courseTableEntry = new CourseTable.CourseTableEntry();//-->课程表里有一项
//                    for (int i=0;i<weeklist.length;i++){//遍历weeklist
//                        if (weeklist[i] == null){//单双周-->没找到
//                            continue;
//                        }else
//                        if (Integer.parseInt(weeklist[i]) == week){//如果该周上课
//                            stmt4.setInt(1,sectionid);//->获取该课程的courseId
//                            ResultSet rs4 = stmt4.executeQuery();//rs4是本周上课的courseId
//                            String courseid = null;
//                            String sectionname = null;
//                            while (rs4.next()){
//                                courseid = rs4.getString("courseId");
//                                sectionname = rs4.getString("sectionId");
//                            }
//                            stmt5.setString(1,courseid);
//                            ResultSet rs5 = stmt5.executeQuery();
//                            String coursename = null;
//                            while (rs5.next()){
//                                coursename = rs5.getString("courseName");
//                            }
//                            courseTableEntry.courseFullName = coursename+"["+sectionname+"]";
//                            Instructor instructor = new Instructor();
//                            instructor.id = insid;
//                            stmt6.setInt(1,insid);
//                            ResultSet rs6 = stmt6.executeQuery();
//                            String firstName = null;
//                            String lastName = null;
//                            while (rs6.next()){
//                                firstName = rs6.getString("firstName");
//                                lastName = rs6.getString("lastName");
//                            }
//                            assert firstName != null;
//                            char[] arrays = firstName.toCharArray();
//                            boolean English = false;
//                            if(firstName.equals(" ") && lastName.equals(" ")){
//                                English = true;
//                            }else{
//                                for(int k=0;k<firstName.length();k++){
//                                    if((arrays[k] >='a'&&arrays[k]<='z')||(arrays[k]>='A'&&arrays[k]<='Z')){
//                                        English = true;
//                                        break;
//                                    }
//                                }
//                                arrays = lastName.toCharArray();
//                                for(int k=0;k<lastName.length();k++){
//                                    if(English)break;
//                                    if((arrays[k] >='a'&&arrays[k]<='z')||(arrays[k]>='A'&&arrays[k]<='Z')){
//                                        English = true;
//                                        break;
//                                    }
//                                }
//                            }
//                            if(English){
//                                instructor.fullName =firstName+" "+lastName;
//                            }else {
//                                instructor.fullName =firstName+lastName;
//                            }
//                            courseTableEntry.instructor = instructor;
//                            courseTableEntry.classBegin = begin;
//                            courseTableEntry.classEnd = end;
//                            courseTableEntry.location = location;
//                            if (dayOfWeek.equals("MONDAY")){
//                                sevenSetList.get(0).add(courseTableEntry);
//                            }else if (dayOfWeek.equals("TUESDAY")){
//                                sevenSetList.get(1).add(courseTableEntry);
//                            }else if (dayOfWeek.equals("WEDNESDAY")){
//                                sevenSetList.get(2).add(courseTableEntry);
//                            }else if (dayOfWeek.equals("THURSDAY")){
//                                sevenSetList.get(3).add(courseTableEntry);
//                            }else if (dayOfWeek.equals("FRIDAY")){
//                                sevenSetList.get(4).add(courseTableEntry);
//                            }else if (dayOfWeek.equals("SATURDAY")){
//                                sevenSetList.get(5).add(courseTableEntry);
//                            }else if (dayOfWeek.equals("SUNDAY")){
//                                sevenSetList.get(6).add(courseTableEntry);
//                            }
//                            break;
//                        }
//                    }
//                }
//            }
//            Map<DayOfWeek, Set<CourseTable.CourseTableEntry>> table = new HashMap<>();
//
//            table.put(DayOfWeek.MONDAY,sevenSetList.get(0));
//            table.put(DayOfWeek.TUESDAY,sevenSetList.get(1));
//            table.put(DayOfWeek.WEDNESDAY,sevenSetList.get(2));
//            table.put(DayOfWeek.THURSDAY,sevenSetList.get(3));
//            table.put(DayOfWeek.FRIDAY,sevenSetList.get(4));
//            table.put(DayOfWeek.SATURDAY,sevenSetList.get(5));
//            table.put(DayOfWeek.SUNDAY,sevenSetList.get(6));
//            courseTable.table = table;
//        } catch (SQLException | EntityNotFoundException e) {
//            e.printStackTrace();
//        }
//        return courseTable;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt1 = connection.prepareStatement("select courseid,preid,groupid from coursepre where courseid = ?");
            PreparedStatement stmt = connection.prepareStatement("select cp.courseId, cp.preId,cp.groupid,cur.sectionid,cur.grade from coursepre cp \n" +
                    "    left join (select cs.courseId,e.sectionId,e.grade from coursesection cs \n" +
                    "    join enrollcoursegrade e on cs.sectionid = e.sectionid where studentId = ?) cur\n" +
                    "on cp.preid = cur.courseid where cp.courseid = ? order by cp.groupid")){
            stmt1.setString(1,courseId);
            ResultSet rst = stmt1.executeQuery();
            if(!rst.next()){
                return true;
            }
            stmt.setInt(1,studentId);
            stmt.setString(2,courseId);
            ResultSet rs = stmt.executeQuery();
            int group = 0;
            boolean flag =false;
            while (rs.next()){
                if(group!=rs.getInt("groupId")&&flag){//上一组全部正确
                    break;
                }else if(group==rs.getInt("groupId")&&flag){//在本组中，之前的都实现了
                    String cur = rs.getString("grade");
                    if(cur==null){//无成绩
                        flag = false;
                    }else if(cur.equals("PASS")){//通过
                        flag = true;
                    }else if(cur.equals("FAIL")){//挂科
                        flag = false;
                    }else {//>=60通过
                        int score = Integer.parseInt(cur);
                        flag = score >= 60;
                    }
                }else if(group!=rs.getInt("groupId")&& !flag){//上一组未能实现 开始本组
                    group = rs.getInt("groupId");
                    String cur = rs.getString("grade");
                    if(cur==null){//无成绩
                        flag = false;
                    }else if(cur.equals("PASS")){//通过
                        flag = true;
                    }else if(cur.equals("FAIL")){//挂科
                        flag = false;
                    }else {//>=60通过
                        int score = Integer.parseInt(cur);
                        flag = score >= 60;
                    }
                }//本组之前存在为实现的，没有继续的必要 直接跳过
            }
            if(flag){//存在某组完全实现
                return true;
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Major getStudentMajor(int studentId) {
        Major major = new Major();
        Department department = new Department();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("select major_id,M.name as majorname,departmentId,D.name as departmentname from Student s" +
                     "join Major M on M.majorId = s.major_id" +
                     "join Department D on D.departmentId = M.department_id" +
                     "where userId = ?")){
            stmt.setInt(1,studentId);
            ResultSet rs = stmt.executeQuery();
            major.id = rs.getInt("major_id");
            major.name = rs.getString("majorname");
            department.id = rs.getInt("departmentId");
            department.name = rs.getString("departmentname");
            major.department = department;
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
        }
        return major;
    }
}
