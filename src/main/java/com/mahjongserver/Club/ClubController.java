package com.mahjongserver.Club;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api")
public class ClubController {

    @Autowired
    private ClubService clubService;

    @PostMapping("/createClub")
    public Map<String, Object> createClub(@RequestBody Map<String, String> request) {
        String clubName = request.get("name");
        Map<String, Object> response = new HashMap<>();

        if (clubService.createClub(clubName)) {
            response.put("success", true);
            response.put("message", "Club created successfully");
        } else {
            response.put("success", false);
            response.put("message", "Error creating club");
        }

        return response;
    }
}