package com.fable.app.presentation.auth

import com.fable.app.data.repository.UsernameAlreadyTakenException
import com.fable.app.di.FirebaseConfigurationRequiredException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

internal object FirebaseErrorMessages {
    fun message(error: Throwable): String = when (error) {
        is FirebaseConfigurationRequiredException -> error.message.orEmpty()
        is UsernameAlreadyTakenException -> error.message.orEmpty()
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 8 characters."
        is FirebaseAuthUserCollisionException -> "An account already exists for that email. Try signing in instead."
        is FirebaseAuthInvalidCredentialsException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
            "ERROR_INVALID_CREDENTIAL" -> "Email or password is incorrect."
            else -> "Invalid credentials. Please check your email and password."
        }
        is FirebaseAuthInvalidUserException -> when (error.errorCode) {
            "ERROR_USER_DISABLED" -> "This account has been disabled. Please contact support."
            "ERROR_USER_NOT_FOUND" -> "No account found for that email. Please create an account."
            else -> "No account found for that email address."
        }
        is FirebaseAuthActionCodeException -> "The password reset link is invalid or has expired."
        is FirebaseAuthRecentLoginRequiredException -> "Please sign in again before performing this action."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a moment and try again."
        is FirebaseNetworkException -> "No internet connection. Please check your network and try again."
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Access denied. Please check your Firestore security rules."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Authentication service is temporarily unavailable. Please try again later."
            else -> "Unable to save your profile to the cloud. Please try again."
        }
        is IOException -> "No internet connection. Please check your network and try again."
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password sign-in is not enabled in Firebase Console. Please enable it under Authentication > Sign-in method."
            "ERROR_USER_NOT_FOUND" -> "No account found for that email. Please check your email or sign up."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists for that email. Try signing in instead."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
            "ERROR_NETWORK_REQUEST_FAILED" -> "No internet connection. Please check your network and try again."
            else -> "Authentication failed (${error.errorCode ?: "unknown"}). Please check your details."
        }
        is IllegalStateException -> {
            val msg = error.message.orEmpty()
            if (msg.contains("Firebase is not configured", ignoreCase = true) ||
                msg.contains("Default FirebaseApp is not initialized", ignoreCase = true)) {
                "Firebase is not configured. Add app/google-services.json and sync the project."
            } else {
                sanitizeMessage(error)
            }
        }
        else -> sanitizeMessage(error)
    }

    private fun sanitizeMessage(error: Throwable): String {
        val msg = error.message.orEmpty().trim()
        return if (msg.isNotBlank() && !msg.contains("Exception") && !msg.contains("at ") && !msg.contains("\n") && !msg.contains("{")) {
            msg
        } else {
            "An unexpected error occurred. Please try again."
        }
    }
}
