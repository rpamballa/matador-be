package com.matador.shared.security;

/**
 * Authorization roles. Staff roles enforce admin-API access; {@code CUSTOMER}
 * is the only end-user role.
 */
public enum Role {
    ADMIN,
    DISPATCHER,
    SUPPORT,
    READONLY,
    CUSTOMER
}
