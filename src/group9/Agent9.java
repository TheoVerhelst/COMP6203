package group9;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;

public class Agent9 extends AbstractNegotiationParty {

    private final HashMap<AgentID, ArrayList<Double>> analysisTimeReceivedOffers = new HashMap<>();
    private final List<AbstractNegotiationParty> pokemons = new ArrayList<>();
    private AbstractNegotiationParty chosenPokemon;
    private boolean choiceMade = false;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        pokemons.add(new HardHeaded());
        pokemons.add(new ATriNeS());
        pokemons.add(new Athrines());
        for(AbstractNegotiationParty pokemon : pokemons) {
            pokemon.init(info);
        }
    }

    private void addAnalysisOfffer(AgentID agentID, double receivedUtility) {
        analysisTimeReceivedOffers.putIfAbsent(agentID, new ArrayList<Double>());
        analysisTimeReceivedOffers.get(agentID).add(receivedUtility);
    }

    private void choosePokemon() {
        List<Double> means = new ArrayList<>();
        List<Double> stds = new ArrayList<>();
        for (List<Double> utilities : analysisTimeReceivedOffers.values()) {
            // Compute the mean received from this agent
            double mean = 0;
            for (Double utility : utilities) {
                mean += utility;
            }
            mean /= utilities.size();
            means.add(mean);
            
            // Compute the standard deviation received from this agent
            double std = 0;
            for (Double utility : utilities) {
                std += (utility - mean) * (utility - mean);
            }
            std /= utilities.size();
            stds.add(std);
        }
        
        double[] predictedScores = getPredictedScores(means.get(0), stds.get(0), means.get(1), stds.get(1));
        int bestScoreIndex = -1;
        for(int i = 0; i < predictedScores.length; ++i) {
            if(bestScoreIndex == -1 || predictedScores[bestScoreIndex] < predictedScores[i]) {
                bestScoreIndex = i;
            }
        }
        chosenPokemon = pokemons.get(bestScoreIndex);
        System.out.println(chosenPokemon.toString() + ", I choose you!");
    }
    
    private double[] getPredictedScores(double mean1, double std1, double mean2, double std2) {
        // Those weights have been calculated from testing negotiations and linear regression
        double[][] weights = {{0.0311632723234050 , 11.3890261814731 , 1.37498485260551 , 7.57488885843407 , 0.585245861458856},
                {-0.504747034606162 , 21.3564825802284 , 2.93644981430394 , 11.6484361428377 , -0.119766699076194},
                {-0.526942565179179 , 27.3114207973019 , 2.55112005222630 , 18.9063457493229 , -0.148290860834988}};
        double[] input = {mean1, std1, mean2, std2, 1};
        double[] scores = new double[3];
        for(int i = 0; i < 3; ++i) {
            scores[i] = 0;
            for(int j = 0; j < 5; ++j) {
                scores[i] += input[j] * weights[i][j];
            }
        }
        return scores;
    }

    @Override
    public void receiveMessage(AgentID sender, Action act) {
        double currentTime = getTimeLine().getTime();

        if (currentTime <= 0.1) {
            if (act instanceof Offer) {
                Bid receivedBid = ((Offer) act).getBid();
                addAnalysisOfffer(sender, getUtility(receivedBid));
            }
            for(AbstractNegotiationParty pokemon : pokemons) {
                pokemon.receiveMessage(sender, act);
            }
        } else if (!choiceMade) {
            choosePokemon();
            choiceMade = true;
            chosenPokemon.receiveMessage(sender, act);
        } else {
            chosenPokemon.receiveMessage(sender, act);
        }
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> arg0) {
        double currentTime = getTimeLine().getTime();

        if (currentTime <= 0.1 || !choiceMade) {
            try {
                return new Offer(getPartyId(), utilitySpace.getMaxUtilityBid());
            } catch (Exception e) {
                return new Offer(getPartyId(), generateRandomBid());
            }
        } else {
            return chosenPokemon.chooseAction(arg0);
        }
    }

    public String getName() {
        return "The Pokemon Master";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getDescription() {
        return "Burst the Bubble !";
    }

}
