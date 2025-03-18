package com.example.attendanceapp.models;

public class StudentModel {

    String Name, Course, Teacher, Section;

    public StudentModel() {
    }

    public StudentModel(String name, String course, String teacher, String section) {
        Name = name;
        Course = course;
        Teacher = teacher;
        Section = section;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getCourse() {
        return Course;
    }

    public void setCourse(String course) {
        Course = course;
    }

    public String getTeacher() {
        return Teacher;
    }

    public void setTeacher(String teacher) {
        Teacher = teacher;
    }

    public String getSection() {
        return Section;
    }

    public void setSection(String section) {
        Section = section;
    }
}
