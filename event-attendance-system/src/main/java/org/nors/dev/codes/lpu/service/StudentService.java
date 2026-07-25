package org.nors.dev.codes.lpu.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.nors.dev.codes.lpu.dto.StudentPageResponse;
import org.nors.dev.codes.lpu.dto.StudentResponse;
import org.nors.dev.codes.lpu.model.Student;
import org.nors.dev.codes.lpu.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(transactionManager = "gateTransactionManager", readOnly = true)
    public List<StudentResponse> listActive() {
        return studentRepository.findAllActive().stream()
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional(transactionManager = "gateTransactionManager", readOnly = true)
    public StudentResponse getById(Long id) {
        return StudentResponse.from(requireActive(id));
    }

    @Transactional(transactionManager = "gateTransactionManager", readOnly = true)
    public StudentPageResponse page(String search, int offset, int limit) {
        int size = Math.min(Math.max(limit, 1), 200);
        int from = Math.max(offset, 0);
        List<StudentResponse> items = studentRepository.searchActive(search, from, size).stream()
                .map(StudentResponse::from)
                .toList();
        return new StudentPageResponse(items, studentRepository.countActive(search));
    }

    @Transactional(transactionManager = "gateTransactionManager", readOnly = true)
    public List<StudentResponse> listInactive() {
        return studentRepository.findAllInactive().stream()
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional(transactionManager = "gateTransactionManager", readOnly = true)
    public byte[] exportCsv() {
        List<Student> students = studentRepository.findAllActive();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.println("Name,ID Number,RFID,Department,Course,School,Birthday");
            for (Student student : students) {
                writer.printf(
                        "%s,%s,%s,%s,%s,%s,%s%n",
                        csv(student.getName()),
                        csv(student.getStudentNo()),
                        csv(student.getRfid()),
                        csv(student.getDepartment()),
                        csv(student.getCourse()),
                        csv(student.getSchool()),
                        student.getBirthdate() == null ? "" : student.getBirthdate().toString()
                );
            }
        }
        return baos.toByteArray();
    }

    private Student requireActive(Long id) {
        return studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
