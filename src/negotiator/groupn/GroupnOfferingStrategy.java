package negotiator.groupn;

import java.util.List;
import java.util.Random;

import misc.Range;
import negotiator.Bid;
import negotiator.Timeline;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.utility.UtilitySpace;

public class GroupnOfferingStrategy {
    private final SortedOutcomeSpace outcomeSpace;
    private final List<GroupnOpponentModel> opponentModels;
    
    private final double reserveUtility;
    private final double suggestRange;
    private final double concessionShape;
    
    public GroupnOfferingStrategy(
        UtilitySpace utilitySpace, 
        List<GroupnOpponentModel> opponentModels,
        double reserveUtility,
        double concessionShape,
        double suggestRange
    ) {
        if (concessionShape <= 0.0 || concessionShape >= 2.0) {
            throw new IllegalArgumentException("Concession Shape must be between 0.0 and 2.0");
        }
        
        outcomeSpace = new SortedOutcomeSpace(utilitySpace);
        this.opponentModels = opponentModels;
        this.reserveUtility = reserveUtility;
        this.suggestRange = suggestRange;
        this.concessionShape = concessionShape;
    }
    
    /**
     * Generates an appropriate bid given the current time.
     * 
     * First we take a selection of possible bids, that satisfy our current utilitygoal,
     * which moves from 1.0 to reserve over the course of the bidding.
     * Then, taking a cue from simulated annealing we either pick a random bid within that range,
     * or the "best" bid. Where we define the best as being the one where the lowest utility value
     * among our opponents is maximized.
     * TODO: instead of maximizing the lowest utility value, maybe we should look at utility values
     *       each agent has previously accepted, and maxmize the the distance from the lowest accepted so far.
     * 
     * @param rng a random generator supplied with the seed given to this agent at initialization.
     * @param time the current timeline.
     * @param opponentModels models for all the opponents of the agent.
     * @return
     */
    public Bid generateBid(Random rng, Timeline time, List<GroupnOpponentModel> opponentModels) {
        double temperature = rng.nextDouble();
        
        double targetUtility = utilityGoal(time);
        Range range = new Range(targetUtility - suggestRange/2.0, targetUtility + suggestRange/2.0);
        List<BidDetails> possibleBids = outcomeSpace.getBidsinRange(range);
        
        if (temperature < time.getTime()) {
            // Select best option
            Bid bestBid = null;
            double bestMinUtility = 0.0;
            for (BidDetails bid : possibleBids) {
                double worstOpponentUtility = 
                    opponentModels.stream()
                        .mapToDouble(model -> model.evaluateBid(bid.getBid()))
                        .min().getAsDouble();
                if (worstOpponentUtility > bestMinUtility) {
                    bestBid = bid.getBid();
                    bestMinUtility = worstOpponentUtility;
                }
            }
            return bestBid;
        } else {
            int i = rng.nextInt(possibleBids.size());
            return possibleBids.get(i).getBid();
        }
    }
    
    
    
    /**
     * A function telling us what sort of utility we should aim for at any given time.
     * If the concession shape is between 0.0 and 1.0 we get a conceder type shape,
     * if it is between 1.0 and 2.0 we get a boulware type shape.
     * @param time the current timeline we should base the goal utility on
     * @return a double between 0.0 and 1.0 telling us the utility we should aim to get.
     */
    private double utilityGoal(Timeline time) {
        double utility;
        
        // getTime returns a double between 0.0 (start) and 1.0 (end)
        if (concessionShape < 1.0) {
            utility = 1.0 - Math.pow(time.getTime(), concessionShape);
        } else {
            utility = 1.0 - Math.pow(time.getTime(), 1.0 / (2.0 - concessionShape));
        }
        
        return reserveUtility + (1.0 - reserveUtility) * utility;
    }
}
