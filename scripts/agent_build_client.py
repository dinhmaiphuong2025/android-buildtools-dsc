#!/usr/bin/env python3
import sys
import json
import urllib.request
import urllib.error
import time

SERVER_URL = "http://127.0.0.1:8088"

def check_status(verbose=True):
    try:
        req = urllib.request.Request(f"{SERVER_URL}/status")
        with urllib.request.urlopen(req, timeout=3) as res:
            data = json.loads(res.read().decode('utf-8'))
            if verbose:
                print(f"[BuildTools Daemon] Status: {data.get('status')}")
                print(f"  • Runner mode:    {data.get('runner_mode')}")
                print(f"  • Java ready:     {data.get('java_ready')} (bionic={data.get('java_bionic')})")
                print(f"  • AAPT2 ready:    {data.get('aapt2_ready')}")
                print(f"  • Currently building: {data.get('is_building', False)}")
                if data.get('is_building'):
                    print(f"    - Project: {data.get('current_project')}")
                    print(f"    - Tasks:   {data.get('current_tasks')}")
                print(f"  • Total builds:   {data.get('total_builds', 0)}")
            return data
    except Exception as e:
        if verbose:
            print(f"[BuildTools Daemon] Failed to connect to {SERVER_URL}: {e}")
        return None

def cancel_build():
    print(">> Requesting cancellation of active build...")
    try:
        req = urllib.request.Request(
            f"{SERVER_URL}/build/cancel",
            data=b"{}",
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=5) as res:
            data = json.loads(res.read().decode('utf-8'))
            print(">> Cancellation response:", data)
            return data.get("success", False)
    except Exception as e:
        print(f"!! Error sending cancel request: {e}")
        return False

def trigger_build(project_path, tasks="assembleDebug"):
    print(f">> Project: {project_path}")
    print(f">> Tasks:   {tasks}")
    start_time = time.time()
    
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
        with urllib.request.urlopen(req, timeout=600) as res:
            res_data = json.loads(res.read().decode('utf-8'))
            elapsed = time.time() - start_time
            print(f">> Result ({elapsed:.1f}s): {res_data}")
            
            # Fetch complete logs
            log_req = urllib.request.Request(f"{SERVER_URL}/build/log")
            with urllib.request.urlopen(log_req, timeout=10) as log_res:
                print("\n=== BUILD LOGS ===")
                print(log_res.read().decode('utf-8'))
                print("==================\n")
                
            return res_data.get("success", False)
    except urllib.error.HTTPError as e:
        print(f"!! HTTP Error: {e.code} - {e.read().decode('utf-8')}")
    except Exception as e:
        print(f"!! Build execution error: {e}")
    return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage:")
        print("  agent_build_client.py status               # Check daemon readiness & build status")
        print("  agent_build_client.py cancel               # Cancel currently running build")
        print("  agent_build_client.py <project_path> [tasks] # Run build task (default: assembleDebug)")
        sys.exit(1)
        
    cmd = sys.argv[1]
    
    if cmd == "status":
        st = check_status(verbose=True)
        sys.exit(0 if st is not None else 1)
    elif cmd == "cancel":
        success = cancel_build()
        sys.exit(0 if success else 1)
    else:
        proj = cmd
        task = sys.argv[2] if len(sys.argv) > 2 else "assembleDebug"
        status = check_status(verbose=False)
        if status:
            success = trigger_build(proj, task)
            sys.exit(0 if success else 1)
        else:
            print("Please open Android BuildTools DSC on your device to start the daemon first!")
            sys.exit(1)
