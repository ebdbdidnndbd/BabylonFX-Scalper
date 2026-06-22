package com.babylonfx.scaler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnStop;
    private TextView txtTerminal;
    private ScrollView scrollView;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private Python py;
    private PyObject pyModule;

    private Runnable botRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            
            new Thread(() -> {
                try {
                    // الاتصال بالبايثون مع صائد الأخطاء
                    PyObject result = pyModule.callAttr("analyze_market");
                    String res = result.toString();
                    String[] parts = res.split("\\|", 2);
                    String type = parts[0];
                    String message = parts.length > 1 ? parts[1] : "";

                    runOnUiThread(() -> {
                        if (type.equals("BUY") || type.equals("SELL")) {
                            txtTerminal.append("\n\n====================\n" + message);
                            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                            
                            String alertText = type.equals("BUY") ? "📈 صفقة شراء جديدة (BTC)!" : "📉 صفقة بيع جديدة (BTC)!";
                            Toast.makeText(MainActivity.this, alertText, Toast.LENGTH_LONG).show();
                            
                        } else if (type.equals("ERROR")) {
                            // إذا صار خطأ بالبايثون ينطبع هنا بدل الكراش
                            txtTerminal.append("\n⚠️ " + message);
                            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                        }
                    });
                } catch (Exception e) {
                    // درع حماية الأندرويد الداخلي
                    runOnUiThread(() -> {
                        txtTerminal.append("\n❌ تم منع كراش: " + e.getMessage());
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    });
                }
                
                // نبضة كل 10 ثواني (لا تغيرها حتى ما يختنق السيرفر)
                if (isRunning) {
                    handler.postDelayed(botRunnable, 10000);
                }
            }).start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        txtTerminal = findViewById(R.id.txtTerminal);
        scrollView = findViewById(R.id.scrollView);

        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this));
            }
            py = Python.getInstance();
            pyModule = py.getModule("rocket_brain");
            txtTerminal.setText("✅ النظام جاهز ومستقر. اضغط تشغيل.");
        } catch (Exception e) {
            txtTerminal.setText("❌ مشكلة في قراءة البايثون: " + e.getMessage());
        }

        btnStart.setOnClickListener(v -> {
            if (isRunning) return;
            isRunning = true;
            txtTerminal.append("\n\n>>> 🟢 تم تشغيل القناص! رادار بابل يعمل الآن...");
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            handler.post(botRunnable);
        });

        btnStop.setOnClickListener(v -> {
            if (!isRunning) return;
            isRunning = false;
            handler.removeCallbacks(botRunnable);
            txtTerminal.append("\n\n>>> 🔴 تم إيقاف البوت بنجاح.");
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }
}
