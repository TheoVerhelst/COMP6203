package group9;

import group9.AccumulatedFrequency;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Domain;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michael on 17/11/2017.
 */
public class AgentActionHistory {
    public AgentID AgentID;
    public List<Action> Actions;

    public AccumulatedFrequency accumulatedFrequency;

    static float THRESHOLD_MOVING_AVERAGE = 20;

    public AgentActionHistory(AgentID agentID, Domain domain) {
        AgentID = agentID;
        Actions = new ArrayList<>();
        accumulatedFrequency = new AccumulatedFrequency(domain);
    }

    public void AddAction(Action act, float time) {
        Actions.add(act);

        if(act instanceof Offer) {
            Offer offer = (Offer) act;

            accumulatedFrequency.AddBid(offer.getBid(), time);
        }
    }

    public float GetPredictedThreshold() {
        List<Action> lastNActions = Actions.subList((int) Math.max(0, Actions.size() - THRESHOLD_MOVING_AVERAGE), Actions.size());

        float average = 0;

        lastNActions.sort((o1, o2) -> {
            Bid bid1 = null;

            if(o1 instanceof Offer) {
                bid1 = ((Offer) o1).getBid();
            } else if (o1 instanceof Accept) {
                bid1 = ((Accept) o1).getBid();
            }

            Bid bid2 = null;

            if(o2 instanceof Offer) {
                bid2 = ((Offer) o2).getBid();
            } else if (o2 instanceof Accept) {
                bid2 = ((Accept) o2).getBid();
            }


            return (int) (accumulatedFrequency.GetPredictedUtility(bid1) - accumulatedFrequency.GetPredictedUtility(bid2));
        });

        List<Action> lowestBids = lastNActions.subList(Math.max(0, lastNActions.size() - 5), lastNActions.size());


        for(Action act : lowestBids) {
            Bid actBid = null;

            if(act instanceof Offer) {
                actBid = ((Offer) act).getBid();
            } else if (act instanceof Accept) {
                actBid = ((Accept) act).getBid();
            }

            if(actBid == null)
                continue;

            average += accumulatedFrequency.GetPredictedUtility(actBid);
        }

        return average / lowestBids.size();
    }
}
