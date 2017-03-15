package aima.core.environment.vulcanoEnvironment;

import aima.core.util.Util;

import java.util.Random;

/**
 * @author wormi
 */
public class GoAction {

  int direction;
  double distance;

  public GoAction(int direction, double distance) {
    this.direction = direction % 4;
    this.distance = distance;
  }



  public static GoAction randomWalk(long seed) {
    final Random random = Util.getRandom(seed);
    int direction = random.nextInt(4);
    double _distance = random.nextDouble();
    return new GoAction(direction, _distance);
  }

  /**
   * will create a {@link GoAction} if the randomly created one is within distance. Otherwise the
   * action is "don't move at all". This leads to good approximated
   * {@link VolcanoAgent.MyBelief}s to not change much, while bad approximations
   * can change a lot
   *
   * @param distance
   * @return
   */
  public static GoAction distributed(double distance, long seed) {
    final Random random = Util.getRandom(seed);
    int direction = random.nextInt(4);
    double _distance = random.nextDouble();
    return new GoAction(direction, Math.max(0, distance - _distance));

  }
}
