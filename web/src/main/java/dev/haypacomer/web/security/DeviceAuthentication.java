package dev.haypacomer.web.security;

import dev.haypacomer.domain.device.Device;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class DeviceAuthentication extends AbstractAuthenticationToken {

  private final Device device;

  public DeviceAuthentication(Device device) {
    super(List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
    this.device = device;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public Device getPrincipal() {
    return device;
  }
}
