package org.nors.dev.codes.lpu.controller;

import java.util.List;
import org.nors.dev.codes.lpu.dto.StudentPageResponse;
import org.nors.dev.codes.lpu.dto.StudentResponse;
import org.nors.dev.codes.lpu.service.StudentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> list() {
        return ResponseEntity.ok(studentService.listActive());
    }

    @GetMapping("/page")
    public ResponseEntity<StudentPageResponse> page(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(studentService.page(search, offset, limit));
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<StudentResponse>> listInactive() {
        return ResponseEntity.ok(studentService.listInactive());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export() {
        byte[] csv = studentService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"students.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getById(id));
    }
}
