package com.aefyr.pseudoapksigner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PseudoApkSigner {
    private static final String HASHING_ALGORITHM = "SHA1";

    private RSAPrivateKey mPrivateKey;
    private File mTemplateFile;

    private String mSignerName = "CERT";

    /**
     * Creates a new PseudoApkSigner with given template and private key
     *
     * @param template   .RSA file from an APK signed with an actual apksigner with the same private key with last 256 bytes removed
     * @param privateKey .pk8 private key file
     */
    public PseudoApkSigner(File template, File privateKey) throws Exception {
        mTemplateFile = template;
        mPrivateKey = readPrivateKey(privateKey);
    }

    public void sign(File apkFile, File output) throws Exception {
        ManifestBuilder manifest = new ManifestBuilder();
        SignatureFileGenerator signature = new SignatureFileGenerator(manifest, HASHING_ALGORITHM);

        ZipFile apkZipFile = new ZipFile(apkFile);

        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
        Enumeration<? extends ZipEntry> zipEntries = apkZipFile.entries();
        MessageDigest messageDigest = MessageDigest.getInstance(HASHING_ALGORITHM);
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();

            if (zipEntry.isDirectory() || zipEntry.getName().toLowerCase().startsWith("meta-inf"))
                continue;

            messageDigest.reset();
            DigestInputStream entryInputStream = new DigestInputStream(apkZipFile.getInputStream(zipEntry), messageDigest);

            zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
            Utils.copyStream(entryInputStream, zipOutputStream);
            zipOutputStream.closeEntry();
            entryInputStream.close();

            ManifestBuilder.ManifestEntry manifestEntry = new ManifestBuilder.ManifestEntry();
            manifestEntry.setAttribute("Name", zipEntry.getName());
            manifestEntry.setAttribute(HASHING_ALGORITHM + "-Digest", Utils.base64Encode(messageDigest.digest()));
            manifest.addEntry(manifestEntry);
        }

        zipOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        zipOutputStream.write(manifest.build().getBytes(Constants.UTF8));
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry(String.format("META-INF/%s.SF", mSignerName)));
        zipOutputStream.write(signature.generate().getBytes(Constants.UTF8));
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry(String.format("META-INF/%s.RSA", mSignerName)));
        zipOutputStream.write(readFile(mTemplateFile));
        zipOutputStream.write(sign(mPrivateKey, signature.generate().getBytes(Constants.UTF8)));
        zipOutputStream.closeEntry();

        apkZipFile.close();
        zipOutputStream.close();
    }


    /**
     * Sets name of the .SF and .RSA file in META-INF
     *
     * @param signerName desired .SF and .RSA files name
     */
    public void setSignerName(String signerName) {
        mSignerName = signerName;
    }

    private byte[] sign(PrivateKey privateKey, byte[] message) throws Exception {
        Signature sign = Signature.getInstance(HASHING_ALGORITHM + "withRSA");
        sign.initSign(privateKey);
        sign.update(message);
        return sign.sign();
    }

    private RSAPrivateKey readPrivateKey(File file) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(readFile(file));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private byte[] readFile(File file) throws IOException {
        byte[] fileBytes = new byte[(int) file.length()];

        FileInputStream inputStream = new FileInputStream(file);
        inputStream.read(fileBytes);
        inputStream.close();

        return fileBytes;
    }
}
