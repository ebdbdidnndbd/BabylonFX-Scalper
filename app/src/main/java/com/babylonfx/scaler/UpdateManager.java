package com.babylonfx.scaler;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {

    // استبدل الرابط برابط الـ Raw لملف البايثون بداخل مستودع جيثب مالتك بالظبط
    private static final String GITHUB_URL = "https://raw.githubusercontent.com/ebbdidndnbbd/BabylonFX-Scalper/main/app/src/main/python/rocket_brain.py";

    public static void checkAndDownloadUpdate(Context context) {
        new Thread(() -> {
            try {
                URL url = new URL(GITHUB_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    File outputDir = context.getFilesDir(); 
                    File pyFile = new File(outputDir, "rocket_brain.py");

                    FileOutputStream fos = new FileOutputStream(pyFile);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }

                    fos.close();
                    is.close();
                    System.out.println("✅ Babylon FX: تم تحديث ملف الاستراتيجية هوائياً!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

