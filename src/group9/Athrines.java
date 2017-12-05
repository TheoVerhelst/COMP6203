package group9;

import java.util.Random;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.ValueDiscrete;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.EvaluatorDiscrete;


public class Athrines extends AbstractNegotiationParty {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3357221153268870466L;
	
	private AdditiveUtilitySpace utilitySpace;
	private Action actionOfPartner;
	private Bid lastReceivedBid;

	private int rounds;
	private double maxUtility;	
	private double acceptanceThreshold;
	private double selfishWeight;
	
	private static final double EPSILON = 0.15;
	private static final double EPS_MIN_ACCEPTANCE = 0.885;
	
	private static final double SELFISH_WEIGHT_HARD = 3.0;
	private static final double SELFISH_WEIGHT_SOFT = 2.5;
	
	private static final double POINTE_AMA = 0.85;
	private static final double MIN_OVERALL_UTILITY = 0.825;
	private static final double MIN_BUBBLE_UTILITY = 0.875;
	
	private static final double UCT_OFFSET = 25.0;
	private static final double UCT_SCALER = 17.5;
	
	private static final double FREQ_MIN = 1.0;
	private static final double FREQ_INTERCEPT = 3.5;
	private static final double FREQ_DIVIDER = 50;
	private static final double FREQ_BIAS = 0.25;
	
	private ArrayList<IssueDiscrete> issues;
	private OpponentModel opponentModel;
	
	
	@Override
	public void init(NegotiationInfo info) {
		super.init(info);
		rounds = 0;
		selfishWeight = SELFISH_WEIGHT_HARD;
		actionOfPartner = null;
		lastReceivedBid = null;
	
		utilitySpace = (AdditiveUtilitySpace)info.getUtilitySpace();
		
		try {
			maxUtility = getUtility(utilitySpace.getMaxUtilityBid());
		} catch (Exception e) {
			maxUtility = 1.01;
		}
		acceptanceThreshold = maxUtility;
		
		// initialize array of issues for this domain
		ArrayList<Issue> rawIssues = (ArrayList<Issue>) utilitySpace.getDomain().getIssues();
		issues = new ArrayList<IssueDiscrete>();
		for(Issue rawIssue : rawIssues)
		{
			issues.add((IssueDiscrete)rawIssue);
		}
		
		// initialize opponent preferences
		opponentModel = new OpponentModel(issues);
	}


	@Override
	public void receiveMessage(AgentID sender, Action act) {
		actionOfPartner = act;
		if (actionOfPartner instanceof Offer) {
			lastReceivedBid = ((Offer) actionOfPartner).getBid();
			
			// update the opponent model
			opponentModel.updateFrequencies(lastReceivedBid, sender, getFrequencyWeight());
			opponentModel.updatePreferences();
		}
		
		double t = getTimeLine().getTime();
		
		// update the selfish weight
		selfishWeight = SELFISH_WEIGHT_HARD - t * (SELFISH_WEIGHT_HARD - SELFISH_WEIGHT_SOFT);
		
		// update the acceptance threshold
		if(t < POINTE_AMA)
		{
			acceptanceThreshold = maxUtility - (t / POINTE_AMA) * (maxUtility - MIN_OVERALL_UTILITY);
		}
		else
		{
			acceptanceThreshold = MIN_OVERALL_UTILITY + (t - POINTE_AMA) / (1 - POINTE_AMA) * (MIN_BUBBLE_UTILITY - MIN_OVERALL_UTILITY);
		}
		
	}
	
	
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions) {

		double t = getTimeLine().getTime();
		if(t > 0.1)
		{
			rounds++;
			
			// we are first to act
			if (lastReceivedBid == null || !possibleActions.contains(Accept.class)) {
				// propose maximum utility for us !!!
				try {
					return new Offer(getPartyId(), utilitySpace.getMaxUtilityBid());
				} catch (Exception e){
					return new Offer(getPartyId(), generateRandomBid());
				}
			} 
			else {
				// verify if the offer is good enough
				if(getUtility(lastReceivedBid) > acceptanceThreshold)
				{
					return new Accept(getPartyId(), lastReceivedBid);
				}
		
				// epsilon-greedy approach
				Random rand = new Random();
		        double randNr = rand.nextDouble();
				
		        if(randNr < EPSILON)
		        {
		        	// propose random bid
		        	return new Offer(getPartyId(), offerRandom(10));
		        }
		        else
		        {
					// propose the bid that maximizes the objective
					return new Offer(getPartyId(), bestOffer());
		        }
			}
		}
		else
		{
			try {
				return new Offer(getPartyId(), utilitySpace.getMaxUtilityBid());
			} catch (Exception e) {
				return new Offer(getPartyId(), generateRandomBid());
			}
		}
		
	}

	
	private Bid offerRandom(int bidsNr)
	{
		HashSet<Bid> generatedBids = new HashSet<Bid>();
		
		// generate random bids
		long startTime = System.nanoTime(); 
		while(generatedBids.size() < bidsNr && (System.nanoTime() - startTime < 400000000L))
		{
			Bid randomBid = generateRandomBid();
			if(getUtility(randomBid) >= Math.min(acceptanceThreshold, EPS_MIN_ACCEPTANCE))
			{
				generatedBids.add(randomBid);
			}
		}
		
		// select the one which maximizes our opponents' score
		Bid bestBid = null;
		double maxScore = 0;
		for(Bid b : generatedBids)
		{
			double bidScore = 0;
			for(IssueDiscrete i : issues)
			{
				ValueDiscrete v = (ValueDiscrete)b.getValue(i.getNumber());
				for(Entry<AgentID, Double> e : opponentModel.getValuePrefs().get(i).get(v).entrySet())
				{
					bidScore += opponentModel.getIssuePrefs().get(i).get(e.getKey()) * e.getValue();
				}
			}
			
			if(bidScore > maxScore)
			{
				bestBid = b;
				maxScore = bidScore;
			}
		}
		
		if(bestBid != null)
		{
			return bestBid;
		}
		else
		{
			return bestOffer();
		}
	}
	
	
	private Bid bestOffer() {
		
		Bid bestBid = generateRandomBid();
		EvaluatorDiscrete issueEval;
		double uncertainty = getUncertainty();
				
		for(IssueDiscrete i : issues)
		{
			double maxValueScore = 0;
			issueEval = (EvaluatorDiscrete)utilitySpace.getEvaluator(i.getNumber());
			
			for(ValueDiscrete v : i.getValues())
			{
				double myScore = issueEval.getWeight() * issueEval.getDoubleValue(v);
				double opScore = 0;
				for(Entry<AgentID, Double> e : opponentModel.getValuePrefs().get(i).get(v).entrySet())
				{
					opScore += opponentModel.getIssuePrefs().get(i).get(e.getKey()) * e.getValue();
				}
				
				double finalScore = selfishWeight * myScore + uncertainty * opScore;
				if(finalScore > maxValueScore)
				{
					bestBid = bestBid.putValue(i.getNumber(), v);
					maxValueScore = finalScore;
				}
			}
		}
		return bestBid;
	}
	
	private double getFrequencyWeight() {
		return Math.max(FREQ_INTERCEPT - Math.log((double)rounds / FREQ_DIVIDER + FREQ_BIAS), FREQ_MIN);
	}

	private double getUncertainty() {
		return 1 / (1 + Math.exp(-(rounds - UCT_OFFSET) / UCT_SCALER));
	}
	
	public String getName() {
		return "Athrines";
	}	
	
	@Override
	public String toString() {
		return "Athrines";
	}

	@Override
	public String getDescription() {
		return "Burst the Bubble !";
	}

}
