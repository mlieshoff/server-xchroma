package server;

import com.psiclops.cryptix.CryptChain;
import com.psiclops.cryptix.aes.AesCryptProcessorWithFixedKey;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class FileCipher {

    public static void main(String[] args) throws Throwable {
        System.out.println("base dir: " + new File("").getAbsolutePath());
        if (args == null || args.length != 4) {
            showHelp();
        }
        File keyFile = new File(args[0]);
        byte[] password = Files.readAllBytes(keyFile.toPath());
        File inFile = new File(args[1]);
        File outFile = new File(args[3]);
        FileInputStream fileInputStream = new FileInputStream(inFile);
        FileOutputStream fileOutputStream = new FileOutputStream(outFile);
        CryptChain cryptChain = CryptChain.chain(List.of(new AesCryptProcessorWithFixedKey(password)));
        if ("code".equals(args[2])) {
            cryptChain.encrypt(fileInputStream, fileOutputStream);
        } else if ("decode".equals(args[2])) {
            cryptChain.decrypt(fileInputStream, fileOutputStream);
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

}
