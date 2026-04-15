package com.jobcupid.job_cupid.user.controller;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author yuze1
 */
@RequestMapping("/api/v1/User")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user){
        User response = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
}
