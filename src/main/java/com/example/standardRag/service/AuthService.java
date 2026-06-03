package com.example.standardRag.service;

import com.example.standardRag.dto.LoginRequest;
import com.example.standardRag.dto.SignupRequest;
import com.example.standardRag.dto.SignupResponse;
import com.example.standardRag.entity.User;
import com.example.standardRag.error.BadRequestException;
import com.example.standardRag.repository.UserRepository;
import com.example.standardRag.security.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;


    public SignupResponse signup(SignupRequest signupRequest) {
        userRepository.findByUsername(signupRequest.getUsername()).ifPresent(user -> {
            throw new BadRequestException("Username is already in use");
        });
        User user = modelMapper.map(signupRequest, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return modelMapper.map(user, SignupResponse.class);
    }

    public String login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));
        return authUtil.generateAccessToken(user);
    }
}
