from flask import Flask, request, jsonify
from flask_cors import CORS
import psycopg2

app = Flask(__name__)
CORS(app)

DB_CONFIG = {
    "host": "localhost",
    "database": "DemoDb",
    "user": "postgres",
    "password": "474bNM47",
    "port": "5432"
}

def get_db():
    return psycopg2.connect(**DB_CONFIG)

@app.route('/register_user', methods=['POST'])
def register_user():
    username = request.form.get('username')
    print(f"📥 注册请求：username={username}")

    if not username:
        print("❌ 用户名为空")
        return jsonify({'status': 'error', 'msg': '用户名为空'}), 400

    try:
        conn = get_db()
        cur = conn.cursor()

        # 检查是否已存在用户
        cur.execute('SELECT user_id FROM "user" WHERE user_name = %s', (username,))
        if cur.fetchone():
            print(f"⚠️ 用户已存在：{username}")
            return jsonify({'status': 'exists', 'msg': '用户已存在'})

        # 插入新用户
        cur.execute('INSERT INTO "user" (user_name, user_create_time) VALUES (%s, NOW())', (username,))
        conn.commit()
        print(f"✅ 成功插入用户：{username}")

        return jsonify({'status': 'success'})

    except Exception as e:
        print(f"🔥 数据库错误：{e}")
        return jsonify({'status': 'error', 'msg': str(e)}), 500

    finally:
        cur.close()
        conn.close()
@app.route('/check_user', methods=['GET'])
def check_user():
    username = request.args.get('username')
    print(f"📥 收到请求：/check_user?username={username}")

    try:
        conn = get_db()
        cur = conn.cursor()

        cur.execute('SELECT user_id FROM "user" WHERE user_name = %s', (username,))
        row = cur.fetchone()

        if not row:
            print(f"❌ 用户 '{username}' 不存在")
            return jsonify({'status': 'not_found'})

        user_id = row[0]
        print(f"✅ 用户 '{username}' 的 user_id = {user_id}")

        cur.execute('SELECT COUNT(*) FROM model WHERE user_user_id = %s', (user_id,))
        model_count = cur.fetchone()[0]
        print(f"🔎 用户模型数量 = {model_count}")

        status = 'has_model' if model_count > 0 else 'no_model'
        print(f"🚀 返回状态：{status}")

        return jsonify({'status': status})

    except Exception as e:
        print(f"🔥 发生错误：{e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500

    finally:
        cur.close()
        conn.close()


# ✅ 添加主程序启动
if __name__ == '__main__':
    print("✅ Flask 正在运行于 http://0.0.0.0:5000")
    app.run(host='0.0.0.0', port=5000, debug=True)

