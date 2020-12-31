package net.babys_care.app.utils

import android.util.Base64
import net.babys_care.app.AppManager
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class Encrypt {

    fun encrypt(dataToEncrypt: String, pass: String): HashMap<String, ByteArray>? {
        try {
            val map = HashMap<String, ByteArray>()
            val byteData = dataToEncrypt.toByteArray(Charsets.UTF_8)

            val random = SecureRandom()
            val salt = ByteArray(256)
            random.nextBytes(salt)

            val pbeKeySpec = PBEKeySpec(pass.toCharArray(), salt,1324,256)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val keyBytes = secretKeyFactory.generateSecret(pbeKeySpec).encoded
            val keySpec = SecretKeySpec(keyBytes, "AES")

            val ivRandom = SecureRandom()
            val iv = ByteArray(16)
            ivRandom.nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

            val encrypted = cipher.doFinal(byteData)

            map["salt"] = salt
            map["iv"] = iv
            map["encrypted"] = encrypted

            return map
        } catch (ex: java.lang.Exception) {
            debugLogInfo("Encryption error: $ex")
            return null
        }
    }

    fun decrypt(map: HashMap<String, ByteArray>, pass: String): String? {
        try {
            val iv = map["iv"]
            val salt = map["salt"]
            val encrypted = map["encrypted"]
            //regenerate key from password
            val pbKeySpec = PBEKeySpec(pass.toCharArray(), salt, 1324, 256)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded
            val keySpec = SecretKeySpec(keyBytes, "AES")

            //Decrypt
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }catch (ex: java.lang.Exception) {
            debugLogInfo("Exception in decrypting: $ex")
            return null
        }
    }

    fun getEncryptedPassword(dataToEncrypt: String, key: String): String {
        val encryptedMap = Encrypt().encrypt(dataToEncrypt, key) ?: return dataToEncrypt

        val editor = AppManager.sharedPreference.edit()
        editor.putString("p_iv", Base64.encodeToString(encryptedMap["iv"], Base64.NO_WRAP))
        editor.putString("p_salt", Base64.encodeToString(encryptedMap["salt"], Base64.NO_WRAP))
        editor.apply()

        return Base64.encodeToString(encryptedMap["encrypted"], Base64.NO_WRAP)
    }

    fun getDecryptedPassword(dataToDecrypt: String, key: String): String {
        val hashMap = java.util.HashMap<String, ByteArray>()
        val sharedPreferences = AppManager.sharedPreference
        val iv = sharedPreferences.getString("p_iv", "")
        val salt = sharedPreferences.getString("p_salt", "")

        if (iv.isNullOrEmpty() || salt.isNullOrEmpty()) {
            return dataToDecrypt
        }

        hashMap["iv"] = Base64.decode(iv, Base64.NO_WRAP)
        hashMap["salt"] = Base64.decode(salt, Base64.NO_WRAP)
        hashMap["encrypted"] = Base64.decode(dataToDecrypt, Base64.NO_WRAP)

        return Encrypt().decrypt(hashMap, key) ?: dataToDecrypt
    }
}
