package com.saif.JobNet.services;

import com.saif.JobNet.model.MyUserDetails;
import com.saif.JobNet.model.User;
import com.saif.JobNet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Optional<User> userBox=userRepository.getUserByUserName(usernameOrEmail);

        if(userBox.isEmpty()){
            userBox=userRepository.getUserByEmail(usernameOrEmail);
            if(userBox.isEmpty()) {
                System.err.println("User not found");
                throw new UsernameNotFoundException("User not found");
            }
        }

        return new MyUserDetails(userBox.get());
    }
}
