import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'tilt_platform_interface.dart';

/// An implementation of [TiltPlatform] that uses method channels.
class MethodChannelTilt extends TiltPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('tilt');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<List<String>?> getLogLines() async {
    final logs = await methodChannel.invokeListMethod<String>('getLogLines');
    return logs;  // já é List<String>?
  }
}

// static const _channel = MethodChannel('tilt');
