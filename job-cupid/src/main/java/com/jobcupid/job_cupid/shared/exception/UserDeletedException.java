/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.jobcupid.job_cupid.shared.exception;

/**
 *
 * @author yuze1
 */
public class UserDeletedException extends RuntimeException{
    
    public UserDeletedException() {
        super("Your account has been deleted. Please contact support.");
    }


}
