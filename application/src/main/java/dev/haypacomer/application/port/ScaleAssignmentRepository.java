package dev.haypacomer.application.port;

import dev.haypacomer.application.scale.ScaleAssignment;
import dev.haypacomer.domain.device.DeviceId;
import java.util.Optional;

public interface ScaleAssignmentRepository {

  void save(ScaleAssignment assignment);

  Optional<ScaleAssignment> find(DeviceId scale);

  void remove(DeviceId scale);
}
