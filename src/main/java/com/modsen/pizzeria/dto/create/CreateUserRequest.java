package com.modsen.pizzeria.dto.create;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(

        @NotNull(message = "{user.firstname.notnull}")
        String firstname,

        @NotNull(message = "{user.lastname.notnull}")
        String lastname,

        @NotNull(message = "{user.password.notnull}")
        String password,

        @Email(message = "{user.email.valid}")
        @NotNull(message = "{user.email.notnull}")
        String email

) {}
