package dev.haypacomer.web.household;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.household.AcceptInvitation;
import dev.haypacomer.application.household.CancelInvitation;
import dev.haypacomer.application.household.ChangeMemberRole;
import dev.haypacomer.application.household.CreateHousehold;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.household.InviteMember;
import dev.haypacomer.application.household.ListHouseholds;
import dev.haypacomer.application.household.ListInvitations;
import dev.haypacomer.application.household.RemoveMember;
import dev.haypacomer.application.household.TransferOwnership;
import dev.haypacomer.application.household.UpdateHousehold;
import dev.haypacomer.application.inventory.FoodAccessGuard;
import dev.haypacomer.application.mail.MailLinks;
import dev.haypacomer.application.port.EmailSender;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InvitationRepository;
import dev.haypacomer.application.port.UserRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HouseholdConfiguration {

  @Bean
  CreateHousehold createHousehold(HouseholdRepository households, Clock clock) {
    return new CreateHousehold(households, clock);
  }

  @Bean
  ListHouseholds listHouseholds(HouseholdRepository households) {
    return new ListHouseholds(households);
  }

  @Bean
  GetHousehold getHousehold(HouseholdRepository households) {
    return new GetHousehold(households);
  }

  @Bean
  UpdateHousehold updateHousehold(HouseholdRepository households) {
    return new UpdateHousehold(households);
  }

  @Bean
  ChangeMemberRole changeMemberRole(HouseholdRepository households) {
    return new ChangeMemberRole(households);
  }

  @Bean
  RemoveMember removeMember(HouseholdRepository households) {
    return new RemoveMember(households);
  }

  @Bean
  TransferOwnership transferOwnership(HouseholdRepository households) {
    return new TransferOwnership(households);
  }

  @Bean
  InviteMember inviteMember(
      HouseholdRepository households,
      InvitationRepository invitations,
      OpaqueTokens opaqueTokens,
      EmailSender email,
      MailLinks links,
      Clock clock) {
    return new InviteMember(households, invitations, opaqueTokens, email, links, clock);
  }

  @Bean
  ListInvitations listInvitations(
      HouseholdRepository households, InvitationRepository invitations, Clock clock) {
    return new ListInvitations(households, invitations, clock);
  }

  @Bean
  CancelInvitation cancelInvitation(
      HouseholdRepository households, InvitationRepository invitations) {
    return new CancelInvitation(households, invitations);
  }

  @Bean
  AcceptInvitation acceptInvitation(
      InvitationRepository invitations,
      HouseholdRepository households,
      UserRepository users,
      OpaqueTokens opaqueTokens,
      Clock clock) {
    return new AcceptInvitation(invitations, households, users, opaqueTokens, clock);
  }

  @Bean
  FoodAccessGuard foodAccessGuard() {
    return new FoodAccessGuard();
  }
}
