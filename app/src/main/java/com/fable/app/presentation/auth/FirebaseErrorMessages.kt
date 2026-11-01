package com.fable.app.presentation.auth

import com.fable.app.di.FirebaseConfigurationRequiredException
import com.fable.app.data.repository.UsernameAlreadyTakenException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

internal object FirebaseErrorMessages {
    fun message(error: Throwable): String = when (error) {
        is FirebaseConfigurationRequiredException -> error.message.orEmpty()
        is UsernameAlreadyTakenException -> error.message.orEmpty()
        is FirebaseAuthWeakPasswordException -> "Choose a stronger password with at least 8 characters."
        is FirebaseAuthUserCollisionException -> "An account already exists for that email. Try signing in instead."
        is FirebaseAuthInvalidCredentialsException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "That email address is not valid."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Email or password is incorrect."
            else -> "Email or password is incorrect."
        }
        is FirebaseTooManyRequestsException -> "Too many attempts. Wait a little and try again."
        is FirebaseNetworkException -> "Network connection failed. Check your connection and try again."
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "This action is not permitted. Check your Firestore rules."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Firestore is temporarily unavailable. Try again shortly."
            else -> "We could not save your profile. Please try again."
        }
        is IOException -> "Network connection failed. Check your connection and try again."
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_USER_NOT_FOUND" -> "Email or password is incorrect."
            "ERROR_INVALID_EMAIL" -> "That email address is not valid."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists for that email. Try signing in instead."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a little and try again."
            else -> "Authentication failed. Check your details and try again."
        }
        else -> error.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
    }
}
