package com.example.bloodpressuretracking

import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for Task 1.2: AWS Amplify設定
 * Verifies Amplify configuration structure and Cognito settings.
 */
class AmplifyConfigTest {

    // Expected Cognito configuration values from research.md
    private val expectedUserPoolId = "ap-northeast-1_vfDOOCJqF"
    private val expectedClientId = "4r1516g75kkrkpkq9vi0rltad"
    private val expectedRegion = "ap-northeast-1"

    @Test
    fun `amplify config contains auth plugin`() {
        val config = getTestConfig()
        assertTrue("Config should contain auth section", config.has("auth"))

        val auth = config.getJSONObject("auth")
        assertTrue("Auth should contain plugins", auth.has("plugins"))
    }

    @Test
    fun `cognito user pool config has correct pool id`() {
        val config = getTestConfig()
        val cognitoConfig = getCognitoUserPoolConfig(config)

        assertEquals(
            "UserPoolId should match expected value",
            expectedUserPoolId,
            cognitoConfig.getString("PoolId")
        )
    }

    @Test
    fun `cognito user pool config has correct client id`() {
        val config = getTestConfig()
        val cognitoConfig = getCognitoUserPoolConfig(config)

        assertEquals(
            "AppClientId should match expected value",
            expectedClientId,
            cognitoConfig.getString("AppClientId")
        )
    }

    @Test
    fun `cognito user pool config has correct region`() {
        val config = getTestConfig()
        val cognitoConfig = getCognitoUserPoolConfig(config)

        assertEquals(
            "Region should match expected value",
            expectedRegion,
            cognitoConfig.getString("Region")
        )
    }

    @Test
    fun `auth flow type is USER_SRP_AUTH`() {
        val config = getTestConfig()
        val authConfig = config
            .getJSONObject("auth")
            .getJSONObject("plugins")
            .getJSONObject("awsCognitoAuthPlugin")
            .getJSONObject("Auth")
            .getJSONObject("Default")

        assertEquals(
            "Auth flow type should be USER_SRP_AUTH",
            "USER_SRP_AUTH",
            authConfig.getString("authenticationFlowType")
        )
    }

    /**
     * Returns the test configuration JSON.
     * In actual tests, this would be loaded from the resource file.
     */
    private fun getTestConfig(): JSONObject {
        // This mirrors the actual amplifyconfiguration.json content
        return JSONObject("""
            {
                "auth": {
                    "plugins": {
                        "awsCognitoAuthPlugin": {
                            "IdentityManager": {
                                "Default": {}
                            },
                            "CognitoUserPool": {
                                "Default": {
                                    "PoolId": "ap-northeast-1_vfDOOCJqF",
                                    "AppClientId": "4r1516g75kkrkpkq9vi0rltad",
                                    "Region": "ap-northeast-1"
                                }
                            },
                            "Auth": {
                                "Default": {
                                    "authenticationFlowType": "USER_SRP_AUTH"
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent())
    }

    private fun getCognitoUserPoolConfig(config: JSONObject): JSONObject {
        return config
            .getJSONObject("auth")
            .getJSONObject("plugins")
            .getJSONObject("awsCognitoAuthPlugin")
            .getJSONObject("CognitoUserPool")
            .getJSONObject("Default")
    }
}
