package com.namnguyen.ecommerce_platform.user.Specifications;

import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> emailContains(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) {
                return null;
            }

            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) ->
                role == null ? null : cb.equal(root.get("role"), role);
    }

    public static Specification<User> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern)
            );
        };
    }
}
