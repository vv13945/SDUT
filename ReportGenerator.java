package com.grademanagement.report;

import com.grademanagement.student.StudentManager;
import com.grademanagement.course.CourseManager;
import com.grademanagement.grade.GradeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表生成类
 * 负责生成各种统计报表
 */
public class ReportGenerator {
    private final StudentManager studentManager;
    private final CourseManager courseManager;
    private final GradeManager gradeManager;

    public ReportGenerator(StudentManager studentManager,
                           CourseManager courseManager,
                           GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.courseManager = courseManager;
        this.gradeManager = gradeManager;
    }

    /**
     * 生成学生成绩单
     * @param studentId 学号
     */
    public void generateStudentTranscript(String studentId) {
        StudentManager.Student student = studentManager.getStudent(studentId);
        if (student == null) {
            System.out.println("该学号不存在！");
            return;
        }

        Map<String, Double> grades = gradeManager.getStudentGrades(studentId);
        if (grades.isEmpty()) {
            System.out.println("该学生暂无成绩记录！");
            return;
        }

        System.out.println("===== 学生成绩单 =====");
        System.out.println("学号: " + student.getStudentId());
        System.out.println("姓名: " + student.getName());
        System.out.println("院系: " + student.getDepartment());
        System.out.println("\n课程成绩:");

        double totalCredits = 0;
        double weightedSum = 0;

        for (Map.Entry<String, Double> entry : grades.entrySet()) {
            String courseId = entry.getKey();
            double grade = entry.getValue();
            CourseManager.Course course = courseManager.getCourse(courseId);

            if (course != null) {
                double credit = course.getCredit();
                totalCredits += credit;
                weightedSum += grade * credit;

                System.out.printf("%s (%s): %.1f (学分: %.1f)\n",
                        course.getCourseName(), courseId,
                        grade, credit);
            }
        }

        double gpa = weightedSum / totalCredits;
        System.out.println("\nGPA: " + String.format("%.2f", gpa));
    }

    /**
     * 生成课程成绩报表
     * @param courseId 课程编号
     */
    public void generateCourseReport(String courseId) {
        CourseManager.Course course = courseManager.getCourse(courseId);
        if (course == null) {
            System.out.println("该课程不存在！");
            return;
        }

        Map<String, Double> grades = gradeManager.getCourseGrades(courseId);
        if (grades.isEmpty()) {
            System.out.println("该课程暂无成绩记录！");
            return;
        }

        System.out.println("===== 课程成绩报表 =====");
        System.out.println("课程名称: " + course.getCourseName());
        System.out.println("课程编号: " + course.getCourseId());
        System.out.println("学分: " + course.getCredit());
        System.out.println("授课教师: " + course.getTeacher());
        System.out.println("\n学生成绩列表:");

        List<String> ranking = gradeManager.getGradeRanking(courseId);
        for (int i = 0; i < ranking.size(); i++) {
            String studentId = ranking.get(i);
            StudentManager.Student student = studentManager.getStudent(studentId);
            double grade = grades.get(studentId);

            System.out.printf("%d. %s (%s): %.1f\n",
                    i + 1, student.getName(),
                    studentId, grade);
        }

        gradeManager.displayCourseStatistics(courseId);
    }

    /**
     * 生成院系成绩分析报告
     * @param department 院系名称
     */
    public void generateDepartmentReport(String department) {
        List<StudentManager.Student> students = studentManager.getStudentsByDepartment(department);
        if (students.isEmpty()) {
            System.out.println("该院系不存在或没有学生！");
            return;
        }

        System.out.println("===== 院系成绩分析报告 =====");
        System.out.println("院系: " + department);
        System.out.println("学生人数: " + students.size());

        int studentWithGrades = 0;
        double totalGPA = 0;
        Map<String, Integer> courseCount = new HashMap<>();
        Map<String, Double> courseAvgGrade = new HashMap<>();

        for (StudentManager.Student student : students) {
            Map<String, Double> grades = gradeManager.getStudentGrades(student.getStudentId());
            if (grades.isEmpty()) continue;

            studentWithGrades++;

            // 计算学生GPA
            double weightedSum = 0;
            double totalCredits = 0;

            for (Map.Entry<String, Double> entry : grades.entrySet()) {
                String courseId = entry.getKey();
                double grade = entry.getValue();
                CourseManager.Course course = courseManager.getCourse(courseId);

                if (course != null) {
                    double credit = course.getCredit();
                    totalCredits += credit;
                    weightedSum += grade * credit;

                    // 统计课程信息
                    courseCount.put(courseId, courseCount.getOrDefault(courseId, 0) + 1);
                    courseAvgGrade.put(courseId,
                            courseAvgGrade.getOrDefault(courseId, 0.0) + grade);
                }
            }

            if (totalCredits > 0) {
                totalGPA += weightedSum / totalCredits;
            }
        }

        System.out.println("有成绩记录的学生: " + studentWithGrades);
        if (studentWithGrades > 0) {
            System.out.println("平均GPA: " + String.format("%.2f", totalGPA / studentWithGrades));
        }

        System.out.println("\n热门课程统计:");
        for (Map.Entry<String, Integer> entry : courseCount.entrySet()) {
            String courseId = entry.getKey();
            int count = entry.getValue();
            double avgGrade = courseAvgGrade.get(courseId) / count;
            CourseManager.Course course = courseManager.getCourse(courseId);

            System.out.printf("%s (%s): %d人选修, 平均成绩: %.1f\n",
                    course.getCourseName(), courseId,
                    count, avgGrade);
        }
    }

    /**
     * 生成教师教学报告
     * @param teacher 教师姓名
     */
    public void generateTeacherReport(String teacher) {
        List<CourseManager.Course> courses = courseManager.getCoursesByTeacher(teacher);
        if (courses.isEmpty()) {
            System.out.println("该教师不存在或没有授课！");
            return;
        }

        System.out.println("===== 教师教学报告 =====");
        System.out.println("教师: " + teacher);
        System.out.println("教授课程数: " + courses.size());

        int totalStudents = 0;
        double totalCourseAvg = 0;

        for (CourseManager.Course course : courses) {
            Map<String, Double> grades = gradeManager.getCourseGrades(course.getCourseId());
            int studentCount = grades.size();
            totalStudents += studentCount;

            System.out.println("\n课程: " + course.getCourseName() +
                    " (" + course.getCourseId() + ")");
            System.out.println("学分: " + course.getCredit());
            System.out.println("选课人数: " + studentCount);

            if (studentCount > 0) {
                double avg = gradeManager.calculateCourseAverage(course.getCourseId());
                totalCourseAvg += avg;
                System.out.println("平均成绩: " + String.format("%.1f", avg));

                // 成绩分布简览
                int excellent = 0; // >=90
                int good = 0;       // 80-89
                int medium = 0;     // 70-79
                int pass = 0;       // 60-69
                int fail = 0;       // <60

                for (double grade : grades.values()) {
                    if (grade >= 90) excellent++;
                    else if (grade >= 80) good++;
                    else if (grade >= 70) medium++;
                    else if (grade >= 60) pass++;
                    else fail++;
                }

                System.out.println("成绩分布:");
                System.out.println("优秀(≥90): " + excellent + "人");
                System.out.println("良好(80-89): " + good + "人");
                System.out.println("中等(70-79): " + medium + "人");
                System.out.println("及格(60-69): " + pass + "人");
                System.out.println("不及格(<60): " + fail + "人");
            }
        }

        if (courses.size() > 0) {
            System.out.println("\n综合统计:");
            System.out.println("平均每门课选课人数: " + totalStudents / courses.size());
            System.out.println("所有课程平均成绩: " +
                    String.format("%.1f", totalCourseAvg / courses.size()));
        }
    }
}