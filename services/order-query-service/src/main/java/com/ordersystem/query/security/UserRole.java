package com.ordersystem.query.security;

import java.util.Set;

/**
 * Enterprise-grade Role-Based Access Control (RBAC)
 * Defines user roles and their associated permissions
 */
public enum UserRole {
    CUSTOMER(Set.of(
        "ORDER_VIEW_OWN",
        "ORDER_CREATE",
        "ORDER_CANCEL_OWN"
    )),
    
    ADMIN(Set.of(
        "ORDER_VIEW_ALL",
        "ORDER_MODIFY",
        "ORDER_DELETE",
        "INVENTORY_MANAGE",
        "USER_MANAGE",
        "SYSTEM_CONFIG"
    )),
    
    OPERATOR(Set.of(
        "ORDER_VIEW_ALL",
        "ORDER_MODIFY",
        "PAYMENT_PROCESS",
        "INVENTORY_VIEW",
        "REPORTS_VIEW"
    )),
    
    GUEST(Set.of(
        "ORDER_VIEW_PUBLIC",
        "CATALOG_VIEW"
    ));

    private final Set<String> permissions;

    UserRole(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * Get all permissions for multiple roles
     */
    public static Set<String> getAllPermissions(Set<UserRole> roles) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(java.util.stream.Collectors.toSet());
    }
}