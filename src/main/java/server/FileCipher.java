package server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class FileCipher {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";

    private static final int TAG_LENGTH_BIT = 128; // must be one of {128, 120, 112, 104, 96}
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;

    public static void main(String[] args) throws Throwable {
        System.out.println("base dir: " + new File("").getAbsolutePath());
        if (args == null || args.length != 4) {
            showHelp();
        }
        File keyFile = new File(args[0]);
        String password = new String(Files.readAllBytes(keyFile.toPath()));
        if ("code".equals(args[2])) {
            encryptFile(args[1], args[3], password);
        } else if ("decode".equals(args[2])) {
            decryptFile(args[1], args[3], password);
        } else {
            showHelp();
        }
        System.exit(0);
    }

    private static void showHelp() {
        System.err.println("Usage:");
        System.err.println("    FileCipher [key] [filename] [code|decode] [outfilename]");
        System.exit(1);
    }

    private static byte[] encrypt(byte[] pText, String password) throws Exception {

        // 16 bytes salt
        byte[] salt = getRandomNonce(SALT_LENGTH_BYTE);

        // GCM recommended 12 bytes iv?
        byte[] iv = getRandomNonce(IV_LENGTH_BYTE);

        // secret key from password
        SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

        // ASE-GCM needs GCMParameterSpec
        cipher.init(Cipher.ENCRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] cipherText = cipher.doFinal(pText);

        // prefix IV and Salt to cipher text
        return ByteBuffer.allocate(iv.length + salt.length + cipherText.length)
                .put(iv)
                .put(salt)
                .put(cipherText)
                .array();
    }

    // we need the same password, salt and iv to decrypt it
    private static byte[] decrypt(byte[] cText, String password) throws Exception {

        // get back the iv and salt that was prefixed in the cipher text
        ByteBuffer bb = ByteBuffer.wrap(cText);

        byte[] iv = new byte[12];
        bb.get(iv);

        byte[] salt = new byte[16];
        bb.get(salt);

        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);

        // get back the aes key from the same password and salt
        SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

        cipher.init(Cipher.DECRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        return cipher.doFinal(cipherText);
    }

    private static void encryptFile(String fromFile, String toFile, String password) throws Exception {
        byte[] fileContent = Files.readAllBytes(Paths.get(fromFile));
        byte[] encryptedText = encrypt(fileContent, password);
        Path path = Paths.get(toFile);
        Files.write(path, encryptedText);
    }

    private static void decryptFile(String fromEncryptedFile, String toDecryptedFile, String password) throws Exception {
        byte[] fileContent = Files.readAllBytes(Paths.get(fromEncryptedFile));
        byte[] decrypted = decrypt(fileContent, password);
        Path path = Paths.get(toDecryptedFile);
        Files.write(path, decrypted);
    }

    private static byte[] getRandomNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    private static SecretKey getAESKeyFromPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = new byte[64];
        int size;
        while ((size = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, size);
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

}
