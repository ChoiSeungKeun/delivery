package com.example.delivery.menu.service;

import com.example.delivery.global.exception.BusinessException;
import com.example.delivery.global.exception.ErrorCode;
import com.example.delivery.global.response.PageResponse;
import com.example.delivery.menu.dto.MenuCreateRequest;
import com.example.delivery.menu.dto.MenuResponse;
import com.example.delivery.menu.repository.MenuRepository;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("id", "price", "type");

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;

    @Transactional
    public MenuResponse create(Long userId, MenuCreateRequest request) {
        User user = userRepository.getReferenceById(userId);

        return MenuResponse.from(menuRepository.save(request.toEntity(user)));
    }

    public MenuResponse getMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .map(MenuResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
    }

    public PageResponse<MenuResponse> getMenus(Pageable pageable) {
        validateSort(pageable.getSort());

        Pageable resolved = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), addIdOrderIfAbsent(pageable.getSort())
        );

        return PageResponse.from(menuRepository.findAll(resolved).map(MenuResponse::from));
    }

    private void validateSort(Sort sort) {
        boolean hasInvalid = sort.stream()
                .anyMatch(order -> !ALLOWED_SORT_PROPERTIES.contains(order.getProperty()));

        if (hasInvalid) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private Sort addIdOrderIfAbsent(Sort sort) {
        return sort.getOrderFor("id") == null ? sort.and(Sort.by(Sort.Direction.DESC, "id")) : sort;
    }

}