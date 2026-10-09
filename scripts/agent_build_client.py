#!/usr/bin/env python3
import sys
import json
import urllib.request
import urllib.error
import time

SERVER_URL = "http://127.0.0.1:8088"

def check_status():
    try:
        req = urllib.request.Request(f"{SERVER_URL}/status")
        with urllib.request.urlopen(req, timeout=3) as res:
            data = json.loads(res.read().decode('utf-8'))
            print(f"[ZeroLite Daemon] Trạng thái: {data}")
            return True
    except Exception as e:
        print(f"[ZeroLite Daemon] Chưa kết nối được daemon tại {SERVER_URL}: {e}")
        return False

def trigger_build(project_path, tasks="assembleDebug"):
    print(f">> Gửi yêu cầu build dự án: {project_path}")
    print(f">> Tasks: {tasks}")
    
    payload = json.dumps({
        "projectPath": project_path,
        "tasks": tasks
    }).encode('utf-8')
    
    req = urllib.request.Request(
        f"{SERVER_URL}/build",
        data=payload,
        headers={"Content-Type": "application/json"}
    )
    
    try:
        with urllib.request.urlopen(req, timeout=300) as res:
            res_data = json.loads(res.read().decode('utf-8'))
            print(">> Kết quả build:", res_data)
            
            # Đọc toàn bộ logs
            log_req = urllib.request.Request(f"{SERVER_URL}/build/log")
            with urllib.request.urlopen(log_req, timeout=10) as log_res:
                print("\n=== BUILD LOGS ===")
                print(log_res.read().decode('utf-8'))
                print("==================\n")
                
            return res_data.get("success", False)
    except urllib.error.HTTPError as e:
        print(f"!! Lỗi HTTP: {e.code} - {e.read().decode('utf-8')}")
    except Exception as e:
        print(f"!! Lỗi khi gọi build: {e}")
    return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Sử dụng: agent_build_client.py <đường_dẫn_dự_án> [tasks]")
        sys.exit(1)
        
    proj = sys.argv[1]
    task = sys.argv[2] if len(sys.argv) > 2 else "assembleDebug"
    
    if check_status():
        success = trigger_build(proj, task)
        sys.exit(0 if success else 1)
    else:
        print("Vui lòng mở app ZeroStudio Super Lite trên điện thoại để khởi động daemon trước!")
        sys.exit(1)
