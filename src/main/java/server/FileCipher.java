package server;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.*;
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
        File inFile = new File(args[1]);
        File outFile = new File(args[3]);
        FileInputStream fileInputStream = new FileInputStream(inFile);
        FileOutputStream fileOutputStream = new FileOutputStream(outFile);
        if ("code".equals(args[2])) {
            encrypt(fileInputStream, fileOutputStream, password);
        } else if ("decode".equals(args[2])) {
            decrypt(fileInputStream, fileOutputStream, password);
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

    private static void encrypt(InputStream inputStream, OutputStream outputStream, String password) throws Exception {
        byte[] salt = getRandomNonce(SALT_LENGTH_BYTE);
        byte[] iv = getRandomNonce(IV_LENGTH_BYTE);
        SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        OutputStream result = new BufferedOutputStream(outputStream);
        result.write(ByteBuffer.allocate(iv.length + salt.length)
                .put(iv)
                .put(salt)
                .array());
        CipherOutputStream cipherOutputStream = new CipherOutputStream(result, cipher);
        copy(inputStream, cipherOutputStream);
        result.close();
    }

    private static void decrypt(InputStream inputStream, OutputStream outputStream, String password) throws Exception {
        InputStream input = new BufferedInputStream(inputStream);
        byte[] iv = input.readNBytes(IV_LENGTH_BYTE);
        byte[] salt = input.readNBytes(SALT_LENGTH_BYTE);
        SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        CipherInputStream cipherInputStream = new CipherInputStream(input, cipher);
        copy(cipherInputStream, outputStream);
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
