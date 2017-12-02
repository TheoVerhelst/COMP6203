/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package group9;

import java.util.HashMap;
import java.util.Map;
import negotiator.Bid;
import negotiator.issue.Value;

/**
 * Opponent utility estimation model based on ABiNeS and AgentMR ideas.
 */
public class OpponentModel {
    /**
     * Number of time each object of each issue have been proposed so far.
     */
    private final Map<Integer, Map<Value, Integer>> itemCounts = new HashMap<>();
    
    /**
     * Estimated score of each object in each issue. ABiNeS opponent model is
     * used to this end.
     */
    private final Map<Integer, Map<Value, Double>> itemScores = new HashMap<>();
    /**
     * Utility of our agent on the first bid proposed by the opponent.
     */
    private Double firstBidUtility = null;
    
    private double scoreDecayFactor = 4;
    
    public void registerBid(Bid newBid, double ourUtility) {
        if(firstBidUtility == null) {
            firstBidUtility = ourUtility;
        }
        
        // This concession estimator is taken from AgentMR paper.
        double opponentConcession = (ourUtility - firstBidUtility) / (1 - firstBidUtility);
        
        HashMap<Integer, Value> issueValues = newBid.getValues();
        
        for (int issueNumber : issueValues.keySet()) {
            itemCounts.putIfAbsent(issueNumber, new HashMap<>());
            Map<Value, Integer> issueCounts = itemCounts.get(issueNumber);
            itemScores.putIfAbsent(issueNumber, new HashMap<>());
            Map<Value, Double> issueScores = itemScores.get(issueNumber);
            Value value = issueValues.get(issueNumber);
            
            // Initiate the values if they are not already present
            issueScores.putIfAbsent(value, 0.);
            issueCounts.putIfAbsent(value, 0);
            
            // Calculate the new score as described in ABiNeS strategy.
            double newItemScore = issueScores.get(value)
                    + Math.pow(1 - opponentConcession, scoreDecayFactor * issueCounts.get(value));
            
            issueScores.put(value, newItemScore);
            issueCounts.put(value, issueCounts.get(value) + 1);
        }
    }
    
    /**
     * Computes the accumulated frequency as defined in the ABiNeS paper. This
     * gives a score which is not normalized, and is only intended to be
     * compared with other values of this function, in order to sort bids.
     */
    public double getEstimatedScore(Bid bid) {
        double score = 0;
        for(Map.Entry<Integer, Value> bidIssue : bid.getValues().entrySet()) {
                score += itemScores.get(bidIssue.getKey()).getOrDefault(bidIssue.getValue(), 0.);
        }
        return score;
    }

}
