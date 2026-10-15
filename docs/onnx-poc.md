# Native offline AI face-swap proof of concept (Android)

**Branch:** `experiment/onnx-face-swap-poc`  
**Status:** Experimental build pipeline; no claim of successful APK or tested phone until CI/device verifies it.  
**Production:** `main` stays untouched.  

## Purpose

Check whether a real neural face-swap pipeline runs on Motorola Edge 70 Fusion before changing the current FaceSwap Studio video pipeline. The first prototype swaps **photos only**, not videos. This is intentional: test model download, runtime memory, face detection/alignment, output realism, and performance on real Android hardware.

## Provenance and licensing

The POC workflow fetches source from:
- `https://github.com/Parasaran-Python/android-face-fusion`
- immutable source commit `aafb3c3638529a61ba94fe8de579097fbcfb6885`
- source license MIT (see upstream `LICENSE`)
- uses its separate app package; it does **not** overwrite the current FaceSwap Studio application.

**Models are separate from the code.** The app downloads SCRFD, ArcFace and INSwapper on first use (about 739 MB). Model weights are subject to InsightFace's non-commercial research terms and are **not** redistributed inside this APK. Only use images of people who agreed to the processing. There are no cloud inference APIs; first-time model downloads do require internet.

## Build pipeline

`.github/workflows/poc-onnx-faceswap.yml` clones the fixed upstream commit, sets up JDK 21, Android SDK and NDK, builds `assembleDebug`, checks for a non-empty APK and uploads `android-onnx-faceswap-photo-poc` if and only if compilation succeeds.

This is a **from-source, independent photo-POC build** on our CI, NOT a claim that the current `llipinsk82-rgb/Face` app already implements AI swapping. The build is not merged into `main` automatically. The test app is not yet performance-validated.

## Test on Motorola

1. Go to the GitHub Actions run for **POC Android ONNX Face Swap (source build)** on the experiment branch.
2. Check if the build job has **success**; if so download artifact `android-onnx-faceswap-photo-poc` (ZIP containing debug APK).
3. Install it separately from the existing FaceSwap Studio app.
4. Start app, permit one-time download of model weights over Wi-Fi (reserve at least 1 GB storage and RAM headroom).
5. Swap one consenting source face onto one target still image; assess placement, eyes/mouth alignment, image artifacts, speed, memory pressure.
6. If the still image is acceptable, implement a short (5–10 s) **720p** test-video prototype on our own source branch, including streaming decode/encode, audio retention and true processed preview.
7. Do not promote to `main` before **physical device PASS**.

## Acceptance gate

- No fake detector or bitmap-mask-only rendering.
- Measurable ONNX inference, not simply loading a model.
- Real source-to-target identity transfer on a still image.
- CPU fallback that functions on Motorola.
- Avoid declaring performance results until measured.
- If CI fails, capture Gradle logs and fix the build; never claim APK exists based solely on a source commit.

## Why start with photos?

The current application can play/export MP4 but its "face swap" literally pastes a bitmap. Adding more controls does not fix it. We must validate real on-device neural inference *first*; the image pipeline can then be used frame-by-frame for short videos with proper encoding and tracking.
