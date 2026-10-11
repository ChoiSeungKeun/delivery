package com.example.delivery.menu.controller;

import com.example.delivery.global.response.ApiResponse;
import com.example.delivery.global.response.PageResponse;
import com.example.delivery.global.security.CustomUserDetails;
import com.example.delivery.menu.dto.MenuCreateRequest;
import com.example.delivery.menu.dto.MenuResponse;
import com.example.delivery.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MenuResponse>> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @Valid @RequestBody MenuCreateRequest request) {
        MenuResponse response = menuService.create(userDetails.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("메뉴가 등록되었습니다.", response));
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenu(@PathVariable Long menuId) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getMenu(menuId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MenuResponse>>> getMenus(
            @PageableDefault(size = 10, sort = "type", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(menuService.getMenus(pageable)));
    }

}
