package com.banquemisr.recruitment.Authentication.Security;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepo.findByUserEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<SimpleGrantedAuthority> authorities = Collections.emptyList();
        if (user.getRole() != null && user.getRole().getRoleName() != null) {
            authorities = List.of(new SimpleGrantedAuthority(user.getRole().getRoleName()));
        }

        return new User(
                user.getUserEmail(),
                user.getUserPassword(),
                user.getEnabled() != null ? user.getEnabled() : true,
                true,
                true,
                true,
                authorities
        );
    }
}
