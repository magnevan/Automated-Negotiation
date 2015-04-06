package negotiator.GroupVikjordWangsholm;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import misc.Range;
import negotiator.Bid;
import negotiator.Timeline;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.utility.UtilitySpace;

public class GroupVikjordWangsholmOfferingStrategy {
    private final SortedOutcomeSpace outcomeSpace;
    
    private final double reserveUtility;
    private final double suggestRange;
    private final double concessionShape;
    
    public GroupVikjordWangsholmOfferingStrategy(
        UtilitySpace utilitySpace, 
        double reserveUtility,
        double concessionShape,
        double suggestRange
    ) {
        if (concessionShape <= 0.0 || concessionShape >= 2.0) {
            throw new IllegalArgumentException("Concession Shape must be between 0.0 and 2.0");
        }
        
        outcomeSpace = new SortedOutcomeSpace(utilitySpace);
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
    public Bid generateBid(Random rng, Timeline time, Collection<GroupVikjordWangsholmOpponentModel> opponentModels) {
        double targetUtility = utilityGoal(time, this.concessionShape, this.reserveUtility);
        Range range = new Range(targetUtility - suggestRange/2.0, targetUtility + suggestRange/2.0);
        if (range.getUpperbound() > 1.0) {
            range.setLowerbound(range.getLowerbound() - (range.getUpperbound() - 1.0));
            range.setUpperbound(1.0);
        }
        List<BidDetails> possibleBids = outcomeSpace.getBidsinRange(range);
        System.out.println("Number of bids in range " + possibleBids.size());
        
        double temperature = rng.nextDouble();
        if (temperature > utilityGoal(time, 0.25, 0.0)) {
            // Select best option
            System.out.println("Suggested best bid");
            
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
            
            System.out.println("Worst opponent utility " + bestMinUtility);
            return bestBid;
            
        } else {
            // Select random bid in range
            System.out.println("Suggested random bid");
            int i = rng.nextInt(possibleBids.size());
            return possibleBids.get(i).getBid();
        }
    }
    
    public Bid getInitialBid() {
        return outcomeSpace.getMaxBidPossible().getBid();
    }
    
    
    /**
     * A function telling us what sort of utility we should aim for at any given time.
     * If the concession shape is between 0.0 and 1.0 we get a conceder type shape,
     * if it is between 1.0 and 2.0 we get a boulware type shape.
     * @param time the current timeline we should base the goal utility on
     * @return a double between 0.0 and 1.0 telling us the utility we should aim to get.
     */
    private double utilityGoal(Timeline time, double concessionShape, double reserveUtility) {
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
