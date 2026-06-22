import yfinance as yf
import pandas as pd
import numpy as np

def calculate_stc(df):
    fast_ema = df['Close'].ewm(span=23, adjust=False).mean()
    slow_ema = df['Close'].ewm(span=50, adjust=False).mean()
    macd = fast_ema - slow_ema
    lowest_macd = macd.rolling(10).min()
    highest_macd = macd.rolling(10).max()
    stoch_macd = 100 * ((macd - lowest_macd) / (highest_macd - lowest_macd + 1e-10))
    return stoch_macd.ewm(span=5, adjust=False).mean()

def calculate_ut_bot(df):
    tr = pd.concat([df['High']-df['Low'], abs(df['High']-df['Close'].shift(1)), abs(df['Low']-df['Close'].shift(1))], axis=1).max(axis=1)
    atr = tr.rolling(window=11).mean()
    ut_signal = np.where(df['Close'] > df['Close'].rolling(7).mean(), 1, -1)
    return ut_signal

def check_gold_signal():
    try:
        # 1. سحب داتا رينج الساعة السابقة لـ CRT المعكوس للذهب
        df_h1 = yf.download("GC=F", period="2d", interval="1h", auto_adjust=True, progress=False)
        if isinstance(df_h1.columns, pd.MultiIndex): df_h1.columns = df_h1.columns.get_level_values(0)
        range_high = float(df_h1.dropna().iloc[-2]['High'])
        range_low = float(df_h1.dropna().iloc[-2]['Low'])

        # 2. سحب داتا الدقيقة الحية للسكالب السريع
        df = yf.download("GC=F", period="1d", interval="1m", auto_adjust=True, progress=False)
        if isinstance(df.columns, pd.MultiIndex): df.columns = df.columns.get_level_values(0)
        df = df.dropna()
        
        df['Trend_Line'] = df['Close'].rolling(window=7).mean()
        df['STC'] = calculate_stc(df)
        df['UT_Signal'] = calculate_ut_bot(df)
        
        latest = df.dropna().iloc[-2]
        price = float(latest['Close'])
        ut = float(latest['UT_Signal'])
        stc = float(latest['STC'])
        trend = float(latest['Trend_Line'])

        # فحص الشروط الميكانيكية الحوت للخطف السريع
        if (price > range_high) and price > trend and ut == 1 and stc >= 65:
            return f"BUY|{price:.2f}"
        elif (price < range_low) and price < trend and ut == -1 and stc <= 35:
            return f"SELL|{price:.2f}"
            
        return "⏳ NO_SIGNAL"
    except Exception as e:
        return f"ERROR|{str(e)}"

