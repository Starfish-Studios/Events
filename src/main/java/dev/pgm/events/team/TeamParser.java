package dev.pgm.events.team;

import java.util.Collection;

public interface TeamParser {

  TournamentTeam getTeam(String name);

  Collection<TournamentTeam> getTeams();
}
