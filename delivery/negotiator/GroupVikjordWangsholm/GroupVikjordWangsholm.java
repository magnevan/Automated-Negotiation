package negotiator.GroupVikjordWangsholm;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.DeadlineType;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

import org.json.simple.JSONObject;

/**
 * This is your negotiation party.
 */
public class GroupVikjordWangsholm extends AbstractNegotiationParty {

    private final Random rng;
    private final Map<Object, GroupVikjordWangsholmOpponentModel> opponentModels;
    private final GroupVikjordWangsholmOfferingStrategy offeringStrategy;
    private final GroupVikjordWangsholmAcceptanceStrategy acceptanceStrategy;
    
    private Writer logWriter;
    
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
    public GroupVikjordWangsholm(
        UtilitySpace utilitySpace,
        Map<DeadlineType, Object> deadlines, 
        Timeline timeline,
        long randomSeed
    ) {

        super(utilitySpace, deadlines, timeline, randomSeed);
        
        rng = new Random(randomSeed);
        opponentModels = new HashMap<>();
        offeringStrategy = new GroupVikjordWangsholmOfferingStrategy(utilitySpace, 0.5, 1.7, 0.05);
        acceptanceStrategy = new GroupVikjordWangsholmAcceptanceStrategy(utilitySpace);
        
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date date = new Date();
        String logName = "OpponentModel-Session_" + dateFormat.format(date) + ".json";
        try {
            logWriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logName), "utf-8")
            );
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            e.printStackTrace();
        }
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
        System.out.println("Entering round " + timeline.getCurrentTime());
        
        // Log what we think the opponents models look like
        /*
        if (this.getPartyId().equals(new AgentID("Party 1"))) {
            JSONObject obj = new JSONObject();
            obj.put("turn", timeline.getCurrentTime());
            opponentModels.forEach(
                (op, model) -> obj.put(op.toString(), model.json())
            );
            try {
                logWriter.write(obj.toJSONString());
                logWriter.write("\n");
                logWriter.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        */
        
        if (!validActions.contains(Accept.class)) {
            // This is the first offer made, so we suggest our best utility.
            return new Offer(offeringStrategy.getInitialBid());
        }
        
        Bid counterOffer = offeringStrategy.generateBid(
            rng, timeline, opponentModels.values()
        );
        
        try { 
            if (acceptanceStrategy.isBidAcceptable(currentBidOffered, counterOffer)) {
                System.out.println("Accepted offer of utility " + utilitySpace.getUtility(currentBidOffered));
                return new Accept();
            } else {
                System.out.printf(
                    "Countered offer of utility %.3f with offer of utility %.3f\n",
                    utilitySpace.getUtility(currentBidOffered),
                    utilitySpace.getUtility(counterOffer)
                );
                currentBidOffered = counterOffer;
                return new Offer(counterOffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Accept();
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
            System.out.println("Received unhandled action " + action.toString());
            return;
        }
        
        if (action instanceof Offer) {
            Offer offer = (Offer) action;
            currentBidOffered = offer.getBid();
        } else if (action instanceof Accept) {
            try {
                System.out.printf("%s accepted offer of utility %.3f\n",
                    sender.toString(),
                    utilitySpace.getUtility(currentBidOffered)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        opponentModels.putIfAbsent(sender, new GroupVikjordWangsholmOpponentModel(utilitySpace.getDomain()));
        
        // TODO: distinguish between accepted and offered? 
        opponentModels.get(sender).addApprovedBid(currentBidOffered);
    }

}
