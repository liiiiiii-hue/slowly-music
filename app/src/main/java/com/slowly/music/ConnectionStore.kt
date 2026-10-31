package com.slowly.music

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SavedConnection(val address:String,val user:String,val password:String)

/** The password never enters ordinary preferences, logs, or Android backups. */
class ConnectionStore(context:Context, private val namespace:String="connection") {
    private val prefs=context.getSharedPreferences(namespace,Context.MODE_PRIVATE)
    private val alias="slowly-$namespace-v1"
    private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey(alias,null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(value:SavedConnection) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.ENCRYPT_MODE,key())}
        val payload=JSONObject().put("address",value.address).put("user",value.user).put("password",value.password).toString()
        val encrypted=cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP))
            .putString("payload",Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit())
    }
    fun read():SavedConnection? = runCatching {
        val payload=prefs.getString("payload",null) ?: return null
        val iv=Base64.decode(prefs.getString("iv",null),Base64.NO_WRAP)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv))}
        val json=JSONObject(cipher.doFinal(Base64.decode(payload,Base64.NO_WRAP)).toString(Charsets.UTF_8))
        SavedConnection(json.getString("address"),json.getString("user"),json.getString("password"))
    }.getOrNull()
    fun clear(){prefs.edit().clear().commit()}
}
