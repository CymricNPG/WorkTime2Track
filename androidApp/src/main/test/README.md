# WorkTime2Track Android icon resources

Copy the `res` directory into your Android app module:

```text
androidApp/src/main/res/
```

Use the transparent logo inside the application:

```xml
android:src="@drawable/wt2t_logo"
```

Use the launcher icons in `AndroidManifest.xml`:

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round">
```

Included resources:

- `drawable/wt2t_logo.png`: transparent in-app logo, 512 × 512 px
- `drawable-v24/ic_launcher_foreground.png`: adaptive icon foreground
- `drawable/ic_launcher_background.xml`: adaptive icon background
- `mipmap-anydpi-v26`: adaptive icon definitions
- `mipmap-mdpi`: 48 × 48 px
- `mipmap-hdpi`: 72 × 72 px
- `mipmap-xhdpi`: 96 × 96 px
- `mipmap-xxhdpi`: 144 × 144 px
- `mipmap-xxxhdpi`: 192 × 192 px
