/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.jobcupid.job_cupid.user.dto;

import java.util.UUID;

import com.jobcupid.job_cupid.user.entity.UserRole;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 *
 * @author yuze1
 */
@RequiredArgsConstructor
@Getter
@Setter
public class UserResponse {
    private final UUID userId;
    private final String email;
    private final UserRole role;
    private final boolean premium;
}
