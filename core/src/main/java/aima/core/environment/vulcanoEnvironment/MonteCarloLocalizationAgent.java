package aima.core.environment.vulcanoEnvironment;

import aima.core.agent.api.Agent;
import aima.core.agent.api.Sensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wormi
 */
public class MonteCarloLocalizationAgent implements Agent<GoAction, AgentPercept> {

  private static final long seed = 1L;
  public static final int SAMPLE_SIZE = 1000;
  private final VolcanoEnvironment volcanoEnvironment;
  private List<Sensor> sensors = new ArrayList<>(2);
  private List<MyBelief> beliefs = new ArrayList<>(SAMPLE_SIZE);

  {
    final double weight = 1d / SAMPLE_SIZE;
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      beliefs.add(new MyBelief(weight, AgentState.random(seed)));
    }
    sensors.add(new Heat());
    sensors.add(new RepairBaseDistance());
  }

  public MonteCarloLocalizationAgent(VolcanoEnvironment volcanoEnvironment) {
    this.volcanoEnvironment = volcanoEnvironment;
  }

  /**
   * this agent randomly walks its environment, no decision logic
   */
  @Override
  public GoAction perceive(AgentPercept percept) {
    return GoAction.randomWalk(seed);
  }


  public List<Sensor> getSensors() {
    return sensors;
  }

  public List<MyBelief> localize(AgentPercept percept, GoAction action) {
    return localize(beliefs, action, percept, volcanoEnvironment);
  }

  private static List<MyBelief> localize(List<MyBelief> beliefs,
                                         GoAction action,
                                         AgentPercept actualPercept,
                                         VolcanoEnvironment _volcanoEnvironment) {

    final List<MyBelief> actionUpdate = applyAction(beliefs, action);
    final List<MyBelief> sensorUpdate =
        calculateWeights(actionUpdate, actualPercept, _volcanoEnvironment);
    return resample(sensorUpdate, actualPercept, _volcanoEnvironment);
  }

  private static List<MyBelief> resample(List<MyBelief> sensorUpdate, AgentPercept actualPercept, VolcanoEnvironment _volcanoEnvironment) {

    final List<MyBelief> newBeliefs = new ArrayList<>(SAMPLE_SIZE);
    sensorUpdate.forEach(myBelief -> {
      final AgentPercept agentPercept = _volcanoEnvironment.generatePercept(myBelief.agentState);
      final double distance = agentPercept.distanceTo(actualPercept);
      myBelief.agentState.apply(GoAction.distributed(distance, seed));
    });

    for (int i = newBeliefs.size(); i < SAMPLE_SIZE; i = newBeliefs.size()) {
      newBeliefs.add(MyBelief.random());
    }
    return newBeliefs;
  }

  public static List<MyBelief> calculateWeights(List<MyBelief> beliefs,
                                                 AgentPercept actualPercept,
                                                 VolcanoEnvironment volcanoEnvironment) {
    List<AgentState> agentStates = beliefs.stream().map(b -> b.agentState).collect(Collectors.toList());
    Collections.sort(agentStates, (o1, o2) -> {
      final AgentPercept percept = volcanoEnvironment.generatePercept(o1);
      final double distance = percept.distanceTo(actualPercept);
      final AgentPercept otherPercept = volcanoEnvironment.generatePercept(o2);
      final double otherDistance = otherPercept.distanceTo(actualPercept);
      return ((Double) distance).compareTo(otherDistance);
    });

    ArrayList<MyBelief> newBeliefs = new ArrayList<>(SAMPLE_SIZE);
    double weight = 1.0;
    double step = 1.0 / SAMPLE_SIZE;

    // drop 50 % of beliefs
    for (int i = 0; i < agentStates.size(); i+= 2) {
      newBeliefs.add(new MyBelief(weight, agentStates.get(i)));
      weight -= step;
    }
    return newBeliefs;
  }

  public static List<MyBelief> applyAction(List<MyBelief> beliefs, GoAction action) {
    List<MyBelief> newBeliefs = new ArrayList<>(SAMPLE_SIZE);
    beliefs.forEach(myBelief ->
        newBeliefs.add(new MyBelief(myBelief.weight, myBelief.agentState.apply(action)))
    );
    return newBeliefs;
  }

  public void setBeliefs(List<MyBelief> beliefs) {
    this.beliefs = beliefs;
  }


  class Heat implements Sensor {}

  class RepairBaseDistance implements Sensor { }

  public static class MyBelief {
    private final double weight;
    private final AgentState agentState;

    public MyBelief(double weight, AgentState agentState) {
      this.weight = weight;
      this.agentState = agentState;
    }

    public static MyBelief random() {
      return new MyBelief(1.0 / SAMPLE_SIZE, AgentState.random(seed));
    }

    public AgentState getAgentState() {
      return agentState;
    }
  }


}
