package group9;

import java.util.*;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;

/**
 * Created by Michael on 17/11/2017.
 */
public class ATriNeS extends AbstractNegotiationParty {
    private final String description = "Multilateral implementation of ABiNeS";

    private final HashMap<AgentID, AgentActionHistory> history = new HashMap<>();

    /**
     * Determines how high we set our threshold based on that of our opponents.
     */
    private static final float THRESHOLD_GREED = 1.1f;

    /**
     * Determines the rate at which the threshold decays over time.
     * Threshold decays linearly from 1 to 1 - (1 / THRESHOLD_TIME_DECAY)
     */
    private static final float THRESHOLD_TIME_DECAY = 10f;

    private static final float CONCEDE_TIME = 0.95f;

    private static final float CONCEDE_MIN = 0.5f;

    private static final float MAX_BID_SEARCH_TIME = 0.002f;

    private static final float IDLE_BID_SEARCH_TIME = 0.0003f;

    private static final float MAX_ACCEPTED_BID_WEIGHT = 6f;

    private static final float MAX_REJECTED_BID_WEIGHT = 3f;

    private static final float CURVE_VALUE = 10f;

    private float acceptThreshold = 1f;

    private AgentID _opp1;
    private AgentID _opp2;

    private Action _opp1LastAction = null;
    private Action _opp2LastAction = null;

    private Bid lastReceivedOffer;

    private int _round;



    @Override
    public void init(NegotiationInfo info) {
        super.init(info);

        System.out.println("Init");
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {

        _round += 1;
        
        // Simulate the first 10% of testing
        double time = getTimeLine().getTime();
        if(time <= 0.1) {
            return new Offer(this.getPartyId(), getMaxUtilityBid());
        }

        //System.out.println("----------------------------------");
        //System.out.println("Round: " + _round);

        // Offer our best bid as the first bid, until we have information about our opponents.
        if(_opp1LastAction == null || _opp2LastAction == null) {
            return new Offer(this.getPartyId(), this.getMaxUtilityBid());
        }

        AgentActionHistory history1 = history.get(_opp1);
        AgentActionHistory history2 = history.get(_opp2);

        // Calculate acceptance thresholds.
        float opp1Threshold = history1.GetPredictedThreshold();
        float opp2Threshold = history2.GetPredictedThreshold();
        acceptThreshold = CalculateAcceptThreshold(opp1Threshold, opp2Threshold, this.timeline.getTime());

        //System.out.println(opp1Threshold + ", " + opp2Threshold + ", " + acceptThreshold);
        //history1.accumulatedFrequency.printFrequency();
        //history1.accumulatedFrequency.printCount();
        //history2.accumulatedFrequency.printFrequency();
        //history2.accumulatedFrequency.printCount();


        // Accept the bid if it meets our acceptance threshold.
        // TODO improve this.
        if(this.utilitySpace.getUtility(lastReceivedOffer) > acceptThreshold) {
            return new Accept(this.getPartyId(), lastReceivedOffer);
        }

        // Generate a set of new bids better than our acceptanceThreshold.
        HashSet<Bid> bids = GetBidsAboveThreshold(acceptThreshold);

        // Choose the best bid based on the CalculatedBidValue
        Bid bid = ChooseBid(bids, opp1Threshold, opp2Threshold);

        //System.out.println("Found " + bids.size() + " bids.");
        //System.out.println(bid);
        //System.out.println(history1.accumulatedFrequency.GetPredictedUtility(bid) + ", " + history2.accumulatedFrequency.GetPredictedUtility(bid) + ", " + this.getUtility(bid));
        //System.out.println("----------------------------------");

        return new Offer(this.getPartyId(), bid);
    }

    /**
     * Calculate our acceptance threshold based on the predicted thresholds of our opponents. Our threshold should be
     * greater than the maximum threshold of our opponents, although it should also decay over time if our opponents thresholds
     * remain to high.
     * @param opp1Threshold
     * @param opp2Threshold
     * @param time
     * @return
     */
    public float CalculateAcceptThreshold(float opp1Threshold, float opp2Threshold, double time) {
        float threshold = Math.max(opp1Threshold, opp2Threshold) * THRESHOLD_GREED;


        float timeAdjusted;

        if(time > CONCEDE_TIME) {
            float y1 = 1 - (CONCEDE_TIME / THRESHOLD_TIME_DECAY);
            float x1 = CONCEDE_TIME;

            float y2 = CONCEDE_MIN;
            float x2 = 1;

            float grad = (y2 - y1) / (x2 - x1);

            float ans = grad * ((float) time - x1) + y1;
            System.out.println(ans);

            timeAdjusted = Math.min(threshold, ans);
        } else {
            timeAdjusted = Math.min(threshold, 1 - ((float) time / THRESHOLD_TIME_DECAY));
        }

        return timeAdjusted;
    }

    /**
     * Get a set of bids that are above a threshold.
     * @param threshold
     * @return
     */
    public HashSet<Bid> GetBidsAboveThreshold(float threshold) {
        HashSet<Bid> bids = new HashSet<>();
        bids.add(getMaxUtilityBid());

        double startTime = this.timeline.getTime();
        double endTime = startTime + MAX_BID_SEARCH_TIME;

        double lastBidFoundTime = startTime;

        while(this.timeline.getTime() < endTime && this.timeline.getTime() - lastBidFoundTime < IDLE_BID_SEARCH_TIME) {
            Bid bid = generateRandomBid();

            if(bids.contains(bid))
                continue;

            float utility = (float) this.utilitySpace.getUtility(bid);

            if(utility >= threshold) {
                bids.add(bid);
                lastBidFoundTime = this.timeline.getTime();
            }

            if(this.timeline.getTime() >= 1)
                return bids;
        }

        return bids;
    }

    /**
     * Chooses the best bid from a set of bids. Based on the CalculatedBidValue()
     * @param bids
     * @param opp1Threshold
     * @param opp2Threshold
     * @return
     */
    public Bid ChooseBid(HashSet<Bid> bids, float opp1Threshold, float opp2Threshold) {
        float value = 0f;
        Bid bestBid = getMaxUtilityBid();

        for(Bid bid : bids) {
            float bidValue = CalculateBidValue(bid, opp1Threshold, opp2Threshold);

            if(bidValue < value)
                continue;

            value = bidValue;
            bestBid = bid;
        }

        return bestBid;
    }

    /**
     * Calculates a value for a bid based on our utility and the predicted utility of our opponents.
     * @param bid
     * @param opp1Threshold
     * @param opp2Threshold
     * @return
     */
    public float CalculateBidValue(Bid bid, float opp1Threshold, float opp2Threshold) {
        AgentActionHistory opp1History = history.get(_opp1);
        AgentActionHistory opp2History = history.get(_opp2);

        float opp1Utility = opp1History.accumulatedFrequency.GetPredictedUtility(bid);
        float opp2Utility = opp2History.accumulatedFrequency.GetPredictedUtility(bid);
        float ourUtility = (float) this.utilitySpace.getUtility(bid);

        float opp1DiffValue = CalculateDiffValue(opp1Utility - opp1Threshold);
        float opp2DiffValue = CalculateDiffValue(opp2Utility - opp2Threshold);
        float variance = rand.nextFloat() * (float) (1 - this.timeline.getTime());
        float ourDiffValue = ourUtility - acceptThreshold;


        float ans = ourDiffValue + opp1DiffValue + opp2DiffValue;

        return ans;
    }

    public float CalculateDiffValue(float diff) {
        if(diff >= 0)
            return 1 / ((CURVE_VALUE * diff) + (1 / MAX_ACCEPTED_BID_WEIGHT));
        else
            return 1 / ((-CURVE_VALUE * diff) + (1 / MAX_REJECTED_BID_WEIGHT));
    }

    @Override
    public void receiveMessage(AgentID sender, Action act) {
        super.receiveMessage(sender, act);

        if(sender == null)
            return;

        if(act instanceof Offer)
            lastReceivedOffer = ((Offer) act).getBid();

        if(_opp1 == null) {
            _opp1 = sender;
        }

        if(_opp2 == null && _opp1 != sender) {
            _opp2 = sender;
        }

        // If sender of message doesn't have a history create one.
        if(!history.containsKey(sender))
            history.put((sender), new AgentActionHistory(sender, this.utilitySpace.getDomain()));

        // Add the action of the sender to their history.
        history.get(sender).AddAction(act, (float) this.timeline.getTime());

        if(sender == _opp1) {
            _opp1LastAction = act;
        } else if (sender == _opp2) {
            _opp2LastAction = act;
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    private Bid getMaxUtilityBid() {
        try {
            return this.utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
