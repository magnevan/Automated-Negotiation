package negotiator.groupn;

import negotiator.Bid;
import negotiator.utility.UtilitySpace;

public class GroupnAcceptanceStrategy {
    private final UtilitySpace utilitySpace;
    
    public GroupnAcceptanceStrategy(
        UtilitySpace utilitySpace
    ) {
        this.utilitySpace = utilitySpace;
    }
    
    public boolean isBidAcceptable(
        Bid bidOffered,
        Bid counterOffer
    ) {
        try {
            // If the bid is better for us than our potential counteroffer
            // we should (obviously?) accept it.
            if (counterOffer != null
                && utilitySpace.getUtility(bidOffered)
                   >= utilitySpace.getUtility(counterOffer)
            ) {
                return true;
            }
            
            /**
             * TODO: We want some sort of moving target that tells us what bids
             * we should accept given various parameters. The parameters we would
             * like to include are:
             * - time remaining. As we approach the deadline, we should perhaps
             *   be more willing to compromise. However the counteroffer check
             *   above does implicitly do this, so it might not be needed.
             * 
             * - bid history. If the utility of the bids happening seem to be
             *   increasing, we should not accept right away, and wait until
             *   we get a better offer. However, if it seems to be stabilizing
             *   and is within some reasonable value of our target utility,
             *   we should accept.
             */
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
}
