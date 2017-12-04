package group9;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;
import negotiator.utility.AdditiveUtilitySpace;


/**
 * Bargaining agent with simple boulware strategy for multilateral negotiation.
 * It keeps track of the frequency of the items the opponents propose, as rough
 * estimate of their utility function. It has a simple boulware acceptance
 * threshold. It tries to generate randomly a bunch of bids having an utility
 * greater than the threshold. If it cannot find such bid, it proposes the
 * maximum utility bid. Otherwise, it proposes the bid which maximises the
 * average of estimated opponent utilities.
 */
public class HardHeaded extends AbstractNegotiationParty {

    private final String description = "HardHeaded";
    /**
     * Holds the estimation of the profile of the opponents. See OpponentModel
     * class for further explanations.
     */
    private final Map<AgentID, OpponentModelTheo> opponentsModels = new HashMap<>();
    /**
     * Allows access to information about our negotiation.
     */
    private AdditiveUtilitySpace additiveUtilitySpace;
    /**
     * Utility of the best possible bid.
     */
    private double minUtility;
    /**
     * Utility of the worst possible bid.
     */
    private double maxUtility;
    /**
     * The rate that defines our conceding strategy.
     */
    private final double concessionRate = 0.3;
    /**
     * Probability of choosing a random bid instead of the best according to
     * opponent model.
     */
    private final double epsilon = 0.05;
    /**
     * Random number generator, for using epsilon-greedy algorithm.
     */
    private final Random randomGenerator = new Random(); 
    /**
     * The bid on the table.
     */
    private Bid lastReceivedBid;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        additiveUtilitySpace = (AdditiveUtilitySpace) info.getUtilitySpace();
        try {
            Bid maxBid = this.additiveUtilitySpace.getMaxUtilityBid();
            maxUtility = additiveUtilitySpace.getUtility(maxBid);
            Bid minBid = this.additiveUtilitySpace.getMinUtilityBid();
            minUtility = additiveUtilitySpace.getUtility(minBid);
        } catch (Exception ex) {
            Logger.getLogger(Agent9.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * When this function is called, it is expected that the Party chooses one
     * of the actions from the possible action list and returns an instance of
     * the chosen action.
     *
     * @param list A list of our possible actions, namely Accept, Offer and
     * EndNegotiation.
     * @return The chosen action.
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        // Simulate the first 10% of testing
        double time = getTimeLine().getTime();
        if(time <= 0.1) {
            return new Offer(this.getPartyId(), getMaxUtilityBid());
        }
        
        double utilityThreshold = getUtilityThreshold();

        // Check if the last bid is above our threshold
        if (lastReceivedBid != null) {
            double lastBidUtility = additiveUtilitySpace.getUtility(lastReceivedBid);
            if (lastBidUtility >= utilityThreshold) {
                return new Accept(this.getPartyId(), lastReceivedBid);
            }
        }

        // Generate a bunch of bids above the threshold
        Set<Bid> bidSet = generateBids(utilityThreshold, 30, 10000);
        
        // Epsilon-greedy: with probability eps, we send a random acceptable offer
        if(randomGenerator.nextDouble() <= epsilon) {
            return new Offer(this.getPartyId(), takeRandomBid(bidSet));
        } else {
            // Else, find the best bid according to our model of the opponent
            Bid bestBid = Collections.max(bidSet, (Bid lhs, Bid rhs) -> 
                Double.compare(getOpponentScore(lhs), getOpponentScore(rhs))
            );
            return new Offer(this.getPartyId(), bestBid);
        }
    }
    
    private double getUtilityThreshold() {
        return maxUtility - (maxUtility - minUtility)
                * Math.pow(getTimeLine().getTime(), 1 / concessionRate);

    }

    /** 
     * Generates a set of bids with utility above a given threshold. The function
     * stops after a certain number of iteration, to avoid spinning until the
     * end of the negotiation.
     * @param threshold The utility threshold
     * @param numberOfBids The desired number of bids above this threshold in
     * the result.
     * @param spinLimit The maximum number of iterations.
     * @return A set that, hopefully, contains numberOfBids bids having a utility
     * above the threshold.
     */
    private Set<Bid> generateBids(double threshold, int numberOfBids, int spinLimit) {
        Set<Bid> result = new HashSet<>();
        // Fail-safe: we ensure that we at least always propose our max utility bid
        result.add(getMaxUtilityBid());
        // If the threshold is not within the possible limits
        if (threshold > maxUtility || threshold < minUtility) {
            return result;
        }

        // If we spend 1/10 of the allowed time without finding anything new, then stop
        int deadSpinLimit = spinLimit / 10;
        int spinCount = 0;
        int deadSpinCount = 0;
        do {
            Bid randomBid = generateRandomBid();
            if (additiveUtilitySpace.getUtility(randomBid) >= threshold) {
                if(!result.contains(randomBid)) {
                    deadSpinCount = -1;
                }
                result.add(randomBid);
            }
            spinCount++;
            deadSpinCount++;
        } while (result.size() < numberOfBids && spinCount < spinLimit && deadSpinCount < deadSpinLimit);
        return result;
    }

    /**
     * This method is called to inform the party that another NegotiationParty
     * chose an Action.
     */
    @Override
    public void receiveMessage(AgentID sender, Action act) {
        super.receiveMessage(sender, act);
        if (act instanceof Offer) {
            Bid bid = ((Offer) act).getBid();
            opponentsModels.putIfAbsent(sender, new OpponentModelTheo());
            opponentsModels.get(sender).registerBid(bid, getUtility(bid));

            // Storing last received bid
            lastReceivedBid = bid;
        }
    }

    /**
     * Calculates a score representing the preferences of the opponents on a bid.
     * @param bid The bid to evaluate.
     * @return An estimated measure of preference of the opponents on this bid.
     */
    private double getOpponentScore(Bid bid) {
        double score = 0;
        for(OpponentModelTheo model : opponentsModels.values()) {
            score += model.getEstimatedScore(bid);
        }
        return score;
    }

    /**
     * A human-readable description for this party.
     */
    @Override
    public String getDescription() {
        return description;
    }
	
    @Override
    public String toString() {
        return description;
    }

    /**
     * @return the bid with the maximum possible utility, if it exists.
     */
    private Bid getMaxUtilityBid() {
        try {
            return this.additiveUtilitySpace.getMaxUtilityBid();
        } catch (Exception ex) {
            Logger.getLogger(Agent9.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private Bid takeRandomBid(Set<Bid> bidSet) {
        List<Bid> list = new ArrayList<Bid>(bidSet.size());
        list.addAll(bidSet);
        Collections.shuffle(list);
        return list.get(0);
    }
}