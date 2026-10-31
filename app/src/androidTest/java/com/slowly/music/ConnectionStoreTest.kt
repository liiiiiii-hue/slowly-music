package com.slowly.music

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectionStoreTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun encryptedConnectionSurvivesReopeningWithoutPlaintext(){
        val name="connection-test-${java.util.UUID.randomUUID()}"
        val value=SavedConnection("https://example.com/dav/","test-user","synthetic-secret-for-test")
        val store=ConnectionStore(context,name)
        try{
            store.save(value)
            assertEquals(value,ConnectionStore(context,name).read())
            val persisted=context.getSharedPreferences(name,0).all.toString()
            assertFalse(persisted.contains(value.password))
            assertFalse(persisted.contains(value.user))
            store.clear();assertNull(ConnectionStore(context,name).read())
        }finally{store.clear();java.security.KeyStore.getInstance("AndroidKeyStore").apply{load(null);deleteEntry("slowly-$name-v1")}}
    }
    @Test fun damagedConnectionFailsClosed(){
        val name="connection-test-${java.util.UUID.randomUUID()}"
        val store=ConnectionStore(context,name)
        try{
            store.save(SavedConnection("https://example.com/dav/","test","test"))
            context.getSharedPreferences(name,0).edit().putString("payload","broken").commit()
            assertNull(ConnectionStore(context,name).read())
        }finally{store.clear();java.security.KeyStore.getInstance("AndroidKeyStore").apply{load(null);deleteEntry("slowly-$name-v1")}}
    }
}
