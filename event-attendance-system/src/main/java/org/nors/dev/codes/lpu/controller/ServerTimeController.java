package org.nors.dev.codes.lpu.controller;

import org.nors.dev.codes.lpu.dto.ServerTimeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/time")
public class ServerTimeController {

    /** Public — kiosks sync their clocks to the application server. */
    @GetMapping
    public ResponseEntity<ServerTimeResponse> now() {
        return ResponseEntity.ok(ServerTimeResponse.current());
    }
}
