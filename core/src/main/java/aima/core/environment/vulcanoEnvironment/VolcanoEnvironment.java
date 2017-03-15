package aima.core.environment.vulcanoEnvironment;

import aima.core.util.datastructure.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple continuous environment. 3d edition implementation used a 2d environment with obstacles.
 * But 2d geometry is hard, so just use a map with heat point sources.
 * @author wormi
 *
 * Narritive: A seriously damaged drone lands on vulcanic planet. The map is known, but the
 * location sensor was destroyed. The heat sensor is still working and the range sensor still
 * reads the approximate range to the next repair station
 */
public class VolcanoEnvironment {

  private VolcanoAgent agent;
  private AgentState agentState;

  private List<HeatSource> heatSources;
  private final RobotBase robotRepairBase;

  {
    Point2D p1 = new Point2D(0.3, 0.2);
    Point2D p2 = new Point2D(0.7, 0.3);
    Point2D p3 = new Point2D(0.1, 0.6);
    Point2D pStation = new Point2D(0.5, 0.5);
    heatSources = new ArrayList<>(3);
    heatSources.add(new HeatSource(p1, 0.9));
    heatSources.add(new HeatSource(p2, 0.8));
    heatSources.add(new HeatSource(p3, 0.3));
    robotRepairBase = new RobotBase(pStation);
  }

  public void putAgent(VolcanoAgent a, AgentState s) {
    this.agent = a;
    this.agentState = s;
  }

  private long approximateHeat(AgentState agentState) {
    return Math.round(heatAtPosition(agentState.getPosition()) *100);
  }

  private double heatAtPosition(Point2D position) {
    double heat = 0;
    for (HeatSource heatSource : heatSources) {
      heat += heatSource.temperature - ((double) heatSource.position.distance(position)) / 10;
    }
    return Math.max(heat, 0);
  }

  private long unreliableDistance(Point2D p1, Point2D p2) {
    return Math.round(p1.distance(p2) * 100);
  }

  public AgentPercept generatePercept() {
   return  generatePercept(agentState);
  }

  public void setAgentState(AgentState agentState) {
    this.agentState = agentState;
  }

  public AgentPercept generatePercept(AgentState _agentState) {
    final AgentPercept.Builder b = new AgentPercept.Builder();
    b.setHeat(approximateHeat(_agentState));
    b.setDistanceToRepairBase(
        unreliableDistance(_agentState.getPosition(), robotRepairBase.position));
    return b.build();
  }

  private class HeatSource {
    final Point2D position;
    final double temperature;

    HeatSource(Point2D p, double k) {
      position = p;
      temperature = k;
    }
  }

  private class RobotBase {
    final Point2D position;

    public RobotBase(Point2D position) {
      this.position = position;
    }
  }
}
