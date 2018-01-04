package group9;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        
        double mean1 = means.get(0), mean2, std1 = stds.get(0), std2;
        // If we play against two identical opponents
        if(means.size() == 1) {
            mean2 = means.get(0);
            std2 = stds.get(0);
        } else {
            mean2 = means.get(1);
            std2 = stds.get(1);
        }
        
        double meanMean = (mean1 + mean2) / 2;
        double meanStd = (std1 + std2) / 2;
        if(meanMean > 0.55) {
            chosenPokemon = pokemons.get(2);
        } else if(meanStd > 0.015) {
            chosenPokemon = pokemons.get(1);
        } else {
            chosenPokemon = pokemons.get(0);
        }
        
        System.out.println(chosenPokemon.toString() + ", I choose you!");
        try {
            Files.write(Paths.get("choosen_pokemon_2"),
                    (chosenPokemon.toString() + ", I choose you!\n").getBytes(),
                    StandardOpenOption.APPEND);
        }catch (IOException ex) {
            Logger.getLogger(Agent9.class.getName()).log(Level.SEVERE, null, ex);
        }
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
