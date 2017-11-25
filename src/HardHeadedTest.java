
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Value;
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
public class HardHeadedTest extends AbstractNegotiationParty {

    private final String description = "HardHeaded test";
    /**
     * Holds the estimation of the model of the opponent, by storing a double
     * for each possible issue value. The outer Map has the opponent ID as key,
     * and their respective model as value. Each opponent model is a Map with
     * the issue number as key, and a Map of frequency for each possible issue
     * item as value.
     */
    private final Map<AgentID, Map<Integer, Map<Value, Integer>>> opponentsModels = new HashMap<>();
    /**
     * Keep track of how many rounds have passed, to compute the frequencies
     * properly.
     */
    private int roundCount = 0;
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
     * The bid on the table.
     */
    private Bid lastReceivedBid;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        additiveUtilitySpace = (AdditiveUtilitySpace) info.getUtilitySpace();
        try {
            Bid minBid = this.additiveUtilitySpace.getMinUtilityBid();
            Bid maxBid = this.additiveUtilitySpace.getMaxUtilityBid();
            minUtility = additiveUtilitySpace.getUtility(minBid);
            maxUtility = additiveUtilitySpace.getUtility(maxBid);
        } catch (Exception ex) {
            Logger.getLogger(HardHeadedTest.class.getName()).log(Level.SEVERE, null, ex);
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
        // Update time-related stuff
        roundCount++;
        double time = getTimeLine().getTime();
        double utilityThreshold = maxUtility
                - (maxUtility - minUtility) * Math.pow(time, 1 / concessionRate);

        // Check if the last bid is above our threshold
        if (lastReceivedBid != null) {
            double lastBidUtility = additiveUtilitySpace.getUtility(lastReceivedBid);
            if (lastBidUtility >= utilityThreshold) {
                return new Accept(this.getPartyId(), lastReceivedBid);
            }
        }

        // Generate a bunch of bids above the threshold
        Set<Bid> setBid = generateBids(utilityThreshold, 10, 10000);
        // Find the best bid according to our model of the opponent
        Bid bestBid = Collections.max(setBid, (Bid lhs, Bid rhs) -> {
            double lhsUtility = getEstimatedOpponentUtility(lhs);
            double rhsUtility = getEstimatedOpponentUtility(rhs);
            return Double.compare(lhsUtility, rhsUtility);
        });
        return new Offer(this.getPartyId(), bestBid);
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

        int spinCount = 0;
        do {
            Bid randomBid = generateRandomBid();
            if (additiveUtilitySpace.getUtility(randomBid) >= threshold) {
                result.add(randomBid);
            }
            spinCount++;
        } while (result.size() < numberOfBids && spinCount < spinLimit);
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

            // Storing last received bid
            lastReceivedBid = bid;

            // Get the data structure of the model of the opponent
            // (and create one if it does not exist)
            Map<Integer, Map<Value, Integer>> senderModel
                    = opponentsModels.getOrDefault(sender, new HashMap<>());

            // Get the list of issues and values associated in the proposed bid
            HashMap<Integer, Value> issueValues = bid.getValues();
            // Iterate on each issue, and update the frequency of the offered value
            for (int issueNumber : issueValues.keySet()) {
                Map<Value, Integer> issueModel = senderModel.getOrDefault(issueNumber, new HashMap<>());
                // Get the count of the given item
                int valueCount = issueModel.getOrDefault(issueValues.get(issueNumber), 0);
                // Update it, by incrementing it
                issueModel.put(issueValues.get(issueNumber), valueCount + 1);
            }
        }
    }

    /**
     * Calculates an estimation of the utility of the opponents on a bid.
     * @param bid The bid to evaluate.
     * @return The average of the estimated utility of the opponents on this bid.
     */
    private double getEstimatedOpponentUtility(Bid bid) {
        double estimatedUtility = 0;
        for (AgentID agentID : opponentsModels.keySet()) {
            Map<Integer, Map<Value, Integer>> agentModel = opponentsModels.get(agentID);

            HashMap<Integer, Value> issueValues = bid.getValues();
            for (int issueNumber : issueValues.keySet()) {
                estimatedUtility += agentModel.get(issueNumber).get(issueValues.get(issueNumber));
            }
        }
        // divide by the round count to obtain proper frequencies
        estimatedUtility /= roundCount;
        // divide by number of issues, since we don't know the weights
        // (thus assuming equal weight for every issue)
        estimatedUtility /= additiveUtilitySpace.getDomain().getIssues().size();
        
        // and divide by the number of opponents
        return estimatedUtility / opponentsModels.size();
    }

    /**
     * A human-readable description for this party.
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * @return the bid with the maximum possible utility, if it exists.
     */
    private Bid getMaxUtilityBid() {
        try {
            return this.additiveUtilitySpace.getMaxUtilityBid();
        } catch (Exception ex) {
            Logger.getLogger(HardHeadedTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
