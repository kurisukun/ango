package Crypto;

import Zip.ZipMaker;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import lombok.Getter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/*
inspiré de https://www.baeldung.com/java-aes-encryption-decryption
 */
public class AesCBC {

  private static final String algorithm = "AES/CBC/PKCS5Padding";
  private Cipher cipher;
  final static String TMP_FILENAME_ENCRYPTION = "TMP_FILENAME_ENCRYPTION.zip";
  final static String TMP_FILENAME_DECRYPTION = "TMP_FILENAME_DECRYPTION";


  @Getter
  private final IvParameterSpec iv;
  private final SecretKey key;
  private final Logger logger = LogManager.getLogger(AesCBC.class);

  public AesCBC(SecretKey key) {
    this.key = key;
    this.iv = new IvParameterSpec(getIvBytes());
    try {
      cipher = Cipher.getInstance(algorithm);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      logger.error(e.getMessage());
    }
  }

  public AesCBC(SecretKey key, IvParameterSpec iv) {
    this.key = key;
    this.iv = iv;
    try {
      cipher = Cipher.getInstance(algorithm);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      logger.error(e.getMessage());
    }
  }

  /**
   * encrypt srcPath and write it to dstPath
   * @param srcPath file to encrypt
   * @param dstPath encrypted file
   * @throws InvalidAlgorithmParameterException
   * @throws InvalidKeyException
   * @throws FileNotFoundException
   */
  public void encrypt(String srcPath, String dstPath)
      throws InvalidAlgorithmParameterException, InvalidKeyException, FileNotFoundException {
    logger.trace("encrypt");
    cipher.init(Cipher.ENCRYPT_MODE, key, iv);
    ZipMaker z = new ZipMaker();

    try {
      z.zip(srcPath, TMP_FILENAME_ENCRYPTION);
    } catch (IOException e) {
      logger.error(e);
    }
    processData(TMP_FILENAME_ENCRYPTION, dstPath);
    logger.info("Encryption correctly done");
    if (!new File(TMP_FILENAME_ENCRYPTION).delete()){
      logger.warn("The file could not be deleted correctly");
    }
    logger.info("Deletion of temporary file correctly done");
  }

  /**
   * decrypt a file
   * @param srcPath file to decrypt
   * @param dstPath decrypted file
   * @throws InvalidAlgorithmParameterException
   * @throws InvalidKeyException
   */
  public void decrypt(String srcPath, String dstPath)
      throws InvalidAlgorithmParameterException, InvalidKeyException {
    logger.trace("decrypt");

    cipher.init(Cipher.DECRYPT_MODE, key, iv);
    processData(srcPath, TMP_FILENAME_DECRYPTION);
    logger.info("Decryption correctly done");
    ZipMaker z = new ZipMaker();
    try {
      z.unzip(TMP_FILENAME_DECRYPTION, dstPath);
    } catch (IOException e) {
      logger.error(e);
    }
    if (!new File(TMP_FILENAME_DECRYPTION).delete()){
      logger.warn("The file could not be deleted correctly");
    }
    logger.info("Deletion of temporary file correctly done");
  }

  /**
   * read from srcPath, make an operation, and write it to dstPath
   * @param srcPath read srcPath
   * @param dstPath write in dstPath
   */
  private void processData(String srcPath, String dstPath) {
    logger.trace("From : " + srcPath + ", To : " + dstPath);
    try (FileInputStream inputStream = new FileInputStream(srcPath);
        FileOutputStream outputStream = new FileOutputStream(dstPath)) {

      int bytesRead;
      byte[] buffer = new byte[16];
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        byte[] output = cipher.update(buffer, 0, bytesRead);
        if (output != null) {
          outputStream.write(output);
        }
      }

      // write last 16 bits
      byte[] outputBytes = cipher.doFinal();
      if (outputBytes != null) {
        outputStream.write(outputBytes);
      }
    } catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
      logger.error(e.getMessage());
    }
  }

  /**
   * generate random IV
   * @return IV for aes-cbc
   */
  private byte[] getIvBytes() {
    logger.trace("getIvBytes");
    SecureRandom randomSecureRandom = new SecureRandom();
    byte[] iv = new byte[16];
    randomSecureRandom.nextBytes(iv);
    return iv;
  }
}
