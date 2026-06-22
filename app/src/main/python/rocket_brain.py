import yfinance as yf
import pandas as pd
import numpy as np
import requests
import time
from datetime import datetime

# --- جسر الأندرويد لعرض الصفقات على الشاشة ---
android_ui = None
def connect_ui(ui_bridge):
    global android_ui
    android_ui = ui_bridge

# --- إعدادات التليجرام ---
BOT_TOKEN = "8704425941:AAHIsmktU4A4VsY1iHz1MwP9tWbS_oDk1oo"  
CHAT_ID = "7259620384"            

def send_telegram_alert(message):
    # إرسال للتليجرام
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
    payload = {"chat_id": CHAT_ID, "text": message, "parse_mode": "Markdown"}
    try: requests.post(url, json=payload, timeout=5)
    except: pass
    
    # إرسال نفس الصفقة لشاشة التطبيق مباشرة (الجسر)
    if android_ui is not None:
        android_ui.onNewSignal(message)

# دالة حساب مؤشر STC (نفس كودك)
def calculate_stc(df, len_fast=23, len_slow=50, cycle=10):
    fast_ema = df['Close'].ewm(span=len_fast, adjust=False).mean()
    slow_ema = df['Close'].ewm(span=len_slow, adjust=False).mean()
    macd = fast_ema - slow_ema
    lowest_macd = macd.rolling(window=cycle).min()
    highest_macd = macd.rolling(window=cycle).max()
    stoch_macd = 100 * ((macd - lowest_macd) / (highest_macd - lowest_macd + 1e-10))
    stc = stoch_macd.ewm(span=cycle/2, adjust=False).mean()
    return stc

# دالة حساب مؤشر UT Bot Alerts (نفس كودك)
def calculate_ut_bot(df, sensitivity=2, target_len=11):
    tr1 = pd.DataFrame(df['High'] - df['Low'])
    tr2 = pd.DataFrame(abs(df['High'] - df['Close'].shift(1)))
    tr3 = pd.DataFrame(abs(df['Low'] - df['Close'].shift(1)))
    tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
    atr = tr.rolling(window=target_len).mean()
    trailing_stop = df['Close'].copy()
    n_loss = sensitivity * atr
    for i in range(1, len(df)):
        if df['Close'].values[i] > trailing_stop.values[i-1] and df['Close'].values[i-1] > trailing_stop.values[i-1]:
            trailing_stop.values[i] = max(trailing_stop.values[i-1], df['Close'].values[i] - n_loss.values[i])
        elif df['Close'].values[i] < trailing_stop.values[i-1] and df['Close'].values[i-1] < trailing_stop.values[i-1]:
            trailing_stop.values[i] = min(trailing_stop.values[i-1], df['Close'].values[i] + n_loss.values[i])
        else:
            trailing_stop.values[i] = df['Close'].values[i] - n_loss.values[i] if df['Close'].values[i] > trailing_stop.values[i-1] else df['Close'].values[i] + n_loss.values[i]
    ut_signal = np.where(df['Close'] > trailing_stop, 1, -1)
    return ut_signal

# --- الدالة الرئيسية (وضعنا كودك هنا حتى الأندرويد يشغله بدون كراش) ---
def start_bot():
    send_telegram_alert("🟢 *تم تشغيل بوت القناص الحي بنجاح!* جاري رصد الشارت بالثانية..")
    last_processed_timestamp = None

    while True:
        try:
            df = yf.download("BTC-USD", period="1d", interval="1m", auto_adjust=True, progress=False)
            
            if not df.empty:
                if isinstance(df.columns, pd.MultiIndex):
                    df.columns = df.columns.get_level_values(0)
                    
                df = df.dropna()
                
                df['Trend_Line'] = df['Close'].rolling(window=7).mean()
                df['STC'] = calculate_stc(df)
                df['UT_Signal'] = calculate_ut_bot(df, sensitivity=2, target_len=11)
                df = df.dropna()
                
                latest_candle = df.iloc[-2]
                current_timestamp = str(df.index[-2])
                
                if current_timestamp != last_processed_timestamp:
                    c_close = float(latest_candle['Close'])
                    c_open = float(latest_candle['Open'])
                    c_trend = float(latest_candle['Trend_Line'])
                    c_stc = float(latest_candle['STC'])
                    c_ut = float(latest_candle['UT_Signal'])
                    
                    tp_target = 40.0
                    sl_target = 10.0
                    
                    if (c_close > c_trend) and (c_ut == 1) and (c_stc >= 65) and (c_close > c_open):
                        msg = f"🚨 *إشارة شراء حية (BUY) 📈*\n• الرمز: BTCUSDT\n• الدخول: {c_close:.2f} $\n• الهدف: {c_close + tp_target:.2f} $\n• الاستوب: {c_close - sl_target:.2f} $\n⏰ {datetime.now().strftime('%H:%M:%S')}"
                        send_telegram_alert(msg)
                        last_processed_timestamp = current_timestamp
                    
                    elif (c_close < c_trend) and (c_ut == -1) and (c_stc <= 35) and (c_close < c_open):
                        msg = f"🚨 *إشارة بيع حية (SELL) 📉*\n• الرمز: BTCUSDT\n• الدخول: {c_close:.2f} $\n• الهدف: {c_close - tp_target:.2f} $\n• الاستوب: {c_close + sl_target:.2f} $\n⏰ {datetime.now().strftime('%H:%M:%S')}"
                        send_telegram_alert(msg)
                        last_processed_timestamp = current_timestamp

            time.sleep(10)
            
        except Exception as error:
            if android_ui is not None:
                android_ui.onNewSignal(f"⚠️ خطأ عابر: {error}")
            time.sleep(5)
