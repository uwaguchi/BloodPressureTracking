package com.example.bloodpressuretracking.data.repository

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.core.Amplify
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production implementation of AmplifyAuthWrapper.
 * Wraps AWS Amplify Auth SDK calls.
 */
@Singleton
class AmplifyAuthWrapperImpl @Inject constructor() : AmplifyAuthWrapper {

    override suspend fun fetchAuthSession(): AuthSessionResult {
        return suspendCancellableCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { authSession ->
                    continuation.resume(AuthSessionResult.Success(authSession.isSignedIn))
                },
                { error ->
                    continuation.resume(AuthSessionResult.Error(error.message ?: "セッション確認に失敗しました"))
                }
            )
        }
    }

    override suspend fun signIn(username: String, password: String): SignInResult {
        return suspendCancellableCoroutine { continuation ->
            Amplify.Auth.signIn(
                username,
                password,
                { result ->
                    if (result.isSignedIn) {
                        continuation.resume(SignInResult.Success)
                    } else {
                        continuation.resume(SignInResult.Error("追加の認証ステップが必要です"))
                    }
                },
                { error ->
                    val message = when {
                        error.message?.contains("NotAuthorizedException") == true ->
                            "ユーザー名またはパスワードが正しくありません"
                        error.message?.contains("UserNotFoundException") == true ->
                            "ユーザーが見つかりません"
                        error.message?.contains("network") == true ->
                            "ネットワークに接続できません"
                        else -> error.message ?: "ログインに失敗しました"
                    }
                    continuation.resume(SignInResult.Error(message))
                }
            )
        }
    }

    override suspend fun getIdToken(): String? {
        return suspendCancellableCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { authSession ->
                    val cognitoSession = authSession as? AWSCognitoAuthSession
                    val token = cognitoSession?.userPoolTokensResult?.value?.idToken
                    continuation.resume(token)
                },
                { _ ->
                    continuation.resume(null)
                }
            )
        }
    }

    override suspend fun signOut(): SignOutResult {
        return suspendCancellableCoroutine { continuation ->
            val options = AuthSignOutOptions.builder()
                .globalSignOut(false)
                .build()

            Amplify.Auth.signOut(options) { signOutResult ->
                when (signOutResult) {
                    is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                        continuation.resume(SignOutResult.Success)
                    }
                    is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                        continuation.resume(SignOutResult.Success)
                    }
                    is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                        continuation.resume(
                            SignOutResult.Error(signOutResult.exception.message ?: "サインアウトに失敗しました")
                        )
                    }
                }
            }
        }
    }
}
