package dao;
import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;
public class ImServiceFactory extends ServiceFactory {
    public ImServiceFactory(){
        registerService(UserService.class, new ImUserService());
        registerService(StudentService.class, new ImStudentService());
        registerService(CourseService.class, new ImCourseService());
        registerService(DepartmentService.class,new ImDepartmentService());
        registerService(MajorService.class,new ImMajorService());
        registerService(SemesterService.class,new ImSemesterService());
        registerService(InstructorService.class,new ImInstructor());
        // registerService(<interface name>.class, new <your implementation>());
    }
}
