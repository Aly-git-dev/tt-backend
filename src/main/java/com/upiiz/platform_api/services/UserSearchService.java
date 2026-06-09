package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.UserSearchResponse;
import com.upiiz.platform_api.repositories.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserSearchService {

    private final UserRepository userRepo;

    public UserSearchService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchTeachers(String query) {
        String term = query == null ? "" : query.trim();
        return userRepo
                .searchActiveUsersByRoles(term, List.of("PROFESOR"), PageRequest.of(0, 20))
                .stream()
                .map(this::mapUserSearchResponse)
                .toList();
    }

    private UserSearchResponse mapUserSearchResponse(Object[] row) {
        UserSearchResponse response = new UserSearchResponse();
        response.id = (UUID) row[0];
        response.email = (String) row[1];
        response.name = (String) row[2];
        return response;
    }
}
