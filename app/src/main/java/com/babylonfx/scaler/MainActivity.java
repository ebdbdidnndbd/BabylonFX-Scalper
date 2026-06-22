package com.babylonfx.scaler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnStop, btnSubscribe;
    private TextView txtStatus, txtLicense;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSubscribe = findViewById(R.id.btnSubscribe);
        txtStatus = findViewById(R.id.txtStatus);
        txtLicense = findViewById(R.id.txtLicense);

        prefs = getSharedPreferences("BabylonPrefs", MODE_PRIVATE);

        // 1. تشغيل الفحص والتحديث الهوائي فوراً عند فتح التطبيق
        UpdateManager.checkAndDownloadUpdate(this);

        // 2. التحقق من حالة تفعيل رخصة الاشتراك داخل الجهاز
        boolean isLicensed = prefs.getBoolean("is_active", false);
        if (isLicensed) {
            txtLicense.setText("الترخيص: مفعّل لغاية نهاية الشهر الحالي! 🟢");
        }

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!prefs.getBoolean("is_active", false)) {
                    Toast.makeText(MainActivity.this, "🚫 يرجى تفعيل الباقة أولاً لتتمكن من تشغيل البوت!", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent serviceIntent = new Intent(MainActivity.this, SignalService.class);
                startService(serviceIntent);
                txtStatus.setText("حالة البوت: يعمل بالخلفية ويرصد الآن 🟢");
                Toast.makeText(MainActivity.this, "🟢 تم تشغيل الخدمة بنجاح!", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(MainActivity.this, SignalService.class);
                stopService(serviceIntent);
                txtStatus.setText("حالة البوت: متوقف حالياً 🔴");
                Toast.makeText(MainActivity.this, "🔴 تم إيقاف المراقبة مؤقتاً.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // محاكاة التفعيل الذاتي الآمن (تعدل حسب وسيلة الدفع أو جوجل بلاي)
                prefs.edit().putBoolean("is_active", true).apply();
                txtLicense.setText("الترخيص: مفعّل لغاية نهاية الشهر الحالي! 🟢");
                Toast.makeText(MainActivity.this, "🪙 تم تفعيل الباقة بنجاح! يمكنك التشغيل الآن.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
