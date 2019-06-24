import 'dart:async';

import 'package:flutter/services.dart';

class FlutterOkhttpWs {
  static const MethodChannel _channel =
      const MethodChannel('flutter_okhttp_ws');

  static Future<String> connect(String url) async {
    return await _channel.invokeMethod('connect', url);
  }
  static Future<String> send(String message) async {
    return await _channel.invokeMethod('send', message);
  }
  static Future<int> close() async {
    return await _channel.invokeMethod('close');
  }
}
