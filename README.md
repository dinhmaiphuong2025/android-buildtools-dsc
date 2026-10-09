# Android BuildTools DSC (Super Lite Compiler)

A minimalist headless compiler and background daemon designed for **vibecoding Android apps** directly from Android Linux containers (Droidspaces, PRoot, Chroot) on ARM64 devices without bloating container rootfs.

---

## 🎯 Purpose & Architecture

1. **Zero Storage Waste on Container:**
   - Android SDK, AAPT2 (ARM64), and build-tools are isolated within the Android application storage (`/data/data/com.zerostudio.lite/files/`), preserving the container's limited rootfs (e.g. 16GB).
   - Solves the `noexec` permission restriction on `/storage/emulated/0` by executing native ARM64 toolchains from private app storage.

2. **Headless IPC Daemon:**
   - Runs a background HTTP server on `127.0.0.1:8088`.
   - The CLI agent inside the Linux container triggers builds and streams logs in real-time via `scripts/agent_build_client.py`.

3. **Prebuilt AAPT2 for ARM64:**
   - Ships with pre-compiled ARM64 AAPT2 (`aapt2-35.0.2-arm64-v8a`) extracted on first launch.
   - Passes `-Pandroid.aapt2FromMavenOverride` to Gradle to bypass Google's missing Linux aarch64 Maven artifact.

---

## 🚀 Usage

### 1. Install & Launch App
- Install `android-buildtools-dsc-debug.apk` on your Android device.
- Open the app once to let it extract AAPT2 and start the daemon at `127.0.0.1:8088`.

### 2. Compile from Container
Run the build client from your Droidspaces or PRoot shell:
```bash
python3 scripts/agent_build_client.py /storage/emulated/0/YourApp assembleDebug
```

---

## 📜 Credits & License

- **Base Project:** Derived and trimmed from [ZeroStudio](https://github.com/msmt2018/ZeroStudio) and [AndroidIDE](https://github.com/AndroidIDEOfficial/AndroidIDE).
- **Authors & Contributors:** Originally authored by `android_zero`, `itsaky`, and community contributors.
- **License:** Licensed under the [ZeroStudio Community License (ZSCL)](LICENSE) / GNU General Public License v3 (GPLv3). Strictly non-commercial for personal development.
