package aima.core.environment.vulcanoEnvironment;

import java.util.Comparator;

/**
 * @author wormi
 * @see <a href="https://to.headissue.net/radar/browse/"></a>
 */
public class AgentPercept {

  private final long heat;
  private final long distanceToRepairBase;

  public AgentPercept(long heat, long distanceToRepairBase) {
    this.heat = heat;
    this.distanceToRepairBase = distanceToRepairBase;
  }

  public double distanceTo(AgentPercept expectedPercept) {
    return ((double) heat/expectedPercept.heat +
        (double) distanceToRepairBase/expectedPercept.distanceToRepairBase)
        / 2;
  }

  public static class Builder {

    private long heat;
    private long distanceToRepairBase;

    public Builder setHeat(long heat) {
      this.heat = heat;
      return this;
    }

    public Builder setDistanceToRepairBase(long distanceToRepairBase) {
      this.distanceToRepairBase = distanceToRepairBase;
      return this;
    }

    public AgentPercept build() {
      return new AgentPercept(heat, distanceToRepairBase);
    }
  }

  public long getHeat() {
    return heat;
  }

  public long getDistanceToRepairBase() {
    return distanceToRepairBase;
  }
}
