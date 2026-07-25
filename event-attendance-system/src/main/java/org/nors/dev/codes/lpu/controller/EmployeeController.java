package org.nors.dev.codes.lpu.controller;

import java.util.List;
import org.nors.dev.codes.lpu.dto.EmployeePageResponse;
import org.nors.dev.codes.lpu.dto.EmployeeResponse;
import org.nors.dev.codes.lpu.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only view of gate attendance employees. */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> list() {
        return ResponseEntity.ok(employeeService.listActive());
    }

    @GetMapping("/page")
    public ResponseEntity<EmployeePageResponse> page(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(employeeService.page(search, offset, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }
}
