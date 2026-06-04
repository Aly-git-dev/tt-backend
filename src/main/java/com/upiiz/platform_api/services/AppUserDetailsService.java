package com.upiiz.platform_api.services;

import com.upiiz.platform_api.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository repo;
    public AppUserDetailsService(UserRepository repo){this.repo=repo;}
    @Override public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var u = repo.findByEmailInstIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException(email));
        return org.springframework.security.core.userdetails.User.withUsername(u.getEmailInst())
                .password(u.getPasswordHash()==null? "":u.getPasswordHash())
                .authorities(u.getRoles().stream().map(r->"ROLE_"+r.getName()).toArray(String[]::new))
                .build();
    }
}

