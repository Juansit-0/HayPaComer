package dev.haypacomer.application.port;

public interface ChatModel {

  String name();

  String completeJson(String system, String user);
}
