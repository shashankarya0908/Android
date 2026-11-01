package com.fable.app.domain

import com.fable.app.domain.validation.AuthValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthValidationTest {
    @Test
    fun loginRequiresValidEmailAndPassword() {
        assertNotNull(AuthValidation.validateLogin("bad-email", "password"))
        assertNotNull(AuthValidation.validateLogin("reader@example.com", ""))
        assertNull(AuthValidation.validateLogin("reader@example.com", "short"))
    }

    @Test
    fun registrationValidatesUsernameDisplayNameAndMatchingPasswords() {
        assertNull(
            AuthValidation.validateRegistration(
                username = "Book_Lover7",
                displayName = "Book Lover",
                email = "reader@example.com",
                password = "correct-horse-42",
                confirmPassword = "correct-horse-42",
            ),
        )
        assertNotNull(AuthValidation.validateRegistration("a!", "Reader", "a@b.com", "password123", "password123"))
        assertNotNull(AuthValidation.validateRegistration("reader", "R", "a@b.com", "password123", "password123"))
        assertNotNull(AuthValidation.validateRegistration("reader", "Reader", "a@b.com", "password123", "different"))
    }

    @Test
    fun usernameIsTrimmedAndLowercasedConsistently() {
        assertEquals("book_lover7", AuthValidation.normalizeUsername("  Book_Lover7 "))
        assertNotNull(AuthValidation.validateProfile("spaces are bad", "Reader", ""))
    }
}
