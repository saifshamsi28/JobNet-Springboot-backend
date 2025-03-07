package com.saif.JobNet.model;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String id;
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
}
