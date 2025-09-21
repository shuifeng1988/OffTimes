package com.offtime.app.utils

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

object AlipaySignUtils {

    private const val TAG = "AlipaySignUtils"
    private const val ALGORITHM = "RSA"
    private const val SIGN_ALGORITHMS = "SHA256WithRSA"
    private const val DEFAULT_CHARSET = "UTF-8"

    fun sign(content: String, privateKey: String, rsa2: Boolean): String {
        try {
            val privateKeySpec = PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.DEFAULT))
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            val priKey: PrivateKey = keyFactory.generatePrivate(privateKeySpec)
            val signature = Signature.getInstance(SIGN_ALGORITHMS)
            signature.initSign(priKey)
            signature.update(content.toByteArray(charset(DEFAULT_CHARSET)))
            val signed = signature.sign()
            return Base64.encodeToString(signed, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing string", e)
        }
        return ""
    }
}
