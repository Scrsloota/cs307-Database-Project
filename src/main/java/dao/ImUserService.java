package dao;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.UserService;

import javax.persistence.criteria.CriteriaBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class ImUserService implements UserService {
    @Override
    public void removeUser(int userId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt1 = connection.prepareStatement("delete from Student where userId = ?")) {
            stmt1.setInt(1, userId);
            stmt1.execute();
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
        }
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt2 = connection.prepareStatement("delete from Instructor where userId = ?")) {
            stmt2.setInt(1, userId);
            stmt2.execute();
        } catch (SQLException | EntityNotFoundException e) {
            e.printStackTrace();
        }
        //System.out.println("remove User successful!");
    }

    @Override
    public List<User> getAllUsers() {
        List<User> AllUser =new LinkedList<>();
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
        PreparedStatement stmt1 = connection.prepareStatement("select * from Student")){
            ResultSet rs1 = stmt1.executeQuery();
            while (rs1.next()){
                Student curUser = new Student();
                curUser.id = rs1.getInt("userId");
                String firstName = rs1.getString("firstname");
                String lastName = rs1.getString("lastname");
                char[] array = firstName.toCharArray();
                boolean English = false;
                if(firstName.equals(" ") && lastName.equals(" ")){
                    English = true;
                }else{
                    for(int i=0;i<firstName.length();i++){
                        if((array[i] >='a'&&array[i]<='z')||(array[i]>='A'&&array[i]<='Z')){
                            English = true;
                            break;
                        }
                    }
                    array = lastName.toCharArray();
                    for(int i=0;i<lastName.length();i++){
                        if(English)break;
                        if((array[i] >='a'&&array[i]<='z')||(array[i]>='A'&&array[i]<='Z')){
                            English = true;
                            break;
                        }
                    }
                }
                if(English){
                    curUser.fullName =firstName+" "+lastName;
                }else {
                    curUser.fullName =firstName+lastName;
                }
                AllUser.add(curUser);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt2 = connection.prepareStatement("select * from Instructor ")){
            ResultSet rs2 = stmt2.executeQuery();
            while (rs2.next()){
                Instructor curUser = new Instructor();
                curUser.id = rs2.getInt("userId");
                String firstName = rs2.getString("firstname");
                String lastName = rs2.getString("lastname");
                char[] array = firstName.toCharArray();
                boolean English = false;
                if(firstName.equals(" ") && lastName.equals(" ")){
                    English = true;
                }else{
                    for(int i=0;i<firstName.length();i++){
                        if((array[i] >='a'&&array[i]<='z')||(array[i]>='A'&&array[i]<='Z')){
                            English = true;
                            break;
                        }
                    }
                    array = lastName.toCharArray();
                    for(int i=0;i<lastName.length();i++){
                        if(English)break;
                        if((array[i] >='a'&&array[i]<='z')||(array[i]>='A'&&array[i]<='Z')){
                            English = true;
                            break;
                        }
                    }
                }
                if(English){
                    curUser.fullName =firstName+" "+lastName;
                }else {
                    curUser.fullName =firstName+lastName;
                }
                AllUser.add(curUser);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        //System.out.println("getAllUsers successful!");
        return AllUser;
    }

    @Override
    public User getUser(int userId) {
        Student student = new Student();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt1 = connection.prepareStatement("select * from Student where userId = ?")
             ) {
            stmt1.setInt(1, userId);
            ResultSet rs1 =stmt1.executeQuery();
            while (rs1.next()){
                student.id = rs1.getInt("userId");
                String firstName = rs1.getString("firstname");
                String lastName = rs1.getString("lastname");
                char[] array = firstName.toCharArray();
                boolean English = false;
                if(firstName.equals(" ") && lastName.equals(" ")){
                    English = true;
                }else{
                    for(int i=0;i<firstName.length();i++){
                        if((array[i] >='a'&&array[i]<='z')||(array[i]>='A'&&array[i]<='Z')){
                            English = true;
                            break;
                        }
                    }
                    array = lastName.toCharArray();
                    for(int i=0;i<lastName.length();i++){
                        if(English)break;
                        if((array[i] >='a'&&array[i]<='z')||(array[i]>='A'&&array[i]<='Z')){
                            English = true;
                            break;
                        }
                    }
                }
                if(English){
                    student.fullName =firstName+" "+lastName;
                }else {
                    student.fullName =firstName+lastName;
                }
            }
            return student;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Instructor instructor = new Instructor();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt2 = connection.prepareStatement("select * from Instructor where userId = ?")
        ) {
            stmt2.setInt(1, userId);
            ResultSet rs2 =stmt2.executeQuery();
            while (rs2.next()){
                instructor.id = rs2.getInt("userId");
                String firstName = rs2.getString("firstname");
                String lastName = rs2.getString("lastname");
                char[] array = firstName.toCharArray();
                boolean English = false;
                if(firstName.equals(" ") && lastName.equals(" ")){
                    English = true;
                }else{
                    for(int i=0;i<firstName.length();i++){
                        if((array[i] >='a'&&array[i]<='z')||(array[i]>='A'&&array[i]<='Z')){
                            English = true;
                            break;
                        }
                    }
                    array = lastName.toCharArray();
                    for(int i=0;i<lastName.length();i++){
                        if(English)break;
                        if((array[i] >='a'&&array[i]<='z')||(array[i]>='A'&&array[i]<='Z')){
                            English = true;
                            break;
                        }
                    }
                }
                if(English){
                    instructor.fullName =firstName+" "+lastName;
                }else {
                    instructor.fullName =firstName+lastName;
                }
            }
            return instructor;
        } catch (SQLException | EntityNotFoundException e){
            e.printStackTrace();
        }
        return instructor;
    }
}
