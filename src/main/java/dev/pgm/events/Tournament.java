package dev.pgm.events;

import dev.pgm.events.commands.TournamentAdminCommands;
import dev.pgm.events.commands.TournamentUserCommands;
import dev.pgm.events.commands.providers.TournamentProvider;
import dev.pgm.events.format.TournamentFormat;
import dev.pgm.events.listeners.MatchLoadListener;
import dev.pgm.events.listeners.PlayerJoinListen;
import dev.pgm.events.ready.ReadyCommands;
import dev.pgm.events.ready.ReadyListener;
import dev.pgm.events.ready.ReadyParties;
import dev.pgm.events.ready.ReadySystem;
import dev.pgm.events.team.ConfigTeamParser;
import dev.pgm.events.team.DefaultTeamManager;
import dev.pgm.events.team.TeamParser;
import dev.pgm.events.team.TournamentTeamManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.graph.CommandExecutor;
import tc.oc.pgm.command.graph.MatchPlayerProvider;
import tc.oc.pgm.command.graph.MatchProvider;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import tc.oc.pgm.lib.app.ashcon.intake.fluent.DispatcherNode;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.AbstractModule;

public class Tournament extends JavaPlugin {

  private TournamentTeamManager teamManager;
  private TournamentManager tournamentManager;
  private TeamParser teamParser;

  private static Tournament plugin;

  public static Tournament get() {
    return Tournament.plugin;
  }

  public void setTeamParser(TeamParser teamParser) {
    this.teamParser = teamParser;
  }

  public TournamentTeamManager getTeamManager() {
    return this.teamManager;
  }

  public TournamentManager getTournamentManager() {
    return this.tournamentManager;
  }

  public TeamParser getTeamParser() {
    return this.teamParser;
  }

  @Override
  public void onEnable() {
    Tournament.plugin = this;
    this.saveDefaultConfig();

    this.teamManager = DefaultTeamManager.manager();
    this.tournamentManager = new TournamentManager();
    this.teamParser = new ConfigTeamParser(); // load teams now

    ReadySystem system = new ReadySystem();
    ReadyParties parties = new ReadyParties();
    ReadyListener readyListener = new ReadyListener(system, parties);
    ReadyCommands readyCommands = new ReadyCommands(system, parties);

    BasicBukkitCommandGraph g =
        new BasicBukkitCommandGraph(new CommandModule(this.tournamentManager, this.teamManager));
    DispatcherNode node = g.getRootDispatcherNode();
    node = node.registerNode("tourney", "tournament", "tm", "events");
    node.registerCommands(readyCommands);
    node.registerCommands(new TournamentAdminCommands());
    g.getRootDispatcherNode().registerCommands(new TournamentUserCommands());

    Bukkit.getPluginManager().registerEvents(new MatchLoadListener(this.teamManager), this);
    Bukkit.getPluginManager().registerEvents(new PlayerJoinListen(this.teamManager), this);
    Bukkit.getPluginManager().registerEvents(readyListener, this);
    new CommandExecutor(this, g).register();
  }

  @Override
  public void onDisable() {
    Tournament.plugin = null;
  }

  private static class CommandModule extends AbstractModule {

    private final TournamentManager tournamentManager;
    private final TournamentTeamManager teamManager;

    public CommandModule(TournamentManager tournamentManager, TournamentTeamManager teamManager) {
      this.tournamentManager = tournamentManager;
      this.teamManager = teamManager;
    }

    private void configureInstances() {
      this.bind(PGM.class).toInstance(PGM.get());
    }

    private void configureProviders() {
      this.bind(MatchPlayer.class).toProvider(new MatchPlayerProvider());
      this.bind(Match.class).toProvider(new MatchProvider());
      this.bind(TournamentManager.class).toInstance(this.tournamentManager);
      this.bind(TournamentTeamManager.class).toInstance(this.teamManager);
      this.bind(TournamentFormat.class).toProvider(new TournamentProvider(this.tournamentManager));
    }

    @Override
    protected void configure() {
      this.configureInstances();
      this.configureProviders();
    }
  }
}
