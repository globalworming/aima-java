package aima.core.search.basic.uninformed;

import aima.core.search.api.Node;
import aima.core.search.api.NodeFactory;
import aima.core.search.api.Problem;
import aima.core.search.api.DefinedGoalStatesProblem;
import aima.core.search.api.SearchController;
import aima.core.search.api.SearchForActionsFunction;
import aima.core.search.basic.support.BasicNodeFactory;
import aima.core.search.basic.support.BasicSearchController;
import aima.core.search.basic.support.StateActionTimeLine;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * find a solution by searching from a start and goals in parallel. When multiple solutions
 * exist, a random one is returned, see unit test arad2oreda. The processes expand states
 * parallel and check each others frontiers if they meet.
 *
 * An optional {@link BiDirectionalBreadthFirstSearch#history} can be used for println debugging
 * but may slow down execution significantly
 *
 * @param <A> actions to take
 * @param <S> states which exist
 */
public class BiDirectionalBreadthFirstSearch<A, S> implements SearchForActionsFunction<A, S> {

  private NodeFactory<A, S> nodeFactory = new BasicNodeFactory<>();
  private SearchController<A, S> searchController = new BasicSearchController<>();
  private StateActionTimeLine<String, String> history;

  /**
   * @param problem to solve
   * @return list of actions to solve the problem
   */
  public List<A> apply(DefinedGoalStatesProblem<A, S> problem) {

    final int pathCost = 0;
    // initial check
    if (problem.isGoalState(problem.initialState())) {
      return solution(newRootNode(problem.initialState(), pathCost));
    }

    // build everything needed for the two searches
    Set<S> exploredFromStart = newExploredSet();
    Set<S> exploredFromGoal = newExploredSet();
    Node<A, S> startNode = newRootNode(problem.initialState(), pathCost);
    Queue<Node<A, S>> fromStartFrontier = newFIFOQueue();
    rememberEvent("addToFrontier: " + startNode.state(), true);
    fromStartFrontier.add(startNode);
    Queue<Node<A, S>> fromGoalFrontier = newFIFOQueue();
    for (S state : problem.goalStates()) {
      rememberEvent("addToFrontier: " + state, false);
      fromGoalFrontier.add(newRootNode(state, pathCost));
    }

    // build two Searches
    SearchDirection fromStartToGoal = new SearchDirection(exploredFromStart, fromGoalFrontier,
        fromStartFrontier, true);
    SearchDirection fromGoalToStart = new SearchDirection(exploredFromGoal, fromStartFrontier,
        fromGoalFrontier, false);

    // execute in parallel and return the first result one finds
    return Stream.of(fromStartToGoal, fromGoalToStart).parallel().map(searchDirection -> {
      // initial check of the frontier
      Node<A, S> nextNodeFromFrontier = searchDirection.frontier.peek();
      // the whole work is done here, expand the node, add and remove frontier nodes and check if
      // our parallel searches meet
      while (nextNodeFromFrontier != null) {
        Node<A, S> nodeToExpand = nextNodeFromFrontier;
        Optional<Node<A, S>> _solution = getSolution(problem, searchDirection, nodeToExpand);
        if (_solution.isPresent()) {
          return solution(_solution.get());
        }
        nextNodeFromFrontier = searchDirection.frontier.peek();
        rememberEvent("remove from frontier " + nodeToExpand.state(), searchDirection.fromStartToGoal);
        searchDirection.frontier.remove(nodeToExpand);
      }
      rememberEvent("abort search, frontier is empty", searchDirection.fromStartToGoal);
      return null;
    }).filter(result -> result != null).findFirst().orElse(failure());
  }

  private Optional<Node<A, S>> getSolution(DefinedGoalStatesProblem<A, S> problem, SearchDirection
      searchDirection, Node<A, S> nodeToExpand) {

    Queue<Node<A, S>> frontier = searchDirection.frontier;
    List<A> actions = problem.actions(nodeToExpand.state());
    Set<S> exploredStates = searchDirection.exploredStates;
    Queue<Node<A, S>> otherFrontier = searchDirection.otherFrontier;

    rememberEvent("expand " + nodeToExpand.state(), searchDirection.fromStartToGoal);
    return actions.parallelStream()
        // first filter for an action which leads to an leaf getting to an explored node of the
        // other tree
        .filter(action -> addLeafReachesOtherBranch(exploredStates, frontier, otherFrontier,
            newLeafNode(problem, nodeToExpand, action), searchDirection.fromStartToGoal))
        .limit(1)
        .collect(Collectors.toSet()).stream()
        // then - if any - use this action to generate the meeting node and build the result
        .map(action -> resultFromMeetingNodes(problem, searchDirection, newLeafNode(problem, nodeToExpand, action)))
        .findFirst();
  }

  private void rememberEvent(String action, boolean direction) {
    if (history != null) {
      rememberEvent(direction ? "forward" : "backward", action);
    }
  }

  private void rememberEvent(String state, String action) {
    history.add(state, action);
  }

  /**
   * add a new node to the frontier if the state is not explored
   * @return true when the new node reaches one of the other searchDirections branch
   */
  private boolean addLeafReachesOtherBranch(Set<S> exploredStates,
                                            Queue<Node<A, S>> thisFrontier,
                                            Queue<Node<A, S>> otherFrontier,
                                            Node<A, S> leaf,
                                            boolean direction) {
    rememberEvent("addToFrontier: " + leaf.state(), direction);
    thisFrontier.add(leaf);
    if (exploredStates.contains(leaf.state())) {
      return false;
    } else {
      exploredStates.add(leaf.state());
    }
    return doMeet(leaf, otherFrontier, direction);
  }

  /**
   * to merge the results of searchDirections, we get both nodes where there meet and apply the
   * necessary actions to get one node to the root state of the other
   */
  private Node<A, S> resultFromMeetingNodes(DefinedGoalStatesProblem<A, S> problem,
                                            SearchDirection searchDirection,
                                            Node<A, S> leaf) {

    boolean direction = searchDirection.fromStartToGoal;
    Queue<Node<A, S>> otherLeafs = searchDirection.otherFrontier;
    Node<A, S> nodeWithEqualState = findNodeWithEqualState(leaf, otherLeafs, direction);

    rememberEvent("buildFromMeetinNode " + leaf.state(), direction);
    if (direction) {
      return buildResultPath(leaf, nodeWithEqualState, problem);
    } else {
      // reverse build order
      return buildResultPath(nodeWithEqualState, leaf, problem);
    }
  }

  /**
   * @return node with equal state from the other frontier or null if none exists
   */
  private Node<A, S> findNodeWithEqualState(Node<A, S> leaf, Queue<Node<A, S>> otherFrontier,
                                            boolean direction) {
    rememberEvent("search for " + leaf.state() + "in other frontier", direction);
    return otherFrontier.parallelStream()
        .filter(node -> node.state().equals(leaf.state()))
        .findFirst()
        .orElse(null);
  }

  private boolean doMeet(Node<A, S> leaf, Queue<Node<A, S>> otherFrontier, boolean direction) {
    return findNodeWithEqualState(leaf, otherFrontier, direction) != null;
  }

  /**
   * merge the notes of a path from the start with a reversed path originating from the goal
   */
  public Node<A, S> buildResultPath(Node<A, S> fromStartNode, Node<A, S> fromGoalNode,
                                    Problem<A, S> problem) {

    if (!fromGoalNode.state().equals(fromStartNode.state())) {
      throw new UnsupportedOperationException("can build result path from same state only");
    }

    Node<A, S> result = fromStartNode;
    Node<A, S> nextNode = fromGoalNode.parent();
    while (nextNode != null) {
      S state = result.state();
      S intendedState = nextNode.state();
      A actionFromStateToIntendedState = problem.actions(state).parallelStream()
          .filter(action -> intendedState.equals(problem.result(state, action))).findFirst()
          .orElseThrow(() -> new NullPointerException("can't happen if we assume that nodes are " +
              "bidirectional linked")
          );
      result = newLeafNode(problem, result, actionFromStateToIntendedState);
      nextNode = nextNode.parent();
    }
    return result;
  }

  public Node<A, S> newRootNode(S initialState, double pathCost) {
    return nodeFactory.newRootNode(initialState, pathCost);
  }

  private Node<A, S> newLeafNode(Problem<A, S> problem, Node<A, S>
      node, A action) {
    return nodeFactory.newChildNode(problem, node, action);
  }

  public Queue<Node<A, S>> newFIFOQueue() {
    return new ConcurrentLinkedQueue<>();
  }

  public Set<S> newExploredSet() {
    return new HashSet<>();
  }

  public List<A> failure() {
    return searchController.failure();
  }

  public List<A> solution(Node<A, S> child) {
    return searchController.solution(child);
  }

  @Override
  public List<A> apply(Problem<A, S> problem) {
    return apply((DefinedGoalStatesProblem<A, S>) problem);
  }

  public void register(StateActionTimeLine<String, String> timeLine) {
    history = timeLine;
  }

  /**
   * Helper class to make use of parallel processing with streams
   */
  private class SearchDirection {
    final Set<S> exploredStates;
    final Queue<Node<A, S>> otherFrontier;
    final Queue<Node<A, S>> frontier;
    final boolean fromStartToGoal;

    /**
     * @param exploredStates      remembers all explored states from this direction
     * @param otherFrontier remembers all explored states from an other direction
     * @param frontier            frontierNodes of the other direction to check against
     * @param fromStartToGoal     indicator if a solution path needs to be reversed
     */
    SearchDirection(Set<S> exploredStates, Queue<Node<A, S>> otherFrontier, Queue<Node<A, S>>
        frontier, boolean fromStartToGoal) {
      this.exploredStates = exploredStates;
      this.otherFrontier = otherFrontier;
      this.frontier = frontier;
      this.fromStartToGoal = fromStartToGoal;
    }
  }
}
