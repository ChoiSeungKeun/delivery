package com.example.delivery.menu.service;

import com.example.delivery.menu.dto.MenuCreateRequest;
import com.example.delivery.menu.dto.MenuResponse;
import com.example.delivery.menu.repository.MenuRepository;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;

    @Transactional
    public MenuResponse create(Long userId, MenuCreateRequest request) {
        User user = userRepository.getReferenceById(userId);

        return MenuResponse.from(menuRepository.save(request.toEntity(user)));
    }
}
