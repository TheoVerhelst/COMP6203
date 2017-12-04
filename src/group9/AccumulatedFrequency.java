package group9;

import negotiator.Bid;
import negotiator.Domain;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;

import java.util.*;

/**
 * Created by Michael on 17/11/2017.
 */
public class AccumulatedFrequency {
    private HashMap<Value, Integer>[] _bidValueCount;
    private HashMap<Value, Float>[] _accumulatedFrequency;
    private HashSet<Bid> _countedBids;

    private static final float LEARNING_CURVE = 0.1f;

    private float _uniqueBids;
    private float _totalBids;

    private Integer _issueCount;

    private Domain _domain;

    public AccumulatedFrequency(Domain domain) {
        _domain = domain;

        _issueCount = domain.getIssues().size();

        _bidValueCount = new HashMap[_issueCount];
        _accumulatedFrequency = new HashMap[_issueCount];
        _countedBids = new HashSet<>();

        List<Issue> issues = domain.getIssues();

        for(int i = 0; i < _issueCount; i++) {
            _bidValueCount[i] = new HashMap<>();
            _accumulatedFrequency[i] = new HashMap<>();

            IssueDiscrete issue = (IssueDiscrete) issues.get(i);
            List<ValueDiscrete> values = issue.getValues();

            for(Value v : values) {
                _bidValueCount[i].put(v, 0);
                _accumulatedFrequency[i].put(v, 1f);
            }
        }
    }

    public void AddBid(Bid bid, float time) {
        _totalBids += 1;

        if(_countedBids.contains(bid))
            return;

        _countedBids.add(bid);
        _uniqueBids += 1;

        for(int i = 0; i < _issueCount; i++) {
            Value v = bid.getValue(i + 1);

            int newCount = _bidValueCount[i].get(v) + 1;
            _bidValueCount[i].put(v, newCount);

            float oldFrequency = _accumulatedFrequency[i].get(v);

            //float learningValue = 1f / ((_uniqueBids * LEARNING_CURVE) + 1f);

            float L = 1;
            float k = -0.02f;
            float mid = 100;

            float learningValue = (float) (L / (1f + Math.exp(-k * (_uniqueBids - mid))));

            float newFrequency = oldFrequency + (float) Math.pow(learningValue, newCount);
            _accumulatedFrequency[i].put(v, newFrequency);
        }
    }

    public Bid GetPredictedBestBid() {
        HashMap<Integer, Value> bidP = new HashMap<>();

        for(int i = 0; i < _issueCount; i++) {
            HashMap<Value, Float> valueFrequency = _accumulatedFrequency[i];

            float maxFrequency = 0;
            Value value = null;

            for(Map.Entry<Value, Float> e : valueFrequency.entrySet()) {
                if(e.getValue() > maxFrequency) {
                    maxFrequency = e.getValue();
                    value = e.getKey();
                }
            }

            bidP.put(i + 1, value);
        }

        return new Bid(_domain, bidP);
    }

    public float GetPredictedUtility(Bid bid) {
        Bid best = GetPredictedBestBid();

        float uMax = GetTotalFrequency(best);
        float uBid = GetTotalFrequency(bid);

        float utility = uBid / uMax;

        return utility;
    }

    private float GetTotalFrequency(Bid bid) {
        float sum = 0;

        try {
            for (int i = 0; i < _issueCount; i++) {
                Value v = bid.getValue(i + 1);
                sum += _accumulatedFrequency[i].get(v);
            }
        } catch (Exception e) {
            System.out.println(bid);
            System.out.println(bid.getValues());

            throw e;
        }

        return sum;
    }

    public void printCount() {
        List<Issue> issues = _domain.getIssues();

        for(int i = 0; i < _issueCount; i++) {
            String s = issues.get(i).getName() + " [ ";

            HashMap<Value, Integer> values = _bidValueCount[i];

            for(Map.Entry<Value, Integer> e : values.entrySet()) {
                s = s + e.getKey() + ": " + e.getValue() + ", ";
            }

            s = s + " ]";

            System.out.println(s);
        }
    }

    public void printFrequency() {
        List<Issue> issues = _domain.getIssues();

        for(int i = 0; i < _issueCount; i++) {
            String s = issues.get(i).getName() + " [ ";

            HashMap<Value, Float> values = _accumulatedFrequency[i];

            for(Map.Entry<Value, Float> e : values.entrySet()) {
                s = s + e.getKey() + ": " + e.getValue() + ", ";
            }

            s = s + " ]";

            System.out.println(s);
        }
    }
}
