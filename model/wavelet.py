import pywt
import numpy as np
from matplotlib import pyplot as plt
import pandas as pd
from scipy import signal
from scipy.io import loadmat

path = "wanglei-0311-1-0.mat"
# 信号
sig = loadmat(path)["data"]
sig = sig.reshape(-1)
t = np.arange(len(sig))

# 进行小波变换
wavelet = 'db4'  # 选择小波函数
levels = 5  # 分解的层数
coeffs = pywt.wavedec(sig, wavelet, level=levels)

# 选择适当的阈值
threshold = np.std(coeffs[-1]) * np.sqrt(2 * np.log(len(sig)))

# 对小波系数进行阈值处理
# coeffs = [pywt.threshold(c, 0.06, mode='soft') for c in coeffs]
coeffs = [pywt.threshold(c, threshold / 2, mode='soft') for c in coeffs]
coeffs[0] = np.zeros(coeffs[0].shape)


# 逆小波变换重构信号
reconstructed_signal = pywt.waverec(coeffs, wavelet)

# 绘制原始信号和降噪后的信号
plt.figure(figsize=(10, 8))
plt.subplot(2, 1, 1)
plt.plot(t, sig)
plt.title('Original Signal')

plt.subplot(2, 1, 2)
plt.plot(t, reconstructed_signal)
plt.title('Denoised Signal')
plt.show()
