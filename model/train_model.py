from torch.utils.data import DataLoader
from torch.utils.data import TensorDataset
from DRSN import DRSN_CW
import torch.nn as nn
import torch
import pandas as pd
import numpy as np

"""
读取数据：pd读取后，分别取出sample和label，转换为Tensor类型，接着
dataset = TensorDataset(samples, labels)

接着就可以生成data_iter迭代器了
batch_size = 10
data_iter = Data.DataLoader(dataset, batch_size, shuffle=False, sampler=torch.utils.data.sampler.RandomSampler(dataset))

"""

# 读取数据
data = pd.read_csv("train_data.csv")
samples = data.iloc[:, :-1]
labels = data.iloc[:, -1]
# 转换为Tensor类型
samples = np.array(samples)
samples = samples.reshape((samples.shape[0], 1, samples.shape[1]))
labels = np.array(labels)
# labels = labels.reshape((-1, 1))

samples = torch.Tensor(samples)
labels = torch.Tensor(labels)
dataset = TensorDataset(samples, labels)
batch_size = 64
data_iter = DataLoader(dataset, batch_size=batch_size)


# 模型训练
model = DRSN_CW.rsnet18()
loss = nn.BCELoss()
learnstep = 0.001
optim = torch.optim.Adam(model.parameters(), lr=learnstep)
epoch = 100

train_step = 0  # 每轮训练的次数
device = torch.device("cuda:0")
model.to(device)

model.train()  # 模型在训练状态
for i in range(epoch):
    print("第{}轮训练".format(i + 1))
    train_step = 0
    for data in data_iter:
        imgs, targets = data
        imgs = imgs.to(device)
        targets = targets.to(device)
        outputs = model(imgs)
        outputs = torch.flatten(outputs)
        result_loss = loss(outputs, targets)
        optim.zero_grad()
        result_loss.backward()
        optim.step()

        train_step += 1
        if train_step % 10 == 0:
            print("第{}轮的第{}次训练的loss:{}".format((i + 1), train_step, result_loss.item()))

path = "trained_model/DRSN_CW3.ptl"
torch.save(model, path)
torch.save(model.state_dict(), "trained_model/DRSN_CW3.pth")


