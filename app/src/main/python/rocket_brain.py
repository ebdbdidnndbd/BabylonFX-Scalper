import yfinance as yf
import pandas as pd
import numpy as np
from datetime import datetime

last_processed_timestamp = None

def calculate_stc(df, len_fast=23, len_slow=50, cycle=10):
    fast_ema = df['Close'].ewm(span=len_fast, adjust=False).mean()
    slow_ema = df['Close'].ewm(span=len_slow, adjust=False).mean()
    macd = fast_ema - slow_ema
    lowest_macd = macd.rolling(window=cycle).min()
    highest_macd = macd.rolling(window=cycle).max()
    stoch_macd = 100 * ((macd - lowest_macd) / (highest_macd - lowest_macd + 1e-10))
    stc = stoch_macd.ewm(span=cycle/2, adjust=False).mean()
    return stc

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

def analyze_market():
    global last_processed_timestamp
    try:
        # الضربة القاضية للكراش: threads=False
        df = yf.download("BTC-USD", period="1d", interval="1m", auto_adjust=True, progress=False, threads=False)
        
        if df.empty:
            return "WAIT|جاري انتظار تحميل الشارت..."
            
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
                msg = f"🟢 إشارة شراء (BUY)\nالرمز: BTCUSDT\nالدخول: {c_close:.2f} $\nالهدف: {c_close + tp_target:.2f} $\nالاستوب: {c_close - sl_target:.2f} $\nالوقت: {datetime.now().strftime('%H:%M:%S')}"
                last_processed_timestamp = current_timestamp
                return f"BUY|{msg}"
            
            elif (c_close < c_trend) and (c_ut == -1) and (c_stc <= 35) and (c_close < c_open):
                msg = f"🔴 إشارة بيع (SELL)\nالرمز: BTCUSDT\nالدخول: {c_close:.2f} $\nالهدف: {c_close - tp_target:.2f} $\nالاستوب: {c_close + sl_target:.2f} $\nالوقت: {datetime.now().strftime('%H:%M:%S')}"
                last_processed_timestamp = current_timestamp
                return f"SELL|{msg}"
                
        return "WAIT|جاري مراقبة السوق بدقة..."
        
    except Exception as e:
        return f"ERROR|خطأ أثناء الفحص: {str(e)}"
