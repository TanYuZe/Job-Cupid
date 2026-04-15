/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.jobcupid.job_cupid.user.service;

import org.springframework.stereotype.Service;

import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.repository.UserRepository;

import lombok.AllArgsConstructor;

/**
 *
 * @author yuze1
 */
@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;


    public User createUser(User user){
        userRepository.save(user);
        return user;
    }

}
