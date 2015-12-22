package tools.fileencrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;

/**
 * Hello world!
 *
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final String ALGORITHM_KEYSPEC = "AES";
    private static final String ALGORITHM_CIPHER = "AES/CBC/PKCS5Padding";
    private static final String ENCRYPT_IV = "abcdefghijklmnop";
    private static final String ENCRYPTED_SUFFIX = ".encrypted";
    private static final int KEY_LENGTH = 128;  // 128bit

    public static void main( String[] args ) {
        App app = new App();
        try {
            app.execute(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void execute(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
        if (args.length==0) {
            createNextPasswordFile();
            return ;
        }
        for (String arg: args) {
            Path source = Paths.get(arg);
            Path destination = getDestination(source);
            flush(source, destination, needEncryptMode(source));
        }
    }

    public Path getDestination(Path path) {
        if (needEncryptMode(path)==Cipher.DECRYPT_MODE) {
            // 複合化が必要なら、複合化ファイルとして末尾のENCRYPTED_SUFFIXを取り除いたファイル名を返す
            return Paths.get(path.toString().substring(0, path.toString().lastIndexOf(ENCRYPTED_SUFFIX)));
        }
        // 暗号化が必要なら、暗号化ファイルとして末尾にENCRYPTED_SUFFIXを付けたファイル名を返す
        return Paths.get(path.toString()+ENCRYPTED_SUFFIX);
    }

    public int needEncryptMode(Path path) {
        if (path.toString().lastIndexOf(ENCRYPTED_SUFFIX)!=-1) {
            // ファイル末尾がENCRYPTED_SUFFIXなら、暗号化済みなのでDECRYPT_MODEを返す
            return Cipher.DECRYPT_MODE;
        }
        // ファイル末尾がENCRYPTED_SUFFIXではないなら、暗号化していないのでENCRYPT_MODEを返す
        return Cipher.ENCRYPT_MODE;
    }

    private Path getPasswordDirectory() throws IOException {
        // ホームディレクトリの「.fileencrypt」ディレクトリを確認し、なければ作成
        Path passwordDirectory = Paths.get(System.getProperty("user.home"), ".fileencrypt");
        if (!Files.isDirectory(passwordDirectory)) {
            Files.createDirectory(passwordDirectory);
        }
        return passwordDirectory;
    }
    public Path getCurrentPasswordFile() throws IOException {
        Path passwordDirectory = getPasswordDirectory();

        // 空ならnullを返す
        if (Files.list(passwordDirectory).count()==0) {
            return null;
        }

        // 最後の1つを取得
        Path[] passwordFiles =
            Files.list(passwordDirectory)               // passwordDirectory内のファイルをstreamにする
                    .sorted(Comparator.reverseOrder())  // 逆順でソートする
                    .limit(1)                           // 上限1つにする（逆順にしているから、最後の1つ）
                    .toArray(Path[]::new);               // 配列に変換する

        logger.info("use password file -> "+passwordFiles[0].getFileName());
        return passwordFiles[0];
    }

    public Path createNextPasswordFile() throws IOException {
        Path passwordDirectory = getPasswordDirectory();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String today = sdf.format(new Date());

        // ファイルの解決
        Path passwordFile = null;
        for (int loop=0; loop<999; loop++) {
            Path p = Paths.get(passwordDirectory.toString(), String.format("password_%s%03d.txt", today, loop+1));

            // ファイルがなければそれを採用
            if (Files.notExists(p)) {
                passwordFile = p;
                break;
            }
        }
        if (passwordFile==null) {
            return null;
        }
        logger.info("create password file -> "+passwordFile.getFileName());

        // パスワードの生成
        byte[] passwordBytes = new byte[KEY_LENGTH/8];
        Random random = new Random();
        random.nextBytes(passwordBytes);

        // パスワードファイルの書き込み
        try (BufferedWriter writer = Files.newBufferedWriter(passwordFile)) {
            String password = DatatypeConverter.printHexBinary(passwordBytes);
            writer.write(password, 0, password.length());
        }

        return passwordFile;
    }

    private Key makeKey() throws IOException {
        byte[] key = readCurrentPasswordFile();
        return new SecretKeySpec(key, ALGORITHM_KEYSPEC);
    }

    private byte[] readCurrentPasswordFile() throws IOException {
        Path path = getCurrentPasswordFile();
        return DatatypeConverter.parseHexBinary(Files.readAllLines(path).get(0));
    }

    private void flush(Path source, Path destination, int mode) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {

       Cipher cipher = Cipher.getInstance(ALGORITHM_CIPHER);
        IvParameterSpec iv = new IvParameterSpec(ENCRYPT_IV.getBytes("UTF-8"));
        cipher.init(mode, makeKey(), iv);

        try(
                BufferedInputStream in = new BufferedInputStream(new CipherInputStream(new FileInputStream(source.toFile()), cipher));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destination.toFile()))
        ) {
            int data;
            while((data=in.read())!=-1) {
                out.write(data);
            }
        }
    }
}
