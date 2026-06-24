package com.namnguyen.ecommerce_platform.user.Specifications;

import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> emailContains(String email) {
        return (root, query, cb) ->
                email == null ? null : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<User> firstNameContains(String firstName) {
        return (root, query, cb) ->
                firstName == null ? null : cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%");
    }

    public static Specification<User> lastNameContains(String lastName) {
        return (root, query, cb) ->
                lastName == null ? null : cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%");
    }

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) ->
                role == null ? null : cb.equal(root.get("role"), role);
    }
}
