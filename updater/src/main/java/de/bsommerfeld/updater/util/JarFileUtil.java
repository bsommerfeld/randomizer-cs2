package de.bsommerfeld.updater.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class JarFileUtil {

  private JarFileUtil() {}

  public static File getJarFile() throws IOException {
    ProtectionDomain protectionDomain = JarFileUtil.class.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    if (codeSource == null) {
      throw new IOException("CodeSource ist null. Läuft die Anwendung aus einer JAR-Datei?");
    }
    URL location = codeSource.getLocation();

    String decodedPath;
    try {
      decodedPath = URLDecoder.decode(location.getPath(), StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IOException("UTF-8 nicht unterstützt.", e);
    }

    return new File(decodedPath);
  }
}
