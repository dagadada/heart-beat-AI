


# 心跳活体认证系统（Heartbeat Liveness Authentication System）

本项目是一个基于手机加速度计与深度学习模型的心跳活体身份认证系统，结合 Android 前端客户端与 Flask 后端服务，实现对用户心跳信号的采集、活体验证与智能识别。

## 📌 项目简介

- **核心理念**：利用手机加速度计采集用户心跳波形，通过 PyTorch Lite 模型进行实时身份判断，同时结合 TEE/SE 签名机制验证数据真实性，实现高度安全的生物认证系统。
- **适用场景**：移动支付、门禁控制、医疗系统登录等需防重放攻击的高安全认证场合。
- **关键特性**：
  - DRSN 深度神经网络模型（支持噪声鲁棒性）
  - 客户端活体数据采集 + 本地签名
  - 服务端验签 + 实时 AI 推理
  - 可容器化部署（Docker + Flask）

---

## 🗂️ 项目结构

```

project-root/
├── backend/               后端 Flask 服务，验证签名 & 推理
├── app/                   Android 客户端，采集心跳并签名
├── model/                 模型训练与导出脚本、模型文件
├── docker-compose.yaml    一键部署配置（后端服务）
├── .gitignore             Git 忽略文件
├── README.md              当前说明文档

````

---

## 🚀 快速开始

### ✅ 运行 Android 客户端（前端）

> 要求：Android Studio，Android API ≥ 33，支持加速度传感器的设备或模拟器

1. 使用 Android Studio 打开 `app/` 项目文件夹  
2. 连接模拟器或真机设备，点击“运行”启动  
3. 首次启动输入用户名完成注册  
4. 点击“采集心跳”按钮，保持设备平稳约 6.4 秒  
5. 查看认证结果（匹配分数 + 活体验证）

### ✅ 部署 Flask 后端（后端）

> 要求：Python 3.9+，已安装 Docker（推荐）

#### 方法一：使用 Docker 一键部署

```bash
docker-compose up --build
````

服务将在本地 `http://localhost:5000` 启动，供 Android 客户端访问。

#### 方法二：本地直接运行（调试用）

```bash
cd backend/
pip install -r requirements.txt
python app.py
```

---

## 🧠 模型说明（AI 推理）

* 模型结构：DRSN（Deep Residual Shrinkage Network）
* 输入数据：6400 点心跳原始波形（float 数组）
* 输出结果：相似度分数（0\~1）+ 是否匹配阈值判定
* 推理接口：`POST /predict`，JSON 格式
* 推理模型文件：`model/heart_model.ptl`

---

## 🔐 安全机制说明（TEE + SE）

* 所有心跳数据在客户端采集后，使用 TEE/SE 私钥进行 RSA 签名
* 签名内容包括：用户ID、时间戳、心跳向量
* 后端服务验证签名与时间差（±5s）判断是否为活体数据
* 可有效防止“录制回放”、“模型注入”等攻击方式

---

## 📦 接口示例

### 注册用户

```http
POST /register_user
Content-Type: application/x-www-form-urlencoded
Body: username=alice
```

### 验签 + 活体认证

```http
POST /api/verify_heartbeat
Content-Type: application/json
{
  "payload": "Base64编码的心跳数据包",
  "signature": "Base64编码的RSA签名"
}
```

---

## 🛠️ 依赖环境

### Android 客户端依赖（见 `app/build.gradle`）：

* `androidx.appcompat:appcompat:1.6.1`
* `org.pytorch:pytorch_android_lite:1.13.0`
* `androidx.security:security-crypto`
* `com.google.android.gms:play-services-safetynet`

### Flask 后端依赖（见 `requirements.txt`）：

* Flask 2.2+
* Flask-CORS
* psycopg2-binary
* cryptography
* numpy, base64, json

---

## ✅ 开发/测试说明

* 使用 `pytest + requests` 进行后端接口测试
* Android 客户端核心逻辑已通过 JUnit 单元测试
* 模型推理精度：97.58%（测试集中）
* Pixel 5 实测推理时延：\~12ms

---

## ⚠️ 注意事项

* 认证采集时间必须 ≥ 6.4 秒，确保完整心跳波形
* 使用真机时，请确保开启加速度传感器权限
* 若后端返回 `403`，请检查签名或时间戳有效性
* 当前版本不支持 iOS 客户端（可后续扩展）

---

## 📬 联系方式与支持

* 作者：翟海（Suchow University）
* 邮箱：3398299269@qq.com
* 项目地址：[https://github.com/dagadada/heart-beat-AI](https://github.com/dagadada/heart-beat-AI)

---

## 📎 附录

* 系统功能使用说明书：见 `/docs/` 目录或 [链接](https://github.com/dagadada/heart-beat-AI/blob/main/docs/)
* 模型训练脚本：`model/train.py`
* 项目展示截图：可见 `/docs/images/`

---

