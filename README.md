# tilt_network

Flutter plugin that turns an Android device into a **Tilt compute peer** — joining the Tilt distributed grid with a single call.

## Installation

```yaml
dependencies:
  tilt_network: ^0.1.0
```

## Android setup

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>

<service
    android:name="technology.tilt.sdk.Tilt"
    android:exported="false"
    android:foregroundServiceType="dataSync"/>
```

## Usage

```dart
import 'package:tilt_network/tilt.dart';

// Start the peer (runs as a foreground service)
Tilt('pk_your_public_key', environment: 'production');

// Stream runtime logs
EventChannel('tilt/logs')
    .receiveBroadcastStream()
    .map((e) => List<String>.from(e as List))
    .listen((lines) => print(lines.last));
```

## Platform support

| Platform | Status |
|----------|--------|
| Android  | ✅     |
| iOS      | planned |
