package aima.test.unit.environment.vulcanoEnvironment;

import aima.core.environment.vulcanoEnvironment.AgentPercept;
import aima.core.environment.vulcanoEnvironment.AgentState;
import aima.core.environment.vulcanoEnvironment.GoAction;
import aima.core.environment.vulcanoEnvironment.VolcanoEnvironment;
import aima.core.environment.vulcanoEnvironment.MonteCarloLocalizationAgent;
import aima.core.util.datastructure.Point2D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * @author wormi
 */
public class VolcanoEnvironmentTest {

  @Test
  public void placeAgent() throws Exception {
    VolcanoEnvironment volcanoEnvironment = new VolcanoEnvironment();
    final Point2D agentPosition = new Point2D(0.1, 0.1);
    volcanoEnvironment.putAgent(new MonteCarloLocalizationAgent(volcanoEnvironment), new AgentState(agentPosition));
  }

  @Test
  public void repairBaseCenterAndDistance() throws Exception {
    VolcanoEnvironment volcanoEnvironment = new VolcanoEnvironment();
    final Point2D corner = new Point2D(0, 0);
    final AgentState agentState = new AgentState(corner);
    volcanoEnvironment.putAgent(new MonteCarloLocalizationAgent(volcanoEnvironment), agentState);
    final AgentPercept agentPercept = volcanoEnvironment.generatePercept(agentState);
    assertThat(agentPercept.getHeat()).isEqualTo(183);
    assertThat(agentPercept.getDistanceToRepairBase()).isEqualTo(71);
    final AgentState center = new AgentState(new Point2D(0.5, 0.5));
    final AgentPercept perceptInCenter = volcanoEnvironment.generatePercept(center);
    assertThat(perceptInCenter.getHeat()).isEqualTo(189);
    assertThat(perceptInCenter.getDistanceToRepairBase()).isEqualTo(0);
  }

  @Test
  public void scenarioTest() throws Exception {
    // build environment
    final VolcanoEnvironment volcanoEnvironment = new VolcanoEnvironment();
    final MonteCarloLocalizationAgent agent = new MonteCarloLocalizationAgent(volcanoEnvironment);
    final Point2D initialAgentPosition = new Point2D(0.1, 0.1);
    final AgentState agentState = new AgentState(initialAgentPosition);
    volcanoEnvironment.putAgent(agent, agentState);

    // pick first action
    GoAction agentAction = new GoAction(0, 0.1);

    // calculate new agentState
    AgentState agentStateAfterAction = agentState.apply(agentAction);
    Point2D actualAgentPosition = agentStateAfterAction.getPosition();
    assertThat(actualAgentPosition).isNotSameAs(initialAgentPosition);

    // agent has new state
    volcanoEnvironment.setAgentState(agentStateAfterAction);
    // new percept
    AgentPercept agentPerceptAfterAction = volcanoEnvironment.generatePercept();
    List<MonteCarloLocalizationAgent.MyBelief> localize = agent.localize(agentPerceptAfterAction, agentAction);
    assertThat(localize.size()).isEqualTo(MonteCarloLocalizationAgent.SAMPLE_SIZE);
    // new localized beliefs
    agent.setBeliefs(localize);



    for (int i = 0; i < 10; i++) {
      // next action
      agentAction = new GoAction((i % 2) * 2, 0.1);
      // calculate new agentState
      agentStateAfterAction = agentStateAfterAction.apply(agentAction);
      actualAgentPosition = agentStateAfterAction.getPosition();

      // agent has new state
      volcanoEnvironment.setAgentState(agentStateAfterAction);
      // new percept
      agentPerceptAfterAction = volcanoEnvironment.generatePercept();
      localize = agent.localize(agentPerceptAfterAction, agentAction);
    }
    assertThat(actualAgentPosition.getX()).isEqualTo(0.7);
    assertThat(actualAgentPosition.getY()).isEqualTo(0.6);
    assertThat(localize.get(0).getAgentState().getPosition().distance(actualAgentPosition)
    ).isLessThan(0.1);



  }

  @Test
  public void applyAction() throws Exception {
    List<MonteCarloLocalizationAgent.MyBelief> beliefs = new ArrayList<>();
    final Point2D initialPosition = new Point2D(0.1,
        0.1);
    beliefs.add(new MonteCarloLocalizationAgent.MyBelief(1.0, new AgentState(initialPosition)));
    beliefs = MonteCarloLocalizationAgent.applyAction(beliefs, new GoAction(0, 0.1));
    final Point2D position = beliefs.get(0).getAgentState().getPosition();
    assertThat(position.getX()).isEqualTo(0.2);
    assertThat(position.getY()).isEqualTo(0.1);
  }

  @Test
  public void calculateWeights() throws Exception {
    List<MonteCarloLocalizationAgent.MyBelief> beliefs = new ArrayList<>();
    beliefs.add(new MonteCarloLocalizationAgent.MyBelief(
        1.0, new AgentState(new Point2D(1.0, 1.0))
    ));
   beliefs.add(new MonteCarloLocalizationAgent.MyBelief(
        1.0, new AgentState(new Point2D(2.0, 2.0))
    ));
   beliefs.add(new MonteCarloLocalizationAgent.MyBelief(
        1.0, new AgentState(new Point2D(0.5, 0.5))
    ));
   beliefs.add(new MonteCarloLocalizationAgent.MyBelief(
        1.0, new AgentState(new Point2D(0.7, 0.7))
    ));

    AgentPercept actualPercept = new AgentPercept(183L, 71);
    VolcanoEnvironment volcanoEnvironment = new VolcanoEnvironment();
    beliefs = MonteCarloLocalizationAgent.calculateWeights(beliefs, actualPercept, volcanoEnvironment);
    assertThat(beliefs.get(0).getAgentState().getPosition().getX()).isEqualTo(0.3);
  }
}

