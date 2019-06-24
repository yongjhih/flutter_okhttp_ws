import 'dart:async';

import 'package:flutter/services.dart';

class FlutterOkhttpWs {
  static const MethodChannel _channel =
      const MethodChannel('flutter_okhttp_ws');

  static Future<String> get connect async {
    return await _channel.invokeMethod('connect');
  }
  static Future<String> get send async {
    return await _channel.invokeMethod('send');
  }
  static Future<int> get close async {
    return await _channel.invokeMethod('close');
  }
}
