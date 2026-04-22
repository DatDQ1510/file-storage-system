package com.java.file_storage_system.custom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    Permission value();

    enum Permission {
        READ(1),
        WRITE(2),
        DELETE(4),
        MANAGE_MEMBER(8);

        private final int bit;

        Permission(int bit) {
            this.bit = bit;
        }

        public int bit() {
            return bit;
        }
    }
}
