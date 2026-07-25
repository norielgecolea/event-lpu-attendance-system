package org.nors.dev.codes.lpu.service;

import java.util.List;
import org.nors.dev.codes.lpu.dto.EmployeePageResponse;
import org.nors.dev.codes.lpu.dto.EmployeeResponse;
import org.nors.dev.codes.lpu.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(transactionManager = "gateTransactionManager", readOnly = true)
    public List<EmployeeResponse> listActive() {
        return employeeRepository.findAllActive().stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Transactional(transactionManager = "gateTransactionManager", readOnly = true)
    public EmployeeResponse getById(Long id) {
        return employeeRepository.findActiveById(id)
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @Transactional(transactionManager = "gateTransactionManager", readOnly = true)
    public EmployeePageResponse page(String search, int offset, int limit) {
        int size = Math.min(Math.max(limit, 1), 200);
        int from = Math.max(offset, 0);
        List<EmployeeResponse> items = employeeRepository.searchActive(search, from, size).stream()
                .map(EmployeeResponse::from)
                .toList();
        return new EmployeePageResponse(items, employeeRepository.countActive(search));
    }
}
