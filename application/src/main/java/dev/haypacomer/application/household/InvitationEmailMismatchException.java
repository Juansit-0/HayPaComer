package dev.haypacomer.application.household;

public final class InvitationEmailMismatchException extends RuntimeException {

  public InvitationEmailMismatchException() {
    super("This invitation was sent to a different email address");
  }
}
