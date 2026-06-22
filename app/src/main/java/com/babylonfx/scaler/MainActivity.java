package com.babylonfx.scaler;

import android.os.Bundle;
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
    private Thread botThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        txtTerminal = findViewById(R.id.txtTerminal);
        scrollView = findViewById(R.id.scrollView);

        // تشغيل محرك البايثون
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        btnStart.setOnClickListener(v -> {
            if (botThread != null && botThread.isAlive()) {
                Toast.makeText(MainActivity.this, "البوت شغال بالفعل!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            txtTerminal.append("\n\n>>> جاري تشغيل محرك القناص...");
            
            botThread = new Thread(() -> {
                Python py = Python.getInstance();
                PyObject pyModule = py.getModule("rocket_brain");
                
                // ربط شاشة التطبيق بالبايثون لاستلام الصفقات
                pyModule.callAttr("connect_ui", this);
                
                // تشغيل كودك بالكامل
                pyModule.callAttr("start_bot");
            });
            botThread.start();
        });

        btnStop.setOnClickListener(v -> {
            if (botThread != null) {
                botThread.interrupt();
                botThread = null;
                txtTerminal.append("\n\n>>> 🔴 تم إيقاف البوت بنجاح.");
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    // هاي الدالة يستدعيها كود البايثون مالتك حتى يطبع الصفقات بالشاشة
    public void onNewSignal(String message) {
        runOnUiThread(() -> {
            txtTerminal.append("\n\n====================\n" + message);
            // النزول التلقائي لأسفل الشاشة لرؤية أحدث صفقة
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }
}
