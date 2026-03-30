package com.chuber.expenseclaim.entity;

import com.chuber.expenseclaim.enums.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

    @Id
    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;
}
