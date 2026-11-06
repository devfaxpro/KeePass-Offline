package com.kunzisoft.keepass.activities

/** Credential modes are available only through these non-exported components. */
class InternalFileDatabaseSelectActivity : FileDatabaseSelectActivity() {
    override val acceptsCredentialRequests: Boolean = true
}

class InternalMainCredentialActivity : MainCredentialActivity() {
    override val acceptsCredentialRequests: Boolean = true
}
