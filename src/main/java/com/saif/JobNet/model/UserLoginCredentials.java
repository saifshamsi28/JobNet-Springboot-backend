package com.saif.JobNet.model;

import lombok.Data;

@Data
public class UserLoginCredentials {
    String userNameOrEmail;
    String password;
}
