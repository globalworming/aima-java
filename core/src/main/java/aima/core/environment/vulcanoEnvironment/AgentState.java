package aima.core.environment.vulcanoEnvironment;

import aima.core.util.Util;
import aima.core.util.datastructure.Point2D;

import java.util.Random;

/**
 * @author wormi
 */
public class AgentState {

  private final Point2D position;

  public AgentState(Point2D position) {
    this.position = position;
  }

  public Point2D getPosition() {
    return position;
  }

  public AgentState apply(GoAction a) {

    if (a.direction > 4) {
      throw new UnsupportedOperationException();
    }

    Point2D newPoint = null;
    switch (a.direction) {
      case 0:
        newPoint = new Point2D(position.getX() + a.distance, position.getY()); break;
      case 1:
        newPoint = new Point2D(position.getX() - a.distance, position.getY()); break;
      case 2:
        newPoint = new Point2D(position.getX(), position.getY() + a.distance); break;
      case 3:
        newPoint = new Point2D(position.getX(), position.getY() - a.distance); break;
    }
    return new AgentState(newPoint);
  }

  public static AgentState random(long seed) {
    final Random random = Util.getRandom(seed);
    double x = random.nextDouble();
    double y = random.nextDouble();
    return new AgentState(new Point2D(x, y));
  }

  @Override
  public String toString() {
    return "AgentState{" +
        "position=" + position +
        '}';
  }
}
