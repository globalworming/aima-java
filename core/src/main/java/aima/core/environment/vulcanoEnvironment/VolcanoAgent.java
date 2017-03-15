package aima.core.environment.vulcanoEnvironment;

import aima.core.agent.api.Belief;
import aima.core.agent.api.MonteCarloLocalizationAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wormi
 */
public class VolcanoAgent implements MonteCarloLocalizationAgent<GoAction, AgentState, AgentPercept> {

  private static final long seed = 1L;
  public static final int SAMPLE_SIZE = 1000;
  private final VolcanoEnvironment volcanoEnvironment;
  private List<Belief<AgentState>> beliefs = new ArrayList<>(SAMPLE_SIZE);

  {
    final double weight = 1d / SAMPLE_SIZE;
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      beliefs.add(new MyBelief(weight, AgentState.random(seed)));
    }
  }

  public VolcanoAgent(VolcanoEnvironment volcanoEnvironment) {
    this.volcanoEnvironment = volcanoEnvironment;
  }

  /**
   * this agent randomly walks its environment, no decision logic
   */
  @Override
  public GoAction perceive(AgentPercept percept) {
    return GoAction.randomWalk(seed);
  }


  public List<Belief<AgentState>> localize(AgentPercept percept, GoAction action) {
    return localize(beliefs, action, percept);
  }

  @Override
  public List<Belief<AgentState>> localize(List<Belief<AgentState>> beliefs, GoAction action, AgentPercept percept) {

    final List<Belief<AgentState>> actionUpdate = motionUpdate(beliefs, action);
    final List<Belief<AgentState>> sensorUpdate =
        sensorUpdate(actionUpdate, percept);
    return resample(sensorUpdate, percept);
  }

  public List<Belief<AgentState>> resample(List<Belief<AgentState>> sensorUpdate,
                                                   AgentPercept actualPercept) {

    final List<Belief<AgentState>> newBeliefs = new ArrayList<>(SAMPLE_SIZE);
    sensorUpdate.forEach(myBelief -> {
      final AgentPercept agentPercept = volcanoEnvironment.generatePercept(myBelief.getState());
      final double distance = agentPercept.distanceTo(actualPercept);
      myBelief.getState().apply(GoAction.distributed(distance, seed));
    });

    for (int i = newBeliefs.size(); i < SAMPLE_SIZE; i = newBeliefs.size()) {
      newBeliefs.add(MyBelief.random());
    }
    return newBeliefs;
  }

  public List<Belief<AgentState>> sensorUpdate(List<Belief<AgentState>> beliefs,
                                               AgentPercept actualPercept) {
    List<AgentState> agentStates = beliefs.stream().map(Belief::getState).collect(Collectors.toList());
    Collections.sort(agentStates, (o1, o2) -> {
      final AgentPercept percept = volcanoEnvironment.generatePercept(o1);
      final double distance = percept.distanceTo(actualPercept);
      final AgentPercept otherPercept = volcanoEnvironment.generatePercept(o2);
      final double otherDistance = otherPercept.distanceTo(actualPercept);
      return ((Double) distance).compareTo(otherDistance);
    });

    ArrayList<Belief<AgentState>> newBeliefs = new ArrayList<>(SAMPLE_SIZE);
    double weight = 1.0;
    double step = 1.0 / SAMPLE_SIZE;

    // drop 50 % of beliefs
    for (int i = 0; i < agentStates.size(); i+= 2) {
      newBeliefs.add(new MyBelief(weight, agentStates.get(i)));
      weight -= step;
    }
    return newBeliefs;
  }

  @Override
  public List<Belief<AgentState>> motionUpdate(List<Belief<AgentState>> beliefs, GoAction action) {
    List<Belief<AgentState>> newBeliefs = new ArrayList<>(SAMPLE_SIZE);
    beliefs.forEach(myBelief ->
        newBeliefs.add(new MyBelief(myBelief.getWeight(), myBelief.getState().apply(action)))
    );
    return newBeliefs;
  }

  public void setBeliefs(List<Belief<AgentState>> beliefs) {
    this.beliefs = beliefs;
  }

  public static class MyBelief implements Belief<AgentState> {
    private final double weight;
    private final AgentState agentState;

    public MyBelief(double weight, AgentState agentState) {
      this.weight = weight;
      this.agentState = agentState;
    }

    public static MyBelief random() {
      return new MyBelief(1.0 / SAMPLE_SIZE, AgentState.random(seed));
    }

    @Override
    public double getWeight() {
      return weight;
    }

    @Override
    public AgentState getState() {
      return agentState;
    }
  }


}
