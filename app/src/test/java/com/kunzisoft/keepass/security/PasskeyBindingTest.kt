package com.kunzisoft.keepass.security

import androidx.credentials.exceptions.GetCredentialUnknownException
import com.kunzisoft.encrypt.Base64Helper
import com.kunzisoft.encrypt.Signature
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataBuildResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialRequestOptions
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper
import com.kunzisoft.keepass.model.Passkey
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyPairGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PasskeyBindingTest {
    private fun passkey() = Passkey("user", Signature.convertPrivateKeyToPem(
        KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair().private
    ), "AQIDBA", "BQYH", "example.com", false, false,
        Base64Helper.b64Encode(ByteArray(32) { 7 }).toCharArray())

    private fun request(rp: String, allow: String = "[]") = PublicKeyCredentialRequestOptions(
        """{"challenge":"AQID","rpId":"$rp","allowCredentials":$allow,"extensions":{"prf":{"eval":{"first":"AQI"}}}}"""
    )

    private fun respond(request: PublicKeyCredentialRequestOptions, key: Passkey) =
        PasskeyHelper.buildPasskeyPublicKeyCredential(request,
            ClientDataBuildResponse(ClientDataBuildResponse.Type.GET, request.challenge, "https://${request.rpId}"),
            key, true, false, false)

    @Test fun wrongRpCannotObtainAnAssertionOrPrfOutputEvenThroughManualSelection() {
        val key = passkey()
        for (rp in listOf("attacker.example", "sub.example.com", "com", "", "example.com/evil")) {
            assertThrows(GetCredentialUnknownException::class.java) { respond(request(rp), key) }
        }
    }

    @Test fun completeCredentialIdAndPublicKeyTypeAreRequiredWhenAllowListExists() {
        val key = passkey()
        for (allow in listOf("""[{"type":"public-key","id":"AQID"}]""",
            """[{"type":"public-key","id":"AQIDBAU"}]""",
            """[{"type":"password","id":"AQIDBA"}]""")) {
            assertThrows(GetCredentialUnknownException::class.java) { respond(request("example.com", allow), key) }
        }
    }

    @Test fun malformedAllowListCannotTurnIntoDiscoverableRequest() {
        for (allow in listOf("null", "{}", "[{}]", "[1]",
            """[{"type":"public-key","id":"%%%"}]""",
            """[{"type":"public-key","id":"AQIDBA"},{}]""")) {
            assertThrows(Exception::class.java) { request("example.com", allow) }
        }
    }

    @Test fun validDiscoverableAndPaddedIdRequestsKeepAssertionAndPrf() {
        val key = passkey()
        for (allow in listOf("[]", """[{"type":"public-key","id":"AQIDBA=="}]""")) {
            val response = JSONObject(respond(request("example.com", allow), key).authenticationResponseJson)
            assertEquals("AQIDBA", response.getString("id"))
            assertTrue(response.getJSONObject("response").getString("signature").isNotEmpty())
            assertEquals(Base64Helper.b64Encode(PasskeyHelper.computePrfValue(ByteArray(32) { 7 }, byteArrayOf(1, 2))),
                response.getJSONObject("clientExtensionResults").getJSONObject("prf").getJSONObject("results").getString("first"))
        }
    }
}
