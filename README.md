# Hyouka Video Converter

Native Kotlin Android app for on-device video processing with FFmpeg.

The app lets you choose:
- Resolution: Original (keep source resolution), 1080p (1920×1080), 2K (2560×1440), or 4K (3840×2160)
- Frame rate: 60, 90, 120, 144, 240, or 360 FPS

When Original is selected, the app changes only the frame rate and does not scale or pad the source video. For the other modes, aspect-ratio-preserving scale + pad targets the selected output resolution.

Motion-compensated interpolation generates the selected frame rate, and audio is preserved when present.

Processing happens locally on the Android device and can be CPU intensive, especially at 240/360 FPS and 4K.

The UI uses Material 3 / Material You patterns and applies Android dynamic colors when available on Android 12+.

The project uses the maintained Android continuation of FFmpegKit.
