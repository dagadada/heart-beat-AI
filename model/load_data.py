"""
把数据加载到
train_data.csv
"""
import pandas as pd
import numpy as np
from scipy.io import loadmat
import os
from sklearn.utils import shuffle


def min_max(data):
    data = np.array(data)
    mi = min(data)
    ma = max(data)
    ans = (data - mi) / (ma - mi)
    return ans


root_path = "train_data"

name_list = []  # 存放人名
for path, dir_list, file_list in os.walk(root_path):
    name_list.extend(dir_list)

train_data = []  # 存放所有训练数据
for name in name_list:
    one_person_data = []  # 存放一个人的数据

    one_person_data_list = []  # 存放一个人的数据名
    for path, dir_list, file_list in os.walk(f"{root_path}/{name}"):
        one_person_data_list = file_list

    for one_data_name in one_person_data_list:
        tmp = loadmat(f"{root_path}/{name}/{one_data_name}")
        tmp = tmp["enhance"]
        tmp = tmp.reshape(-1)
        tmp = min_max(tmp)
        one_person_data.append(tmp)

    # 将一个人数据转为pandas，加标签
    one_person_data = np.array(one_person_data, dtype=float)
    one_person_data = pd.DataFrame(one_person_data)
    if name == "pengcheng":
        one_person_data[512] = 1.
    else:
        one_person_data[512] = 0.

    one_person_data = np.array(one_person_data)
    train_data.extend(one_person_data)

train_data = pd.DataFrame(train_data)
train_data = shuffle(train_data)

train_data.to_csv("train_data.csv", index=False)
