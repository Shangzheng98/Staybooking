package com.shangzheng.staybooking.service;

import com.shangzheng.staybooking.exception.UserAlreadyExistException;
import com.shangzheng.staybooking.model.Authority;
import com.shangzheng.staybooking.model.User;
import com.shangzheng.staybooking.model.UserRole;
import com.shangzheng.staybooking.repository.AuthorityRepository;
import com.shangzheng.staybooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterService {
    private UserRepository userRepository;
    private AuthorityRepository authorityRepository;
    private PasswordEncoder passwordEncoder;
    @Autowired
    public RegisterService(UserRepository userRepository, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)

    public void add(User user, UserRole role) throws UserAlreadyExistException {
        if (userRepository.existsById(user.getUsername())) {
            throw new UserAlreadyExistException("User already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        this.userRepository.save(user);
        authorityRepository.save(new Authority(user.getUsername(), role.name()));
    }
}
