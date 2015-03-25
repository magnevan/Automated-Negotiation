package negotiator.groupn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import negotiator.Bid;
import negotiator.DeadlineType;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class Groupn extends AbstractNegotiationParty {

    private final Random rng;
    private final Map<Object, GroupnOpponentModel> opponentModels;
    private final GroupnOfferingStrategy offeringStrategy;
    private final GroupnAcceptanceStrategy acceptanceStrategy;
    
    // Since the calls to chooseAction asks us if we want to accept a bid,
    // but doesn't supply the bid, we have to look at the bids received and
    // remember the most recent one.
    private Bid currentBidOffered;
    
    /**
     * Please keep this constructor. This is called by genius.
     *
     * @param utilitySpace
     *            Your utility space.
     * @param deadlines
     *            The deadlines set for this negotiation.
     * @param timeline
     *            Value counting from 0 (start) to 1 (end).
     * @param randomSeed
     *            If you use any randomization, use this seed for it.
     */
    public Groupn(
        UtilitySpace utilitySpace,
        Map<DeadlineType, Object> deadlines, 
        Timeline timeline,
        long randomSeed
    ) {

        super(utilitySpace, deadlines, timeline, randomSeed);
        
        rng = new Random(randomSeed);
        opponentModels = new HashMap<>();
        offeringStrategy = new GroupnOfferingStrategy(utilitySpace, 0.2, 1.5, 0.1);
        acceptanceStrategy = new GroupnAcceptanceStrategy(utilitySpace);
    }

    /**
     * Each round this method gets called and ask you to accept or offer. The
     * first party in the first round is a bit different, it can only propose an
     * offer.
     *
     * @param validActions
     *            Either a list containing both accept and offer or only offer.
     * @return The chosen action.
     */
    @Override
    public Action chooseAction(List<Class> validActions) {
        if (!validActions.contains(Accept.class)) {
            // This is the first offer made, so we suggest our best utility.
            return new Offer(offeringStrategy.getInitialBid());
        }
        
        Bid counterOffer = offeringStrategy.generateBid(
            rng, timeline, opponentModels.values()
        );
        
        if (acceptanceStrategy.isBidAcceptable(currentBidOffered, counterOffer)) {
            return new Accept();
        } else {
            return new Offer(counterOffer);
        }
    }

    /**
     * All offers proposed by the other parties will be received as a message.
     * You can use this information to your advantage, for example to predict
     * their utility.
     *
     * @param sender
     *            The party that did the action.
     * @param action
     *            The action that party did.
     */
    @Override
    public void receiveMessage(Object sender, Action action) {
        super.receiveMessage(sender, action);
        
        if (!(action instanceof Offer) && !(action instanceof Accept)) {
            throw new IllegalArgumentException(
                "action received was " + action.toString()
                + " master never taught me how to deal with this :("
            );
        }
        
        if (action instanceof Offer) {
            Offer offer = (Offer) action;
            currentBidOffered = offer.getBid();
        }
        
        opponentModels.putIfAbsent(sender, new GroupnOpponentModel(utilitySpace.getDomain()));
        
        // TODO: distinguish between accepted and offered? 
        opponentModels.get(sender).addApprovedBid(currentBidOffered);
    }

}
