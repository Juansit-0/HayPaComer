package dev.haypacomer.application.port;

import dev.haypacomer.application.live.LiveUpdate;

public interface LiveUpdateListener {

  void onUpdate(LiveUpdate update);
}
